-- ============================================================================
-- E-Commerce Database Schema - Demonstrates Foreign Key Relationships
-- ============================================================================
-- This schema is designed to showcase database lookup validations (dbLookup)
-- when importing Excel data with referential integrity constraints.
-- ============================================================================

ALTER SESSION SET CONTAINER = FREEPDB1;
CREATE USER IF NOT EXISTS DEMO IDENTIFIED BY "passx";
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO DEMO;

-- Switch to DEMO schema
ALTER SESSION SET CURRENT_SCHEMA = DEMO;

-- ============================================================================
-- PARENT TABLES (No foreign keys)
-- ============================================================================

-- Categories Table (Parent)
-- Example: Electronics, Clothing, Books, etc.
CREATE TABLE DEMO.categories (
    category_id VARCHAR2(20) PRIMARY KEY,
    category_name VARCHAR2(100) NOT NULL,
    description VARCHAR2(255)
);

-- Customers Table (Parent)
-- Stores customer information
CREATE TABLE DEMO.customers (
    customer_id VARCHAR2(20) PRIMARY KEY,
    first_name VARCHAR2(50) NOT NULL,
    last_name VARCHAR2(50) NOT NULL,
    email VARCHAR2(100) NOT NULL UNIQUE,
    phone VARCHAR2(20),
    registration_date DATE,
    status VARCHAR2(20) -- ACTIVE, INACTIVE, SUSPENDED
);

-- ============================================================================
-- CHILD TABLES (With foreign keys to parent tables)
-- ============================================================================

-- Products Table (Child of Categories)
-- References: categories
CREATE TABLE DEMO.products (
    product_id VARCHAR2(20) PRIMARY KEY,
    product_name VARCHAR2(100) NOT NULL,
    category_id VARCHAR2(20) NOT NULL,
    price NUMBER(10, 2) NOT NULL,
    stock_quantity NUMBER NOT NULL,
    rating NUMBER(3, 2),
    is_active VARCHAR2(10), -- TRUE/FALSE
    CONSTRAINT fk_products_category 
        FOREIGN KEY (category_id) 
        REFERENCES DEMO.categories(category_id)
);

-- Orders Table (Child of Customers)
-- References: customers
CREATE TABLE DEMO.orders (
    order_id VARCHAR2(20) PRIMARY KEY,
    customer_id VARCHAR2(20) NOT NULL,
    order_date DATE NOT NULL,
    total_amount NUMBER(12, 2) NOT NULL,
    status VARCHAR2(20) NOT NULL, -- PENDING, SHIPPED, DELIVERED, CANCELLED
    shipping_address VARCHAR2(255),
    CONSTRAINT fk_orders_customer 
        FOREIGN KEY (customer_id) 
        REFERENCES DEMO.customers(customer_id)
        ON DELETE CASCADE
);

-- ============================================================================
-- GRANDCHILD TABLES (With foreign keys to child tables)
-- ============================================================================

-- Order Items Table (Grandchild - references both Orders and Products)
-- References: orders, products
-- This is a junction/association table demonstrating many-to-many relationships
CREATE TABLE DEMO.order_items (
    order_item_id VARCHAR2(20) PRIMARY KEY,
    order_id VARCHAR2(20) NOT NULL,
    product_id VARCHAR2(20) NOT NULL,
    quantity NUMBER NOT NULL,
    unit_price NUMBER(10, 2) NOT NULL,
    subtotal NUMBER(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order 
        FOREIGN KEY (order_id) 
        REFERENCES DEMO.orders(order_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product 
        FOREIGN KEY (product_id) 
        REFERENCES DEMO.products(product_id)
);

-- ============================================================================
-- INDEXES for better lookup performance
-- ============================================================================

CREATE INDEX idx_products_category ON DEMO.products(category_id);
CREATE INDEX idx_orders_customer ON DEMO.orders(customer_id);
CREATE INDEX idx_orders_status ON DEMO.orders(status);
CREATE INDEX idx_order_items_order ON DEMO.order_items(order_id);
CREATE INDEX idx_order_items_product ON DEMO.order_items(product_id);

-- ============================================================================
-- SUMMARY OF RELATIONSHIPS
-- ============================================================================
-- 1. categories (parent) <- products (child)
-- 2. customers (parent) <- orders (child)
-- 3. orders (child/parent) <- order_items (grandchild)
-- 4. products (child/parent) <- order_items (grandchild)
--
-- RECOMMENDED LOAD ORDER:
-- 1. categories.yml    (no dependencies)
-- 2. customers.yml     (no dependencies)
-- 3. products.yml      (requires categories)
-- 4. orders.yml        (requires customers)
-- 5. order_items.yml   (requires orders AND products)
-- ============================================================================
