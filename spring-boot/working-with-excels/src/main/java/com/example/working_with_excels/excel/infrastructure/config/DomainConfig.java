package com.example.working_with_excels.excel.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.working_with_excels.excel.domain.service.CellValidator;

/**
 * Spring configuration for domain layer beans.
 *
 * <p>
 * This configuration class registers pure domain objects as Spring beans,
 * maintaining the separation between domain logic and framework concerns.
 */
@Configuration
public class DomainConfig {

    /**
     * Provides the CellValidator domain service as a Spring bean.
     *
     * @return a new instance of CellValidator
     */
    @Bean
    public CellValidator cellValidator() {
        return new CellValidator();
    }
}
