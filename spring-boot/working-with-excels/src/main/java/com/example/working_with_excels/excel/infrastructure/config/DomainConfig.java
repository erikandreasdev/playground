package com.example.working_with_excels.excel.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.working_with_excels.excel.domain.service.CellTransformer;
import com.example.working_with_excels.excel.domain.service.CellValidator;
import com.example.working_with_excels.excel.domain.service.CellValueExtractor;

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

    /**
     * Provides the CellTransformer domain service as a Spring bean.
     *
     * @return a new instance of CellTransformer
     */
    @Bean
    public CellTransformer cellTransformer() {
        return new CellTransformer();
    }

    /**
     * Provides the CellValueExtractor domain service as a Spring bean.
     *
     * @param cellTransformer the cell transformer dependency
     * @return a new instance of CellValueExtractor
     */
    @Bean
    public CellValueExtractor cellValueExtractor(CellTransformer cellTransformer) {
        return new CellValueExtractor(cellTransformer);
    }
}
