package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.UserRequest;
import br.com.horizon.horizon_api.application.dto.response.UserResponse;

public interface UpdateUserUseCase {
    UserResponse execute(Long id, UserRequest request);
}
