package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.exception.WeatherIntegrationException;
import br.com.horizon.horizon_api.domain.model.WeatherInfo;
import br.com.horizon.horizon_api.domain.port.WeatherPort;
import br.com.horizon.horizon_api.infrastructure.adapter.dto.OpenMeteoArchiveResponse;
import br.com.horizon.horizon_api.infrastructure.adapter.dto.OpenMeteoForecastResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
public class OpenMeteoAdapter implements WeatherPort {

    private final RestClient forecastClient;
    private final RestClient archiveClient;

    public OpenMeteoAdapter(
            @Qualifier("openMeteoForecastClient") RestClient forecastClient,
            @Qualifier("openMeteoArchiveClient") RestClient archiveClient) {
        this.forecastClient = forecastClient;
        this.archiveClient = archiveClient;
    }

    @Override
    public Optional<WeatherInfo> getDailyForecast(BigDecimal latitude, BigDecimal longitude, LocalDate targetDate) {
        try {
            log.info("Consultando previsão Open-Meteo para lat={}, lon={}, data={}", latitude, longitude, targetDate);

            OpenMeteoForecastResponse response = forecastClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max")
                            .queryParam("forecast_days", 16)
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(OpenMeteoForecastResponse.class);

            if (response == null || response.getDaily() == null || response.getDaily().getTime() == null) {
                throw new WeatherIntegrationException("Resposta inválida ou vazia do serviço de previsão meteorológica.");
            }

            List<String> timeList = response.getDaily().getTime();
            String targetDateStr = targetDate.toString();
            int index = timeList.indexOf(targetDateStr);

            if (index == -1) {
                log.info("Data {} não encontrada nos {} dias retornados pela previsão Open-Meteo", targetDate, timeList.size());
                return Optional.empty();
            }

            OpenMeteoForecastResponse.DailyForecast daily = response.getDaily();
            Double minTemp = getDoubleValue(daily.getTemperature2mMin(), index);
            Double maxTemp = getDoubleValue(daily.getTemperature2mMax(), index);
            Double apparentTemp = getDoubleValue(daily.getApparentTemperatureMax(), index);
            Double precipSum = getDoubleValue(daily.getPrecipitationSum(), index);
            Integer precipProb = getIntegerValue(daily.getPrecipitationProbabilityMax(), index);
            Integer weatherCode = getIntegerValue(daily.getWeatherCode(), index);
            Double windSpeed = getDoubleValue(daily.getWindSpeed10mMax(), index);
            String weatherDesc = translateWeatherCode(weatherCode);

            String summary = String.format(
                    "Previsão meteorológica para a data da viagem (%s): máxima de %.1f°C, mínima de %.1f°C, %s com %d%% de probabilidade de chuva.",
                    targetDate,
                    maxTemp != null ? maxTemp : 0.0,
                    minTemp != null ? minTemp : 0.0,
                    weatherDesc,
                    precipProb != null ? precipProb : 0
            );

            WeatherInfo.ForecastDetails details = WeatherInfo.ForecastDetails.builder()
                    .description("Previsão meteorológica para a data da viagem.")
                    .minTemperature(minTemp)
                    .maxTemperature(maxTemp)
                    .apparentTemperatureMax(apparentTemp)
                    .precipitationSum(precipSum)
                    .precipitationProbability(precipProb)
                    .weatherCode(weatherCode)
                    .weatherDescription(weatherDesc)
                    .maxWindSpeed(windSpeed)
                    .build();

            WeatherInfo weatherInfo = WeatherInfo.builder()
                    .mode(WeatherInfo.WeatherMode.FORECAST)
                    .modeDescription("Previsão meteorológica para a data da viagem")
                    .targetDate(targetDate)
                    .timezone(response.getTimezone())
                    .latitude(latitude)
                    .longitude(longitude)
                    .summaryMessage(summary)
                    .forecast(details)
                    .build();

            return Optional.of(weatherInfo);

        } catch (WeatherIntegrationException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Falha de comunicação com Open-Meteo Forecast API: {}", e.getMessage());
            throw new WeatherIntegrationException("Serviço de previsão meteorológica temporariamente indisponível.", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar previsão do Open-Meteo: {}", e.getMessage());
            throw new WeatherIntegrationException("Falha ao obter dados meteorológicos.", e);
        }
    }

    @Override
    public Optional<WeatherInfo> getHistoricalContext(BigDecimal latitude, BigDecimal longitude, LocalDate targetDate) {
        try {
            int currentYear = LocalDate.now().getYear();
            int endYear = currentYear - 1;
            int startYear = endYear - 2; // Janela representativa de 3 anos fechados
            int numYears = endYear - startYear + 1;

            String startDate = startYear + "-01-01";
            String endDate = endYear + "-12-31";

            log.info("Consultando histórico Open-Meteo para lat={}, lon={}, janela={}-{}, mês={}",
                    latitude, longitude, startYear, endYear, targetDate.getMonthValue());

            OpenMeteoArchiveResponse response = archiveClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/archive")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("start_date", startDate)
                            .queryParam("end_date", endDate)
                            .queryParam("daily", "temperature_2m_max,temperature_2m_min,temperature_2m_mean,precipitation_sum,snowfall_sum,wind_speed_10m_max")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(OpenMeteoArchiveResponse.class);

            if (response == null || response.getDaily() == null || response.getDaily().getTime() == null) {
                throw new WeatherIntegrationException("Resposta inválida ou vazia do serviço meteorológico histórico.");
            }

            OpenMeteoArchiveResponse.DailyArchive daily = response.getDaily();
            List<String> times = daily.getTime();

            int targetMonth = targetDate.getMonthValue();
            double sumMeanTemp = 0.0;
            double sumMinTemp = 0.0;
            double sumMaxTemp = 0.0;
            double sumPrecip = 0.0;
            double sumWind = 0.0;
            int rainyDaysCount = 0;
            int snowDaysCount = 0;
            int daysCount = 0;

            for (int i = 0; i < times.size(); i++) {
                LocalDate date = LocalDate.parse(times.get(i));
                if (date.getMonthValue() == targetMonth) {
                    daysCount++;
                    Double meanT = getDoubleValue(daily.getTemperature2mMean(), i);
                    Double minT = getDoubleValue(daily.getTemperature2mMin(), i);
                    Double maxT = getDoubleValue(daily.getTemperature2mMax(), i);
                    Double precip = getDoubleValue(daily.getPrecipitationSum(), i);
                    Double snow = getDoubleValue(daily.getSnowfallSum(), i);
                    Double wind = getDoubleValue(daily.getWindSpeed10mMax(), i);

                    if (meanT != null) sumMeanTemp += meanT;
                    if (minT != null) sumMinTemp += minT;
                    if (maxT != null) sumMaxTemp += maxT;
                    if (precip != null) {
                        sumPrecip += precip;
                        if (precip >= 1.0) rainyDaysCount++;
                    }
                    if (snow != null && snow > 0.0) {
                        snowDaysCount++;
                    }
                    if (wind != null) sumWind += wind;
                }
            }

            if (daysCount == 0) {
                throw new WeatherIntegrationException("Não foram encontrados dados históricos para o mês selecionado.");
            }

            double avgMeanTemp = round(sumMeanTemp / daysCount, 1);
            double avgMinTemp = round(sumMinTemp / daysCount, 1);
            double avgMaxTemp = round(sumMaxTemp / daysCount, 1);
            double avgMonthlyPrecip = round(sumPrecip / numYears, 1);
            int estimatedRainyDays = (int) Math.round((double) rainyDaysCount / numYears);
            int estimatedSnowDays = (int) Math.round((double) snowDaysCount / numYears);
            double avgWindSpeed = round(sumWind / daysCount, 1);

            String monthName = Month.of(targetMonth).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
            String referencePeriod = String.format("%s (médias históricas %d-%d)", capitalize(monthName), startYear, endYear);

            String summary = String.format(
                    "Contexto climático histórico do período: em %s, a região apresenta tipicamente temperatura média de %.1f°C (faixa esperada de %.1f°C a %.1f°C), cerca de %d dias de chuva e precipitação acumulada de %.1f mm. Médias históricas para planejamento da viagem, sem constituir previsão exata.",
                    monthName, avgMeanTemp, avgMinTemp, avgMaxTemp, estimatedRainyDays, avgMonthlyPrecip
            );

            WeatherInfo.HistoricalContextDetails details = WeatherInfo.HistoricalContextDetails.builder()
                    .referencePeriod(referencePeriod)
                    .description("Contexto climático histórico do período, sem tratar como previsão exata.")
                    .meanTemperature(avgMeanTemp)
                    .minTemperature(avgMinTemp)
                    .maxTemperature(avgMaxTemp)
                    .totalPrecipitation(avgMonthlyPrecip)
                    .rainyDaysCount(estimatedRainyDays)
                    .snowfallDaysCount(estimatedSnowDays)
                    .avgWindSpeed(avgWindSpeed)
                    .build();

            WeatherInfo weatherInfo = WeatherInfo.builder()
                    .mode(WeatherInfo.WeatherMode.HISTORICAL_CONTEXT)
                    .modeDescription("Contexto climático histórico do período, sem tratar como previsão exata")
                    .targetDate(targetDate)
                    .timezone(response.getTimezone())
                    .latitude(latitude)
                    .longitude(longitude)
                    .summaryMessage(summary)
                    .historicalContext(details)
                    .build();

            return Optional.of(weatherInfo);

        } catch (WeatherIntegrationException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Falha de comunicação com Open-Meteo Archive API: {}", e.getMessage());
            throw new WeatherIntegrationException("Serviço de dados climáticos históricos temporariamente indisponível.", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar histórico do Open-Meteo: {}", e.getMessage());
            throw new WeatherIntegrationException("Falha ao obter histórico climático da região.", e);
        }
    }

    private Double getDoubleValue(List<Double> list, int index) {
        if (list != null && index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    private Integer getIntegerValue(List<Integer> list, int index) {
        if (list != null && index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String translateWeatherCode(Integer code) {
        if (code == null) return "Condições não informadas";
        return switch (code) {
            case 0 -> "Céu limpo";
            case 1 -> "Predominantemente limpo";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Nevoeiro";
            case 51, 53, 55 -> "Garoa";
            case 56, 57 -> "Garoa congelante";
            case 61, 63, 65 -> "Chuva";
            case 66, 67 -> "Chuva congelante";
            case 71, 73, 75 -> "Queda de neve";
            case 77 -> "Grãos de neve";
            case 80, 81, 82 -> "Pancadas de chuva";
            case 85, 86 -> "Pancadas de neve";
            case 95 -> "Tempestade";
            case 96, 99 -> "Tempestade com granizo";
            default -> "Tempo instável";
        };
    }
}
