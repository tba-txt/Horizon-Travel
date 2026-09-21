package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.ReservationRequest;
import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;

public interface CreateReservationUseCase {
    ReservationResponse execute(Long userId, ReservationRequest request);
}
