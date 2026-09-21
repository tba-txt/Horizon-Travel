package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.response.DestinationWeatherResponse;
import br.com.horizon.horizon_api.application.usecase.GetDestinationWeatherUseCase;
import br.com.horizon.horizon_api.domain.exception.BusinessException;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import br.com.horizon.horizon_api.domain.exception.WeatherIntegrationException;
import br.com.horizon.horizon_api.domain.model.Destination;
import br.com.horizon.horizon_api.domain.model.WeatherInfo;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.domain.port.WeatherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService implements GetDestinationWeatherUseCase {

    private final DestinationPersistencePort destinationPort;
    private final WeatherPort weatherPort;

    @Override
    public DestinationWeatherResponse execute(Long destinationId, LocalDate travelDate) {
        Destination destination = destinationPort.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado com ID: " + destinationId));

        if (destination.getLatitude() == null || destination.getLongitude() == null) {
            throw new BusinessException("Destino " + destination.getName() + " não possui coordenadas geográficas cadastradas.");
        }

        LocalDate effectiveDate = (travelDate != null) ? travelDate : LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), effectiveDate);

        if (daysUntil < 0) {
            throw new BusinessException("A data da viagem não pode ser anterior à data de hoje.");
        }

        log.info("Processando clima para destino='{}' (ID={}), data={}, diasAteViagem={}",
                destination.getName(), destinationId, effectiveDate, daysUntil);

        WeatherInfo weatherInfo;
        // Regra temporal do Horizon: 0 a 16 dias -> FORECAST; 17 dias ou mais -> HISTORICAL_CONTEXT
        if (daysUntil <= 16) {
            weatherInfo = weatherPort.getDailyForecast(destination.getLatitude(), destination.getLongitude(), effectiveDate)
                    .orElseGet(() -> weatherPort.getHistoricalContext(destination.getLatitude(), destination.getLongitude(), effectiveDate)
                            .orElseThrow(() -> new WeatherIntegrationException("Dados climáticos indisponíveis para a localidade.")));
        } else {
            weatherInfo = weatherPort.getHistoricalContext(destination.getLatitude(), destination.getLongitude(), effectiveDate)
                    .orElseThrow(() -> new WeatherIntegrationException("Dados climáticos históricos indisponíveis para a localidade."));
        }

        return toResponse(destination, weatherInfo, effectiveDate);
    }

    private DestinationWeatherResponse toResponse(Destination destination, WeatherInfo weatherInfo, LocalDate effectiveDate) {
        DestinationWeatherResponse.DestinationWeatherResponseBuilder builder = DestinationWeatherResponse.builder()
                .destinationId(destination.getId())
                .destinationName(destination.getName())
                .city(destination.getCity())
                .country(destination.getCountry())
                .latitude(destination.getLatitude())
                .longitude(destination.getLongitude())
                .targetDate(effectiveDate)
                .mode(weatherInfo.getMode().name())
                .modeDescription(weatherInfo.getModeDescription())
                .timezone(weatherInfo.getTimezone())
                .summary(weatherInfo.getSummaryMessage());

        if (weatherInfo.getMode() == WeatherInfo.WeatherMode.FORECAST && weatherInfo.getForecast() != null) {
            WeatherInfo.ForecastDetails f = weatherInfo.getForecast();
            builder.forecast(DestinationWeatherResponse.ForecastDto.builder()
                    .description(f.getDescription())
                    .minTemperature(f.getMinTemperature())
                    .maxTemperature(f.getMaxTemperature())
                    .apparentTemperature(f.getApparentTemperatureMax())
                    .precipitationMm(f.getPrecipitationSum())
                    .precipitationProbabilityPercent(f.getPrecipitationProbability())
                    .weatherCode(f.getWeatherCode())
                    .weatherDescription(f.getWeatherDescription())
                    .windSpeedKmH(f.getMaxWindSpeed())
                    .build());
        } else if (weatherInfo.getMode() == WeatherInfo.WeatherMode.HISTORICAL_CONTEXT && weatherInfo.getHistoricalContext() != null) {
            WeatherInfo.HistoricalContextDetails h = weatherInfo.getHistoricalContext();
            builder.historicalContext(DestinationWeatherResponse.HistoricalContextDto.builder()
                    .referencePeriod(h.getReferencePeriod())
                    .description(h.getDescription())
                    .meanTemperature(h.getMeanTemperature())
                    .minTemperature(h.getMinTemperature())
                    .maxTemperature(h.getMaxTemperature())
                    .totalPrecipitationMm(h.getTotalPrecipitation())
                    .rainyDaysCount(h.getRainyDaysCount())
                    .snowyDaysCount(h.getSnowfallDaysCount())
                    .avgWindSpeedKmH(h.getAvgWindSpeed())
                    .build());
        }

        return builder.build();
    }
}
