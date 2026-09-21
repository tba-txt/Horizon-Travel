package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.WeatherInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface WeatherPort {
    Optional<WeatherInfo> getDailyForecast(BigDecimal latitude, BigDecimal longitude, LocalDate targetDate);
    Optional<WeatherInfo> getHistoricalContext(BigDecimal latitude, BigDecimal longitude, LocalDate targetDate);
}
