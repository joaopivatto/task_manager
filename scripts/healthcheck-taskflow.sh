#!/usr/bin/env bash
set -euo pipefail

url="${1:-${HEALTHCHECK_URL:-http://127.0.0.1:8080/board}}"
max_attempts="${HEALTHCHECK_ATTEMPTS:-20}"
sleep_seconds="${HEALTHCHECK_SLEEP_SECONDS:-2}"

attempt=1
while [ "$attempt" -le "$max_attempts" ]; do
  http_code="$(curl -sS -o /dev/null -w "%{http_code}" "$url" || true)"
  if [ "$http_code" = "200" ] || [ "$http_code" = "302" ]; then
    echo "Healthcheck OK ($http_code): $url"
    exit 0
  fi

  echo "Waiting for service... attempt $attempt/$max_attempts (status=$http_code)"
  attempt=$((attempt + 1))
  sleep "$sleep_seconds"
done

echo "Healthcheck failed: $url did not return 200/302"
exit 1

