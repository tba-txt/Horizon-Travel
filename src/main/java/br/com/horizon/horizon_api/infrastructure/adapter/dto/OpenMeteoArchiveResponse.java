package br.com.horizon.horizon_api.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMeteoArchiveResponse {

    private Double latitude;
    private Double longitude;
    private String timezone;

    @JsonProperty("daily")
    private DailyArchive daily;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyArchive {
        private List<String> time;

        @JsonProperty("temperature_2m_max")
        private List<Double> temperature2mMax;

        @JsonProperty("temperature_2m_min")
        private List<Double> temperature2mMin;

        @JsonProperty("temperature_2m_mean")
        private List<Double> temperature2mMean;

        @JsonProperty("precipitation_sum")
        private List<Double> precipitationSum;

        @JsonProperty("snowfall_sum")
        private List<Double> snowfallSum;

        @JsonProperty("wind_speed_10m_max")
        private List<Double> windSpeed10mMax;
    }
}
