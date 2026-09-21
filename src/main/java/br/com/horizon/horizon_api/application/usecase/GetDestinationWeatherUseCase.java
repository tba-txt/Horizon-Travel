package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.DestinationWeatherResponse;

import java.time.LocalDate;

public interface GetDestinationWeatherUseCase {
    DestinationWeatherResponse execute(Long destinationId, LocalDate travelDate);
}
