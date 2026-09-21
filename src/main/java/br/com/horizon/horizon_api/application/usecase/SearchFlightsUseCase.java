package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.FlightResponse;
import java.time.LocalDate;
import java.util.List;

public interface SearchFlightsUseCase {
    List<FlightResponse> execute(Long destinationId, LocalDate start, LocalDate end);
}
