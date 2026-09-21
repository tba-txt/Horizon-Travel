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
public class AuthService implements LoginUseCase {

    private final UserPersistencePort userPort;
    private final PasswordEncodePort passwordEncodePort;
    private final TokenPort tokenPort;

    @Override
    public LoginResponse execute(LoginRequest request) {
        User user = userPort.findByEmail(request.getEmail())
                .orElseThrow(() -> new br.com.horizon.horizon_api.domain.exception.UnauthorizedException("E-mail ou senha incorretos."));

        if (!passwordEncodePort.matches(request.getPassword(), user.getPasswordHash())) {
            throw new br.com.horizon.horizon_api.domain.exception.UnauthorizedException("E-mail ou senha incorretos.");
        }

        if (user.getActive() == null || !user.getActive()) {
            throw new InvalidOperationException("Usuario desativado.");
        }

        String token = tokenPort.generateToken(user);
        return new LoginResponse(token);
    }
}
