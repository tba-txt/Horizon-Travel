package br.com.horizon.horizon_api.application.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketRequest {
    private Long reservationId;
    private Long passengerId;
}
