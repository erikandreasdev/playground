# Resources Directory Structure

This directory contains all application configuration files, validation schemas, example data, and generator configurations.

## 📁 Directory Overview

```
src/main/resources/
├── application.yml              # Spring Boot application configuration
├── examples/                    # Example and demo files
│   ├── excel/                  # Sample Excel files for testing
│   └── validations/            # Legacy/example validation configurations
├── generator/                   # Excel file generator configurations
└── validations/                 # Active validation schemas for production use
```

---

## 📋 Directory Details

### 🔧 **application.yml**
Main Spring Boot configuration file containing:
- Server port settings
- Database connection properties
- Application-specific configurations
- Spring profiles

### 📊 **examples/**
Contains sample and demonstration files for testing and learning.

#### **examples/excel/**
Sample Excel files that can be used to test the validation and import features:
- `ecommerce-data.xlsx` - Generated e-commerce dataset with 5 sheets (Categories, Customers, Products, Orders, Order Items)
- `excel-type-a.xlsx` - Legacy example Excel file for testing basic validations

**Usage:**
```bash
# Test validation with ecommerce data
GET http://localhost:8080/api/excel/validate?
    excelFilename=examples/excel/ecommerce-data.xlsx&
    validationsFilename=validations/categories.yml&
    persist=false
```

#### **examples/validations/**
Legacy or example validation configuration files kept for reference:
- `excel-type-a-validations.yml` - Original example validation configuration
- `products.yml` - Simple product validation example
- `transactions.yml` - Transaction validation example

These files demonstrate different validation patterns but are not actively used in production.

---

### 🏭 **generator/**
Contains YAML configurations for the Excel Generator utility.

#### **Purpose**
Define how to generate Excel files with realistic fake data using Datafaker.

#### **Configuration Format**
```yaml
filename: output-file.xlsx
sheets:
  - name: SheetName
    rowCount: 100
    columns:
      - name: ColumnName
        fakerType: email  # or: firstname, number, date, etc.
        format: "yyyy-MM-dd"  # optional
        allowedValues: ["A", "B", "C"]  # optional
        min: 1  # optional for numbers
        max: 100  # optional for numbers
```

#### **Available Files**
- `ecommerce-generator.yml` - Configuration for generating e-commerce test data with proper foreign key relationships

#### **Usage**
```java
// Run the generator from code
ExcelGenerator generator = new ExcelGenerator();
generator.generateFromConfig(
    "generator/ecommerce-generator.yml",
    "output/my-data.xlsx"
);

// Or run from command line
mvn exec:java \
    -Dexec.mainClass="com.example.demo.util.ExcelGeneratorTest" \
    -Dexec.classpathScope=test
```

#### **Supported Faker Types**
- **IDs**: `id`, `uuid`, `sequence_id`
- **Names**: `firstname`, `lastname`, `fullname`
- **Internet**: `email`, `phone`
- **Address**: `city`, `country`, `address`
- **Commerce**: `product_name`, `department`, `price`, `category`
- **Numbers**: `number`, `decimal`, `rating`
- **Boolean**: `boolean`
- **Dates**: `date`, `past_date`
- **Status**: `status`, `order_status`

---

### ✅ **validations/**
Contains active validation schemas used by the Excel Validation Service.

#### **Purpose**
Define validation rules for Excel file imports, including:
- Column structure validation
- Data type checking
- Business rule enforcement
- Database foreign key validation (dbLookup)

#### **Configuration Format**
```yaml
validationName: ValidationName
tableName: TABLE_NAME
sheetName: SheetName
columns:
  - excelColumnName: ColumnName
    dbColumnName: db_column_name
    dataType: STRING  # STRING, NUMBER, DATE, EMAIL, UUID, DECIMAL, BOOLEAN
    required: true
    transformations:
      - type: TRIM
      - type: UPPERCASE
    validations:
      - type: allowedValues
        allowedValues: ["VALUE1", "VALUE2"]
      - type: dbLookup
        tableName: REFERENCED_TABLE
        columnName: referenced_column
```

#### **Available Validation Files**

##### **E-commerce Schema** (with dbLookup support)
The following validation files work together to validate an e-commerce dataset with foreign key relationships:

1. **`categories.yml`** - Parent table (no dependencies)
   - Validates product categories
   - Fields: category_id, category_name, description

2. **`customers.yml`** - Parent table (no dependencies)
   - Validates customer information
   - Fields: customer_id, first_name, last_name, email, city, status
   - Includes email validation and status enum

3. **`products.yml`** - Child of CATEGORIES
   - Validates product catalog
   - Fields: product_id, product_name, sku, category_id, price, stock_quantity, status
   - **Uses dbLookup** to validate category_id against CATEGORIES table

4. **`orders.yml`** - Child of CUSTOMERS
   - Validates order information
   - Fields: order_id, customer_id, order_date, total_amount, status
   - **Uses dbLookup** to validate customer_id against CUSTOMERS table

5. **`order_items.yml`** - Child of ORDERS and PRODUCTS
   - Validates order line items
   - Fields: order_item_id, order_id, product_id, quantity, unit_price, subtotal
   - **Uses multiple dbLookup** validations for order_id and product_id

#### **Load Order (IMPORTANT)**
When loading data with foreign key relationships, **always follow this order**:

```
1. categories.yml    (parent)
2. customers.yml     (parent)
3. products.yml      (child of categories)
4. orders.yml        (child of customers)
5. order_items.yml   (child of orders & products)
```

#### **API Usage Examples**

**Validate Only (Dry Run)**
```bash
GET http://localhost:8080/api/excel/validate?
    excelFilename=examples/excel/ecommerce-data.xlsx&
    validationsFilename=validations/categories.yml&
    persist=false
```

**Validate and Import**
```bash
GET http://localhost:8080/api/excel/validate?
    excelFilename=examples/excel/ecommerce-data.xlsx&
    validationsFilename=validations/categories.yml&
    persist=true
```

#### **dbLookup Validation Feature**
The `dbLookup` validation type checks if a value exists in a referenced database table:

```yaml
validations:
  - type: dbLookup
    tableName: CATEGORIES
    columnName: category_id
```

**How it works:**
1. Checks if the value exists in the specified table/column
2. Returns validation error if the value is not found
3. Prevents orphaned foreign key references
4. Works seamlessly with the persist=true mode

---

## 🚀 Quick Start Guide

### 1. Generate Sample Data
```bash
# Run the Excel generator
mvn exec:java \
    -Dexec.mainClass="com.example.demo.util.ExcelGeneratorTest" \
    -Dexec.classpathScope=test \
    -Dcheckstyle.skip=true
```

### 2. Start the Application
```bash
mvn spring-boot:run
```

### 3. Load E-commerce Data (in order)
```bash
# 1. Load categories
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/categories.yml&persist=true"

# 2. Load customers
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/customers.yml&persist=true"

# 3. Load products
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/products.yml&persist=true"

# 4. Load orders
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/orders.yml&persist=true"

# 5. Load order items
curl "http://localhost:8080/api/excel/validate?excelFilename=examples/excel/ecommerce-data.xlsx&validationsFilename=validations/order_items.yml&persist=true"
```

---

## 📚 Additional Documentation

- **Database Schema**: See `/docs/db-lookup-example.md` for detailed ER diagrams and relationships
- **API Documentation**: See `/docs/cheatsheet.md` for complete API reference
- **Testing Guide**: See `/bruno/` for API test collections

---

## 🔍 Validation Features

### Supported Data Types
- `STRING` - Text values
- `NUMBER` - Integers
- `DECIMAL` - Floating-point numbers
- `BOOLEAN` - true/false values
- `DATE` - Date values (supports various formats)
- `EMAIL` - Email addresses with format validation
- `UUID` - Universally unique identifiers

### Supported Transformations
- `TRIM` - Remove leading/trailing whitespace
- `UPPERCASE` - Convert to uppercase
- `LOWERCASE` - Convert to lowercase
- `CAPITALIZE_WORDS` - Capitalize first letter of each word
- `LIMIT` - Limit string length
- `PAD_LEFT` / `PAD_RIGHT` - Add padding
- `REPLACE` - Replace text patterns
- `REMOVE_SPECIAL_CHARS` - Remove special characters
- `NUMBER_FORMAT` - Format numbers
- `DATE_PARSE` - Parse date strings

### Supported Validations
- `required` - Field must have a value
- `allowedValues` - Value must be in predefined list
- `regex` - Value must match regex pattern
- `email` - Valid email format
- `uuid` - Valid UUID format
- `min` / `max` - Numeric range validation
- `minLength` / `maxLength` - String length validation
- `dbLookup` - Foreign key validation against database

---

**Last Updated**: 2026-01-28
**Version**: 1.0.0
