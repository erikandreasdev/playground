package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.domain.DatabaseMapping;
import com.example.demo.domain.PersistenceConfig;
import com.example.demo.domain.PersistenceResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/** Unit tests for {@link JdbcDatabaseAdapter}. */
@ExtendWith(MockitoExtension.class)
class JdbcDatabaseAdapterTest {

  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private JdbcDatabaseAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new JdbcDatabaseAdapter(jdbcTemplate);
  }

  /** Verifies that the adapter generates valid Oracle MERGE SQL for upserts. */
  @Test
  void persist_shouldGenerateOracleMergeSql_whenUpsertIsTrue() {
    // Arrange
    PersistenceConfig config =
        new PersistenceConfig(
            "users",
            true,
            "id",
            List.of(new DatabaseMapping("ID", "id"), new DatabaseMapping("Name", "full_name")));
    List<Map<String, Object>> rows = List.of(Map.of("id", "1", "full_name", "John Doe"));

    // Act
    PersistenceResult result = adapter.persist(rows, config, true);

    // Assert
    assertThat(result.success()).isTrue();
    assertThat(result.generatedSql())
        .contains("MERGE INTO users target")
        .contains("USING (SELECT :id AS id, :full_name AS full_name FROM DUAL) source")
        .contains("ON (target.id = source.id)")
        .contains("WHEN MATCHED THEN UPDATE SET target.full_name = source.full_name")
        .contains(
            "WHEN NOT MATCHED THEN INSERT (id, full_name) VALUES (source.id, source.full_name)");
  }

  /** Verifies that the adapter executes a batch update when dry run is false. */
  @Test
  void persist_shouldExecuteBatchUpdate_whenDryRunIsFalse() {
    // Arrange
    PersistenceConfig config =
        new PersistenceConfig(
            "users",
            false,
            null,
            List.of(new DatabaseMapping("ID", "id"), new DatabaseMapping("Name", "full_name")));
    List<Map<String, Object>> rows = List.of(Map.of("id", "1", "full_name", "John Doe"));
    when(jdbcTemplate.batchUpdate(any(String.class), any(SqlParameterSource[].class)))
        .thenReturn(new int[] {1});

    // Act
    PersistenceResult result = adapter.persist(rows, config, false);

    // Assert
    assertThat(result.success()).isTrue();
    assertThat(result.rowsAffected()).isEqualTo(1);
    verify(jdbcTemplate).batchUpdate(any(String.class), any(SqlParameterSource[].class));
  }

  @Test
  void persist_shouldResolvePrimaryKeyToDbColumn_whenUpsertIsTrue() {
    // Arrange
    PersistenceConfig config =
        new PersistenceConfig(
            "products",
            true,
            "Product ID",
            List.of(
                new DatabaseMapping("Product ID", "product_id"),
                new DatabaseMapping("Name", "product_name")));
    List<Map<String, Object>> rows = List.of(Map.of("product_id", "P1", "product_name", "Widget"));

    // Act
    PersistenceResult result = adapter.persist(rows, config, true);

    // Assert
    assertThat(result.success()).isTrue();
    String sql = result.generatedSql();

    assertThat(sql).contains("ON (target.product_id = source.product_id)");
    assertThat(sql).doesNotContain("target.Product ID");
  }
}
