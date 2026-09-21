package br.com.horizon.horizon_api.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DestinationWeatherResponse {

    private Long destinationId;
    private String destinationName;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private LocalDate targetDate;
    private String mode;
    private String modeDescription;
    private String timezone;
    private String summary;

    private ForecastDto forecast;
    private HistoricalContextDto historicalContext;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ForecastDto {
        private String description;
        private Double minTemperature;
        private Double maxTemperature;
        private Double apparentTemperature;
        private Double precipitationMm;
        private Integer precipitationProbabilityPercent;
        private Integer weatherCode;
        private String weatherDescription;
        private Double windSpeedKmH;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HistoricalContextDto {
        private String referencePeriod;
        private String description;
        private Double meanTemperature;
        private Double minTemperature;
        private Double maxTemperature;
        private Double totalPrecipitationMm;
        private Integer rainyDaysCount;
        private Integer snowyDaysCount;
        private Double avgWindSpeedKmH;
    }
}
