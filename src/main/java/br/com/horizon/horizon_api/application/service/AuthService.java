package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.request.LoginRequest;
import br.com.horizon.horizon_api.application.dto.response.LoginResponse;
import br.com.horizon.horizon_api.application.usecase.LoginUseCase;
import br.com.horizon.horizon_api.domain.exception.BusinessException;
import br.com.horizon.horizon_api.domain.exception.InvalidOperationException;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.PasswordEncodePort;
import br.com.horizon.horizon_api.domain.port.TokenPort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AuthService implements LoginUseCase {

    private final UserPersistencePort userPort;
    private final PasswordEncodePort passwordEncodePort;
    private final TokenPort tokenPort;
    private final br.com.horizon.horizon_api.domain.port.LoginAttemptPort loginAttemptPort;

    @Override
    public LoginResponse execute(LoginRequest request) {
        String attemptKey = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (loginAttemptPort.isBlocked(attemptKey)) {
            throw new br.com.horizon.horizon_api.domain.exception.UnauthorizedException(
                    "Muitas tentativas de login incorretas. Conta temporariamente bloqueada por 15 minutos.");
        }

        User user = userPort.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginAttemptPort.loginFailed(attemptKey);
                      log.warn("Failed login attempt for email: {}", attemptKey);
                    return new br.com.horizon.horizon_api.domain.exception.UnauthorizedException("E-mail ou senha incorretos.");
                });

        if (!passwordEncodePort.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptPort.loginFailed(attemptKey);
                      log.warn("Failed login attempt for email: {}", attemptKey);
            throw new br.com.horizon.horizon_api.domain.exception.UnauthorizedException("E-mail ou senha incorretos.");
        }

        if (user.getActive() == null || !user.getActive()) {
            throw new InvalidOperationException("Usuario desativado.");
        }

        loginAttemptPort.loginSucceeded(attemptKey);
        String token = tokenPort.generateToken(user);
        log.info("User ID: {} logged in successfully", user.getId());
        return new LoginResponse(token);
    }
}
