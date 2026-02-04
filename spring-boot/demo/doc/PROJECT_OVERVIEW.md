# Spring Boot Excel Validation & Import System

**Version:** 1.0.0  
**Last Updated:** 2026-02-04  
**Java Version:** 21  
**Spring Boot Version:** 3.5.10

---

## 📋 Table of Contents

1. [Project Overview](#-project-overview)
2. [Architecture](#-architecture)
3. [Core Capabilities](#-core-capabilities)
4. [System Components](#-system-components)
5. [Data Flow](#-data-flow)
6. [Database Schema](#-database-schema)
7. [Configuration System](#-configuration-system)
8. [API Endpoints](#-api-endpoints)
9. [Validation Engine](#-validation-engine)
10. [Persistence Mechanism](#-persistence-mechanism)
11. [Running the Application](#-running-the-application)
12. [Development Standards](#-development-standards)

---

## 🎯 Project Overview

This Spring Boot application is a **configurable Excel validation and import system** designed to validate and persist Excel data into an Oracle database using YAML-driven configuration. The system enforces data integrity through comprehensive validation rules, data transformations, and foreign key reference checking (dbLookup).

### Key Characteristics

- **Configuration-Driven**: All validation rules, transformations, and database mappings are defined in YAML files
- **Hexagonal Architecture + DDD**: Clean separation of concerns with domain-driven design principles
- **Type-Safe Validation**: Support for multiple data types (STRING, NUMBER, DECIMAL, DATE, EMAIL, UUID, BOOLEAN)
- **Foreign Key Validation**: Database lookup (dbLookup) validates referential integrity before persistence
- **Flexible Persistence**: Supports both dry-run (validation only) and upsert operations
- **Comprehensive Reporting**: Detailed validation reports with metrics and execution time

---

## 🏗️ Architecture

### Hexagonal Architecture (Ports & Adapters)

The project follows hexagonal architecture with clear layering:

```
┌─────────────────────────────────────────────────────────────┐
│                      REST Layer (Adapters)                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  ExcelValidationController                          │    │
│  │  - GET/POST /api/excel/validate                     │    │
│  │  - GlobalExceptionHandler                           │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                   Application Services                       │
│  ┌────────────────────┐  ┌────────────────────────────┐     │
│  │ResourceLoaderService│ │ExcelValidationService      │     │
│  │- Load from classpath│ │- Orchestrates validation   │     │
│  │- Load from filesystem│ │- Applies transformations  │     │
│  │- Load from web      │  │- Enforces rules           │     │
│  └────────────────────┘  └────────────────────────────┘     │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  ExcelPersistenceService                              │  │
│  │  - Maps Excel columns to DB columns                   │  │
│  │  - Delegates to JdbcDatabaseAdapter                   │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                      Domain Layer                            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Domain Models (Records & Enums)                    │    │
│  │  - ValidationConfig, ValidationReport               │    │
│  │  - SheetConfig, ColumnConfig, ValidationMetrics     │    │
│  │  - DataType, TransformationType, ValidationRule     │    │
│  │  - PersistenceConfig, DatabaseMapping, DbLookup     │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Validation Services                                 │    │
│  │  - CellTransformer (TRIM, UPPERCASE, LOWERCASE...)  │    │
│  │  - CellValueValidator (NOT_EMPTY, EMAIL_FORMAT...)  │    │
│  │  - DbLookupValidator (Foreign key validation)       │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                   Infrastructure Layer                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  JdbcDatabaseAdapter                                 │    │
│  │  - Executes batch MERGE/INSERT operations           │    │
│  │  - Oracle SQL generation                             │    │
│  │  - Primary key-based upsert logic                    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Apache POI                                          │    │
│  │  - Excel file reading (XLSX)                         │    │
│  └─────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.example.demo
├── DemoApplication.java          # Main entry point
├── rest/                          # REST controllers and DTOs
│   ├── ExcelValidationController.java
│   ├── ValidationRequest.java
│   └── GlobalExceptionHandler.java
├── service/                       # Application services
│   ├── ExcelValidationService.java      # Main validation orchestrator
│   ├── ExcelPersistenceService.java     # Persistence orchestrator
│   └── JdbcDatabaseAdapter.java         # Database operations
├── validation/                    # Domain validation services
│   ├── CellTransformer.java
│   ├── CellValueValidator.java
│   └── DbLookupValidator.java
├── domain/                        # Domain models (records & enums)
│   ├── ValidationConfig.java     # Root config structure
│   ├── ValidationReport.java     # Validation result
│   ├── SheetConfig.java, ColumnConfig.java
│   ├── DataType.java, TransformationType.java
│   ├── ValidationRule.java, DbLookup.java
│   ├── PersistenceConfig.java, DatabaseMapping.java
│   └── ValidationMetrics.java, SheetMetrics.java
├── resource/                      # Resource loading
│   ├── ResourceLoaderService.java
│   ├── LoadedResource.java
│   └── SourceType.java
├── util/                          # Utilities
│   └── ExcelGenerator.java       # Generates test Excel files
└── exception/                     # Custom exceptions
    ├── ResourceNotFoundException.java
    └── MappingException.java
```

---

## 🚀 Core Capabilities

### 1. **Excel Validation**
- Validates Excel file structure (sheet names, column headers)
- Enforces data type constraints (STRING, NUMBER, DATE, EMAIL, UUID, DECIMAL, BOOLEAN)
- Validates required fields and allowed values
- Applies cell transformations before validation (TRIM, UPPERCASE, LOWERCASE)
- Checks foreign key references via database lookups (dbLookup)

### 2. **Data Persistence**
- Supports two modes:
  - **Dry Run** (`persist=false`): Validation only, no database writes
  - **Upsert** (`persist=true`): Validation + database persistence
- Uses Oracle `MERGE` statements for efficient upsert operations
- Maintains referential integrity through foreign key validation
- Batch processing for performance optimization

### 3. **Configuration-Driven Behavior**
- YAML configuration defines:
  - Sheet and column structure
  - Validation rules per column
  - Data transformations
  - Database table mappings
  - Foreign key relationships (dbLookup)

### 4. **Excel Generation**
- Utility to generate realistic test data using Datafaker
- Supports multiple faker types (email, firstname, product_name, etc.)
- Maintains foreign key relationships in generated data

---

## 🔧 System Components

### REST Layer

#### **ExcelValidationController**
- **Endpoint**: `GET/POST /api/excel/validate`
- **Responsibilities**:
  - Accept validation requests (query params or JSON body)
  - Load Excel and config resources
  - Delegate to validation service
  - Return structured validation report

**Example Usage:**
```bash
# Dry run (validation only)
GET /api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=false

# Validation + persistence
GET /api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true
```

### Application Services

#### **ExcelValidationService**
Core validation orchestrator that:
1. Loads and parses Excel files using Apache POI
2. Validates sheet and column structure against YAML config
3. Processes each row:
   - Applies cell transformations
   - Validates data types and rules
   - Performs database lookups (dbLookup)
4. Collects valid rows and error messages
5. Generates validation report with metrics

#### **ExcelPersistenceService**
Handles data persistence:
- Maps Excel columns to database columns
- Delegates to JdbcDatabaseAdapter for batch operations
- Returns persistence result (success/failure, row count)

#### **JdbcDatabaseAdapter**
Low-level database operations:
- Generates Oracle SQL (MERGE statements)
- Executes batch operations using Spring JdbcTemplate
- Handles transaction management

#### **ResourceLoaderService**
Flexible resource loading from:
- Classpath (`classpath:examples/excel/file.xlsx`)
- Filesystem (absolute or relative paths)
- Web (HTTP/HTTPS URLs)

Returns `LoadedResource` with metadata (filename, size, source type, media type)

### Validation Services

#### **CellTransformer**
Applies transformations to cell values before validation:
- `TRIM`: Remove leading/trailing whitespace
- `UPPERCASE`: Convert to uppercase
- `LOWERCASE`: Convert to lowercase
- `CAPITALIZE_WORDS`: Capitalize first letter of each word
- And more...

#### **CellValueValidator**
Validates cell values against rules:
- `NOT_NULL`, `NOT_EMPTY`
- `EMAIL_FORMAT`
- `allowedValues` (enum validation)
- Type-specific validation (NUMBER, DATE, UUID, etc.)

#### **DbLookupValidator**
Validates foreign key references:
- Queries database to check if value exists
- Returns specific error message if reference not found
- Prevents orphaned foreign key data

---

## 🔄 Data Flow

### Validation Flow (persist=false)

```
1. HTTP Request → ExcelValidationController
           ↓
2. Load Excel File → ResourceLoaderService
           ↓
3. Load YAML Config → ResourceLoaderService
           ↓
4. Parse Config → Jackson (YAML → ValidationConfig)
           ↓
5. Validate Structure → ExcelValidationService
   - Check sheet names
   - Check column headers
           ↓
6. Process Each Row → For each data row:
   a. Extract cell values
   b. Apply transformations (CellTransformer)
   c. Validate data types (CellValueValidator)
   d. Check validation rules (CellValueValidator)
   e. Perform dbLookup (DbLookupValidator)
   f. Collect valid rows & errors
           ↓
7. Generate Report → ValidationReport
   - File metadata
   - Sheet metrics (total rows, valid, invalid)
   - Error messages
   - Execution time
           ↓
8. HTTP Response → JSON ValidationReport
```

### Persistence Flow (persist=true)

```
Steps 1-6: Same as validation flow
           ↓
7. Map to DB → ExcelPersistenceService
   - Map Excel columns to DB columns
   - Apply column name mappings
           ↓
8. Batch Persist → JdbcDatabaseAdapter
   - Generate MERGE SQL statements
   - Execute batch operation
   - Return row count
           ↓
9. Generate Report → ValidationReport + PersistenceResult
           ↓
10. HTTP Response → JSON ValidationReport
```

---

## 🗄️ Database Schema

### E-Commerce Schema (Oracle)

The application demonstrates a complete e-commerce database with foreign key relationships:

```sql
-- Parent Tables (no dependencies)
CATEGORIES (category_id PK, category_name, description)
CUSTOMERS (customer_id PK, first_name, last_name, email, phone, registration_date, status)

-- Child Tables (with foreign keys)
PRODUCTS (product_id PK, product_name, category_id FK→CATEGORIES, price, stock_quantity, rating, is_active)
ORDERS (order_id PK, customer_id FK→CUSTOMERS, order_date, total_amount, status, shipping_address)

-- Grandchild Table (multiple foreign keys)
ORDER_ITEMS (order_item_id PK, order_id FK→ORDERS, product_id FK→PRODUCTS, quantity, unit_price, subtotal)
```

### Entity Relationship Diagram

```
┌────────────┐
│ CATEGORIES │
│────────────│
│ category_id│◄────┐
│ name       │     │
└────────────┘     │
                   │ FK
               ┌───┴──────┐
               │ PRODUCTS │
               │──────────│
               │product_id│◄────┐
               │category  │     │
               │price     │     │
               └──────────┘     │ FK
                                │
┌──────────┐              ┌────┴─────────┐
│CUSTOMERS │              │ ORDER_ITEMS  │
│──────────│              │──────────────│
│customer  │◄────┐        │order_item_id │
│first_name│     │        │order_id   FK─┼──┐
│email     │     │ FK     │product_id FK─┘  │
└──────────┘     │        │quantity         │
                 │        │unit_price       │
            ┌────┴───┐    └─────────────────┘
            │ ORDERS │
            │────────│
            │order_id│
            │customer│
            │date    │
            └────────┘
```

### Load Order (Critical!)

When loading data with `persist=true`, **always follow this order** to maintain referential integrity:

1. ✅ **categories** (no dependencies)
2. ✅ **customers** (no dependencies)
3. ⚠️  **products** (requires categories)
4. ⚠️  **orders** (requires customers)
5. ⚠️  **order_items** (requires orders AND products)

---

## ⚙️ Configuration System

### YAML Structure

All validation logic is defined in YAML files under `src/main/resources/validations/`:

```yaml
sheets:
  - name: "SheetName"
    columns:
      - name: "Column Name"
        required: true
        type: EMAIL              # Optional: STRING, NUMBER, DATE, EMAIL, UUID, DECIMAL, BOOLEAN
        transformations:         # Optional: Applied before validation
          - TRIM
          - UPPERCASE
        rules:                   # Validation rules
          - NOT_EMPTY
          - EMAIL_FORMAT
        allowedValues:           # Optional: Enum validation
          - "VALUE1"
          - "VALUE2"
        dbLookup:                # Optional: Foreign key validation
          table: "REFERENCED_TABLE"
          column: "referenced_column"
          errorMessage: "Custom error message"
    persistence:
      tableName: "DB_TABLE"
      upsert: true
      primaryKey: "Column Name"
      mappings:
        - excelColumn: "Excel Column"
          dbColumn: "db_column"
```

### Example: Products Configuration

```yaml
- name: "Products"
  columns:
    - name: "Product ID"
      required: true
      rules:
        - NOT_EMPTY
    - name: "Category ID"
      required: true
      rules:
        - NOT_EMPTY
      dbLookup:
        table: "CATEGORIES"
        column: "category_id"
        errorMessage: "Category ID does not exist. Please load categories first."
    - name: "Price"
      required: true
      type: DECIMAL
      rules:
        - NOT_EMPTY
  persistence:
    tableName: "PRODUCTS"
    upsert: true
    primaryKey: "Product ID"
    mappings:
      - excelColumn: "Product ID"
        dbColumn: "product_id"
      - excelColumn: "Category ID"
        dbColumn: "category_id"
```

### Configuration Features

1. **Data Types**: STRING, NUMBER, DECIMAL, DATE, EMAIL, UUID, BOOLEAN
2. **Transformations**: TRIM, UPPERCASE, LOWERCASE, CAPITALIZE_WORDS, etc.
3. **Validation Rules**: NOT_NULL, NOT_EMPTY, EMAIL_FORMAT
4. **Allowed Values**: Enum validation (list of permitted values)
5. **DB Lookup**: Foreign key validation against existing database records
6. **Persistence Mapping**: Excel column → Database column mapping
7. **Upsert Support**: Primary key-based MERGE operations

---

## 🌐 API Endpoints

### GET `/api/excel/validate`

Validates an Excel file using query parameters.

**Parameters:**
- `excelFilename` (string, required): Path to Excel file
- `validationsFilename` (string, required): Path to validation YAML
- `persist` (boolean, optional, default=false): Whether to persist data

**Example:**
```bash
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true"
```

**Response:**
```json
{
  "filename": "ecommerce-data.xlsx",
  "configFilename": "ecommerce-validation.yml",
  "status": "VALID",
  "sheetMetrics": [
    {
      "sheetName": "Categories",
      "totalRows": 10,
      "validRows": 10,
      "invalidRows": 0,
      "errors": []
    }
  ],
  "totalErrors": 0,
  "executionTime": "1.234 seconds",
  "processingMetrics": {
    "totalRowsProcessed": 10,
    "totalRowsPersisted": 10
  }
}
```

### POST `/api/excel/validate`

Validates an Excel file using JSON request body.

**Request Body:**
```json
{
  "excelFilename": "examples/excel/ecommerce-data.xlsx",
  "validationsFilename": "validations/ecommerce-validation.yml",
  "persist": true
}
```

**Response:** Same as GET endpoint

---

## ✅ Validation Engine

### Supported Data Types

| Type | Description | Example |
|------|-------------|---------|
| `STRING` | Text values | "John Doe" |
| `NUMBER` | Integer values | 123 |
| `DECIMAL` | Floating-point values | 123.45 |
| `BOOLEAN` | Boolean values | TRUE, FALSE |
| `DATE` | Date values | 2024-01-15 |
| `EMAIL` | Email addresses | user@example.com |
| `UUID` | UUIDs | 550e8400-e29b-41d4-a716-446655440000 |

### Transformations

Applied to cell values **before** validation:

| Transformation | Description |
|----------------|-------------|
| `TRIM` | Remove leading/trailing whitespace |
| `UPPERCASE` | Convert to uppercase |
| `LOWERCASE` | Convert to lowercase |
| `CAPITALIZE_WORDS` | Capitalize first letter of each word |

### Validation Rules

| Rule | Description |
|------|-------------|
| `NOT_NULL` | Cell value must not be null |
| `NOT_EMPTY` | Cell value must not be null/empty after trimming |
| `EMAIL_FORMAT` | Must be valid email format |
| `allowedValues` | Value must be in predefined list |

### Foreign Key Validation (dbLookup)

The **dbLookup** feature validates that a cell value exists in a referenced database table:

```yaml
dbLookup:
  table: "CATEGORIES"
  column: "category_id"
  errorMessage: "Category ID does not exist. Please load categories first."
```

**How it works:**
1. Extracts cell value (e.g., "CAT-001")
2. Queries database: `SELECT 1 FROM CATEGORIES WHERE category_id = ?`
3. If no match found, adds error to validation report
4. Prevents orphaned foreign key references

---

## 💾 Persistence Mechanism

### Upsert Strategy

The system uses Oracle `MERGE` statements for efficient upsert operations:

```sql
MERGE INTO PRODUCTS target
USING (SELECT ? AS product_id, ? AS product_name, ? AS category_id FROM DUAL) source
ON (target.product_id = source.product_id)
WHEN MATCHED THEN
  UPDATE SET
    product_name = source.product_name,
    category_id = source.category_id
WHEN NOT MATCHED THEN
  INSERT (product_id, product_name, category_id)
  VALUES (source.product_id, source.product_name, source.category_id)
```

### Batch Processing

- Valid rows are collected and persisted in batches
- Uses Spring `NamedParameterJdbcTemplate` for batch operations
- Optimizes database round-trips

### Dry Run Mode

When `persist=false`:
- All validation rules are executed (including dbLookup)
- No database writes occur
- Validation report shows what **would** be persisted

---

## 🏃 Running the Application

### Prerequisites

- Java 21
- Maven 3.6+
- Oracle Database (or use Docker Compose)

### Setup Database (Docker)

```bash
# Start Oracle database
docker-compose up -d

# Initialize schema
docker exec -i oracle-db sqlplus sys/password@//localhost:1521/FREEPDB1 as sysdba < init-db.sql
```

### Run Application

```bash
# Clean build (with quality gates)
mvn clean verify

# Run application
mvn spring-boot:run
```

### Generate Test Data

```bash
# Generate ecommerce-data.xlsx with 80 rows
mvn exec:java \
    -Dexec.mainClass="com.example.demo.util.ExcelGeneratorTest" \
    -Dexec.classpathScope=test \
    -Dcheckstyle.skip=true
```

### Load E-Commerce Data (Complete Flow)

```bash
# 1. Categories (parent)
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true"

# 2. Customers (parent)
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true"

# 3. Products (child of categories)
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true"

# 4. Orders (child of customers)
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true"

# 5. Order Items (child of orders & products)
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true"
```

---

## 📐 Development Standards

### Code Quality Gates

This project enforces strict quality standards:

1. **Google Java Style** (Spotless)
   - 2-space indentation
   - Enforced at compile time: `mvn spotless:check`
   - Auto-format: `mvn spotless:apply`

2. **Checkstyle** (Google Checks)
   - Enforced at validate phase
   - Fails build on warnings

3. **Code Coverage** (JaCoCo)
   - Minimum 80% instruction coverage
   - DemoApplication.class excluded
   - Enforced at test phase

### Testing Requirements

- **Every new Java class MUST have a corresponding test class (JUnit 5)**
- Tests must cover at least 80% of logic branches
- When modifying existing code, update tests first

### Javadoc Requirements

- Every public class and public method MUST have Javadoc
- Use standard Google Style Javadoc (@param, @return, @throws)
- Explain **why** the code exists, not just **what** it does

### Build Commands

```bash
# Run all quality checks
mvn clean verify

# Auto-format code
mvn spotless:apply

# Check code coverage
mvn jacoco:report
open target/site/jacoco/index.html

# Run specific test
mvn test -Dtest=ExcelValidationServiceTest
```

---

## 📚 Additional Resources

- [Validation Documentation](validation.md) - Legacy validation docs
- [Resources README](../src/main/resources/README.md) - Detailed resource structure
- [Database Schema](../init-db.sql) - Complete DDL with comments
- [Bruno API Collections](../bruno/) - API test collections (if exists)

---

## 🎓 Key Learnings & Design Decisions

### Why Hexagonal Architecture?

- **Testability**: Business logic isolated from frameworks
- **Flexibility**: Easy to swap adapters (e.g., switch from Oracle to PostgreSQL)
- **Maintainability**: Clear separation of concerns

### Why YAML Configuration?

- **Non-Technical Users**: Business analysts can modify validation rules
- **Version Control**: Configuration changes tracked in git
- **Flexibility**: No code changes required for new validation scenarios

### Why dbLookup?

- **Data Integrity**: Prevents orphaned foreign key references
- **Early Validation**: Catch errors before database constraints
- **Better UX**: Clear error messages (e.g., "Category CAT-999 does not exist")

### Why Batch Operations?

- **Performance**: Reduces database round-trips (100 rows → 1 batch instead of 100 inserts)
- **Transaction Management**: All-or-nothing semantics
- **Scalability**: Handles large Excel files efficiently

---

## 📞 Contact & Support

For questions or issues, please refer to the project repository or contact the development team.

**Happy Validating! 🎉**
