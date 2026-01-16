-- ============================================================================
-- Excel Import Test Database - Oracle Init Script
-- ============================================================================
-- Connect to FREEPDB1 and run as APPUSER
-- ============================================================================

-- ============================================================================
-- 1. LOOKUP REFERENCE TABLES
-- ============================================================================

CREATE TABLE COUNTRIES (
    ID              NUMBER(10) PRIMARY KEY,
    CODE            VARCHAR2(3) NOT NULL UNIQUE,
    NAME            VARCHAR2(100) NOT NULL UNIQUE
);

CREATE TABLE STATUS_TYPES (
    ID              NUMBER(10) PRIMARY KEY,
    CODE            VARCHAR2(20) NOT NULL UNIQUE,
    DESCRIPTION     VARCHAR2(100)
);

CREATE TABLE CATEGORIES (
    ID              NUMBER(10) PRIMARY KEY,
    NAME            VARCHAR2(50) NOT NULL UNIQUE,
    PARENT_ID       NUMBER(10) REFERENCES CATEGORIES(ID)
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

COMMIT;

-- ============================================================================
-- 2. SEQUENCES
-- ============================================================================

CREATE SEQUENCE USER_SEQ START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE PRODUCT_SEQ START WITH 5000 INCREMENT BY 1;
CREATE SEQUENCE ORDER_SEQ START WITH 10000 INCREMENT BY 1;

-- ============================================================================
-- 3. MAIN DATA TABLES
-- ============================================================================

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

CREATE TABLE ORDER_ITEMS (
    ITEM_ID         NUMBER(10) PRIMARY KEY,
    ORDER_ID        NUMBER(10) NOT NULL REFERENCES ORDERS(ORDER_ID),
    PRODUCT_ID      NUMBER(10) NOT NULL REFERENCES PRODUCTS(PRODUCT_ID),
    QUANTITY        NUMBER(10) NOT NULL,
    UNIT_PRICE      NUMBER(10,2) NOT NULL,
    DISCOUNT_PCT    NUMBER(5,2) DEFAULT 0
);

-- ============================================================================
-- 4. TEST TABLES
-- ============================================================================

CREATE TABLE VALIDATION_TEST (
    ID                  NUMBER(10) PRIMARY KEY,
    REQUIRED_FIELD      VARCHAR2(100) NOT NULL,
    CODE_FIELD          VARCHAR2(10),
    PHONE_NUMBER        VARCHAR2(20),
    POSTAL_CODE         VARCHAR2(10),
    PERCENTAGE          NUMBER(5,2),
    AGE                 NUMBER(3),
    PRIORITY            VARCHAR2(10),
    USERNAME            VARCHAR2(50),
    EMAIL               VARCHAR2(150)
);

CREATE TABLE TRANSFORMATION_TEST (
    ID                  NUMBER(10) PRIMARY KEY,
    UPPERCASE_FIELD     VARCHAR2(100),
    LOWERCASE_FIELD     VARCHAR2(100),
    TITLECASE_FIELD     VARCHAR2(100),
    TRIMMED_FIELD       VARCHAR2(100),
    NORMALIZED_FIELD    VARCHAR2(100),
    PADDED_LEFT         VARCHAR2(10),
    PADDED_RIGHT        VARCHAR2(10),
    FORMATTED_DATE      VARCHAR2(20),
    FORMATTED_NUMBER    VARCHAR2(20),
    REPLACED_FIELD      VARCHAR2(100),
    SUBSTRING_FIELD     VARCHAR2(10)
);

-- ============================================================================
-- 5. INDEXES
-- ============================================================================

CREATE INDEX IDX_USERS_EMAIL ON APP_USERS(EMAIL);
CREATE INDEX IDX_USERS_COUNTRY ON APP_USERS(COUNTRY_ID);
CREATE INDEX IDX_PRODUCTS_CATEGORY ON PRODUCTS(CATEGORY_ID);
CREATE INDEX IDX_ORDERS_USER ON ORDERS(USER_ID);
CREATE INDEX IDX_ORDERS_STATUS ON ORDERS(STATUS_ID);

COMMIT;
