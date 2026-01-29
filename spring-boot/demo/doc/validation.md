# Validation Documentation

This project supports configuring Excel validations via YAML files.

## Configuration Structure

The validation configuration YAML file follows this structure:

```yaml
excelFilename: "example.xlsx" # The expected filename
sheets:
  - name: "Sheet1"            # The sheet name to validate
    columns:
      - name: "Email"         # The column header name
        transformations:      # List of transformations to apply before validation
          - TRIM
          - LOWERCASE
        rules:                # List of validation rules to apply
          - NOT_EMPTY
          - EMAIL_FORMAT
    persistence:
      tableName: "users"      # The target database table
      dbMappings:             # Mapping between Excel columns and DB columns
        - excelColumn: "Email"
          dbColumn: "user_email"
```

## Persistence Configuration

The optional `persistence` block defines how valid rows should be saved to the database.

| Field | Description |
| :--- | :--- |
| `tableName` | The target database table name. |
| `dbMappings` | List of mappings from Excel columns to database columns. |

### Database Mapping

| Field | Description |
| :--- | :--- |
| `excelColumn` | The name of the column in the Excel sheet (after transformations). |
| `dbColumn` | The name of the column in the target database table. |

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

## Example

```yaml
excelFilename: "users.xlsx"
sheets:
  - name: "Users"
    columns:
      - name: "Username"
        transformations:
          - TRIM
        rules:
          - NOT_EMPTY
      - name: "Email Address"
        transformations:
          - TRIM
          - LOWERCASE
        rules:
          - NOT_EMPTY
          - EMAIL_FORMAT
```

## Extending Functionality

### Adding a New Transformation

To add a new transformation (e.g., `REMOVE_SPACES`):

1.  **Add the Enum Constant**:
    Open `src/main/java/com/example/demo/domain/TransformationType.java` and add your new type:
    ```java
    public enum TransformationType {
      TRIM,
      UPPERCASE,
      LOWERCASE,
      REMOVE_SPACES // New type
    }
    ```

2.  **Implement Logic**:
    Open `src/main/java/com/example/demo/validation/CellTransformer.java` and update the `switch` statement in the `applySingle` method:
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

### Adding a New Validation Rule

To add a new validation rule (e.g., `IS_NUMERIC`):

1.  **Add the Enum Constant**:
    Open `src/main/java/com/example/demo/domain/ValidationRule.java` and add your new rule:
    ```java
    public enum ValidationRule {
      NOT_EMPTY,
      EMAIL_FORMAT,
      NOT_NULL,
      IS_NUMERIC // New rule
    }
    ```

2.  **Implement Logic**:
    Open `src/main/java/com/example/demo/validation/CellValueValidator.java` and update the `switch` statement in the `validateSingle` method:
    ```java
    private boolean validateSingle(String value, ValidationRule rule) {
      switch (rule) {
        // ... existing cases
        case IS_NUMERIC:
          return value != null && value.matches("-?\\d+(\\.\\d+)?");
        default:
          return true;
      }
    }
    ```
