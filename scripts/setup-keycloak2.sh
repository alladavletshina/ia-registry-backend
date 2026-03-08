#!/bin/bash

echo "=== Setting up Keycloak ==="

# Получаем токен администратора
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

# Создаём realm
curl -X POST http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "asset-management",
    "enabled": true,
    "displayName": "Asset Management"
  }'

# Создаём клиента
curl -X POST http://localhost:8080/admin/realms/asset-management/clients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "frontend-app",
    "publicClient": true,
    "directAccessGrantsEnabled": true,
    "standardFlowEnabled": true,
    "enabled": true
  }'

# Создаём пользователя
curl -X POST http://localhost:8080/admin/realms/asset-management/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "enabled": true,
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "credentials": [{
      "type": "password",
      "value": "admin123",
      "temporary": false
    }]
  }'

# Создаём роль admin
curl -X POST http://localhost:8080/admin/realms/asset-management/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "admin"}'

# Получаем ID пользователя и роли
USER_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/asset-management/users?username=admin" | jq -r '.[0].id')
ROLE_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/asset-management/roles/admin" | jq -r '.id')

# Назначаем роль пользователю
curl -X POST "http://localhost:8080/admin/realms/asset-management/users/$USER_ID/role-mappings/realm" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "[{\"id\":\"$ROLE_ID\",\"name\":\"admin\"}]"

echo "Done"