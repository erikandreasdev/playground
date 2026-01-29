# E-Commerce Database Example - Database Lookup Validation

This example demonstrates the `dbLookup` validation feature with a realistic e-commerce database schema containing foreign key relationships.

## 📊 Database Schema

### Entity-Relationship Diagram

```
┌─────────────┐         ┌──────────────┐
│ CATEGORIES  │◄────────│  PRODUCTS    │
│  (Parent)   │  FK     │  (Child)     │
└─────────────┘         └──────┬───────┘
                               │
                               │ FK
                        ┌──────▼───────┐
                        │ ORDER_ITEMS  │
                        │ (Grandchild) │
                        └──────▲───────┘
                               │ FK
                        ┌──────┴───────┐
┌─────────────┐         │    ORDERS    │
│  CUSTOMERS  │◄────────│    (Child)   │
│  (Parent)   │  FK     │              │
└─────────────┘         └──────────────┘
```

### Tables

1. **CATEGORIES** (Parent)
   - `category_id` (PK)
   - `category_name`
   - `description`

2. **CUSTOMERS** (Parent)
   - `customer_id` (PK)
   - `first_name`, `last_name`, `email` (UNIQUE)
   - `phone`, `registration_date`, `status`

3. **PRODUCTS** (Child → CATEGORIES)
   - `product_id` (PK)
   - `category_id` (FK → categories.category_id)
   - `product_name`, `price`, `stock_quantity`, `rating`, `is_active`

4. **ORDERS** (Child → CUSTOMERS)
   - `order_id` (PK)
   - `customer_id` (FK → customers.customer_id)
   - `order_date`, `total_amount`, `status`, `shipping_address`

5. **ORDER_ITEMS** (Grandchild → ORDERS + PRODUCTS)
   - `order_item_id` (PK)
   - `order_id` (FK → orders.order_id)
   - `product_id` (FK → products.product_id)
   - `quantity`, `unit_price`, `subtotal`

## 🔄 Load Order (CRITICAL!)

Due to foreign key constraints, data **must** be loaded in this specific order:

1. ✅ **categories.yml** - No dependencies
2. ✅ **customers.yml** - No dependencies
3. ⚠️ **products.yml** - Requires CATEGORIES to be loaded first
4. ⚠️ **orders.yml** - Requires CUSTOMERS to be loaded first
5. ⚠️ **order_items.yml** - Requires both ORDERS and PRODUCTS to be loaded first

## 📝 Database Lookup Validation Examples

### Single Column Lookup

In `products.yml`, the `Category ID` column validates against the CATEGORIES table:

```yaml
- name: "Category ID"
  required: true
  rules:
    - NOT_EMPTY
  dbLookup:
    table: "CATEGORIES"
    column: "category_id"
    errorMessage: "Category ID does not exist. Please load categories first."
```

**What happens:**
- Before inserting a product, the validator checks if the `category_id` exists in the CATEGORIES table
- If not found, the row is marked invalid with the custom error message
- This prevents foreign key constraint violations at the database level

### Multiple Lookups in One Table

In `order_items.yml`, both `Order ID` and `Product ID` are validated:

```yaml
- name: "Order ID"
  dbLookup:
    table: "ORDERS"
    column: "order_id"
    errorMessage: "Order ID does not exist. Please load orders first."

- name: "Product ID"
  dbLookup:
    table: "PRODUCTS"
    column: "product_id"
    errorMessage: "Product ID does not exist. Please load products first."
```

**What happens:**
- Each foreign key is validated independently
- A row is valid only if BOTH lookups succeed
- This ensures all foreign key relationships are satisfied before insertion

## 🚀 How to Use

### Step 1: Reset Database
```bash
docker-compose down -v && docker-compose up -d
```

### Step 2: Start Application
```bash
./mvnw spring-boot:run
```

### Step 3: Load Data in Correct Order

**Load Categories (no validation needed):**
```
GET http://localhost:8080/api/excel/validate?excelFilename=YOUR_FILE.xlsx&validationsFilename=validations/categories.yml&persist=true
```

**Load Customers (no validation needed):**
```
GET http://localhost:8080/api/excel/validate?excelFilename=YOUR_FILE.xlsx&validationsFilename=validations/customers.yml&persist=true
```

**Load Products (validates category_id with dbLookup):**
```
GET http://localhost:8080/api/excel/validate?excelFilename=YOUR_FILE.xlsx&validationsFilename=validations/products.yml&persist=true
```

**Load Orders (validates customer_id with dbLookup):**
```
GET http://localhost:8080/api/excel/validate?excelFilename=YOUR_FILE.xlsx&validationsFilename=validations/orders.yml&persist=true
```

**Load Order Items (validates both order_id and product_id with dbLookup):**
```
GET http://localhost:8080/api/excel/validate?excelFilename=YOUR_FILE.xlsx&validationsFilename=validations/order_items.yml&persist=true
```

## 🧪 Testing the dbLookup Feature

### Scenario 1: Valid References
Create an Excel file with:
- Category "ELEC" in Categories sheet
- Product with "Category ID" = "ELEC" in Products sheet

**Result:** ✅ Validation passes, product is inserted

### Scenario 2: Invalid Reference
Create an Excel file with:
- Category "ELEC" in Categories sheet  
- Product with "Category ID" = "INVALID" in Products sheet

**Result:** ❌ Validation fails with error: "Category ID does not exist. Please load categories first."

### Scenario 3: Out of Order Loading
Try loading Products before Categories

**Result:** ❌ All products fail validation because no categories exist yet in the database

## 📋 Excel File Structure

Your Excel file should have these sheets (in any order, but load them in the correct sequence):

1. **Categories** sheet with columns:
   - Category ID, Category Name, Description

2. **Customers** sheet with columns:
   - Customer ID, First Name, Last Name, Email, Phone, Registration Date, Status

3. **Products** sheet with columns:
   - Product ID, Product Name, Category ID, Price, Stock Quantity, Rating, Is Active

4. **Orders** sheet with columns:
   - Order ID, Customer ID, Order Date, Total Amount, Status, Shipping Address

5. **Order Items** sheet with columns:
   - Order Item ID, Order ID, Product ID, Quantity, Unit Price, Subtotal

## 🎯 Key Features Demonstrated

1. **Foreign Key Validation** - Prevents orphaned records
2. **Custom Error Messages** - Clear feedback when references are invalid
3. **Multi-table Dependencies** - Complex relationships like ORDER_ITEMS
4. **Load Order Enforcement** - Ensures data integrity through validation
5. **ON DELETE Policies** - Cascade vs Restrict demonstrated in schema

## 📌 Notes

- The **dbLookup is case-sensitive** for table and column names
- Empty/blank values in FK columns will skip the lookup (use `required: true` to prevent this)
- Database lookups are performed **before** trying to insert, preventing database errors
- Performance: Lookups use indexed columns for optimal performance
