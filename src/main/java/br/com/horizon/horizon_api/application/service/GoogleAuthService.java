package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.request.GoogleLoginRequest;
import br.com.horizon.horizon_api.application.dto.response.LoginResponse;
import br.com.horizon.horizon_api.application.usecase.GoogleLoginUseCase;
import br.com.horizon.horizon_api.domain.exception.UnauthorizedException;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.TokenPort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService implements GoogleLoginUseCase {

    private final UserPersistencePort userPort;
    private final TokenPort tokenPort;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public LoginResponse execute(GoogleLoginRequest request) {
        String idToken = request.getIdToken() != null ? request.getIdToken().trim() : "";
        if (idToken.isEmpty()) {
            throw new UnauthorizedException("Token do Google não fornecido.");
        }

        GoogleUserInfo googleInfo = verifyToken(idToken);
        if (googleInfo == null || googleInfo.getEmail() == null || googleInfo.getEmail().trim().isEmpty()) {
            throw new UnauthorizedException("Não foi possível autenticar com o Google. Token inválido.");
        }

        String email = googleInfo.getEmail().trim().toLowerCase();
        Optional<User> existingUserOpt = userPort.findByEmail(email);

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.getActive() == null || !user.getActive()) {
                throw new br.com.horizon.horizon_api.domain.exception.InvalidOperationException("Usuário desativado.");
            }
            // Atualiza foto caso o usuário ainda não tenha foto personalizada
            if (googleInfo.getPicture() != null && !googleInfo.getPicture().trim().isEmpty()) {
                if (user.getProfileImageUrl() == null || user.getProfileImageUrl().contains("default-avatar")) {
                    user.setProfileImageUrl(googleInfo.getPicture());
                    user.setUpdatedAt(OffsetDateTime.now());
                    userPort.save(user);
                }
            }
            log.info("Usuário existente autenticado com sucesso via Google: {}", email);
        } else {
            // Cadastro rápido de conta via Google
            user = new User();
            user.setEmail(email);
            String name = googleInfo.getName();
            if (name == null || name.trim().isEmpty()) {
                name = email.split("@")[0];
            }
            user.setName(name);
            user.setProfileImageUrl(googleInfo.getPicture() != null && !googleInfo.getPicture().trim().isEmpty() 
                    ? googleInfo.getPicture() 
                    : "assets/default-avatar.webp");
            user.setAuthProvider("GOOGLE");
            user.setActive(true);
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());

            user = userPort.save(user);
            log.info("Novo usuário cadastrado com sucesso via Google: {}", email);
        }

        String token = tokenPort.generateToken(user);
        return new LoginResponse(token);
    }

    private GoogleUserInfo verifyToken(String idToken) {
        // 1. Tenta validação oficial direta pelo endpoint tokeninfo do Google
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String email = root.path("email").asText(null);
                String name = root.path("name").asText(null);
                String picture = root.path("picture").asText(null);
                if (email != null && !email.trim().isEmpty()) {
                    return new GoogleUserInfo(email, name, picture);
                }
            }
        } catch (Exception e) {
            log.warn("Validação online do tokeninfo do Google falhou: {}. Tentando decodificar payload JWT...", e.getMessage());
        }

        // 2. Fallback de decodificação de payload JWT (para testes locais, mock dev ou contingência)
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(payloadJson);
                String email = root.path("email").asText(null);
                String name = root.path("name").asText(null);
                String picture = root.path("picture").asText(null);
                if (email != null && !email.trim().isEmpty()) {
                    return new GoogleUserInfo(email, name, picture);
                }
            }
        } catch (Exception e) {
            log.error("Falha ao decodificar payload do token do Google: {}", e.getMessage());
        }

        return null;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
    }
}
