package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.TicketResponse;
import java.util.List;

public interface GetReservationTicketsUseCase {
    List<TicketResponse> execute(Long reservationId, Long userId);
}
