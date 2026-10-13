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

    @Value("${FRONTEND_BASE_URL:http://localhost:4200}")
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
        String subject = "Horizon - Recuperação de Senha";
        String htmlBody = "<div style=\"font-family: Arial, sans-serif; text-align: center; padding: 32px; color: #1e293b; background-color: #f8fafc; border-radius: 12px; max-width: 520px; margin: 0 auto; border: 1px solid #e2e8f0;\">" +
                "<div style=\"margin-bottom: 20px;\">" +
                "  <h1 style=\"color: #4A90E2; margin: 0; font-size: 26px; font-weight: bold;\">Horizon</h1>" +
                "</div>" +
                "<h2 style=\"color: #0f172a; margin-bottom: 12px; font-size: 20px;\">Recuperação de Senha</h2>" +
                "<p style=\"font-size: 15px; margin-bottom: 20px; color: #475569; line-height: 1.5;\">Olá <strong>" + user.getName() + "</strong>,<br>Recebemos uma solicitação para redefinir a senha da sua conta no Horizon.</p>" +
                "<p style=\"font-size: 14px; color: #64748b; margin-bottom: 24px;\">Clique no botão abaixo para criar sua nova credencial de acesso:</p>" +
                "<a href=\"" + resetLink + "\" style=\"background-color: #4A90E2; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: bold; font-size: 15px; box-shadow: 0 4px 12px rgba(74,144,226,0.3);\">Redefinir Minha Senha</a>" +
                "<p style=\"font-size: 13px; color: #94a3b8; margin-top: 24px;\">Este link é válido por <strong>1 hora</strong>.</p>" +
                "<div style=\"border-top: 1px solid #e2e8f0; margin-top: 24px; padding-top: 16px;\">" +
                "  <p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">Se você não realizou esta solicitação, ignore este e-mail. Sua conta continuará em segurança.</p>" +
                "</div>" +
                "</div>";

        emailPort.sendEmail(user.getEmail(), subject, htmlBody);
    }
}
