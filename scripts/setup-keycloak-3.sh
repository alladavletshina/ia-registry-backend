#!/bin/bash

# Скрипт для настройки Keycloak: создание realm, клиента, пользователя admin, роли admin и назначение роли.
# Предполагается, что Keycloak уже запущен и доступен по адресу http://localhost:8080.
# Использует admin-cli клиент для аутентификации.

set -e

KEYCLOAK_URL="http://localhost:8080"
REALM="asset-management"
CLIENT_ID="asset-frontend"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin123"

echo "=== Настройка Keycloak ==="

# Получение токена администратора
echo "Получение токена администратора..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  echo "Ошибка: не удалось получить токен администратора"
  exit 1
fi
echo "Токен получен."

# Проверка существования realm
echo "Проверка существования realm ${REALM}..."
if curl -s -f -o /dev/null "${KEYCLOAK_URL}/realms/${REALM}"; then
  echo "Realm ${REALM} уже существует."
else
  echo "Создание realm ${REALM}..."
  curl -X POST "${KEYCLOAK_URL}/admin/realms" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"realm\": \"${REALM}\",
      \"enabled\": true,
      \"displayName\": \"Asset Management\",
      \"sslRequired\": \"none\"
    }"
  echo "Realm создан."
fi

# Проверка существования клиента
echo "Проверка существования клиента ${CLIENT_ID}..."
CLIENT_ID_DATA=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}")

if echo "$CLIENT_ID_DATA" | jq -e '.[0].id' >/dev/null 2>&1; then
  CLIENT_UUID=$(echo "$CLIENT_ID_DATA" | jq -r '.[0].id')
  echo "Клиент ${CLIENT_ID} уже существует (UUID: ${CLIENT_UUID})."
else
  echo "Создание клиента ${CLIENT_ID}..."
  curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"clientId\": \"${CLIENT_ID}\",
      \"publicClient\": true,
      \"directAccessGrantsEnabled\": true,
      \"enabled\": true,
      \"redirectUris\": [\"http://localhost:3000/*\"],
      \"webOrigins\": [\"http://localhost:3000\"]
    }"
  # Получаем UUID созданного клиента
  CLIENT_UUID=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" | jq -r '.[0].id')
  echo "Клиент создан (UUID: ${CLIENT_UUID})."
fi

# Проверка существования маппера ролей
echo "Проверка наличия маппера ролей для клиента..."
MAPPERS=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}/protocol-mappers/models")
if echo "$MAPPERS" | jq -e '.[] | select(.name == "realm roles")' >/dev/null 2>&1; then
  echo "Маппер ролей уже существует."
else
  echo "Добавление маппера ролей..."
  curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}/protocol-mappers/models" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "realm roles",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-usermodel-realm-role-mapper",
      "config": {
        "multivalued": "true",
        "userinfo.token.claim": "true",
        "id.token.claim": "true",
        "access.token.claim": "true",
        "claim.name": "realm_access.roles",
        "jsonType.label": "String"
      }
    }'
  echo "Маппер добавлен."
fi

# Проверка существования роли admin
echo "Проверка существования роли admin..."
ROLE_DATA=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/admin")
if echo "$ROLE_DATA" | jq -e '.id' >/dev/null 2>&1; then
  ROLE_ID=$(echo "$ROLE_DATA" | jq -r '.id')
  echo "Роль admin уже существует (ID: ${ROLE_ID})."
else
  echo "Создание роли admin..."
  curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/roles" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"name":"admin"}'
  ROLE_ID=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/admin" | jq -r '.id')
  echo "Роль admin создана (ID: ${ROLE_ID})."
fi

# Проверка существования пользователя admin
echo "Проверка существования пользователя admin..."
USER_DATA=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=admin")
if echo "$USER_DATA" | jq -e '.[0].id' >/dev/null 2>&1; then
  USER_ID=$(echo "$USER_DATA" | jq -r '.[0].id')
  echo "Пользователь admin уже существует (ID: ${USER_ID})."
else
  echo "Создание пользователя admin..."
  curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"admin\",
      \"enabled\": true,
      \"email\": \"admin@example.com\",
      \"firstName\": \"Admin\",
      \"lastName\": \"User\",
      \"credentials\": [{
        \"type\": \"password\",
        \"value\": \"admin123\",
        \"temporary\": false
      }]
    }"
  USER_ID=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=admin" | jq -r '.[0].id')
  echo "Пользователь admin создан (ID: ${USER_ID})."
fi

# Назначение роли admin пользователю
echo "Проверка наличия роли admin у пользователя..."
CURRENT_ROLES=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_ID}/role-mappings/realm")
if echo "$CURRENT_ROLES" | jq -e '.[] | select(.name == "admin")' >/dev/null 2>&1; then
  echo "Роль admin уже назначена пользователю."
else
  echo "Назначение роли admin пользователю..."
  curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_ID}/role-mappings/realm" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "[{\"id\":\"${ROLE_ID}\",\"name\":\"admin\"}]"
  echo "Роль назначена."
fi

echo "=== Настройка Keycloak завершена ==="