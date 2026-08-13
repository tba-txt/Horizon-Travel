package br.com.horizon.horizon_api.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "br.com.horizon.horizon_api.infrastructure.persistence")
public class JpaConfig {
}
