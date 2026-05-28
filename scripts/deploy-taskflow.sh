#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/home/joao/Documentos/Projetos/task}"
SERVICE_NAME="${SERVICE_NAME:-taskflow}"
SERVICE_USER="${SERVICE_USER:-taskflow}"
INSTALL_DIR="${INSTALL_DIR:-/opt/taskflow}"
CURRENT_DIR="$INSTALL_DIR/current"
RELEASES_DIR="$INSTALL_DIR/releases"
JAR_TARGET_PATTERN="${JAR_TARGET_PATTERN:-target/*.jar}"
HEALTHCHECK_SCRIPT="${HEALTHCHECK_SCRIPT:-$PROJECT_DIR/scripts/healthcheck-taskflow.sh}"
SYSTEMD_UNIT_SOURCE="${SYSTEMD_UNIT_SOURCE:-$PROJECT_DIR/ops/systemd/taskflow.service}"
AUTO_INSTALL_SERVICE="${AUTO_INSTALL_SERVICE:-false}"

cd "$PROJECT_DIR"

if ! sudo systemctl cat "$SERVICE_NAME" >/dev/null 2>&1; then
  if [ "$AUTO_INSTALL_SERVICE" = "true" ]; then
    echo "Systemd service '$SERVICE_NAME' not found. Installing from $SYSTEMD_UNIT_SOURCE"
    sudo cp "$SYSTEMD_UNIT_SOURCE" "/etc/systemd/system/${SERVICE_NAME}.service"
    sudo systemctl daemon-reload
    sudo systemctl enable "$SERVICE_NAME"
    sudo systemctl start "$SERVICE_NAME"
  else
    echo "Systemd service '$SERVICE_NAME' not found."
    echo "Run the initial setup first:"
    echo "  sudo cp $SYSTEMD_UNIT_SOURCE /etc/systemd/system/${SERVICE_NAME}.service"
    echo "  sudo systemctl daemon-reload"
    echo "  sudo systemctl enable $SERVICE_NAME"
    echo "  sudo systemctl start $SERVICE_NAME"
    echo "Or run this deploy with AUTO_INSTALL_SERVICE=true."
    exit 1
  fi
fi

echo "[1/8] Updating source"
git pull --ff-only

echo "[2/8] Building artifact"
./mvnw clean package -DskipTests

new_jar="$(ls -t $JAR_TARGET_PATTERN | head -n 1)"
if [ -z "$new_jar" ]; then
  echo "No JAR found under $JAR_TARGET_PATTERN"
  exit 1
fi

release_id="$(date +%Y%m%d-%H%M%S)"
release_dir="$RELEASES_DIR/$release_id"
release_jar="$release_dir/task.jar"

previous_jar=""
if [ -L "$CURRENT_DIR/task.jar" ] || [ -f "$CURRENT_DIR/task.jar" ]; then
  previous_jar="$(readlink -f "$CURRENT_DIR/task.jar" || true)"
fi

echo "[3/8] Preparing release directories"
sudo mkdir -p "$release_dir" "$CURRENT_DIR"
sudo chown "$SERVICE_USER:$SERVICE_USER" "$RELEASES_DIR" "$CURRENT_DIR" "$release_dir"

echo "[4/8] Publishing JAR to release $release_id"
sudo cp "$new_jar" "$release_jar"
sudo chown "$SERVICE_USER:$SERVICE_USER" "$release_jar"
sudo chmod 640 "$release_jar"

echo "[5/8] Pointing current symlink to new release"
sudo ln -sfn "$release_jar" "$CURRENT_DIR/task.jar"
sudo chown -h "$SERVICE_USER:$SERVICE_USER" "$CURRENT_DIR/task.jar"

echo "[6/8] Restarting service: $SERVICE_NAME"
if ! sudo systemctl restart "$SERVICE_NAME"; then
  echo "Service restart failed. Attempting rollback..."
  if [ -n "$previous_jar" ] && [ -f "$previous_jar" ]; then
    sudo ln -sfn "$previous_jar" "$CURRENT_DIR/task.jar"
    sudo systemctl restart "$SERVICE_NAME"
    echo "Rollback complete. Restored previous release."
  fi
  exit 1
fi

echo "[7/8] Running healthcheck"
if ! "$HEALTHCHECK_SCRIPT"; then
  echo "Healthcheck failed. Attempting rollback..."
  if [ -n "$previous_jar" ] && [ -f "$previous_jar" ]; then
    sudo ln -sfn "$previous_jar" "$CURRENT_DIR/task.jar"
    sudo systemctl restart "$SERVICE_NAME"
    echo "Rollback complete. Restored previous release."
  fi
  exit 1
fi

echo "[8/8] Service status"
sudo systemctl status "$SERVICE_NAME" --no-pager

echo "Deploy completed successfully."
