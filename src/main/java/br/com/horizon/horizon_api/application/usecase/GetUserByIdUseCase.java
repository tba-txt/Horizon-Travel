package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.UserResponse;

public interface GetUserByIdUseCase {
    UserResponse execute(Long id);
}
