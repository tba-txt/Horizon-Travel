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

        WeatherInfo weatherInfo = null;
        // Regra temporal do Horizon: 0 a 16 dias -> FORECAST; 17 dias ou mais -> HISTORICAL_CONTEXT
        if (daysUntil <= 16) {
            try {
                weatherInfo = weatherPort.getDailyForecast(destination.getLatitude(), destination.getLongitude(), effectiveDate).orElse(null);
            } catch (Exception e) {
                log.warn("Falha ao obter previsão direta para {}: {}. Tentando histórico...", destination.getName(), e.getMessage());
            }
        }

        if (weatherInfo == null) {
            try {
                weatherInfo = weatherPort.getHistoricalContext(destination.getLatitude(), destination.getLongitude(), effectiveDate).orElse(null);
            } catch (Exception e) {
                log.warn("Falha ao obter contexto histórico para {}: {}", destination.getName(), e.getMessage());
            }
        }

        if (weatherInfo == null) {
            log.info("Usando fallback de estimativa climática para o destino {}", destination.getName());
            weatherInfo = buildFallbackWeather(destination, effectiveDate);
        }

        return toResponse(destination, weatherInfo, effectiveDate);
    }

    private WeatherInfo buildFallbackWeather(Destination destination, LocalDate effectiveDate) {
        double lat = destination.getLatitude() != null ? destination.getLatitude().doubleValue() : 0.0;
        boolean isPolar = Math.abs(lat) >= 60.0 || (destination.getName() != null && destination.getName().toLowerCase().contains("antártida"));
        boolean isTropical = Math.abs(lat) <= 23.5;

        double minTemp = isPolar ? -18.0 : (isTropical ? 22.0 : 15.0);
        double maxTemp = isPolar ? -8.0 : (isTropical ? 31.0 : 24.0);
        double appTemp = isPolar ? -22.0 : (isTropical ? 33.0 : 25.0);
        String desc = isPolar ? "Frio intenso e neve" : (isTropical ? "Ensolarado com clima tropical" : "Tempo ameno");

        WeatherInfo.ForecastDetails forecast = WeatherInfo.ForecastDetails.builder()
                .description(desc)
                .minTemperature(minTemp)
                .maxTemperature(maxTemp)
                .apparentTemperatureMax(appTemp)
                .precipitationSum(isTropical ? 2.0 : 0.0)
                .precipitationProbability(isTropical ? 20 : 10)
                .weatherCode(1)
                .weatherDescription(desc)
                .maxWindSpeed(isPolar ? 35.0 : 12.0)
                .build();

        return WeatherInfo.builder()
                .mode(WeatherInfo.WeatherMode.FORECAST)
                .modeDescription("Estimativa meteorológica regional")
                .targetDate(effectiveDate)
                .timezone("auto")
                .latitude(destination.getLatitude())
                .longitude(destination.getLongitude())
                .summaryMessage("Estimativa meteorológica baseada na localização geográfica do destino.")
                .forecast(forecast)
                .build();
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
