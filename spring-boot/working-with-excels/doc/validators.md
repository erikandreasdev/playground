# Excel Validators Reference

This project supports column-level validation for Excel files using a YAML mapping configuration.

## Processing Order

> [!IMPORTANT]
> Validations are split into two phases:
> 1. **Pre-transform validation** - Type checking, required fields, regex, length, numeric ranges
> 2. **Post-transform validation** - `allowedValues`, `excludedValues` (checked on transformed value)

This means you can transform a value (e.g., UPPERCASE) and then validate against an allowed list.

## Validation Structure

```yaml
columns:
  - name: "Column Name"
    type: STRING | INTEGER | DECIMAL | DATE | BOOLEAN | EMAIL
    validation:
      notEmpty: true
      regex: "..."
      minLength: 5
      maxLength: 10
      min: 0.0
      max: 100.0
      allowedValues: ["A", "B", "C"]
      excludedValues: ["X", "Y"]
```

## Available Validators

### 1. Required Value (Not Empty)
**Applies to:** All types  
**Phase:** Pre-transform

Enforces that a cell cannot be blank or null.

```yaml
validation:
  notEmpty: true
```

### 2. Type Validation
**Applies to:** All types  
**Phase:** Pre-transform (automatic)

The `type` field automatically validates cell data type:

| Type | Validation |
|------|------------|
| `STRING` | Cell must be text type |
| `EMAIL` | Text + valid email format |
| `INTEGER` | Numeric with no decimals |
| `DECIMAL` | Any numeric value |
| `BOOLEAN` | Boolean cell type |
| `DATE` | Date formatted cell |

### 3. Regex Validation
**Applies to:** `STRING`  
**Phase:** Pre-transform

Validates that the cell value matches the provided Java Regular Expression.

```yaml
validation:
  regex: "^[A-Z]{2,3}-[0-9]{4,6}$"  # e.g., "AB-12345"
```

**Common patterns:**

| Pattern | Description |
|---------|-------------|
| `^[A-Z0-9]+$` | Uppercase alphanumeric only |
| `^\\+?[0-9]{10,15}$` | Phone number |
| `^[0-9]{5}(-[0-9]{4})?$` | US postal code |

### 4. Length Validation
**Applies to:** `STRING`  
**Phase:** Pre-transform

Validates string length limits.

```yaml
validation:
  minLength: 2
  maxLength: 50
```

### 5. Numeric Range Validation
**Applies to:** `INTEGER`, `DECIMAL`  
**Phase:** Pre-transform

Validates that a numeric value falls within a specific range.

```yaml
validation:
  min: 0
  max: 100
```

### 6. Allowed Values (Enum)
**Applies to:** `STRING`  
**Phase:** Post-transform ✨

Validates that the **transformed** value is in the allowed list.

```yaml
transformations:
  - type: UPPERCASE
validation:
  allowedValues:
    - "LOW"
    - "MEDIUM"
    - "HIGH"
```

With the above config:
- Input `"low"` → transforms to `"LOW"` → **passes** ✅
- Input `"invalid"` → transforms to `"INVALID"` → **fails** ❌

### 7. Excluded Values (Blocklist)
**Applies to:** `STRING`  
**Phase:** Post-transform ✨

Validates that the **transformed** value is NOT in the excluded list.

```yaml
transformations:
  - type: LOWERCASE
validation:
  excludedValues:
    - "admin"
    - "root"
    - "system"
```

## Complete Example

```yaml
columns:
  - name: "Status"
    type: STRING
    validation:
      notEmpty: true                      # Pre-transform: must exist
      allowedValues: [ACTIVE, INACTIVE]   # Post-transform: after UPPERCASE
    transformations:
      - type: TRIM
      - type: UPPERCASE

  - name: "Email"
    type: EMAIL                           # Auto-validates email format
    validation:
      notEmpty: true
    transformations:
      - type: TRIM
      - type: LOWERCASE

  - name: "Age"
    type: INTEGER
    validation:
      min: 18
      max: 120

  - name: "SKU"
    type: STRING
    validation:
      regex: "^[A-Z]{2}-[0-9]{4}$"        # Pre-transform validation
    transformations:
      - type: UPPERCASE                   # Transform after regex? ❌ Use post-transform!
```

## Quick Reference

| Validator | Phase | Applies To | Parameters |
|-----------|-------|------------|------------|
| `notEmpty` | Pre-transform | All | `true/false` |
| Type check | Pre-transform | All | automatic |
| `regex` | Pre-transform | STRING | pattern string |
| `minLength` | Pre-transform | STRING | integer |
| `maxLength` | Pre-transform | STRING | integer |
| `min` | Pre-transform | NUMERIC | number |
| `max` | Pre-transform | NUMERIC | number |
| `allowedValues` | **Post-transform** | STRING | list |
| `excludedValues` | **Post-transform** | STRING | list |
