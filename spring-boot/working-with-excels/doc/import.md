# Excel Database Import Reference

This module imports Excel data into database tables with validation, transformation, and configurable error handling.

## Quick Start

### Programmatic Usage

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

### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/import/dry-run` | GET | Quick test with default files (DRY_RUN) |
| `/api/import/execute` | POST | Execute import with default files |
| `/api/import/classpath` | POST | Import from classpath resources |
| `/api/import/filesystem` | POST | Import from filesystem paths |
| `/api/import/upload` | POST | Import via file upload (multipart) |
| `/api/import/url` | POST | Import from URLs (cloud storage) |

**Examples:**

```bash
# Dry run test
curl -s http://localhost:8081/api/import/dry-run | jq

# Execute import
curl -s -X POST http://localhost:8081/api/import/execute | jq

# Upload files
curl -X POST http://localhost:8081/api/import/upload \
  -F "excel=@data.xlsx" \
  -F "config=@mapping.yml" \
  -F "mode=EXECUTE"

# Import from URL
curl -X POST "http://localhost:8081/api/import/url?excelUrl=https://...&configUrl=https://...&mode=EXECUTE"
```

## File Sources

The import service supports multiple file origins:

```java
// Classpath (src/main/resources/)
importUseCase.importExcel(
    FileSource.classpath("users.xlsx"),
    FileSource.classpath("users_mapping.yml"),
    ImportMode.EXECUTE
);

// Filesystem paths
importUseCase.importExcel(
    FileSource.filesystem("/data/users.xlsx"),
    FileSource.filesystem("/config/mapping.yml"),
    ImportMode.EXECUTE
);

// InputStream (for uploads)
importUseCase.importExcel(
    FileSource.stream(inputStream, "file.xlsx"),
    FileSource.stream(configStream, "mapping.yml"),
    ImportMode.DRY_RUN
);

// URL download (cloud storage)
importUseCase.importExcel(
    FileSource.url("https://bucket.s3.amazonaws.com/data.xlsx"),
    FileSource.url("https://bucket.s3.amazonaws.com/mapping.yml"),
    ImportMode.EXECUTE
);
```

## Import Modes

| Mode | Description |
|------|-------------|
| `DRY_RUN` | Logs SQL statements without executing. Lookups still query DB. |
| `EXECUTE` | Inserts data into the database with transaction management. |

## Processing Pipeline

> [!IMPORTANT]
> **Order of operations per row:**
> 1. **Type validation** - Check cell type (STRING, DATE, INTEGER, etc.)
> 2. **Transform** - Apply UPPERCASE, TRIM, etc.
> 3. **Value validation** - Check `allowedValues`, `excludedValues` on **transformed** value
> 4. **Lookup** - Resolve foreign keys from lookup tables
> 5. **Insert** - Execute SQL

This means if you have:
```yaml
transformations:
  - type: UPPERCASE
validation:
  allowedValues: [LOW, MEDIUM, HIGH]
```

The value `"low"` → transforms to `"LOW"` → validates against allowed list → **passes** ✅

## YAML Configuration

```yaml
files:
  - filename: "users.xlsx"
    sheets:
      - name: "Users"
        table: "APP_USERS"           # Target database table
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
String duration = report.getDurationFormatted();  // "1.50s"
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

### Production (Oracle)

```properties
spring.datasource.url=jdbc:oracle:thin:@//hostname:1521/servicename
spring.datasource.username=user
spring.datasource.password=password
```

### Development (Docker Oracle)

```properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
spring.datasource.username=appuser
spring.datasource.password=appuser123
```

### No Database (DryRun only)

When no datasource is configured, `DryRunDatabaseAdapter` is used automatically.
