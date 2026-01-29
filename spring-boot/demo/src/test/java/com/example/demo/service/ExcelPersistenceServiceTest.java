package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.demo.domain.DatabaseMapping;
import com.example.demo.domain.PersistenceConfig;
import com.example.demo.domain.PersistenceResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcelPersistenceServiceTest {

  @Mock private JdbcDatabaseAdapter databaseAdapter;

  @InjectMocks private ExcelPersistenceService persistenceService;

  @Test
  void persistValidRows_shouldReturnSuccess_whenConfigNull() {
    PersistenceResult result =
        persistenceService.persistValidRows(Collections.emptyList(), null, false);
    assertThat(result.success()).isTrue();
    assertThat(result.rowsAffected()).isZero();
  }

  @Test
  void persistValidRows_shouldMapAndPersist() {
    // Arrange
    PersistenceConfig config =
        new PersistenceConfig(
            "users", false, null, List.of(new DatabaseMapping("Name", "user_name")));

    List<Map<String, Object>> rows = List.of(Map.of("Name", "Alice"));

    when(databaseAdapter.persist(anyList(), eq(config), eq(false)))
        .thenReturn(PersistenceResult.success(1, "INSERT..."));

    // Act
    PersistenceResult result = persistenceService.persistValidRows(rows, config, false);

    // Assert
    assertThat(result.success()).isTrue();
    assertThat(result.rowsAffected()).isEqualTo(1);
  }

  @Test
  void persistValidRows_shouldReturnSuccess_whenRowsEmpty() {
    PersistenceConfig config = new PersistenceConfig("users", false, null, List.of());
    PersistenceResult result =
        persistenceService.persistValidRows(Collections.emptyList(), config, false);
    assertThat(result.success()).isTrue();
    assertThat(result.rowsAffected()).isZero();
  }

  @Test
  void persistValidRows_shouldSkipMissingColumns() {
    // Arrange
    PersistenceConfig config =
        new PersistenceConfig(
            "users", false, null, List.of(new DatabaseMapping("Missing", "none")));

    List<Map<String, Object>> rows = List.of(Map.of("Existing", "Value"));

    when(databaseAdapter.persist(anyList(), eq(config), eq(false)))
        .thenReturn(PersistenceResult.success(1, "INSERT..."));

    // Act
    PersistenceResult result = persistenceService.persistValidRows(rows, config, false);

    // Assert
    assertThat(result.success()).isTrue();
  }
}
