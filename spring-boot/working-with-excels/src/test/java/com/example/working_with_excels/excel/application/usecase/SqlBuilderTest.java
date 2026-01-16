package com.example.working_with_excels.excel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.DbColumnMapping;
import com.example.working_with_excels.excel.domain.model.ExcelColumnType;
import com.example.working_with_excels.excel.domain.model.SheetConfig;

/**
 * Unit tests for the SqlBuilder service.
 */
class SqlBuilderTest {

        private SqlBuilder sqlBuilder;

        @BeforeEach
        void setUp() {
                sqlBuilder = new SqlBuilder();
        }

        @Nested
        @DisplayName("buildInsertSql")
        class BuildInsertSql {

                @Test
                @DisplayName("should build INSERT statement with all mapped columns")
                void shouldBuildInsertWithAllMappedColumns() {
                        // Arrange
                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Name", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("user_name")),
                                        new ColumnConfig("Email", ExcelColumnType.EMAIL, null, null,
                                                        DbColumnMapping.of("email_address")),
                                        new ColumnConfig("Age", ExcelColumnType.INTEGER, null, null,
                                                        DbColumnMapping.of("age")));

                        SheetConfig sheetConfig = new SheetConfig("Users", columns, "APP_USERS",
                                        null, null, null, null);

                        // Act
                        String sql = sqlBuilder.buildInsertSql(sheetConfig);

                        // Assert
                        assertThat(sql).isEqualTo(
                                        "INSERT INTO APP_USERS (user_name, email_address, age) VALUES (:user_name, :email_address, :age)");
                }

                @Test
                @DisplayName("should skip columns without database mapping")
                void shouldSkipColumnsWithoutMapping() {
                        // Arrange
                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Name", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("user_name")),
                                        new ColumnConfig("Notes", ExcelColumnType.STRING, null, null, null), // No
                                                                                                             // mapping
                                        new ColumnConfig("Email", ExcelColumnType.EMAIL, null, null,
                                                        DbColumnMapping.of("email")));

                        SheetConfig sheetConfig = new SheetConfig("Users", columns, "APP_USERS",
                                        null, null, null, null);

                        // Act
                        String sql = sqlBuilder.buildInsertSql(sheetConfig);

                        // Assert
                        assertThat(sql).isEqualTo(
                                        "INSERT INTO APP_USERS (user_name, email) VALUES (:user_name, :email)");
                }

                @Test
                @DisplayName("should handle single column")
                void shouldHandleSingleColumn() {
                        // Arrange
                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("ID", ExcelColumnType.INTEGER, null, null,
                                                        DbColumnMapping.of("id")));

                        SheetConfig sheetConfig = new SheetConfig("Data", columns, "SIMPLE_TABLE",
                                        null, null, null, null);

                        // Act
                        String sql = sqlBuilder.buildInsertSql(sheetConfig);

                        // Assert
                        assertThat(sql).isEqualTo("INSERT INTO SIMPLE_TABLE (id) VALUES (:id)");
                }

                @Test
                @DisplayName("should build empty INSERT when no columns have mappings")
                void shouldHandleNoMappedColumns() {
                        // Arrange
                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Notes", ExcelColumnType.STRING, null, null, null));

                        SheetConfig sheetConfig = new SheetConfig("Data", columns, "TABLE",
                                        null, null, null, null);

                        // Act
                        String sql = sqlBuilder.buildInsertSql(sheetConfig);

                        // Assert
                        assertThat(sql).isEqualTo("INSERT INTO TABLE () VALUES ()");
                }

                @Test
                @DisplayName("should build MERGE statement when primary key exists")
                void shouldBuildMergeWhenPrimaryKeyExists() {
                        // Arrange
                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("ID", ExcelColumnType.INTEGER, null, null,
                                                        DbColumnMapping.of("id")),
                                        new ColumnConfig("Name", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("name")));

                        SheetConfig sheetConfig = new SheetConfig("Users", columns, "APP_USERS",
                                        null, null, null, List.of("id"));

                        // Act
                        String sql = sqlBuilder.buildInsertSql(sheetConfig);

                        // Assert
                        String expectedSql = "MERGE INTO APP_USERS t " +
                                        "USING (SELECT :id AS id, :name AS name FROM dual) s " +
                                        "ON (t.id = s.id) " +
                                        "WHEN MATCHED THEN UPDATE SET t.name = s.name " +
                                        "WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name)";

                        assertThat(sql).isEqualTo(expectedSql);
                }
        }
}
