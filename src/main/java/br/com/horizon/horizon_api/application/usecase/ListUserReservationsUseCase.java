package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.MyTripsResponse;
import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;
import java.util.List;

public interface ListUserReservationsUseCase {
    List<ReservationResponse> execute(Long userId);
    MyTripsResponse executeMyTrips(Long userId);
}
