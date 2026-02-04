# YAML Mapper Quick Reference for Excel Validation

Complete guide to using YAML mappers for validating Excel files.

---

## Execution Order

Understanding the processing order is critical:

1. **Cell Transformations** (TRIM, UPPERCASE, etc.) - Applied to raw cell values
2. **Row Operations** (CONCATENATE, REPLACE, etc.) - Creates computed columns
3. **Validation Rules** (NOT_EMPTY, dbLookup, etc.) - Validates all columns
4. **Type Conversion** (String → Number, Date, etc.) - Converts valid data
5. **Persistence** (Database INSERT/MERGE) - Saves valid rows

---

## YAML Configuration Structure

```yaml
sheets:
  - name: "Sheet Name"
    
    # Define columns (from Excel + computed columns)
    columns:
      - name: "Column Name"
        required: true|false
        type: STRING|NUMBER|DATE|UUID
        transformations: [TRIM, UPPERCASE, LOWERCASE]
        rules:
          - NOT_EMPTY
          - CUSTOM_REGEX: "^[A-Z0-9]+$"
        allowedValues: ["VALUE1", "VALUE2"]
        dbLookup:
          table: "TABLE_NAME"
          column: "column_name"
    
    # Define row operations (creates computed columns)
    rowOperations:
      - targetColumn: "Computed Column Name"
        operations:
          - type: CONCATENATE
            sourceColumns: ["Col1", "Col2"]
            separator: "-"
          - type: UPPERCASE
          - type: REPLACE
            pattern: " "
            replacement: "_"
    
    # Define database persistence
    persistence:
      tableName: "DB_TABLE"
      primaryKey: "id_column"
      mode: INSERT|MERGE
      mappings:
        - excelColumn: "Excel Column"
          dbColumn: "db_column"
```

---

## Cell Transformations

Applied **first** to clean raw cell values.

| Transformation | Description | Example |
|---------------|-------------|---------|
| `TRIM` | Remove leading/trailing whitespace | `"  text  "` → `"text"` |
| `UPPERCASE` | Convert to uppercase | `"hello"` → `"HELLO"` |
| `LOWERCASE` | Convert to lowercase | `"HELLO"` → `"hello"` |

**Usage:**
```yaml
columns:
  - name: "Customer Name"
    transformations: [TRIM, UPPERCASE]  # Applied in order
```

---

## Row Operations

Applied **after cell transformations** to create computed columns.

### CONCATENATE
Join values from multiple columns.

```yaml
rowOperations:
  - targetColumn: "Full Name"
    operations:
      - type: CONCATENATE
        sourceColumns: ["First Name", "Last Name"]
        separator: " "
```

**Result:** `"John"` + `"Doe"` → `"John Doe"`

### REPLACE
Replace pattern in current value.

```yaml
rowOperations:
  - targetColumn: "Clean ID"
    operations:
      - type: REPLACE
        pattern: " "
        replacement: "_"
```

**Result:** `"CUST 001"` → `"CUST_001"`

### SUBSTRING
Extract substring from current value.

```yaml
rowOperations:
  - targetColumn: "Year"
    operations:
      - type: SUBSTRING
        startIndex: 0
        endIndex: 4
```

**Result:** `"2024-01-15"` → `"2024"`

### String Transformations

```yaml
rowOperations:
  - targetColumn: "Uppercase ID"
    operations:
      - type: UPPERCASE  # Convert to uppercase
  
  - targetColumn: "Lowercase Name"
    operations:
      - type: LOWERCASE  # Convert to lowercase
  
  - targetColumn: "Trimmed Value"
    operations:
      - type: TRIM  # Remove whitespace
```

### Chaining Operations

Operations execute **sequentially**:

```yaml
rowOperations:
  - targetColumn: "Customer ID"
    operations:
      - type: CONCATENATE
        sourceColumns: ["Prefix", "Number"]
        separator: "-"
      - type: UPPERCASE
      - type: REPLACE
        pattern: " "
        replacement: "_"
```

**Flow:** `"cust"` + `"001"` → `"cust-001"` → `"CUST-001"` → `"CUST-001"`

---

## Validation Rules

Applied **after row operations** to validate all columns (including computed).

### NOT_EMPTY
Ensure value is not blank.

```yaml
columns:
  - name: "Required Field"
    rules:
      - NOT_EMPTY
```

### CUSTOM_REGEX
Validate against regex pattern.

```yaml
columns:
  - name: "Product Code"
    rules:
      - CUSTOM_REGEX: "^[A-Z]{3}-[0-9]{4}$"
```

### allowedValues
Restrict to specific values.

```yaml
columns:
  - name: "Status"
    allowedValues: ["ACTIVE", "INACTIVE", "PENDING"]
```

### dbLookup
Check if value exists in database.

```yaml
columns:
  - name: "Customer ID"
    dbLookup:
      table: "CUSTOMERS"
      column: "customer_id"
```

> [!NOTE]
> Database lookups validate that the value exists. Use `required: true` to prevent nulls.

---

## Data Types

Applied **after validation** to convert valid data.

| Type | Java Type | Example Input | Example Output |
|------|-----------|---------------|----------------|
| `STRING` | String | `"hello"` | `"hello"` |
| `NUMBER` | Double | `"123"` | `123.0` |
| `DATE` | LocalDate | `"2024-01-15"` | `2024-01-15` |
| `UUID` | UUID | `"550e8400-e29b..."` | UUID object |

**Usage:**
```yaml
columns:
  - name: "Price"
    type: NUMBER
  - name: "Birth Date"
    type: DATE
```

---

## Persistence Configuration

Applied **last** to save valid rows to database.

### Basic Configuration

```yaml
persistence:
  tableName: "CUSTOMERS"
  primaryKey: "customer_id"
  mode: INSERT  # INSERT or MERGE
  mappings:
    - excelColumn: "Customer ID"
      dbColumn: "customer_id"
    - excelColumn: "Full Name"
      dbColumn: "full_name"
```

### Modes

- **INSERT**: Add new rows (fails if exists)
- **MERGE**: Update if exists, insert if not

---

## Complete Example

Excel file with columns: `First Name`, `Last Name`, `ID Prefix`, `ID Number`

```yaml
sheets:
  - name: "Customers"
    
    columns:
      # Source columns from Excel
      - name: "First Name"
        required: true
        transformations: [TRIM, UPPERCASE]
      
      - name: "Last Name"
        required: true
        transformations: [TRIM, UPPERCASE]
      
      - name: "ID Prefix"
        required: true
        transformations: [TRIM]
      
      - name: "ID Number"
        required: true
        transformations: [TRIM]
      
      # Computed column (created by row operations)
      - name: "Customer ID"
        required: true
        type: STRING
        rules:
          - NOT_EMPTY
          - CUSTOM_REGEX: "^[A-Z]+-[0-9]+$"
        dbLookup:
          table: "CUSTOMERS"
          column: "customer_id"
    
    # Create computed column
    rowOperations:
      - targetColumn: "Customer ID"
        operations:
          - type: CONCATENATE
            sourceColumns: ["ID Prefix", "ID Number"]
            separator: "-"
          - type: UPPERCASE
    
    # Persist to database
    persistence:
      tableName: "CUSTOMERS"
      primaryKey: "customer_id"
      mode: MERGE
      mappings:
        - excelColumn: "Customer ID"
          dbColumn: "customer_id"
        - excelColumn: "First Name"
          dbColumn: "first_name"
        - excelColumn: "Last Name"
          dbColumn: "last_name"
```

### Processing Flow

**Input Row:**
| First Name | Last Name | ID Prefix | ID Number |
|-----------|-----------|-----------|-----------|
| `  john  ` | `  doe  ` | `cust` | `001` |

**After Cell Transformations:**
```json
{
  "First Name": "JOHN",
  "Last Name": "DOE",
  "ID Prefix": "cust",
  "ID Number": "001"
}
```

**After Row Operations:**
```json
{
  "First Name": "JOHN",
  "Last Name": "DOE",
  "ID Prefix": "cust",
  "ID Number": "001",
  "Customer ID": "CUST-001"  // ← Computed
}
```

**After Validation:**
- ✅ All required fields present
- ✅ Customer ID matches regex `^[A-Z]+-[0-9]+$`
- ✅ Customer ID exists in database

**After Type Conversion:**
```json
{
  "First Name": "JOHN",
  "Last Name": "DOE",
  "ID Prefix": "cust",
  "ID Number": "001",
  "Customer ID": "CUST-001"
}
```

**Database Persistence:**
```sql
MERGE INTO CUSTOMERS (customer_id, first_name, last_name)
VALUES ('CUST-001', 'JOHN', 'DOE');
```

---

## Common Patterns

### Pattern 1: Generate Unique ID from Multiple Columns

```yaml
rowOperations:
  - targetColumn: "Unique ID"
    operations:
      - type: CONCATENATE
        sourceColumns: ["Department", "Year", "Sequence"]
        separator: "-"
      - type: UPPERCASE
```

### Pattern 2: Clean and Normalize Data

```yaml
columns:
  - name: "Email"
    transformations: [TRIM, LOWERCASE]
    rules:
      - CUSTOM_REGEX: "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$"
```

### Pattern 3: Extract and Validate Date Components

```yaml
rowOperations:
  - targetColumn: "Year"
    operations:
      - type: SUBSTRING
        startIndex: 0
        endIndex: 4

columns:
  - name: "Year"
    type: NUMBER
    rules:
      - CUSTOM_REGEX: "^(19|20)[0-9]{2}$"
```

### Pattern 4: Conditional Validation with Allowed Values

```yaml
columns:
  - name: "Status"
    required: true
    allowedValues: ["ACTIVE", "INACTIVE", "PENDING", "ARCHIVED"]
    transformations: [TRIM, UPPERCASE]
```

---

## Troubleshooting

### Issue: Validation fails but data looks correct
✅ **Solution:** Check execution order. Cell transformations run before validation.

```yaml
# ❌ Wrong: Validation expects uppercase but transformation not applied
columns:
  - name: "Code"
    rules:
      - CUSTOM_REGEX: "^[A-Z]+$"  # Expects uppercase
    # Missing: transformations: [UPPERCASE]

# ✅ Correct: Apply transformation first
columns:
  - name: "Code"
    transformations: [UPPERCASE]
    rules:
      - CUSTOM_REGEX: "^[A-Z]+$"
```

### Issue: Computed column not found
✅ **Solution:** Define column in `columns` section AND create it in `rowOperations`.

```yaml
# Define the computed column
columns:
  - name: "Full Name"
    required: true

# Create the computed column
rowOperations:
  - targetColumn: "Full Name"
    operations:
      - type: CONCATENATE
        sourceColumns: ["First Name", "Last Name"]
        separator: " "
```

### Issue: Database lookup fails
✅ **Solution:** Ensure value exists in database BEFORE validation runs.

```yaml
# Validate that value exists in CUSTOMERS table
columns:
  - name: "Customer ID"
    dbLookup:
      table: "CUSTOMERS"
      column: "customer_id"  # Must match actual DB column name
```

---

## API Usage

### Validate Excel File

```bash
curl "http://localhost:8080/api/excel/validate?excelFilename=customers.xlsx&validationsFilename=customer-validation.yml&persist=false"
```

**Parameters:**
- `excelFilename`: Name of Excel file in configured directory
- `validationsFilename`: Name of YAML config file
- `persist`: `true` to save to database, `false` for dry-run

**Response:**
```json
{
  "status": "SUCCESS",
  "sheets": {
    "Customers": {
      "totalRows": 100,
      "validRows": 95,
      "invalidRows": 5,
      "errors": [
        {
          "rowNumber": 5,
          "columnName": "Customer ID",
          "errorMessage": "Value does not exist in database table CUSTOMERS"
        }
      ]
    }
  }
}
```

---

## Key Takeaways

1. **Order matters**: Transformations → Row Operations → Validation → Type Conversion → Persistence
2. **Computed columns**: Can be validated and persisted like regular columns
3. **Sequential operations**: Row operations execute in defined order
4. **Database lookups**: Validate existence before persistence
5. **Dry-run mode**: Use `persist=false` to test without saving

---

## Quick Reference Tables

### All Transformation Types
| Type | Level | When Applied | Purpose |
|------|-------|--------------|---------|
| TRIM | Cell | Phase 1 | Remove whitespace |
| UPPERCASE | Cell | Phase 1 | Convert to uppercase |
| LOWERCASE | Cell | Phase 1 | Convert to lowercase |
| CONCATENATE | Row | Phase 2 | Join columns |
| REPLACE | Row | Phase 2 | Find and replace |
| SUBSTRING | Row | Phase 2 | Extract substring |

### All Validation Types
| Type | Purpose | Example |
|------|---------|---------|
| NOT_EMPTY | Prevent nulls | `rules: [NOT_EMPTY]` |
| CUSTOM_REGEX | Pattern match | `CUSTOM_REGEX: "^[A-Z]+$"` |
| allowedValues | Whitelist | `allowedValues: ["A", "B"]` |
| dbLookup | Database check | `dbLookup: {table: "T", column: "c"}` |

### All Data Types
| Type | Java Type | Validation |
|------|-----------|------------|
| STRING | String | None (default) |
| NUMBER | Double | Must be numeric |
| DATE | LocalDate | Must be valid date |
| UUID | UUID | Must be valid UUID |
