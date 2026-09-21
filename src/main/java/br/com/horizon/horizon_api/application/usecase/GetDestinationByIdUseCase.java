package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.DestinationResponse;

public interface GetDestinationByIdUseCase {
    DestinationResponse execute(Long id);
}
