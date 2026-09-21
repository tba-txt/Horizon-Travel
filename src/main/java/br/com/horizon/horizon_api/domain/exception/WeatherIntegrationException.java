package br.com.horizon.horizon_api.domain.exception;

public class WeatherIntegrationException extends RuntimeException {
    public WeatherIntegrationException(String message) {
        super(message);
    }

    public WeatherIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
