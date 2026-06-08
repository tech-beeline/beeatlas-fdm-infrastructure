#!/bin/sh
set -eu

UNLA_URL="${UNLA_URL:-http://mcp-gateway:5234}"
UNLA_USER="${UNLA_USER:-admin}"
UNLA_PASSWORD="${UNLA_PASSWORD:-secure-password}"
CONFIG_FILE="${CONFIG_FILE:-/configs/beeatlas-fdm.yaml}"
FDM_GATEWAY_URL="${FDM_GATEWAY_URL:-http://gateway:8080}"

echo "Waiting for Unla apiserver at ${UNLA_URL} ..."
attempt=0
while [ "$attempt" -lt 60 ]; do
  if curl -sf "${UNLA_URL}/api/runtime-config" >/dev/null 2>&1; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 2
done
if [ "$attempt" -eq 60 ]; then
  echo "ERROR: Unla did not become ready." >&2
  exit 1
fi

echo "Logging in..."
login_response=$(curl -s -X POST "${UNLA_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${UNLA_USER}\",\"password\":\"${UNLA_PASSWORD}\"}")

token=$(printf '%s' "$login_response" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
if [ -z "$token" ]; then
  echo "ERROR: login failed: ${login_response}" >&2
  exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
  echo "ERROR: config not found: ${CONFIG_FILE}" >&2
  exit 1
fi

name=$(grep '^name:' "$CONFIG_FILE" | head -1 | sed 's/^name:[[:space:]]*//' | tr -d '"')
tmp="/tmp/beeatlas-fdm.yaml"
sed "s|^      url: .*|      url: ${FDM_GATEWAY_URL}|" "$CONFIG_FILE" > "$tmp"

echo "Importing ${name} -> ${FDM_GATEWAY_URL} ..."
http_code=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${UNLA_URL}/api/mcp/configs" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/yaml" \
  --data-binary "@${tmp}")

if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
  echo "MCP config imported."
  exit 0
fi

echo "POST failed (${http_code}): $(cat /tmp/response.txt)" >&2
exit 1
