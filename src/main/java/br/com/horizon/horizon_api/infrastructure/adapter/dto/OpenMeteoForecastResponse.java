package br.com.horizon.horizon_api.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMeteoForecastResponse {

    private Double latitude;
    private Double longitude;
    private String timezone;

    @JsonProperty("daily")
    private DailyForecast daily;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyForecast {
        private List<String> time;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;

        @JsonProperty("temperature_2m_max")
        private List<Double> temperature2mMax;

        @JsonProperty("temperature_2m_min")
        private List<Double> temperature2mMin;

        @JsonProperty("apparent_temperature_max")
        private List<Double> apparentTemperatureMax;

        @JsonProperty("precipitation_sum")
        private List<Double> precipitationSum;

        @JsonProperty("precipitation_probability_max")
        private List<Integer> precipitationProbabilityMax;

        @JsonProperty("wind_speed_10m_max")
        private List<Double> windSpeed10mMax;
    }
}
