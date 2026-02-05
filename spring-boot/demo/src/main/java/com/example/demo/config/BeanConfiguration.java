package com.example.demo.config;

import com.example.demo.adapters.outbound.persistence.JdbcDatabaseAdapter;
import com.example.demo.adapters.outbound.resource.SpringResourceLoaderAdapter;
import com.example.demo.adapters.outbound.resource.YamlConfigLoaderAdapter;
import com.example.demo.core.internal.services.CellTransformerService;
import com.example.demo.core.internal.services.CellValidatorService;
import com.example.demo.core.internal.services.DbLookupService;
import com.example.demo.core.internal.services.ExcelPersistenceService;
import com.example.demo.core.internal.services.ExcelRowValidatorService;
import com.example.demo.core.internal.services.ExcelSheetProcessorService;
import com.example.demo.core.internal.services.RowOperationService;
import com.example.demo.core.internal.usecases.ValidateExcelUseCase;
import com.example.demo.core.ports.inbound.ExcelValidationPort;
import com.example.demo.core.ports.outbound.ConfigLoaderPort;
import com.example.demo.core.ports.outbound.DatabasePort;
import com.example.demo.core.ports.outbound.ResourceLoaderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Spring configuration for wiring hexagonal architecture components.
 *
 * <p>This configuration class defines all beans following the hexagonal architecture pattern:
 *
 * <ul>
 *   <li>Outbound Adapters: Implement ports for external systems (database, file system)
 *   <li>Domain Services: Pure business logic with no framework dependencies
 *   <li>Use Cases: Implement inbound ports and orchestrate domain services
 * </ul>
 */
@Configuration
public class BeanConfiguration {

  // ========== OUTBOUND ADAPTERS ==========

  /**
   * Creates the database port adapter.
   *
   * @param jdbcTemplate Spring's JDBC template
   * @return database port implementation
   */
  @Bean
  public DatabasePort databasePort(NamedParameterJdbcTemplate jdbcTemplate) {
    return new JdbcDatabaseAdapter(jdbcTemplate);
  }

  /**
   * Creates the resource loader port adapter.
   *
   * @param resourceLoader Spring's resource loader
   * @return resource loader port implementation
   */
  @Bean
  public ResourceLoaderPort resourceLoaderPort(ResourceLoader resourceLoader) {
    return new SpringResourceLoaderAdapter(resourceLoader);
  }

  /**
   * Creates the config loader port adapter.
   *
   * @return config loader port implementation
   */
  @Bean
  public ConfigLoaderPort configLoaderPort() {
    return new YamlConfigLoaderAdapter();
  }

  // ========== DOMAIN SERVICES ==========

  /**
   * Creates the cell transformer service.
   *
   * @return cell transformer service
   */
  @Bean
  public CellTransformerService cellTransformerService() {
    return new CellTransformerService();
  }

  /**
   * Creates the database lookup service.
   *
   * @param databasePort database port for lookups
   * @return database lookup service
   */
  @Bean
  public DbLookupService dbLookupService(DatabasePort databasePort) {
    return new DbLookupService(databasePort);
  }

  /**
   * Creates the cell validator service.
   *
   * @param dbLookupService database lookup service
   * @return cell validator service
   */
  @Bean
  public CellValidatorService cellValidatorService(DbLookupService dbLookupService) {
    return new CellValidatorService(dbLookupService);
  }

  /**
   * Creates the row operation service.
   *
   * @return row operation service
   */
  @Bean
  public RowOperationService rowOperationService() {
    return new RowOperationService();
  }

  /**
   * Creates the Excel row validator service.
   *
   * @param cellTransformerService cell transformer
   * @param cellValidatorService cell validator
   * @param rowOperationService row operation processor
   * @return Excel row validator service
   */
  @Bean
  public ExcelRowValidatorService excelRowValidatorService(
      CellTransformerService cellTransformerService,
      CellValidatorService cellValidatorService,
      RowOperationService rowOperationService) {
    return new ExcelRowValidatorService(
        cellTransformerService, cellValidatorService, rowOperationService);
  }

  /**
   * Creates the Excel persistence service.
   *
   * @param databasePort database port
   * @return Excel persistence service
   */
  @Bean
  public ExcelPersistenceService excelPersistenceService(DatabasePort databasePort) {
    return new ExcelPersistenceService(databasePort);
  }

  /**
   * Creates the Excel sheet processor service.
   *
   * @param excelRowValidatorService row validator
   * @param excelPersistenceService persistence service
   * @return Excel sheet processor service
   */
  @Bean
  public ExcelSheetProcessorService excelSheetProcessorService(
      ExcelRowValidatorService excelRowValidatorService,
      ExcelPersistenceService excelPersistenceService) {
    return new ExcelSheetProcessorService(excelRowValidatorService, excelPersistenceService);
  }

  // ========== USE CASES (INBOUND PORTS) ==========

  /**
   * Creates the Excel validation use case (inbound port implementation).
   *
   * @param configLoaderPort config loader port
   * @param excelSheetProcessorService sheet processor service
   * @return Excel validation port implementation
   */
  @Bean
  public ExcelValidationPort excelValidationPort(
      ConfigLoaderPort configLoaderPort, ExcelSheetProcessorService excelSheetProcessorService) {
    return new ValidateExcelUseCase(configLoaderPort, excelSheetProcessorService);
  }
}
