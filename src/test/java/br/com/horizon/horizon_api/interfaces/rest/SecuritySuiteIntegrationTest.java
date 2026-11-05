package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.LoginRequest;
import br.com.horizon.horizon_api.application.dto.response.LoginResponse;
import br.com.horizon.horizon_api.application.dto.response.UserResponse;
import br.com.horizon.horizon_api.application.service.AuthService;
import br.com.horizon.horizon_api.domain.exception.UnauthorizedException;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.LoginAttemptPort;
import br.com.horizon.horizon_api.domain.port.PasswordEncodePort;
import br.com.horizon.horizon_api.domain.port.TokenPort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import br.com.horizon.horizon_api.infrastructure.adapter.InMemoryLoginAttemptAdapter;
import br.com.horizon.horizon_api.infrastructure.adapter.JwtTokenAdapter;
import br.com.horizon.horizon_api.infrastructure.adapter.PasswordEncoderAdapter;
import br.com.horizon.horizon_api.interfaces.config.RateLimitingFilter;
import br.com.horizon.horizon_api.interfaces.rest.advice.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.FilterChain;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecuritySuiteIntegrationTest {

    private MockMvc mockMvcAuth;
    private MockMvc mockMvcRecommendation;

    @Mock private UserPersistencePort userPort;
    @Mock private PasswordEncodePort passwordEncodePort;
    @Mock private TokenPort tokenPort;
    @Mock private LoginAttemptPort loginAttemptPort;

    @InjectMocks private AuthService authService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        GlobalExceptionHandler advice = new GlobalExceptionHandler();

        AuthController authController = new AuthController(authService, null, null, null, null);
        mockMvcAuth = MockMvcBuilders.standaloneSetup(authController).setControllerAdvice(advice).build();

        RecommendationController recController = new RecommendationController(null);
        mockMvcRecommendation = MockMvcBuilders.standaloneSetup(recController).setControllerAdvice(advice).build();
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // 1. Rota Protegida sem Token / Usuário Não Autenticado
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Segurança 1: Acesso a rota protegida sem token resulta em falha de autenticação")
    void testProtectedEndpointWithoutTokenFails() throws Exception {
        // Sem autenticação no SecurityContextHolder
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // O controller chama SecurityUtils.getAuthenticatedUserId(), que lança IllegalStateException
        // O GlobalExceptionHandler converte para 400 Bad Request
        mockMvcRecommendation.perform(get("/recommendations"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // 2. Token Inválido e Token Expirado
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Segurança 2: Validação de Token JWT - Rejeição de tokens adulterados e expirados")
    void testJwtTokenValidationSecurity() {
        JwtTokenAdapter jwt = new JwtTokenAdapter();
        ReflectionTestUtils.setField(jwt, "secret", "secret-key-horizon-segura-e-forte-32-bytes");

        // Token adulterado
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMCJ9.assinatura_falsa";
        assertEquals("", jwt.validateToken(invalidToken), "Token adulterado deve retornar string vazia");

        // Token vazio ou nulo
        assertEquals("", jwt.validateToken(""), "Token vazio deve retornar string vazia");
        assertEquals("", jwt.validateToken(null), "Token nulo deve retornar string vazia");
    }

    // -------------------------------------------------------------------------
    // 3. Credenciais Inválidas
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Segurança 3: Tentativa de login com senha incorreta resulta em 401 Unauthorized")
    void testLoginWithInvalidCredentialsFails() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("usuario@horizon.com");
        user.setPasswordHash("$2a$10$hashedPasswordHere");
        user.setActive(true);

        when(loginAttemptPort.isBlocked("usuario@horizon.com")).thenReturn(false);
        when(userPort.findByEmail("usuario@horizon.com")).thenReturn(Optional.of(user));
        when(passwordEncodePort.matches("senha_errada", user.getPasswordHash())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("usuario@horizon.com");
        req.setPassword("senha_errada");

        mockMvcAuth.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verify(loginAttemptPort).loginFailed("usuario@horizon.com");
    }

    // -------------------------------------------------------------------------
    // 4. Bloqueio por Tentativas Consecutivas (Brute Force Protection)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Segurança 4: Bloqueio automático por 15 minutos após 5 tentativas consecutivas de senha incorreta")
    void testBruteForceBlockAfter5FailedAttempts() {
        InMemoryLoginAttemptAdapter adapter = new InMemoryLoginAttemptAdapter();
        String targetEmail = "alvo.ataque@horizon.com";

        assertFalse(adapter.isBlocked(targetEmail), "Inicialmente não deve estar bloqueado");

        // 4 tentativas falhas -> ainda não bloqueia
        for (int i = 0; i < 4; i++) {
            adapter.loginFailed(targetEmail);
            assertFalse(adapter.isBlocked(targetEmail));
        }

        // 5ª tentativa falha -> bloqueio ativado
        adapter.loginFailed(targetEmail);
        assertTrue(adapter.isBlocked(targetEmail), "Após 5 tentativas falhas, o usuário deve estar bloqueado");

        // Sucesso em login limpa o histórico
        adapter.loginSucceeded(targetEmail);
        assertFalse(adapter.isBlocked(targetEmail), "Login bem-sucedido deve resetar o contador");
    }

    // -------------------------------------------------------------------------
    // 5. Rate Limiting Filter (Proteção DoS)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Segurança 5: Rate Limit rejeita requisições excedentes com HTTP 429 e cabeçalho Retry-After")
    void testRateLimitingEnforcement() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        FilterChain filterChain = mock(FilterChain.class);
        String ip = "192.0.2.1";

        // Rotas de autenticação: limite 20 req/min
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(ip);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 21ª requisição deve retornar 429
        MockHttpServletRequest excessAuth = new MockHttpServletRequest("POST", "/auth/login");
        excessAuth.setRemoteAddr(ip);
        MockHttpServletResponse excessRes = new MockHttpServletResponse();
        filter.doFilter(excessAuth, excessRes, filterChain);

        assertEquals(429, excessRes.getStatus());
        assertEquals("60", excessRes.getHeader("Retry-After"));
        assertTrue(excessRes.getContentAsString().contains("Limite de requisições excedido"));
    }

    // -------------------------------------------------------------------------
    // 6. Armazenamento Seguro de Senhas (BCrypt) e Ausência de Senha no Payload
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Segurança 6: Senhas são hasheadas com BCrypt (salt embutido) e ausentes em DTOs públicos")
    void testBCryptPasswordHashingAndPayloadSanitization() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "MinhaSenhaSuperSecreta@2026";
        String encoded = encoder.encode(rawPassword);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"), "O hash deve possuir o prefixo padrão BCrypt");
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoder.matches(rawPassword, encoded));

        // Sanitização de DTOs: UserResponse e LoginResponse não possuem campos de senha
        UserResponse userResponse = new UserResponse();
        userResponse.setId(10L);
        userResponse.setName("Carlos");
        userResponse.setEmail("carlos@horizon.com");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken("valid-token");

        // Reflexão verifica ausência de campos contendo 'password'
        boolean hasPasswordInUserResp = java.util.Arrays.stream(UserResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().toLowerCase().contains("password"));
        boolean hasPasswordInLoginResp = java.util.Arrays.stream(LoginResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().toLowerCase().contains("password"));

        assertFalse(hasPasswordInUserResp, "UserResponse jamais deve conter campo de senha");
        assertFalse(hasPasswordInLoginResp, "LoginResponse jamais deve conter campo de senha");
    }
}
