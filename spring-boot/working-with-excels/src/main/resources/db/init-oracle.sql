-- ============================================================================
-- Excel Import Test Database - DDL Reference Script
-- ============================================================================
-- This script creates tables that demonstrate EVERY import feature:
-- - Basic column types (STRING, INTEGER, DECIMAL, DATE, BOOLEAN, EMAIL)
-- - Validation scenarios (required, regex, min/max, allowed values)
-- - Transformations (trim, case, formatting)
-- - Database lookups (foreign key resolution)
-- - Custom SQL (sequences, timestamps, defaults)
-- ============================================================================

-- ============================================================================
-- 1. LOOKUP REFERENCE TABLES (must be created first for foreign keys)
-- ============================================================================

-- Countries lookup table (for testing FK lookups)
CREATE TABLE COUNTRIES (
    ID              NUMBER(10) PRIMARY KEY,
    CODE            VARCHAR2(3) NOT NULL UNIQUE,
    NAME            VARCHAR2(100) NOT NULL UNIQUE
);

-- Status lookup table
CREATE TABLE STATUS_TYPES (
    ID              NUMBER(10) PRIMARY KEY,
    CODE            VARCHAR2(20) NOT NULL UNIQUE,
    DESCRIPTION     VARCHAR2(100)
);

-- Categories lookup table
CREATE TABLE CATEGORIES (
    ID              NUMBER(10) PRIMARY KEY,
    NAME            VARCHAR2(50) NOT NULL UNIQUE,
    PARENT_ID       NUMBER(10) REFERENCES CATEGORIES(ID)
);

-- Insert reference data
INSERT INTO COUNTRIES (ID, CODE, NAME) VALUES (1, 'US', 'United States');
INSERT INTO COUNTRIES (ID, CODE, NAME) VALUES (2, 'UK', 'United Kingdom');
INSERT INTO COUNTRIES (ID, CODE, NAME) VALUES (3, 'DE', 'Germany');
INSERT INTO COUNTRIES (ID, CODE, NAME) VALUES (4, 'FR', 'France');
INSERT INTO COUNTRIES (ID, CODE, NAME) VALUES (5, 'ES', 'Spain');

INSERT INTO STATUS_TYPES (ID, CODE, DESCRIPTION) VALUES (1, 'ACTIVE', 'Active record');
INSERT INTO STATUS_TYPES (ID, CODE, DESCRIPTION) VALUES (2, 'INACTIVE', 'Inactive record');
INSERT INTO STATUS_TYPES (ID, CODE, DESCRIPTION) VALUES (3, 'PENDING', 'Pending approval');
INSERT INTO STATUS_TYPES (ID, CODE, DESCRIPTION) VALUES (4, 'ARCHIVED', 'Archived record');

INSERT INTO CATEGORIES (ID, NAME, PARENT_ID) VALUES (1, 'Electronics', NULL);
INSERT INTO CATEGORIES (ID, NAME, PARENT_ID) VALUES (2, 'Computers', 1);
INSERT INTO CATEGORIES (ID, NAME, PARENT_ID) VALUES (3, 'Phones', 1);
INSERT INTO CATEGORIES (ID, NAME, PARENT_ID) VALUES (4, 'Clothing', NULL);
INSERT INTO CATEGORIES (ID, NAME, PARENT_ID) VALUES (5, 'Food', NULL);

COMMIT;

-- ============================================================================
-- 2. MAIN DATA TABLES
-- ============================================================================

-- Sequence for auto-generated IDs
CREATE SEQUENCE USER_SEQ START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE PRODUCT_SEQ START WITH 5000 INCREMENT BY 1;
CREATE SEQUENCE ORDER_SEQ START WITH 10000 INCREMENT BY 1;

-- Users table - demonstrates ALL column types
CREATE TABLE APP_USERS (
    USER_ID         NUMBER(10) PRIMARY KEY,
    FULL_NAME       VARCHAR2(100) NOT NULL,
    EMAIL           VARCHAR2(150) NOT NULL UNIQUE,
    BIRTH_DATE      DATE,
    IS_ACTIVE       NUMBER(1) DEFAULT 1,
    BALANCE         NUMBER(15,2) DEFAULT 0,
    COUNTRY_ID      NUMBER(10) REFERENCES COUNTRIES(ID),
    STATUS_ID       NUMBER(10) REFERENCES STATUS_TYPES(ID),
    CREATED_AT      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CREATED_BY      VARCHAR2(50) DEFAULT 'IMPORT'
);

-- Products table - demonstrates custom SQL with sequences
CREATE TABLE PRODUCTS (
    PRODUCT_ID      NUMBER(10) PRIMARY KEY,
    PRODUCT_NAME    VARCHAR2(100) NOT NULL,
    DESCRIPTION     VARCHAR2(500),
    PRICE           NUMBER(10,2) NOT NULL,
    CATEGORY_ID     NUMBER(10) REFERENCES CATEGORIES(ID),
    STOCK_COUNT     NUMBER(10) DEFAULT 0,
    SKU             VARCHAR2(20) UNIQUE,
    CREATED_AT      TIMESTAMP DEFAULT SYSTIMESTAMP,
    UPDATED_AT      TIMESTAMP
);

-- Orders table - demonstrates multiple lookups in one row
CREATE TABLE ORDERS (
    ORDER_ID        NUMBER(10) PRIMARY KEY,
    ORDER_REF       VARCHAR2(50) NOT NULL UNIQUE,
    USER_ID         NUMBER(10) REFERENCES APP_USERS(USER_ID),
    TOTAL_AMOUNT    NUMBER(15,2) NOT NULL,
    STATUS_ID       NUMBER(10) REFERENCES STATUS_TYPES(ID),
    ORDER_DATE      DATE DEFAULT SYSDATE,
    SHIPPED_DATE    DATE,
    NOTES           VARCHAR2(1000)
);

-- Order items - demonstrates parent-child relationships
CREATE TABLE ORDER_ITEMS (
    ITEM_ID         NUMBER(10) PRIMARY KEY,
    ORDER_ID        NUMBER(10) NOT NULL REFERENCES ORDERS(ORDER_ID),
    PRODUCT_ID      NUMBER(10) NOT NULL REFERENCES PRODUCTS(PRODUCT_ID),
    QUANTITY        NUMBER(10) NOT NULL,
    UNIT_PRICE      NUMBER(10,2) NOT NULL,
    DISCOUNT_PCT    NUMBER(5,2) DEFAULT 0
);

-- ============================================================================
-- 3. VALIDATION TEST TABLE - All validation rules in one place
-- ============================================================================

CREATE TABLE VALIDATION_TEST (
    ID                  NUMBER(10) PRIMARY KEY,
    
    -- Required field (notEmpty: true)
    REQUIRED_FIELD      VARCHAR2(100) NOT NULL,
    
    -- Length validation (minLength/maxLength)
    CODE_FIELD          VARCHAR2(10),  -- minLength: 3, maxLength: 10
    
    -- Regex validation
    PHONE_NUMBER        VARCHAR2(20),  -- regex: "^\+?[0-9]{10,15}$"
    POSTAL_CODE         VARCHAR2(10),  -- regex: "^[0-9]{5}(-[0-9]{4})?$"
    
    -- Numeric range validation (min/max)
    PERCENTAGE          NUMBER(5,2),   -- min: 0, max: 100
    AGE                 NUMBER(3),     -- min: 0, max: 150
    
    -- Allowed values (enum)
    PRIORITY            VARCHAR2(10),  -- allowedValues: [LOW, MEDIUM, HIGH, CRITICAL]
    
    -- Excluded values
    USERNAME            VARCHAR2(50),  -- excludedValues: [admin, root, system]
    
    -- Email type
    EMAIL               VARCHAR2(150)
);

-- ============================================================================
-- 4. TRANSFORMATION TEST TABLE
-- ============================================================================

CREATE TABLE TRANSFORMATION_TEST (
    ID                  NUMBER(10) PRIMARY KEY,
    
    -- Case transformations
    UPPERCASE_FIELD     VARCHAR2(100),  -- UPPERCASE transformer
    LOWERCASE_FIELD     VARCHAR2(100),  -- LOWERCASE transformer
    TITLECASE_FIELD     VARCHAR2(100),  -- TITLE_CASE transformer
    
    -- Whitespace transformations
    TRIMMED_FIELD       VARCHAR2(100),  -- TRIM transformer
    NORMALIZED_FIELD    VARCHAR2(100),  -- NORMALIZE_SPACES transformer
    
    -- Padding transformations
    PADDED_LEFT         VARCHAR2(10),   -- PAD_LEFT with length: 10, padChar: "0"
    PADDED_RIGHT        VARCHAR2(10),   -- PAD_RIGHT with length: 10, padChar: "X"
    
    -- Format transformations
    FORMATTED_DATE      VARCHAR2(20),   -- DATE_FORMAT: "dd/MM/yyyy"
    FORMATTED_NUMBER    VARCHAR2(20),   -- NUMBER_FORMAT: "#,##0.00"
    
    -- Replace/substring
    REPLACED_FIELD      VARCHAR2(100),  -- REPLACE pattern: "-", replacement: "_"
    SUBSTRING_FIELD     VARCHAR2(10)    -- SUBSTRING start: 0, end: 10
);

-- ============================================================================
-- 5. INDEXES for better lookup performance
-- ============================================================================

CREATE INDEX IDX_USERS_EMAIL ON APP_USERS(EMAIL);
CREATE INDEX IDX_USERS_COUNTRY ON APP_USERS(COUNTRY_ID);
CREATE INDEX IDX_PRODUCTS_CATEGORY ON PRODUCTS(CATEGORY_ID);
CREATE INDEX IDX_ORDERS_USER ON ORDERS(USER_ID);
CREATE INDEX IDX_ORDERS_STATUS ON ORDERS(STATUS_ID);
CREATE INDEX IDX_ORDER_ITEMS_ORDER ON ORDER_ITEMS(ORDER_ID);
CREATE INDEX IDX_ORDER_ITEMS_PRODUCT ON ORDER_ITEMS(PRODUCT_ID);

-- ============================================================================
-- USAGE EXAMPLES - YAML Mapping Reference
-- ============================================================================
/*

Example 1: Basic import with lookup
-----------------------------------
columns:
  - name: "Country"
    type: STRING
    dbMapping:
      dbColumn: "COUNTRY_ID"
      lookup:
        table: "COUNTRIES"
        matchColumn: "NAME"
        returnColumn: "ID"

Example 2: Custom SQL with sequence
-----------------------------------
customSql: |
  INSERT INTO PRODUCTS (PRODUCT_ID, PRODUCT_NAME, PRICE, CREATED_AT)
  VALUES (PRODUCT_SEQ.NEXTVAL, :PRODUCT_NAME, :PRICE, SYSTIMESTAMP)

Example 3: All validation rules
-------------------------------
columns:
  - name: "Required Field"
    type: STRING
    validation:
      notEmpty: true
    dbMapping: { dbColumn: "REQUIRED_FIELD" }

  - name: "Code"
    type: STRING
    validation:
      minLength: 3
      maxLength: 10
    dbMapping: { dbColumn: "CODE_FIELD" }

  - name: "Priority"
    type: STRING
    validation:
      allowedValues: ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    dbMapping: { dbColumn: "PRIORITY" }

Example 4: Transformations pipeline
-----------------------------------
columns:
  - name: "Full Name"
    type: STRING
    transformations:
      - type: TRIM
      - type: NORMALIZE_SPACES
      - type: TITLE_CASE
    dbMapping: { dbColumn: "FULL_NAME" }

*/
