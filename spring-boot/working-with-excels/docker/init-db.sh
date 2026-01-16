#!/bin/bash
# Initialize Oracle Database with test tables
# Run this after: docker compose up -d

echo "🔄 Waiting for Oracle to be ready..."
until docker exec excel-import-oracle sqlplus -s system/oracle@//localhost:1521/FREEPDB1 <<< "SELECT 1 FROM DUAL;" > /dev/null 2>&1; do
    echo "   Oracle not ready, waiting 5 seconds..."
    sleep 5
done

echo "✅ Oracle is ready!"

echo "🔄 Creating tables in APPUSER schema..."
docker exec -i excel-import-oracle sqlplus -s appuser/appuser123@//localhost:1521/FREEPDB1 < docker/oracle-init/01-create-tables.sql

echo "✅ Tables created!"
echo ""
echo "📋 Verifying tables..."
docker exec excel-import-oracle sqlplus -s appuser/appuser123@//localhost:1521/FREEPDB1 <<< "SELECT table_name FROM user_tables ORDER BY table_name;"

echo ""
echo "📋 Verifying lookup data..."
docker exec excel-import-oracle sqlplus -s appuser/appuser123@//localhost:1521/FREEPDB1 <<< "SELECT * FROM countries;"
