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
          - name: "Status"
            type: STRING
            skipIf: ["Inactive"]      # Optional: Skip row if value matches
            dbMapping:
              dbColumn: "STATUS"
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

          - name: "Discontinued"
            type: STRING
            skipIf: ["Yes", "true"]

```

## Dynamic Skip Expressions (SpEL)

Use **SpEL (Spring Expression Language)** for powerful skip logic. You can define expressions at the **Sheet** level (to check multiple columns or DB state) or at the **Column** level.

---

### 1. Sheet-Level Expressions (`skipExpression`)

Defined at the root of a sheet config. Can access **all column values** and **database checks**.

```yaml
sheets:
  - name: "Employees"
    table: "users"
    # Skip if Status is 'Archived' OR if email already exists in DB
    skipExpression: "#Status == 'Archived' || #db.exists('users', 'email', #Email)"
    columns:
      - name: "Status"
      - name: "Email" 
        dbMapping: { dbColumn: "email" }
```

#### Available Variables (Sheet Context)
| Variable | Description |
| :--- | :--- |
| `#ColName` | Access any column value by its **name** (case-sensitive). e.g., `#Status`, `#Email`. |
| `#db` | **Database Helper** for lookups and existence checks. |
| `#dateTime` | Date utility helper. |
| `#rowValues` | `Map<String, Object>` containing all extracted column values. |

#### Database Helper Methods (`#db`)
| Method | Example | Description |
| :--- | :--- | :--- |
| `exists(table, col, val)` | `#db.exists('users', 'id', #UserID)` | Returns `true` if a row exists with that value. |
| `lookup(table, matchCol, val, returnCol)` | `#db.lookup('codes', 'code', #Code, 'id')` | Returns a value from another table (or `null`). |

---

### 3. Multiple Expressions (`skipExpressions`)
   
   You can provide a list of expressions. If **ANY** expression evaluates to `true`, the row (or column) is skipped.
   
   ```yaml
   sheets:
     - name: "Employees"
       skipExpressions:
         - "#Status == 'Archived'"
         - "#db.exists('users', 'email', #Email)"
   ```
   
   ---
   
   ### 4. Column-Level Expressions (`skipExpression`)
   
   Defined inside a column. Accesses only the **current cell value** (`#root`).
   
   ```yaml
   columns:
     - name: "Quantity"
       type: INTEGER
       skipExpression: "#root <= 0"  # Skip row if Quantity is 0 or negative
   ```
   
   #### Available Variables (Column Context)
   | Variable | Description |
   | :--- | :--- |
   | `#root` | The typed value of the current cell. |
   | `#dateTime` | Date utility helper. |


---

### 3. Expression Examples

#### A. Basic Logic
| Goal | Expression |
| :--- | :--- |
| **Simple Equality** | `#Status == 'Inactive'` |
| **Numeric Range** | `#Age < 18 || #Age > 65` |
| **Empty Check** | `#Comments == null || #Comments == ''` |
| **Safe Navigation** | `#Description?.length() > 100` |

#### B. String Operations
| Goal | Expression |
| :--- | :--- |
| **Starts With** | `#Sku.startsWith('TEST-')` |
| **Contains** | `#Notes.contains('DELETE')` |
| **Regex Match** | `#Code matches '^[A-Z]{3}-\d{2}$'` |
| **Case Insensitive** | `#Status.equalsIgnoreCase('failed')` |

#### C. Date Logic (using `#dateTime`)
| Goal | Expression |
| :--- | :--- |
| **Future Date** | `#ExpiryDate.isAfter(#dateTime.now())` |
| **Past Date** | `#CreatedDate.isBefore(#dateTime.yearStart(2023))` |
| **Specific Year** | `#JoinDate.getYear() == 2024` |

#### D. Database Checks (Sheet Level Only)
| Goal | Expression |
| :--- | :--- |
| **Skip Existing** | `#db.exists('customers', 'email', #Email)` |
| **Cross-Table Check** | `#db.lookup('products', 'sku', #Sku, 'status') == 'DISCONTINUED'` |
| **Complex Logic** | `!#db.exists('parents', 'id', #ParentID) && #Type == 'Child'` |

#### E. Multi-Column Logic (Sheet Level Only)
| Goal | Expression |
| :--- | :--- |
| **Dependent Fields** | `#Type == 'Business' && #TaxID == null` |
| **Total Check** | `(#Price * #Quantity) < 10.00` |
| **Validation Skip** | `#Status == 'Draft' && #PublishDate != null` |

### ❌ Security / Blocked
*   No **static methods**: `T(java.lang.Math)` ⛔
*   No **constructors**: `new java.util.Date()` ⛔
*   No **bean references**: `@userService` ⛔
*   No **assignments**: `#root = 'val'` ⛔
