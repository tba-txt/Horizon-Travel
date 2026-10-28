package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.ForgotPasswordRequest;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PasswordResetTokenEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PasswordResetTokenRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import br.com.horizon.horizon_api.application.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailPort emailPort;

    @Value("${FRONTEND_BASE_URL:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public void execute(ForgotPasswordRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(request.getEmail());
        if (userOpt.isEmpty()) {
            return;
        }
        UserEntity user = userOpt.get();

        String rawToken = UUID.randomUUID().toString();
        
        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
        tokenEntity.setUser(user);
        tokenEntity.setTokenHash(rawToken);
        tokenEntity.setExpiresAt(OffsetDateTime.now().plusHours(1));
        tokenEntity.setUsed(false);
        tokenEntity.setCreatedAt(OffsetDateTime.now());

        tokenRepository.save(tokenEntity);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
        String subject = "Horizon - Recuperação de Senha";
        String htmlBody = EmailTemplateService.buildPasswordResetEmail(user.getSocialName(), user.getName(), resetLink);

        String recipientEmail = user.getEmail();
        CompletableFuture.runAsync(() -> {
            try {
                emailPort.sendEmail(recipientEmail, subject, htmlBody);
            } catch (Exception ex) {
                // Log and do not fail caller
            }
        });
    }
}
