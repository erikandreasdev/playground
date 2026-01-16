# Excel Database Import Reference

This module imports Excel data into Oracle database tables with validation, transformation, and configurable error handling.

## Quick Start

```java
@Autowired
private ExcelImportUseCase importUseCase;

// Dry-run mode (logs SQL without executing)
ImportReport report = importUseCase.importExcel(
    "users.xlsx",
    "users_mapping.yml",
    ImportMode.DRY_RUN
);

// Execute mode (real inserts)
ImportReport report = importUseCase.importExcel(
    "users.xlsx",
    "users_mapping.yml",
    ImportMode.EXECUTE
);
```

## Import Modes

| Mode | Description |
|------|-------------|
| `DRY_RUN` | Logs SQL statements without executing. Lookups return mock values. |
| `EXECUTE` | Inserts data into the database with transaction management. |

## YAML Configuration

```yaml
files:
  - filename: "users.xlsx"
    sheets:
      - name: "Users"
        table: "APP_USERS"           # Target Oracle table
        onError: SKIP_ROW            # Error handling strategy
        batchSize: 500               # Rows per batch insert
        columns:
          - name: "User ID"
            type: INTEGER
            dbMapping:
              dbColumn: "USER_ID"

          - name: "Full Name"
            type: STRING
            transformations:
              - type: TRIM
              - type: TITLE_CASE
            dbMapping:
              dbColumn: "FULL_NAME"

          - name: "Country"
            type: STRING
            dbMapping:
              dbColumn: "COUNTRY_ID"
              lookup:
                table: "COUNTRIES"
                matchColumn: "NAME"
                returnColumn: "ID"
```

## Column Mapping Rules

> [!IMPORTANT]
> **Only columns with `dbMapping` are included in the SQL query.**
> Columns without `dbMapping` are processed (validated, transformed) but NOT inserted.

## Error Strategies

| Strategy | Behavior |
|----------|----------|
| `SKIP_ROW` | Log error, skip the row, continue processing |
| `FAIL_SHEET` | Stop current sheet, continue with others |
| `FAIL_ALL` | Stop entire import immediately |

## Database Lookups

Resolve cell values to database IDs during import:

```yaml
dbMapping:
  dbColumn: "COUNTRY_ID"
  lookup:
    table: "COUNTRIES"
    matchColumn: "NAME"
    returnColumn: "ID"
```

## Custom SQL with Named Parameters

For complex insert logic, use custom SQL with **named parameters** (`:columnName`).

> [!TIP]
> **Order doesn't matter!** Named parameters match by `dbColumn` name, not by position.

### Syntax

Use `:columnName` where `columnName` matches the `dbColumn` values in your mapping:

```yaml
sheets:
  - name: "Users"
    table: "APP_USERS"
    customSql: |
      INSERT INTO APP_USERS (id, name, email, created_at) 
      VALUES (:USER_ID, :FULL_NAME, :EMAIL, SYSDATE)
    columns:
      - name: "Email Address"      # Order in YAML doesn't matter
        type: EMAIL
        dbMapping: { dbColumn: "EMAIL" }

      - name: "Full Name"
        type: STRING
        dbMapping: { dbColumn: "FULL_NAME" }

      - name: "User ID"
        type: INTEGER
        dbMapping: { dbColumn: "USER_ID" }
```

The `:USER_ID`, `:FULL_NAME`, and `:EMAIL` parameters are matched to the corresponding `dbColumn` values.

### Use Cases

| Scenario | Example |
|----------|---------|
| Oracle sequences | `PRODUCT_SEQ.NEXTVAL` |
| Timestamps | `SYSDATE`, `SYSTIMESTAMP` |
| Default values | Hardcoded values |
| Oracle functions | `SYS_GUID()`, `UPPER(:name)` |

### Example: Sequence + Timestamp

```yaml
customSql: |
  INSERT INTO PRODUCTS (id, name, created_at, created_by) 
  VALUES (PRODUCT_SEQ.NEXTVAL, :PRODUCT_NAME, SYSDATE, 'IMPORT')
columns:
  - name: "Product Name"
    type: STRING
    dbMapping: { dbColumn: "PRODUCT_NAME" }
```

## Import Report

```java
ImportReport report = importUseCase.importExcel(...);

boolean success = report.isSuccess();
ImportMetrics metrics = report.metrics();

for (SheetImportResult sheet : report.sheets()) {
    if (!sheet.isSuccess()) {
        for (ImportError error : sheet.errors()) {
            log.error("Row {}: {}", error.rowNumber(), error.errorMessage());
        }
    }
}
```

## Progress Logging

Large file imports log progress at 10% milestones:

```
INFO  Processing sheet 'Users' -> table 'APP_USERS'
INFO  Progress: 10% (1000/10000 rows)
INFO  Progress: 20% (2000/10000 rows)
...
INFO  Sheet 'Users' completed: total=10000, inserted=9950, skipped=50
```

## Database Configuration

### Production (Oracle 19c)

```properties
spring.datasource.url=jdbc:oracle:thin:@//hostname:1521/servicename
spring.datasource.username=user
spring.datasource.password=password
```

### Development (Oracle gvenzl/oracle-free)

```properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
spring.datasource.username=system
spring.datasource.password=oracle
```

### No Database (DryRun only)

When no datasource is configured, `DryRunDatabaseAdapter` is used automatically.
