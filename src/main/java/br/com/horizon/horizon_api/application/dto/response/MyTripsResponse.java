package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MyTripsResponse {
    // Aguardando pagamento
    private List<ReservationResponse> pending = new ArrayList<>();
    // Confirmadas
    private List<ReservationResponse> confirmed = new ArrayList<>();
    // Finalizadas
    private List<ReservationResponse> finished = new ArrayList<>();
    // Canceladas
    private List<ReservationResponse> cancelled = new ArrayList<>();
}
