#!/bin/bash
set -euo pipefail

# Runs once on an empty MySQL data volume. One schema per microservice: services
# never read another service's tables, they talk over HTTP or Kafka.
APP_USER="${MYSQL_APP_USER:-ecommerce}"
APP_PASSWORD="${MYSQL_APP_PASSWORD:-ecommerce-dev-password}"

DATABASES=(
  ecommerce_db
  product_db
  order_db
  payment_db
  notification_db
)

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-SQL
CREATE USER IF NOT EXISTS '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASSWORD}';
SQL

for database in "${DATABASES[@]}"; do
  mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-SQL
CREATE DATABASE IF NOT EXISTS \`${database}\`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON \`${database}\`.* TO '${APP_USER}'@'%';
SQL
done

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-SQL
FLUSH PRIVILEGES;
SQL

echo "Created schemas: ${DATABASES[*]} for user ${APP_USER}"
