package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.ResetPasswordRequest;
import br.com.horizon.horizon_api.application.service.EmailTemplateService;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PasswordResetTokenEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PasswordResetTokenRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailPort emailPort;

    @Transactional
    public void execute(ResetPasswordRequest request) {
        Optional<PasswordResetTokenEntity> tokenOpt = tokenRepository.findByTokenHash(request.getToken());
        if (tokenOpt.isEmpty()) {
            throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Token inválido ou expirado.");
        }

        PasswordResetTokenEntity tokenEntity = tokenOpt.get();

        if (tokenEntity.getUsed() || tokenEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Token inválido ou expirado.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty() || request.getNewPassword().length() < 6 || request.getNewPassword().length() > 100) {
            throw new br.com.horizon.horizon_api.domain.exception.BusinessException("A senha deve ter entre 6 e 100 caracteres.");
        }

        UserEntity user = tokenEntity.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenEntity.setUsed(true);
        tokenRepository.save(tokenEntity);

        // Disparo assíncrono do e-mail de confirmação de senha alterada
        String userEmail = user.getEmail();
        String htmlBody = EmailTemplateService.buildPasswordChangedConfirmationEmail(user.getSocialName(), user.getName());
        CompletableFuture.runAsync(() -> {
            try {
                emailPort.sendEmail(userEmail, "Horizon - Senha alterada com sucesso", htmlBody);
            } catch (Exception ex) {
                // Silently ignore to avoid failing client response
            }
        });
    }
}
