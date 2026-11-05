package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenAdapterTest {

    private JwtTokenAdapter jwtTokenAdapter;
    private final String secret = "test-secret-key-horizon-32bytes-min";

    @BeforeEach
    void setUp() {
        jwtTokenAdapter = new JwtTokenAdapter();
        ReflectionTestUtils.setField(jwtTokenAdapter, "secret", secret);
    }

    @Test
    @DisplayName("Deve gerar token JWT válido contendo o ID do usuário como Subject")
    void shouldGenerateValidToken() {
        User user = new User();
        user.setId(42L);

        String token = jwtTokenAdapter.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        String subject = jwtTokenAdapter.validateToken(token);
        assertEquals("42", subject, "O Subject do token validado deve corresponder ao ID do usuário");
    }

    @Test
    @DisplayName("Deve retornar string vazia ao validar token com assinatura inválida/adulterada")
    void shouldReturnEmptyWhenTokenSignatureIsInvalid() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsImlzcyI6Imhvcml6b24tYXBpIn0.invalidSignature";

        String subject = jwtTokenAdapter.validateToken(invalidToken);
        assertEquals("", subject, "Token com assinatura adulterada deve ser rejeitado");
    }

    @Test
    @DisplayName("Deve retornar string vazia ao validar token expirado")
    void shouldReturnEmptyWhenTokenIsExpired() {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        String expiredToken = JWT.create()
                .withIssuer("horizon-api")
                .withSubject("42")
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .sign(algorithm);

        String subject = jwtTokenAdapter.validateToken(expiredToken);
        assertEquals("", subject, "Token expirado deve ser rejeitado retornando string vazia");
    }

    @Test
    @DisplayName("Deve retornar string vazia ao validar token emitido por outro emissor (issuer inválido)")
    void shouldReturnEmptyWhenIssuerIsInvalid() {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        String foreignToken = JWT.create()
                .withIssuer("malicious-issuer")
                .withSubject("42")
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign(algorithm);

        String subject = jwtTokenAdapter.validateToken(foreignToken);
        assertEquals("", subject, "Token de issuer desconhecido deve ser rejeitado");
    }
}
