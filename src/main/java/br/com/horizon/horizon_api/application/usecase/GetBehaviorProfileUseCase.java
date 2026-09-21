package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.ProfileResponseDTO;

public interface GetBehaviorProfileUseCase {
    ProfileResponseDTO executeBehavior(Long userId);
}
