package com.example.working_with_excels.excel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.working_with_excels.excel.application.port.output.DatabasePort;
import com.example.working_with_excels.excel.domain.model.ImportMode;

/**
 * Unit tests for the BatchExecutor service.
 */
@ExtendWith(MockitoExtension.class)
class BatchExecutorTest {

    @Mock
    private DatabasePort databasePort;

    @InjectMocks
    private BatchExecutor batchExecutor;

    private static final String SQL = "INSERT INTO users (name) VALUES (:name)";

    @Nested
    @DisplayName("executeBatch in DRY_RUN mode")
    class DryRunMode {

        @Test
        @DisplayName("should return batch size without executing")
        void shouldReturnBatchSizeWithoutExecuting() {
            // Arrange
            List<Map<String, Object>> batch = List.of(
                    Map.of("name", "Alice"),
                    Map.of("name", "Bob"));

            // Act
            int result = batchExecutor.executeBatch(SQL, batch, ImportMode.DRY_RUN);

            // Assert
            assertThat(result).isEqualTo(2);
            verify(databasePort, never()).executeBatch(any(), any());
        }

        @Test
        @DisplayName("should handle empty batch")
        void shouldHandleEmptyBatch() {
            // Arrange
            List<Map<String, Object>> batch = List.of();

            // Act
            int result = batchExecutor.executeBatch(SQL, batch, ImportMode.DRY_RUN);

            // Assert
            assertThat(result).isZero();
            verify(databasePort, never()).executeBatch(any(), any());
        }
    }

    @Nested
    @DisplayName("executeBatch in EXECUTE mode")
    class ExecuteMode {

        @Test
        @DisplayName("should delegate to database port")
        void shouldDelegateToDatabase() {
            // Arrange
            List<Map<String, Object>> batch = List.of(
                    Map.of("name", "Alice"),
                    Map.of("name", "Bob"),
                    Map.of("name", "Charlie"));
            when(databasePort.executeBatch(eq(SQL), eq(batch))).thenReturn(3);

            // Act
            int result = batchExecutor.executeBatch(SQL, batch, ImportMode.EXECUTE);

            // Assert
            assertThat(result).isEqualTo(3);
            verify(databasePort).executeBatch(SQL, batch);
        }

        @Test
        @DisplayName("should return database result count")
        void shouldReturnDatabaseResultCount() {
            // Arrange
            List<Map<String, Object>> batch = List.of(Map.of("name", "Test"));
            when(databasePort.executeBatch(any(), any())).thenReturn(1);

            // Act
            int result = batchExecutor.executeBatch(SQL, batch, ImportMode.EXECUTE);

            // Assert
            assertThat(result).isEqualTo(1);
        }
    }
}
