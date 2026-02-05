# Validation Documentation

This project supports configuring Excel validations via YAML files.

## Configuration Structure

The validation configuration YAML file follows this structure:

```yaml
excelFilename: "example.xlsx" # The expected filename (optional)
sheets:
  - name: "Sheet1"            # The sheet name to validate
    columns:
      - name: "Email"         # The column header name
        required: true        # Whether the column is required
        type: EMAIL           # Data type (optional)
        transformations:      # List of transformations to apply before validation
          - TRIM
          - LOWERCASE
        rules:                # List of validation rules to apply
          - NOT_EMPTY
          - EMAIL_FORMAT
        allowedValues:        # Enum validation (optional)
          - "VALUE1"
          - "VALUE2"
        dbLookup:             # Foreign key validation (optional)
          table: "USERS"
          column: "user_id"
          errorMessage: "User not found"
    rowOperations:            # Computed columns (optional)
      - targetColumn: "Full Name"
        operations:
          - type: CONCATENATE
            sourceColumns: ["First Name", "Last Name"]
            separator: " "
    persistence:
      tableName: "users"      # The target database table
      upsert: true            # Use MERGE (upsert) instead of INSERT
      primaryKey: "Email"     # Primary key column for upsert
      mappings:               # Mapping between Excel columns and DB columns
        - excelColumn: "Email"
          dbColumn: "user_email"
```

## Persistence Configuration

The optional `persistence` block defines how valid rows should be saved to the database.

| Field | Description |
| :--- | :--- |
| `tableName` | The target database table name. |
| `upsert` | If true, uses Oracle MERGE for upsert; otherwise uses INSERT. |
| `primaryKey` | The Excel column name to use as primary key (required for upsert). |
| `mappings` | List of mappings from Excel columns to database columns. |

### Database Mapping

| Field | Description |
| :--- | :--- |
| `excelColumn` | The name of the column in the Excel sheet (after transformations). |
| `dbColumn` | The name of the column in the target database table. |

## Supported Data Types

| Type | Description | Example |
| :--- | :--- | :--- |
| `STRING` | Text values (default) | "John Doe" |
| `NUMBER` | Integer values | 123 |
| `DECIMAL` | Floating-point values | 123.45 |
| `BOOLEAN` | Boolean values | TRUE, FALSE |
| `DATE` | Date values | 2024-01-15 |
| `EMAIL` | Email addresses | user@example.com |
| `UUID` | UUID values | 550e8400-e29b-41d4-a716-446655440000 |

## Supported Transformations

Transformations are applied to cell values **before** validation rules are checked.

| Transformation | Description |
| :--- | :--- |
| `TRIM` | Removes leading and trailing whitespace. |
| `UPPERCASE` | Converts the value to uppercase. |
| `LOWERCASE` | Converts the value to lowercase. |

## Supported Validation Rules

Validation rules are checks performed on the transformed value. If a rule fails, the row is marked as invalid.

| Rule | Description |
| :--- | :--- |
| `NOT_NULL` | Checks that the cell value is not null. |
| `NOT_EMPTY` | Checks that the cell value is not null and contains text (after trimming). |
| `EMAIL_FORMAT` | Checks that the value matches a standard email format. |

## Row Operations (Computed Columns)

Row operations allow creating computed columns from existing column values.

| Operation | Description | Parameters |
| :--- | :--- | :--- |
| `CONCATENATE` | Join values from multiple columns | sourceColumns, separator |
| `REPLACE` | Replace pattern with replacement | pattern, replacement |
| `SUBSTRING` | Extract substring | startIndex, endIndex |
| `UPPERCASE` | Convert to uppercase | - |
| `LOWERCASE` | Convert to lowercase | - |
| `TRIM` | Remove whitespace | - |

### Row Operation Example

```yaml
rowOperations:
  - targetColumn: "Customer ID"
    operations:
      - type: CONCATENATE
        sourceColumns: ["Prefix", "Number"]
        separator: "-"
      - type: UPPERCASE
```

This creates a "Customer ID" column by concatenating "Prefix" and "Number" with a hyphen, then converting to uppercase.

## Foreign Key Validation (dbLookup)

The `dbLookup` feature validates that a value exists in a referenced database table.

```yaml
dbLookup:
  table: "CATEGORIES"
  column: "category_id"
  errorMessage: "Category ID does not exist. Please load categories first."
```

### Composite Key Lookup

For composite foreign keys, use the `columns` field instead:

```yaml
dbLookup:
  table: "ORDER_ITEMS"
  columns:
    - "order_id"
    - "product_id"
  errorMessage: "Order item reference not found."
```

## Example Configuration

```yaml
excelFilename: "users.xlsx"
sheets:
  - name: "Users"
    columns:
      - name: "Username"
        required: true
        transformations:
          - TRIM
        rules:
          - NOT_EMPTY
      - name: "Email Address"
        required: true
        type: EMAIL
        transformations:
          - TRIM
          - LOWERCASE
        rules:
          - NOT_EMPTY
          - EMAIL_FORMAT
      - name: "Status"
        required: true
        allowedValues:
          - "ACTIVE"
          - "INACTIVE"
          - "PENDING"
    persistence:
      tableName: "USERS"
      upsert: true
      primaryKey: "Username"
      mappings:
        - excelColumn: "Username"
          dbColumn: "username"
        - excelColumn: "Email Address"
          dbColumn: "email"
        - excelColumn: "Status"
          dbColumn: "status"
```

## Extending Functionality

### Adding a New Transformation

To add a new transformation (e.g., `REMOVE_SPACES`):

1. **Add the Enum Constant**:
   Open `src/main/java/com/example/demo/core/internal/domain/enums/TransformationType.java` and add your new type:
   ```java
   public enum TransformationType {
     TRIM,
     UPPERCASE,
     LOWERCASE,
     REMOVE_SPACES // New type
   }
   ```

2. **Implement Logic**:
   Open `src/main/java/com/example/demo/core/internal/services/CellTransformerService.java` and update the `switch` statement in the `applySingle` method:
   ```java
   private String applySingle(String value, TransformationType type) {
     switch (type) {
       // ... existing cases
       case REMOVE_SPACES:
         return value.replace(" ", "");
       default:
         return value;
     }
   }
   ```

3. **Add Test**:
   Open `src/test/java/com/example/demo/core/internal/services/CellTransformerServiceTest.java` and add a test:
   ```java
   @Test
   void apply_shouldRemoveSpaces() {
     assertThat(transformer.apply("hello world", List.of(TransformationType.REMOVE_SPACES)))
         .isEqualTo("helloworld");
   }
   ```

### Adding a New Validation Rule

To add a new validation rule (e.g., `IS_NUMERIC`):

1. **Add the Enum Constant**:
   Open `src/main/java/com/example/demo/core/internal/domain/enums/ValidationRule.java` and add your new rule:
   ```java
   public enum ValidationRule {
     NOT_EMPTY,
     EMAIL_FORMAT,
     NOT_NULL,
     IS_NUMERIC // New rule
   }
   ```

2. **Implement Logic**:
   Open `src/main/java/com/example/demo/core/internal/services/CellValidatorService.java` and update the validation logic:
   ```java
   private String validateRule(String value, ValidationRule rule, String columnName) {
     switch (rule) {
       // ... existing cases
       case IS_NUMERIC:
         if (value != null && !value.matches("-?\\d+(\\.\\d+)?")) {
           return columnName + " must be numeric";
         }
         return null;
       default:
         return null;
     }
   }
   ```

3. **Add Test**:
   Open `src/test/java/com/example/demo/core/internal/services/CellValidatorServiceTest.java` and add tests for the new rule.

### Adding a New Row Operation

To add a new row operation (e.g., `REVERSE`):

1. **Add the Enum Constant**:
   Open `src/main/java/com/example/demo/core/internal/domain/enums/OperationType.java`:
   ```java
   public enum OperationType {
     CONCATENATE,
     REPLACE,
     SUBSTRING,
     UPPERCASE,
     LOWERCASE,
     TRIM,
     REVERSE // New operation
   }
   ```

2. **Implement Logic**:
   Open `src/main/java/com/example/demo/core/internal/services/RowOperationService.java` and add the case:
   ```java
   case REVERSE:
     return new StringBuilder(currentValue).reverse().toString();
   ```

3. **Add Test**:
   Open `src/test/java/com/example/demo/core/internal/services/RowOperationServiceTest.java` and add tests.

## Project Structure Reference

The validation-related files are located in the hexagonal architecture structure:

```
com.example.demo/
├── core/
│   ├── internal/
│   │   ├── domain/
│   │   │   ├── config/
│   │   │   │   ├── ValidationConfig.java      # Root configuration
│   │   │   │   ├── SheetConfig.java           # Sheet configuration
│   │   │   │   ├── ColumnConfig.java          # Column configuration
│   │   │   │   ├── DbLookup.java              # Foreign key config
│   │   │   │   ├── Operation.java             # Single operation
│   │   │   │   └── RowOperation.java          # Computed column config
│   │   │   └── enums/
│   │   │       ├── DataType.java              # Data types enum
│   │   │       ├── TransformationType.java    # Transformations enum
│   │   │       ├── ValidationRule.java        # Validation rules enum
│   │   │       └── OperationType.java         # Row operations enum
│   │   └── services/
│   │       ├── CellTransformerService.java    # Applies transformations
│   │       ├── CellValidatorService.java      # Validates cells
│   │       ├── DbLookupService.java           # Foreign key validation
│   │       └── RowOperationService.java       # Computed columns
│   └── ports/
│       └── outbound/
│           └── DatabasePort.java              # DB operations interface
└── adapters/
    └── outbound/
        ├── persistence/
        │   └── JdbcDatabaseAdapter.java       # DB operations implementation
        └── resource/
            └── YamlConfigLoaderAdapter.java   # YAML parsing
```
