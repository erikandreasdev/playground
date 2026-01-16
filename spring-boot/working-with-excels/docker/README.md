# Oracle Database for Excel Import Testing

## Quick Start

```bash
# Start Oracle
docker compose up -d

# Wait for Oracle to be ready (first startup takes ~2-3 minutes)
docker compose logs -f oracle

# When you see "DATABASE IS READY TO USE" you're good to go
```

## Connection Details

| Property | Value |
|----------|-------|
| Host | localhost |
| Port | 1521 |
| Service | FREEPDB1 |
| Username | appuser |
| Password | appuser123 |
| SYS Password | oracle |

## Spring Boot Configuration

Add to `application.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
spring.datasource.username=appuser
spring.datasource.password=appuser123
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
```

## Verify Tables

```bash
# Connect with SQLPlus
docker exec -it excel-import-oracle sqlplus appuser/appuser123@//localhost:1521/FREEPDB1

# List tables
SELECT table_name FROM user_tables;

# Check lookup data
SELECT * FROM countries;
SELECT * FROM status_types;
SELECT * FROM categories;
```

## Useful Commands

```bash
# Stop Oracle
docker compose down

# Stop and remove data (fresh start)
docker compose down -v

# View logs
docker compose logs -f oracle

# Restart
docker compose restart oracle
```

## Tables Created

| Table | Purpose |
|-------|---------|
| COUNTRIES | Lookup table for country names → IDs |
| STATUS_TYPES | Lookup table for status codes → IDs |
| CATEGORIES | Lookup table with hierarchy |
| APP_USERS | Main users table (all column types) |
| PRODUCTS | Products with sequences |
| ORDERS | Orders with multiple FKs |
| ORDER_ITEMS | Order details |
| VALIDATION_TEST | All validation rules demo |
| TRANSFORMATION_TEST | All transformers demo |

## Sequences

- `USER_SEQ` - Starts at 1000
- `PRODUCT_SEQ` - Starts at 5000
- `ORDER_SEQ` - Starts at 10000
