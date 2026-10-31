#!/bin/bash

# Exit immediately if any command fails
set -e

APP_USER="csye6225"
APP_GROUP="csye6225"

DB_NAME="productcatalog_db"
DB_USER="productcatalog"
DB_PASSWORD="productcatalog_password"

APP_DIR="/opt/csye6225"
APP_JAR="${1:-}"

SERVICE_FILE="${2:-}"
SERVICE_NAME="productcatalog.service"

echo "Starting Product Catalog setup..."

echo "Updating package lists..."
apt-get update

echo "Installing application dependencies..."
apt-get install -y \
  openjdk-21-jre-headless \
  postgresql \
  postgresql-contrib

echo "Removing Git from the custom image..."
apt-get remove -y --purge git git-man
apt-get autoremove -y --purge

echo "Creating application group..."
if ! getent group "${APP_GROUP}" > /dev/null; then
  groupadd "${APP_GROUP}"
fi

echo "Creating application user..."
if ! id "${APP_USER}" > /dev/null 2>&1; then
  useradd \
    --gid "${APP_GROUP}" \
    --shell /usr/sbin/nologin \
    "${APP_USER}"
fi

echo "Enabling PostgreSQL..."
systemctl enable postgresql

echo "Starting PostgreSQL..."
systemctl start postgresql

echo "Creating PostgreSQL application user..."
if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1; then
  sudo -u postgres psql -c "CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}';"
fi

echo "Creating PostgreSQL application database..."
if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1; then
  sudo -u postgres createdb --owner="${DB_USER}" "${DB_NAME}"
fi

echo "PostgreSQL application database setup completed."

if [[ -z "${APP_JAR}" ]]; then
  echo "Usage: $0 <application-jar>"
  exit 1
fi

if [[ ! -f "${APP_JAR}" ]]; then
  echo "Application JAR not found: ${APP_JAR}"
  exit 1
fi

if [[ -z "${SERVICE_FILE}" ]]; then
  echo "Usage: $0 <application-jar> <service-file>"
  exit 1
fi

if [[ ! -f "${SERVICE_FILE}" ]]; then
  echo "Systemd service file not found: ${SERVICE_FILE}"
  exit 1
fi

echo "Creating application directory..."
mkdir -p "${APP_DIR}"

echo "Installing application JAR..."
cp "${APP_JAR}" "${APP_DIR}/productcatalog-api.jar"

echo "Setting application ownership..."
chown -R "${APP_USER}:${APP_GROUP}" "${APP_DIR}"

echo "Setting application permissions..."
chmod 750 "${APP_DIR}"
chmod 640 "${APP_DIR}/productcatalog-api.jar"

echo "Application JAR installed successfully."

echo "Installing systemd service..."
cp "${SERVICE_FILE}" "/etc/systemd/system/${SERVICE_NAME}"

echo "Reloading systemd configuration..."
systemctl daemon-reload

echo "Enabling application service..."
systemctl enable "${SERVICE_NAME}"

echo "Systemd service configured successfully."

echo "Base application dependencies and user setup completed."