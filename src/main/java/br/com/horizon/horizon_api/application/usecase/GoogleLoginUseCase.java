package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.GoogleLoginRequest;
import br.com.horizon.horizon_api.application.dto.response.LoginResponse;

public interface GoogleLoginUseCase {
    LoginResponse execute(GoogleLoginRequest request);
}
