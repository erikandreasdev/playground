# Excel Mapping YAML Cheatsheet

Quick reference for configuring `.yml` mapping files.

## Basic Structure

```yaml
files:
  - filename: "data.xlsx"             # Name of the Excel file
    sheets:
      - name: "Customers"             # Sheet name to process
        table: "APP_CUSTOMERS"        # Target database table
        primaryKey: ["CUST_ID"]       # Optional: Unique DB columns for Upsert/Merge
        onError: SKIP_ROW             # Strategy: SKIP_ROW, FAIL_SHEET, FAIL_ALL
        batchSize: 1000               # Optional: Rows per insert batch (default: 500)
        columns:                      # List of column mappings
          - name: "Customer ID"
            type: INTEGER
            dbMapping:
              dbColumn: "CUST_ID"
            validation:
              notEmpty: true
              min: 1
```

## Data Types (`type`)

| Type | Description |
|------|-------------|
| `STRING` | Text values |
| `INTEGER` | Whole numbers |
| `DECIMAL` | Floating point numbers |
| `BOOLEAN` | True/False values |
| `DATE` | Dates and Timestamps |
| `EMAIL` | Email address validation |

## Transformations (`transformations`)

Applied **in order** before validation.

```yaml
transformations:
  - type: TRIM                        # Remove whitespace tokens
  - type: UPPERCASE                   # Convert to UPPERCASE
  - type: LOWERCASE                   # Convert to lowercase
  - type: TITLE_CASE                  # "hello world" -> "Hello World"
  - type: SENTENCE_CASE               # "hello world" -> "Hello world"
  - type: REMOVE_WHITESPACE           # "a b c" -> "abc"
  - type: NORMALIZE_SPACES            # "a   b" -> "a b"
  - type: REPLACE                     # Regex replacement
    pattern: "[^0-9]"
    replacement: ""
  - type: SUBSTRING                   # Extract part of string
    start: 0
    end: 10
  - type: PAD_LEFT                    # Pad with character
    length: 10
    padChar: "0"
```

## Validations (`validation`)

Applied **after** transformation.

```yaml
validation:
  notEmpty: true                      # Cannot be null/empty
  regex: "^[A-Z]{3}$"                 # Must match regex
  minLength: 5                        # Min string length
  maxLength: 100                      # Max string length
  min: 18.0                           # Min numeric value (inclusive)
  max: 150.0                          # Max numeric value (inclusive)
  dateFormat: "yyyy-MM-dd"            # Strict date parsing format
  allowedValues:                      # Enum-style restrictions
    - "ACTIVE"
    - "INACTIVE"
    - "PENDING"
  excludedValues:                     # Blacklist
    - "UNKNOWN"
    - "DELETED"
```

## Database Mapping (`dbMapping`)

```yaml
dbMapping:
  dbColumn: "USER_STATUS"             # Column name in DB table
  
  # Optional: Database Lookup (Foreign Key resolution)
  lookup:
    table: "STATUS_CODES"             # Lookup table
    matchColumn: "CODE_NAME"          # Column to match with Excel value
    returnColumn: "CODE_ID"           # Column to return as value
```

## Full Example Snippet

```yaml
files:
  - filename: "products.xlsx"
    sheets:
      - name: "Inventory"
        table: "PRODUCTS"
        primaryKey: ["SKU"]
        onError: FAIL_SHEET
        columns:
          - name: "SKU"
            type: STRING
            transformations:
              - type: TRIM
              - type: UPPERCASE
            validation:
              notEmpty: true
              regex: "^PROD-\\d{5}$"
            dbMapping:
              dbColumn: "SKU"

          - name: "Category"
            type: STRING
            transformations:
              - type: TITLE_CASE
            dbMapping:
              dbColumn: "CATEGORY_ID"
              lookup:
                table: "CATEGORIES"
                matchColumn: "NAME"
                returnColumn: "ID"

          - name: "Price"
            type: DECIMAL
            validation:
              min: 0.01
            dbMapping:
              dbColumn: "UNIT_PRICE"

          - name: "Launch Date"
            type: DATE
            validation:
              dateFormat: "MM/dd/yyyy"
            dbMapping:
              dbColumn: "LAUNCH_DATE"
```
