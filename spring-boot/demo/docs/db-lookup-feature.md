# Database Lookup Validation Feature

## Overview

The **Database Lookup Validation** feature allows you to validate Excel cell values by checking if they exist in database tables before persisting. This ensures referential integrity and data consistency at the validation stage.

## Quick Start

### Basic Example

```yaml
columns:
  - name: "Department ID"
    type: "NUMBER"
    required: true
    rules:
      - NOT_EMPTY
    dbLookup:
      table: "DEPARTMENTS"
      column: "ID"
      errorMessage: "Department does not exist"
```

This will query the database to verify that each Department ID value exists in the `DEPARTMENTS` table's `ID` column.

## Configuration Options

### Single Column Lookup

For simple foreign key validations:

```yaml
dbLookup:
  table: "TABLE_NAME"           # Required: table to query
  column: "COLUMN_NAME"          # Required for single lookup
  errorMessage: "Custom error"   # Optional: defaults to "Value does not exist in TABLE_NAME.COLUMN_NAME"
```

### Composite Key Lookup

For validations requiring multiple columns (e.g., Country + City):

```yaml
dbLookup:
  table: "WAREHOUSES"
  columns:                       # Use 'columns' (plural) for composite keys
    - "COUNTRY"
    - "CITY"
  errorMessage: "Warehouse location not found"
```

## How It Works

### Validation Flow

1. **Transformation**: Cell values pass through transformations (TRIM, UPPERCASE, etc.)
2. **Type Validation**: Values are validated against specified data types
3. **Rules Check**: Validation rules (NOT_EMPTY, EMAIL_FORMAT, etc.) are applied
4. **Allowed Values**: Values are checked against the allowedValues list (if specified)
5. **Database Lookup**: ✨ NEW - Values are verified to exist in the database
6. **Persistence**: Only valid rows are persisted to the database

### Query Behavior

- **Single Column**: `SELECT COUNT(*) FROM table WHERE column = ?`
- **Composite Key**: `SELECT COUNT(*) FROM table WHERE col1 = ? AND col2 = ? ...`

## Use Cases

### 1. Foreign Key Validation
Ensure related entities exist before creating references:

```yaml
- name: "Customer ID"
  type: "NUMBER"
  dbLookup:
    table: "CUSTOMERS"
    column: "ID"
    errorMessage: "Customer not found"
```

### 2. Code Validation
Validate against master data tables:

```yaml
- name: "Status Code"
  type: "STRING"
  transformations:
    - UPPERCASE
  dbLookup:
    table: "STATUS_CODES"
    column: "CODE"
```

### 3. Hierarchical Data
Validate parent-child relationships:

```yaml
- name: "City"
  type: "STRING"
- name: "State Code"  
  type: "STRING"
  dbLookup:
    table: "STATES"
    column: "CODE"
- name: "Country Code"
  type: "STRING"
  dbLookup:
    table: "COUNTRIES"
    column: "ISO_CODE"
```

### 4. Multi-Tenant Validation
Ensure data belongs to the correct organization:

```yaml
- name: "User Email"
  type: "EMAIL"
  dbLookup:
    table: "USERS"
    columns:
      - "EMAIL"
      - "ORGANIZATION_ID"
    errorMessage: "User not found in this organization"
```

## Important Notes

### Performance Considerations

⚠️ **Database lookups add query overhead**
- Each unique cell value triggers a database query
- Consider adding indexes on lookup columns
- Values are queried during validation, before persistence
- Performance impact increases with unique value count

### Configuration Rules

1. **Mutual Exclusivity**: You must specify EITHER `column` OR `columns`, never both
2. **Required Fields**: `table` is always required
3. **Error Messages**: If omitted, a default descriptive message is generated
4. **Case Sensitivity**: Table and column names follow your database's case rules

### Ordering

Column order in `columns` array must match the order being validated:

```yaml
# ❌ INCORRECT - order mismatch
columns:
  - "ORGANIZATION_ID"  # This will be matched with EMAIL value
  - "EMAIL"             # This will be matched with ORGANIZATION_ID value

# ✅ CORRECT - order matches
columns:
  - "EMAIL"             # Matches with EMAIL column value
  - "ORGANIZATION_ID"   # Matches with ORGANIZATION_ID column value
```

## Examples

See comprehensive examples in:
- **`docs/db-lookup-examples.yml`** - Detailed scenarios and patterns
- **`src/main/resources/excel-type-a-validations.yml`** - Working example in Users sheet

## Technical Details

### Domain Model

- **`DbLookup` record**: Represents the configuration
  - Validates that either `column` or `columns` is specified  
  - Provides `isSingleColumn()` helper method
  - Generates default error messages

### Integration Points

The feature integrates into the existing validation pipeline:
- Configured via YAML (`ColumnConfig.dbLookup`)
- Executed by `DbLookupValidator` service (to be implemented)
- Database queries via existing `DatabaseAdapter`
- Errors reported in standard `ValidationReport`

## Migration Guide

To add database lookup validation to existing configurations:

1. Identify columns that reference other tables
2. Add `dbLookup` block to column configuration
3. Test with small dataset first
4. Monitor performance and add database indexes if needed

```yaml
# Before
- name: "Department ID"
  type: "NUMBER"
  required: true

# After
- name: "Department ID"
  type: "NUMBER"
  required: true
  dbLookup:
    table: "DEPARTMENTS"
    column: "ID"
```

## Best Practices

1. **Add Indexes**: Create database indexes on frequently looked-up columns
2. **Descriptive Errors**: Provide clear error messages for business users
3. **Cache Strategy**: Consider caching for static reference data (future enhancement)
4. **Batch Modes**: Use validation-only mode for large files to verify first
5. **Test Incrementally**: Start with one lookup, verify, then add more

## Future Enhancements

Potential future improvements:
- Caching for static reference tables
- Async/batch lookup queries
- Lookup result transformations
- Cross-database lookups
- Parameterized queries with SpEL expressions
