-- ============================================================================
-- Excel Import Test Database - H2 Compatible DDL (for local testing)
-- ============================================================================
-- H2 version of the Oracle script for integration tests.
-- Compatible with Spring Boot's @DataJdbcTest
-- ============================================================================

-- ============================================================================
-- 1. LOOKUP REFERENCE TABLES
-- ============================================================================

CREATE TABLE IF NOT EXISTS COUNTRIES (
    ID              INT PRIMARY KEY,
    CODE            VARCHAR(3) NOT NULL UNIQUE,
    NAME            VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS STATUS_TYPES (
    ID              INT PRIMARY KEY,
    CODE            VARCHAR(20) NOT NULL UNIQUE,
    DESCRIPTION     VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS CATEGORIES (
    ID              INT PRIMARY KEY,
    NAME            VARCHAR(50) NOT NULL UNIQUE,
    PARENT_ID       INT REFERENCES CATEGORIES(ID)
);

-- Reference data
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

-- ============================================================================
-- 2. MAIN DATA TABLES
-- ============================================================================

CREATE TABLE IF NOT EXISTS APP_USERS (
    USER_ID         INT PRIMARY KEY AUTO_INCREMENT,
    FULL_NAME       VARCHAR(100) NOT NULL,
    EMAIL           VARCHAR(150) NOT NULL UNIQUE,
    BIRTH_DATE      DATE,
    IS_ACTIVE       BOOLEAN DEFAULT TRUE,
    BALANCE         DECIMAL(15,2) DEFAULT 0,
    COUNTRY_ID      INT REFERENCES COUNTRIES(ID),
    STATUS_ID       INT REFERENCES STATUS_TYPES(ID),
    CREATED_AT      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY      VARCHAR(50) DEFAULT 'IMPORT'
);

CREATE TABLE IF NOT EXISTS PRODUCTS (
    PRODUCT_ID      INT PRIMARY KEY AUTO_INCREMENT,
    PRODUCT_NAME    VARCHAR(100) NOT NULL,
    DESCRIPTION     VARCHAR(500),
    PRICE           DECIMAL(10,2) NOT NULL,
    CATEGORY_ID     INT REFERENCES CATEGORIES(ID),
    STOCK_COUNT     INT DEFAULT 0,
    SKU             VARCHAR(20) UNIQUE,
    CREATED_AT      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ORDERS (
    ORDER_ID        INT PRIMARY KEY AUTO_INCREMENT,
    ORDER_REF       VARCHAR(50) NOT NULL UNIQUE,
    USER_ID         INT REFERENCES APP_USERS(USER_ID),
    TOTAL_AMOUNT    DECIMAL(15,2) NOT NULL,
    STATUS_ID       INT REFERENCES STATUS_TYPES(ID),
    ORDER_DATE      DATE DEFAULT CURRENT_DATE,
    SHIPPED_DATE    DATE,
    NOTES           VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS ORDER_ITEMS (
    ITEM_ID         INT PRIMARY KEY AUTO_INCREMENT,
    ORDER_ID        INT NOT NULL REFERENCES ORDERS(ORDER_ID),
    PRODUCT_ID      INT NOT NULL REFERENCES PRODUCTS(PRODUCT_ID),
    QUANTITY        INT NOT NULL,
    UNIT_PRICE      DECIMAL(10,2) NOT NULL,
    DISCOUNT_PCT    DECIMAL(5,2) DEFAULT 0
);

-- ============================================================================
-- 3. VALIDATION TEST TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS VALIDATION_TEST (
    ID                  INT PRIMARY KEY AUTO_INCREMENT,
    REQUIRED_FIELD      VARCHAR(100) NOT NULL,
    CODE_FIELD          VARCHAR(10),
    PHONE_NUMBER        VARCHAR(20),
    POSTAL_CODE         VARCHAR(10),
    PERCENTAGE          DECIMAL(5,2),
    AGE                 INT,
    PRIORITY            VARCHAR(10),
    USERNAME            VARCHAR(50),
    EMAIL               VARCHAR(150)
);

-- ============================================================================
-- 4. TRANSFORMATION TEST TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS TRANSFORMATION_TEST (
    ID                  INT PRIMARY KEY AUTO_INCREMENT,
    UPPERCASE_FIELD     VARCHAR(100),
    LOWERCASE_FIELD     VARCHAR(100),
    TITLECASE_FIELD     VARCHAR(100),
    TRIMMED_FIELD       VARCHAR(100),
    NORMALIZED_FIELD    VARCHAR(100),
    PADDED_LEFT         VARCHAR(10),
    PADDED_RIGHT        VARCHAR(10),
    FORMATTED_DATE      VARCHAR(20),
    FORMATTED_NUMBER    VARCHAR(20),
    REPLACED_FIELD      VARCHAR(100),
    SUBSTRING_FIELD     VARCHAR(10)
);
