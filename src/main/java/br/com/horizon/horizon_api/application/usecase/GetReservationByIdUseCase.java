package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;

public interface GetReservationByIdUseCase {
    ReservationResponse execute(Long id, Long userId);
}
