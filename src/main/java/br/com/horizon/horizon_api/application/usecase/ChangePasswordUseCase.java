package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.ChangePasswordRequest;

public interface ChangePasswordUseCase {
    void execute(Long userId, ChangePasswordRequest request);
}
