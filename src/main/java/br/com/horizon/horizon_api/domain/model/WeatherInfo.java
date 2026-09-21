package br.com.horizon.horizon_api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfo {

    public enum WeatherMode {
        FORECAST,
        HISTORICAL_CONTEXT
    }

    private WeatherMode mode;
    private String modeDescription;
    private LocalDate targetDate;
    private String timezone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String summaryMessage;

    private ForecastDetails forecast;
    private HistoricalContextDetails historicalContext;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastDetails {
        private String description;
        private Double minTemperature;
        private Double maxTemperature;
        private Double apparentTemperatureMax;
        private Double precipitationSum;
        private Integer precipitationProbability;
        private Integer weatherCode;
        private String weatherDescription;
        private Double maxWindSpeed;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalContextDetails {
        private String referencePeriod;
        private String description;
        private Double meanTemperature;
        private Double minTemperature;
        private Double maxTemperature;
        private Double totalPrecipitation;
        private Integer rainyDaysCount;
        private Integer snowfallDaysCount;
        private Double avgWindSpeed;
    }
}
