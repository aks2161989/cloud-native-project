#!/bin/bash

# Exit immediately if any command fails
set -e

APP_USER="csye6225"
APP_GROUP="csye6225"

APP_DIR="/opt/csye6225"
APP_JAR="${1:-}"

SERVICE_FILE="${2:-}"
SERVICE_NAME="productcatalog.service"

CW_CONFIG_FILE="${3:-}"

echo "Starting Product Catalog setup..."

echo "Updating package lists..."
apt-get update

echo "Installing application dependencies..."
apt-get install -y openjdk-21-jre-headless wget

echo "Installing Amazon CloudWatch Agent..."
wget -q https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
dpkg -i -E ./amazon-cloudwatch-agent.deb
rm -f ./amazon-cloudwatch-agent.deb

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

if [[ -z "${APP_JAR}" ]]; then
  echo "Usage: $0 <application-jar> <service-file> [cw-config-file]"
  exit 1
fi

if [[ ! -f "${APP_JAR}" ]]; then
  echo "Application JAR not found: ${APP_JAR}"
  exit 1
fi

if [[ -z "${SERVICE_FILE}" ]]; then
  echo "Usage: $0 <application-jar> <service-file> [cw-config-file]"
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

echo "Creating log directory and file..."
mkdir -p /var/log/csye6225
touch /var/log/csye6225/app.log
chown -R "${APP_USER}:${APP_GROUP}" /var/log/csye6225
chmod 755 /var/log/csye6225
chmod 644 /var/log/csye6225/app.log

if [[ -n "${CW_CONFIG_FILE}" && -f "${CW_CONFIG_FILE}" ]]; then
  echo "Installing CloudWatch Agent configuration..."
  mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
  cp "${CW_CONFIG_FILE}" /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
fi

echo "Installing systemd service..."
cp "${SERVICE_FILE}" "/etc/systemd/system/${SERVICE_NAME}"

echo "Reloading systemd configuration..."
systemctl daemon-reload

echo "Enabling application service..."
systemctl enable "${SERVICE_NAME}"

echo "Enabling Amazon CloudWatch Agent service..."
systemctl enable amazon-cloudwatch-agent

echo "Systemd services configured successfully."
echo "Application dependencies, CloudWatch Agent, and user setup completed."
