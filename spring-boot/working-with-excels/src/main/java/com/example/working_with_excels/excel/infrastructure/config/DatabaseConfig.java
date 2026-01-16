package com.example.working_with_excels.excel.infrastructure.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;

import com.example.working_with_excels.excel.application.port.output.DatabasePort;
import com.example.working_with_excels.excel.infrastructure.adapter.output.DryRunDatabaseAdapter;
import com.example.working_with_excels.excel.infrastructure.adapter.output.JdbcDatabaseAdapter;

/**
 * Spring configuration for database adapters.
 *
 * <p>
 * Provides conditional beans based on DataSource availability.
 * Uses JdbcDatabaseAdapter when a database is configured, otherwise
 * DryRunDatabaseAdapter.
 */
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@RequiredArgsConstructor
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final ObjectProvider<NamedParameterJdbcTemplate> namedJdbcTemplateProvider;
    private final ObjectProvider<PlatformTransactionManager> transactionManagerProvider;

    /**
     * Provides the appropriate database adapter based on DataSource availability.
     *
     * @return JdbcDatabaseAdapter if DataSource is available, otherwise
     *         DryRunDatabaseAdapter
     */
    @Bean
    public DatabasePort databaseAdapter() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        NamedParameterJdbcTemplate namedJdbcTemplate = namedJdbcTemplateProvider.getIfAvailable();
        PlatformTransactionManager transactionManager = transactionManagerProvider.getIfAvailable();

        if (dataSource != null && namedJdbcTemplate != null && transactionManager != null) {
            log.info("Using JdbcDatabaseAdapter with real database connection");
            return new JdbcDatabaseAdapter(namedJdbcTemplate, transactionManager);
        } else {
            log.info("Using DryRunDatabaseAdapter (no database configured)");
            return new DryRunDatabaseAdapter();
        }
    }
}
