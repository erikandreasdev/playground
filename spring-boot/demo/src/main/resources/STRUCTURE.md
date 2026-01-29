📁 src/main/resources/
│
├── 📄 application.yml                          # Spring Boot configuration
├── 📄 README.md                                # This documentation file
│
├── 📂 examples/                                # Example and demo files
│   ├── 📂 excel/                              # Sample Excel files for testing
│   │   ├── 📊 ecommerce-data.xlsx            # Generated e-commerce dataset (5 sheets, 80 rows)
│   │   └── 📊 excel-type-a.xlsx              # Legacy example Excel file
│   │
│   └── 📂 validations/                        # Legacy/example validation configs
│       ├── 📋 excel-type-a-validations.yml   # Original example validation
│       ├── 📋 products.yml                    # Simple product validation example
│       └── 📋 transactions.yml                # Transaction validation example
│
├── 📂 generator/                               # Excel file generator configurations
│   └── 📋 ecommerce-generator.yml             # E-commerce data generator config
│
└── 📂 validations/                             # Active validation schemas (PRODUCTION)
    ├── 📋 categories.yml                      # ✅ Parent table - No dependencies
    ├── 📋 customers.yml                       # ✅ Parent table - No dependencies
    ├── 📋 products.yml                        # ⚠️  Child of CATEGORIES (has dbLookup)
    ├── 📋 orders.yml                          # ⚠️  Child of CUSTOMERS (has dbLookup)
    └── 📋 order_items.yml                     # ⚠️  Child of ORDERS & PRODUCTS (has dbLookup)

---

📌 LOAD ORDER (when using persist=true):
1️⃣  categories.yml
2️⃣  customers.yml
3️⃣  products.yml
4️⃣  orders.yml
5️⃣  order_items.yml

