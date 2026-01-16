# Excel Transformers Reference

This project supports column-level transformations for Excel files using a YAML mapping configuration. Transformers modify cell values during processing, allowing you to normalize, format, and clean data before validation or export.

## Transformation Structure

Transformation rules are defined under the `transformations` key of a column configuration in your YAML file. Transformations are applied **in order**, allowing you to chain multiple operations.

```yaml
columns:
  - name: "Column Name"
    type: STRING | INTEGER | DECIMAL | ...
    transformations:
      - type: TRIM
      - type: UPPERCASE
```

## Available Transformers

### 1. Case Transformations

#### UPPERCASE
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Converts the entire cell value to uppercase letters.

**Example:**
```yaml
transformations:
  - type: UPPERCASE
```

**Input:** `hello world` → **Output:** `HELLO WORLD`

---

#### LOWERCASE
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Converts the entire cell value to lowercase letters.

**Example:**
```yaml
transformations:
  - type: LOWERCASE
```

**Input:** `HELLO WORLD` → **Output:** `hello world`

---

#### TITLE_CASE
**Applies to:** `STRING`

Capitalizes the first letter of each word, lowercases the rest.

**Example:**
```yaml
transformations:
  - type: TITLE_CASE
```

**Input:** `hello WORLD example` → **Output:** `Hello World Example`

---

#### SENTENCE_CASE
**Applies to:** `STRING`

Capitalizes only the first letter of the string, lowercases the rest.

**Example:**
```yaml
transformations:
  - type: SENTENCE_CASE
```

**Input:** `HELLO WORLD` → **Output:** `Hello world`

---

### 2. Whitespace Transformations

#### TRIM
**Applies to:** `STRING`

Removes leading and trailing whitespace from the cell value.

**Example:**
```yaml
transformations:
  - type: TRIM
```

**Input:** `  hello world  ` → **Output:** `hello world`

---

#### REMOVE_WHITESPACE
**Applies to:** `STRING`

Removes all whitespace characters from the cell value.

**Example:**
```yaml
transformations:
  - type: REMOVE_WHITESPACE
```

**Input:** `hello world test` → **Output:** `helloworldtest`

---

#### NORMALIZE_SPACES
**Applies to:** `STRING`

Collapses multiple consecutive spaces into a single space and trims the result.

**Example:**
```yaml
transformations:
  - type: NORMALIZE_SPACES
```

**Input:** `  hello   world  test  ` → **Output:** `hello world test`

---

### 3. Formatting Transformations

#### DATE_FORMAT
**Applies to:** `DATE`

Formats a date value according to a specified pattern using Java's `DateTimeFormatter` syntax.

**Parameters:**
- `format`: The date format pattern (e.g., `dd/MM/yyyy`, `yyyy-MM-dd`, `MMMM dd, yyyy`)

**Example:**
```yaml
transformations:
  - type: DATE_FORMAT
    format: "dd/MM/yyyy"
```

**Input:** `2024-01-15` → **Output:** `15/01/2024`

**Common Format Patterns:**

| Pattern | Example Output |
|---------|---------------|
| `yyyy-MM-dd` | 2024-01-15 |
| `dd/MM/yyyy` | 15/01/2024 |
| `MM-dd-yyyy` | 01-15-2024 |
| `MMMM dd, yyyy` | January 15, 2024 |
| `dd MMM yyyy` | 15 Jan 2024 |

---

#### NUMBER_FORMAT
**Applies to:** `INTEGER`, `DECIMAL`

Formats a numeric value using Java's `DecimalFormat` pattern.

**Parameters:**
- `format`: The number format pattern (e.g., `#,##0.00`, `000000`)

**Example:**
```yaml
transformations:
  - type: NUMBER_FORMAT
    format: "#,##0.00"
```

**Input:** `1234567.89` → **Output:** `1,234,567.89`

**Common Format Patterns:**

| Pattern | Input | Output |
|---------|-------|--------|
| `#,##0.00` | 1234.5 | 1,234.50 |
| `#,##0` | 1234.5 | 1,235 |
| `000000` | 42 | 000042 |
| `0.###` | 1.5 | 1.5 |
| `#.##%` | 0.75 | 75% |

---

### 4. Padding Transformations

#### PAD_LEFT
**Applies to:** `STRING`, `INTEGER`, `DECIMAL` (converted to string)

Pads the value on the left side to reach a minimum length.

**Parameters:**
- `length`: The minimum total length of the result
- `padChar`: The character to use for padding (default: space)

**Example:**
```yaml
transformations:
  - type: PAD_LEFT
    length: 6
    padChar: "0"
```

**Input:** `42` → **Output:** `000042`

---

#### PAD_RIGHT
**Applies to:** `STRING`, `INTEGER`, `DECIMAL` (converted to string)

Pads the value on the right side to reach a minimum length.

**Parameters:**
- `length`: The minimum total length of the result
- `padChar`: The character to use for padding (default: space)

**Example:**
```yaml
transformations:
  - type: PAD_RIGHT
    length: 10
    padChar: "X"
```

**Input:** `ABC` → **Output:** `ABCXXXXXXX`

---

### 5. String Manipulation

#### REPLACE
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Replaces all occurrences of a pattern with a replacement string. The pattern uses Java regex syntax.

**Parameters:**
- `pattern`: The regex pattern to match
- `replacement`: The string to replace matches with (default: empty string)

**Example:**
```yaml
transformations:
  - type: REPLACE
    pattern: "-"
    replacement: "_"
```

**Input:** `hello-world-test` → **Output:** `hello_world_test`

**Advanced Regex Example:**
```yaml
transformations:
  - type: REPLACE
    pattern: "[^a-zA-Z0-9]"
    replacement: ""
```

**Input:** `Hello, World! 123` → **Output:** `HelloWorld123`

---

#### STRIP_CHARS
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Removes all characters matching a regex pattern from the value.

**Parameters:**
- `pattern`: The regex pattern matching characters to remove

**Example:**
```yaml
transformations:
  - type: STRIP_CHARS
    pattern: "[0-9]"
```

**Input:** `a1b2c3d4` → **Output:** `abcd`

---

#### SUBSTRING
**Applies to:** `STRING`, `NUMERIC` (converted to string)

Extracts a portion of the string value.

**Parameters:**
- `start`: The starting index (0-based, inclusive)
- `end`: The ending index (exclusive, optional - defaults to end of string)

**Example:**
```yaml
transformations:
  - type: SUBSTRING
    start: 0
    end: 5
```

**Input:** `hello world` → **Output:** `hello`

**Extract from position to end:**
```yaml
transformations:
  - type: SUBSTRING
    start: 6
```

**Input:** `hello world` → **Output:** `world`

---

## Chaining Transformations

Transformations are applied in the order they are defined. This allows you to build powerful data cleaning pipelines.

**Example: Clean and format a name field**
```yaml
columns:
  - name: "Full Name"
    type: STRING
    transformations:
      - type: TRIM                 # Step 1: Remove outer whitespace
      - type: NORMALIZE_SPACES     # Step 2: Fix internal spacing
      - type: TITLE_CASE           # Step 3: Proper capitalization
```

**Input:** `  john   DOE  ` → **Output:** `John Doe`

---

**Example: Standardize email addresses**
```yaml
columns:
  - name: "Email"
    type: EMAIL
    transformations:
      - type: TRIM
      - type: LOWERCASE
```

**Input:** `  John.Doe@EXAMPLE.COM  ` → **Output:** `john.doe@example.com`

---

**Example: Format product codes**
```yaml
columns:
  - name: "Product Code"
    type: STRING
    transformations:
      - type: TRIM
      - type: UPPERCASE
      - type: PAD_LEFT
        length: 8
        padChar: "0"
```

**Input:** `  abc123  ` → **Output:** `00ABC123`

---

## Combining Transformations with Validation

Transformations are applied **before** `allowedValues` and `excludedValues` validation. This allows you to normalize data first, then validate the normalized result.

> [!IMPORTANT]
> **Validation Order:**
> 1. Type validation (STRING, DATE, etc.) - on raw cell
> 2. Regex, length, range validation - on raw cell
> 3. **Transformations applied**
> 4. `allowedValues` / `excludedValues` - on **transformed** value

**Example:**
```yaml
columns:
  - name: "Status"
    type: STRING
    transformations:
      - type: TRIM
      - type: UPPERCASE
    validation:
      allowedValues:
        - "ACTIVE"
        - "INACTIVE"
        - "PENDING"
```

This configuration will:
1. Check that the cell is a STRING
2. Trim whitespace from the input
3. Convert to uppercase (`"active"` → `"ACTIVE"`)
4. Validate that `"ACTIVE"` is in the allowed values ✅

---

## Java API Usage

### Using the Transformation Service

```java
@Autowired
private ExcelTransformationUseCase transformationUseCase;

public void processExcel() throws IOException {
    ExcelTransformationResult result = transformationUseCase.transformExcel(
        "data.xlsx", 
        "data_mapping.yml"
    );
    
    for (SheetTransformationResult sheet : result.sheets()) {
        System.out.println("Sheet: " + sheet.sheetName());
        
        for (TransformedRow row : sheet.transformedRows()) {
            // Access transformed values by column name
            String email = row.values().get("Email");  // Already lowercase & trimmed
            String name = row.values().get("Full Name");  // Already in Title Case
            
            System.out.printf("Row %d: %s - %s%n", 
                row.rowNumber(), name, email);
        }
    }
}
```

### Programmatic Transformation (without YAML)

```java
CellTransformer transformer = new CellTransformer();

List<ColumnTransformation> transformations = List.of(
    ColumnTransformation.of(TransformerType.TRIM),
    ColumnTransformation.of(TransformerType.LOWERCASE)
);

String result = transformer.transform(cell, transformations);
```

---

## Quick Reference Table

| Transformer | Description | Parameters |
|-------------|-------------|------------|
| `UPPERCASE` | Convert to uppercase | - |
| `LOWERCASE` | Convert to lowercase | - |
| `TRIM` | Remove leading/trailing whitespace | - |
| `TITLE_CASE` | Capitalize first letter of each word | - |
| `SENTENCE_CASE` | Capitalize only first letter | - |
| `REMOVE_WHITESPACE` | Remove all whitespace | - |
| `NORMALIZE_SPACES` | Collapse multiple spaces | - |
| `DATE_FORMAT` | Format date values | `format` |
| `NUMBER_FORMAT` | Format numeric values | `format` |
| `REPLACE` | Replace pattern with string | `pattern`, `replacement` |
| `PAD_LEFT` | Pad on left | `length`, `padChar` |
| `PAD_RIGHT` | Pad on right | `length`, `padChar` |
| `SUBSTRING` | Extract substring | `start`, `end` |
| `STRIP_CHARS` | Remove matching chars | `pattern` |
