package br.com.horizon.horizon_api.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OpenMeteoConfig {

    @Value("${open-meteo.forecast-url:https://api.open-meteo.com}")
    private String forecastUrl;

    @Value("${open-meteo.archive-url:https://archive-api.open-meteo.com}")
    private String archiveUrl;

    @Value("${open-meteo.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${open-meteo.read-timeout-ms:5000}")
    private int readTimeoutMs;

    private SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return factory;
    }

    @Bean
    public RestClient openMeteoForecastClient() {
        return RestClient.builder()
                .baseUrl(forecastUrl)
                .requestFactory(createRequestFactory())
                .build();
    }

    @Bean
    public RestClient openMeteoArchiveClient() {
        return RestClient.builder()
                .baseUrl(archiveUrl)
                .requestFactory(createRequestFactory())
                .build();
    }
}
