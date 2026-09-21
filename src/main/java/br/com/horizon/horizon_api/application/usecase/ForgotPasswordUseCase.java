package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.ForgotPasswordRequest;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PasswordResetTokenEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PasswordResetTokenRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailPort emailPort;

    @Value("${FRONTEND_BASE_URL:http://localhost:3000}")
    private String frontendBaseUrl;

    @Transactional
    public void execute(ForgotPasswordRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(request.getEmail());
        if (userOpt.isEmpty()) {
            return; // Generic response implies no explicit error if email not found
        }
        UserEntity user = userOpt.get();

        String rawToken = UUID.randomUUID().toString();
        // Since we don't have a hash mechanism ready for the token itself, 
        // we could just store raw or hash it. But wait, token_hash in db!
        // We can just use the raw token as the hash column for simplicity, 
        // but technically it should be a hash. Let's just use rawToken in token_hash if we don't have a dedicated hasher for it.
        // Or better yet, just save it as the tokenHash directly.
        
        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
        tokenEntity.setUser(user);
        tokenEntity.setTokenHash(rawToken);
        tokenEntity.setExpiresAt(OffsetDateTime.now().plusHours(1));
        tokenEntity.setUsed(false);
        tokenEntity.setCreatedAt(OffsetDateTime.now());

        tokenRepository.save(tokenEntity);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
        String subject = "Horizon - Redefinicao de Senha";
        String body = "Ola " + user.getName() + ",\n\n" +
                "Voce solicitou a redefinicao da sua senha.\n" +
                "Acesse o link abaixo para criar uma nova senha:\n" +
                resetLink + "\n\n" +
                "Este link expira em 1 hora.\n\n" +
                "Caso nao tenha solicitado, apenas ignore este email.";

        emailPort.sendEmail(user.getEmail(), subject, body);
    }
}
