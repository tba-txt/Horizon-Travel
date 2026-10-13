package br.com.horizon.horizon_api.interfaces.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * Note: CORS is centrally managed in SecurityConfig to prevent conflicting filters.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}

