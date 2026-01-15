# Excel Validators Reference

This project supports column-level validation for Excel files using a YAML mapping configuration. Below is a reference for the available validation rules.

## Validation Structure

Validation rules are defined under the `validation` key of a column configuration in your YAML file.

```yaml
columns:
  - name: "Column Name"
    type: STRING | INTEGER | DECIMAL | ...
    validation:
      regex: "..."
      minLength: 5
      maxLength: 10
      min: 0.0
      max: 100.0
```

## Available Validators

### 1. Regex Validation
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Validates that the cell value matches the provided Java Regular Expression.

**Example:**
```yaml
validation:
  regex: "^[A-Z0-9]+$" 
```

### 2. Length Validation
**Applies to:** `STRING`

Validates strict string length limits.

*   `minLength`: Minimum number of characters.
*   `maxLength`: Maximum number of characters.

**Example:**
```yaml
validation:
  minLength: 2
  maxLength: 50
```

### 3. Numeric Range Validation
**Applies to:** `INTEGER`, `DECIMAL`

Validates that a numeric value falls within a specific range.

*   `min`: Minimum numeric value (inclusive).
*   `max`: Maximum numeric value (inclusive).

**Example:**
```yaml
validation:
  min: 1
  max: 1000
```

### 4. Allowed Values (Enum)
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Validates that the value is present in a specific list of allowed values (allowlist).

**Example:**
```yaml
validation:
  allowedValues:
    - "Active"
    - "Inactive"
    - "Pending"
```

### 5. Excluded Values (blocklist)
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Validates that the value is NOT present in a specific list of excluded values.

**Example:**
```yaml
validation:
  excludedValues:
    - "Admin"
    - "Root"
```

### 6. Required Value (Not Empty)
**Applies to:** All types

Enforces that a cell cannot be blank or null.

**Example:**
```yaml
validation:
  notEmpty: true
```
