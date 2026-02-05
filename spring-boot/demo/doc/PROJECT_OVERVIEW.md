# Spring Boot Excel Validation & Import System

**Version:** 2.0.0
**Last Updated:** 2026-02-05
**Java Version:** 21
**Spring Boot Version:** 3.5.10

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Core Capabilities](#core-capabilities)
4. [System Components](#system-components)
5. [Data Flow](#data-flow)
6. [Database Schema](#database-schema)
7. [Configuration System](#configuration-system)
8. [API Endpoints](#api-endpoints)
9. [Validation Engine](#validation-engine)
10. [Persistence Mechanism](#persistence-mechanism)
11. [Running the Application](#running-the-application)
12. [Development Standards](#development-standards)

---

## Project Overview

This Spring Boot application is a **configurable Excel validation and import system** designed to validate and persist Excel data into an Oracle database using YAML-driven configuration. The system enforces data integrity through comprehensive validation rules, data transformations, and foreign key reference checking (dbLookup).

### Key Characteristics

- **Configuration-Driven**: All validation rules, transformations, and database mappings are defined in YAML files
- **Hexagonal Architecture + DDD**: Clean separation of concerns with domain-driven design principles
- **Type-Safe Validation**: Support for multiple data types (STRING, NUMBER, DECIMAL, DATE, EMAIL, UUID, BOOLEAN)
- **Foreign Key Validation**: Database lookup (dbLookup) validates referential integrity before persistence
- **Flexible Persistence**: Supports both dry-run (validation only) and upsert operations
- **Comprehensive Reporting**: Detailed validation reports with metrics

---

## Architecture

### Hexagonal Architecture (Ports & Adapters)

The project follows hexagonal architecture with clear layering:

```
+-----------------------------------------------------------------------+
|                        ADAPTERS (Inbound)                              |
|  +------------------------------------------------------------------+ |
|  |  adapters/inbound/rest/                                          | |
|  |  - ExcelValidationController (GET/POST /api/excel/validate)      | |
|  |  - ValidationRequest (DTO)                                        | |
|  |  - GlobalExceptionHandler                                         | |
|  +------------------------------------------------------------------+ |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                          CORE (Business Logic)                         |
|  +------------------------------------------------------------------+ |
|  |  core/ports/inbound/                                             | |
|  |  - ExcelValidationPort (interface for validation use case)       | |
|  +------------------------------------------------------------------+ |
|                                    |                                   |
|  +------------------------------------------------------------------+ |
|  |  core/internal/usecases/                                         | |
|  |  - ValidateExcelUseCase (orchestrates validation workflow)       | |
|  +------------------------------------------------------------------+ |
|                                    |                                   |
|  +------------------------------------------------------------------+ |
|  |  core/internal/services/                                         | |
|  |  - ExcelSheetProcessorService (sheet-level processing)           | |
|  |  - ExcelRowValidatorService (row-level validation)               | |
|  |  - CellTransformerService (TRIM, UPPERCASE, LOWERCASE)           | |
|  |  - CellValidatorService (NOT_EMPTY, EMAIL_FORMAT, type checks)   | |
|  |  - DbLookupService (foreign key validation)                      | |
|  |  - RowOperationService (computed columns)                        | |
|  |  - ExcelPersistenceService (maps Excel to DB columns)            | |
|  +------------------------------------------------------------------+ |
|                                    |                                   |
|  +------------------------------------------------------------------+ |
|  |  core/internal/domain/                                           | |
|  |  - config/ (ValidationConfig, SheetConfig, ColumnConfig, etc.)   | |
|  |  - result/ (ValidationReport, SheetMetrics, PersistenceResult)   | |
|  |  - enums/ (DataType, ValidationRule, TransformationType, etc.)   | |
|  +------------------------------------------------------------------+ |
|                                    |                                   |
|  +------------------------------------------------------------------+ |
|  |  core/ports/outbound/                                            | |
|  |  - DatabasePort (interface for DB operations)                    | |
|  |  - ResourceLoaderPort (interface for resource loading)           | |
|  |  - ConfigLoaderPort (interface for config parsing)               | |
|  +------------------------------------------------------------------+ |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                        ADAPTERS (Outbound)                             |
|  +------------------------------------------------------------------+ |
|  |  adapters/outbound/persistence/                                  | |
|  |  - JdbcDatabaseAdapter (implements DatabasePort)                 | |
|  |    - Oracle MERGE/INSERT operations                              | |
|  |    - Batch processing                                            | |
|  +------------------------------------------------------------------+ |
|  +------------------------------------------------------------------+ |
|  |  adapters/outbound/resource/                                     | |
|  |  - SpringResourceLoaderAdapter (implements ResourceLoaderPort)   | |
|  |    - Classpath, filesystem, web resource loading                 | |
|  |  - YamlConfigLoaderAdapter (implements ConfigLoaderPort)         | |
|  |    - YAML parsing with Jackson                                   | |
|  +------------------------------------------------------------------+ |
+-----------------------------------------------------------------------+
```

### Package Structure

```
com.example.demo
├── DemoApplication.java                    # Main entry point
├── adapters/
│   ├── inbound/
│   │   └── rest/                           # REST API controllers
│   │       ├── ExcelValidationController.java
│   │       ├── ValidationRequest.java
│   │       └── GlobalExceptionHandler.java
│   └── outbound/
│       ├── persistence/                    # Database adapter
│       │   └── JdbcDatabaseAdapter.java
│       └── resource/                       # Resource loading adapters
│           ├── SpringResourceLoaderAdapter.java
│           └── YamlConfigLoaderAdapter.java
├── config/
│   └── BeanConfiguration.java              # Spring bean wiring
├── core/
│   ├── exceptions/                         # Domain exceptions
│   │   ├── ResourceNotFoundException.java
│   │   └── MappingException.java
│   ├── internal/
│   │   ├── domain/
│   │   │   ├── config/                     # Configuration domain objects
│   │   │   │   ├── ValidationConfig.java
│   │   │   │   ├── SheetConfig.java
│   │   │   │   ├── ColumnConfig.java
│   │   │   │   ├── PersistenceConfig.java
│   │   │   │   ├── DatabaseMapping.java
│   │   │   │   ├── DbLookup.java
│   │   │   │   ├── Operation.java
│   │   │   │   └── RowOperation.java
│   │   │   ├── result/                     # Result domain objects
│   │   │   │   ├── ValidationReport.java
│   │   │   │   ├── ValidationMetrics.java
│   │   │   │   ├── SheetMetrics.java
│   │   │   │   ├── PersistenceResult.java
│   │   │   │   ├── RowValidationResult.java
│   │   │   │   └── FileMetadata.java
│   │   │   └── enums/                      # Domain enumerations
│   │   │       ├── DataType.java
│   │   │       ├── TransformationType.java
│   │   │       ├── ValidationRule.java
│   │   │       ├── OperationType.java
│   │   │       └── ValidationStatus.java
│   │   ├── services/                       # Domain services
│   │   │   ├── ExcelSheetProcessorService.java
│   │   │   ├── ExcelRowValidatorService.java
│   │   │   ├── CellTransformerService.java
│   │   │   ├── CellValidatorService.java
│   │   │   ├── DbLookupService.java
│   │   │   ├── RowOperationService.java
│   │   │   └── ExcelPersistenceService.java
│   │   ├── usecases/                       # Use case implementations
│   │   │   └── ValidateExcelUseCase.java
│   │   └── valueobjects/                   # Value objects
│   │       ├── LoadedResource.java
│   │       └── SourceType.java
│   └── ports/
│       ├── inbound/                        # Inbound port interfaces
│       │   └── ExcelValidationPort.java
│       └── outbound/                       # Outbound port interfaces
│           ├── DatabasePort.java
│           ├── ResourceLoaderPort.java
│           └── ConfigLoaderPort.java
```

---

## Core Capabilities

### 1. **Excel Validation**
- Validates Excel file structure (sheet names, column headers)
- Enforces data type constraints (STRING, NUMBER, DATE, EMAIL, UUID, DECIMAL, BOOLEAN)
- Validates required fields and allowed values
- Applies cell transformations before validation (TRIM, UPPERCASE, LOWERCASE)
- Checks foreign key references via database lookups (dbLookup)
- Supports computed columns via row operations (CONCATENATE, REPLACE, SUBSTRING)

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
  - Row operations (computed columns)
  - Database table mappings
  - Foreign key relationships (dbLookup)

---

## System Components

### Inbound Adapters (REST Layer)

#### **ExcelValidationController**
- **Location**: `adapters/inbound/rest/ExcelValidationController.java`
- **Endpoint**: `GET/POST /api/excel/validate`
- **Responsibilities**:
  - Accept validation requests (query params or JSON body)
  - Load Excel and config resources via ports
  - Delegate to validation use case
  - Return structured validation report

**Example Usage:**
```bash
# Dry run (validation only)
GET /api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=false

# Validation + persistence
GET /api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/ecommerce-validation.yml&persist=true
```

### Core - Use Cases

#### **ValidateExcelUseCase**
- **Location**: `core/internal/usecases/ValidateExcelUseCase.java`
- **Implements**: `ExcelValidationPort`
- **Responsibilities**:
  1. Load YAML configuration via `ConfigLoaderPort`
  2. Open Excel workbook
  3. Process each sheet via `ExcelSheetProcessorService`
  4. Aggregate metrics and build `ValidationReport`

### Core - Domain Services

#### **ExcelSheetProcessorService**
- **Location**: `core/internal/services/ExcelSheetProcessorService.java`
- Processes individual Excel sheets
- Builds header column mappings
- Validates sheet structure
- Delegates row validation to `ExcelRowValidatorService`
- Coordinates persistence via `ExcelPersistenceService`

#### **ExcelRowValidatorService**
- **Location**: `core/internal/services/ExcelRowValidatorService.java`
- Four-phase row validation:
  1. **Transform & Extract**: Apply transformations, extract cell values
  2. **Compute Columns**: Execute row operations for computed columns
  3. **Validate**: Run validation rules, check foreign keys
  4. **Type Conversion**: Convert strings to target types

#### **CellTransformerService**
- **Location**: `core/internal/services/CellTransformerService.java`
- Applies transformations to cell values before validation:
  - `TRIM`: Remove leading/trailing whitespace
  - `UPPERCASE`: Convert to uppercase
  - `LOWERCASE`: Convert to lowercase

#### **CellValidatorService**
- **Location**: `core/internal/services/CellValidatorService.java`
- Validates cell values against rules:
  - `NOT_NULL`, `NOT_EMPTY`
  - `EMAIL_FORMAT`
  - `allowedValues` (enum validation)
  - Type-specific validation (NUMBER, DATE, UUID, etc.)

#### **DbLookupService**
- **Location**: `core/internal/services/DbLookupService.java`
- Validates foreign key references:
  - Queries database to check if value exists
  - Supports single-column and composite-key lookups
  - Returns specific error message if reference not found

#### **RowOperationService**
- **Location**: `core/internal/services/RowOperationService.java`
- Executes row-level operations to compute new columns:
  - `CONCATENATE`: Join multiple columns
  - `REPLACE`: Replace patterns
  - `SUBSTRING`: Extract substring
  - `UPPERCASE`, `LOWERCASE`, `TRIM`

#### **ExcelPersistenceService**
- **Location**: `core/internal/services/ExcelPersistenceService.java`
- Maps Excel columns to database columns
- Delegates to `DatabasePort` for batch operations

### Outbound Adapters

#### **JdbcDatabaseAdapter**
- **Location**: `adapters/outbound/persistence/JdbcDatabaseAdapter.java`
- **Implements**: `DatabasePort`
- Low-level database operations:
  - Generates Oracle SQL (MERGE statements)
  - Executes batch operations using Spring JdbcTemplate
  - Performs database lookups for foreign key validation

#### **SpringResourceLoaderAdapter**
- **Location**: `adapters/outbound/resource/SpringResourceLoaderAdapter.java`
- **Implements**: `ResourceLoaderPort`
- Flexible resource loading from:
  - Classpath (`classpath:examples/excel/file.xlsx`)
  - Filesystem (absolute or relative paths)
  - Web (HTTP/HTTPS URLs)

#### **YamlConfigLoaderAdapter**
- **Location**: `adapters/outbound/resource/YamlConfigLoaderAdapter.java`
- **Implements**: `ConfigLoaderPort`
- Parses YAML validation configuration using Jackson

### Ports (Interfaces)

#### **ExcelValidationPort** (Inbound)
- **Location**: `core/ports/inbound/ExcelValidationPort.java`
- Defines the contract for Excel validation use case
- Implemented by `ValidateExcelUseCase`

#### **DatabasePort** (Outbound)
- **Location**: `core/ports/outbound/DatabasePort.java`
- Defines database operations:
  - `persist()`: Batch insert/upsert
  - `lookup()`: Single-column foreign key check
  - `lookupComposite()`: Composite-key foreign key check

#### **ResourceLoaderPort** (Outbound)
- **Location**: `core/ports/outbound/ResourceLoaderPort.java`
- Defines resource loading contract
- Returns `LoadedResource` with metadata

#### **ConfigLoaderPort** (Outbound)
- **Location**: `core/ports/outbound/ConfigLoaderPort.java`
- Defines configuration parsing contract
- Returns `ValidationConfig` domain object

---

## Data Flow

### Validation Flow (persist=false)

```
1. HTTP Request -> ExcelValidationController
           |
2. Load Resources via ResourceLoaderPort
           |
3. Delegate to ExcelValidationPort.validate()
           |
4. ValidateExcelUseCase orchestrates:
   a. Load config via ConfigLoaderPort
   b. Open Excel workbook
   c. For each sheet -> ExcelSheetProcessorService:
      - Build header map
      - Validate structure
      - For each row -> ExcelRowValidatorService:
        i.   Apply transformations (CellTransformerService)
        ii.  Execute row operations (RowOperationService)
        iii. Validate cells (CellValidatorService)
        iv.  Check foreign keys (DbLookupService)
        v.   Convert types
      - Collect valid rows and errors
           |
5. Build ValidationReport with metrics
           |
6. HTTP Response -> JSON ValidationReport
```

### Persistence Flow (persist=true)

```
Steps 1-4: Same as validation flow
           |
5. For each sheet with valid rows:
   ExcelPersistenceService:
   a. Map Excel columns to DB columns
   b. Delegate to DatabasePort.persist()
           |
6. JdbcDatabaseAdapter:
   a. Generate MERGE SQL
   b. Execute batch operation
   c. Return PersistenceResult
           |
7. Build ValidationReport with persistence results
           |
8. HTTP Response -> JSON ValidationReport
```

---

## Database Schema

### E-Commerce Schema (Oracle)

The application demonstrates a complete e-commerce database with foreign key relationships:

```sql
-- Parent Tables (no dependencies)
CATEGORIES (category_id PK, category_name, description)
CUSTOMERS (customer_id PK, first_name, last_name, email, phone, registration_date, status)

-- Child Tables (with foreign keys)
PRODUCTS (product_id PK, product_name, category_id FK->CATEGORIES, price, stock_quantity, rating, is_active)
ORDERS (order_id PK, customer_id FK->CUSTOMERS, order_date, total_amount, status, shipping_address)

-- Grandchild Table (multiple foreign keys)
ORDER_ITEMS (order_item_id PK, order_id FK->ORDERS, product_id FK->PRODUCTS, quantity, unit_price, subtotal)
```

### Load Order (Critical!)

When loading data with `persist=true`, **always follow this order** to maintain referential integrity:

1. **categories** (no dependencies)
2. **customers** (no dependencies)
3. **products** (requires categories)
4. **orders** (requires customers)
5. **order_items** (requires orders AND products)

---

## Configuration System

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
    rowOperations:               # Optional: Computed columns
      - targetColumn: "Full Name"
        operations:
          - type: CONCATENATE
            sourceColumns: ["First Name", "Last Name"]
            separator: " "
    persistence:
      tableName: "DB_TABLE"
      upsert: true
      primaryKey: "Column Name"
      mappings:
        - excelColumn: "Excel Column"
          dbColumn: "db_column"
```

### Configuration Features

1. **Data Types**: STRING, NUMBER, DECIMAL, DATE, EMAIL, UUID, BOOLEAN
2. **Transformations**: TRIM, UPPERCASE, LOWERCASE
3. **Validation Rules**: NOT_NULL, NOT_EMPTY, EMAIL_FORMAT
4. **Allowed Values**: Enum validation (list of permitted values)
5. **DB Lookup**: Foreign key validation against existing database records
6. **Row Operations**: Computed columns (CONCATENATE, REPLACE, SUBSTRING, UPPERCASE, LOWERCASE, TRIM)
7. **Persistence Mapping**: Excel column -> Database column mapping
8. **Upsert Support**: Primary key-based MERGE operations

---

## API Endpoints

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

**Response:**
```json
{
  "status": "SUCCESS",
  "excelMetadata": {
    "filename": "ecommerce-data.xlsx",
    "size": 12345,
    "sourceType": "CLASSPATH"
  },
  "rulesMetadata": {
    "filename": "ecommerce-validation.yml",
    "size": 2048,
    "sourceType": "CLASSPATH"
  },
  "globalMetrics": {
    "totalRows": 100,
    "validRows": 95,
    "invalidRows": 5,
    "persistedRows": 95
  },
  "sheetMetrics": [
    {
      "sheetName": "Categories",
      "totalRows": 10,
      "validRows": 10,
      "invalidRows": 0,
      "errors": [],
      "persistence": {
        "success": true,
        "rowsAffected": 10,
        "generatedSql": "MERGE INTO CATEGORIES..."
      }
    }
  ],
  "errors": []
}
```

---

## Validation Engine

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

### Validation Rules

| Rule | Description |
|------|-------------|
| `NOT_NULL` | Cell value must not be null |
| `NOT_EMPTY` | Cell value must not be null/empty after trimming |
| `EMAIL_FORMAT` | Must be valid email format |
| `allowedValues` | Value must be in predefined list |

### Row Operations (Computed Columns)

| Operation | Description | Parameters |
|-----------|-------------|------------|
| `CONCATENATE` | Join values from multiple columns | sourceColumns, separator |
| `REPLACE` | Replace pattern with replacement | pattern, replacement |
| `SUBSTRING` | Extract substring | startIndex, endIndex |
| `UPPERCASE` | Convert to uppercase | - |
| `LOWERCASE` | Convert to lowercase | - |
| `TRIM` | Remove whitespace | - |

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

## Persistence Mechanism

### Upsert Strategy

The system uses Oracle `MERGE` statements for efficient upsert operations:

```sql
MERGE INTO PRODUCTS target
USING (SELECT :product_id AS product_id, :product_name AS product_name, :category_id AS category_id FROM DUAL) source
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

## Running the Application

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

---

## Development Standards

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
mvn test -Dtest=ValidateExcelUseCaseTest
```

---

## Key Design Decisions

### Why Hexagonal Architecture?

- **Testability**: Business logic isolated from frameworks
- **Flexibility**: Easy to swap adapters (e.g., switch from Oracle to PostgreSQL)
- **Maintainability**: Clear separation of concerns
- **Dependency Inversion**: Core depends on abstractions (ports), not implementations

### Why Ports and Adapters?

- **Inbound Ports**: Define what the application CAN DO (use cases)
- **Outbound Ports**: Define what the application NEEDS (external services)
- **Adapters**: Implement the technical details (REST, JDBC, YAML parsing)

### Why DDD Principles?

- **Domain Objects**: Immutable records representing business concepts
- **Value Objects**: Encapsulate related data (LoadedResource, SourceType)
- **Domain Services**: Business logic without framework dependencies
- **Use Cases**: Application-level orchestration

### Why YAML Configuration?

- **Non-Technical Users**: Business analysts can modify validation rules
- **Version Control**: Configuration changes tracked in git
- **Flexibility**: No code changes required for new validation scenarios

---

## Additional Resources

- [Validation Documentation](validation.md) - Extending transformations and rules
- [Resources README](../src/main/resources/README.md) - Resource structure
- [Database Schema](../init-db.sql) - Complete DDL with comments

---

**Happy Validating!**
