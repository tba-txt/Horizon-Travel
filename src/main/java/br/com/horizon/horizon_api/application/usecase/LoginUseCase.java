package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.LoginRequest;
import br.com.horizon.horizon_api.application.dto.response.LoginResponse;

public interface LoginUseCase {
    LoginResponse execute(LoginRequest request);
}
