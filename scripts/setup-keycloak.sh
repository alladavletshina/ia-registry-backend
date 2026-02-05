
#!/bin/bash

echo "Setting up Keycloak..."

# Ожидаем запуск Keycloak
sleep 10

# Получаем токен администратора
TOKEN=$(curl -s -X POST http://localhost:8081/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123&grant_type=password&client_id=admin-cli" \
  | jq -r '.access_token')

# Создаем Realm
curl -X POST http://localhost:8081/admin/realms \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "asset-management",
    "enabled": true
  }'

# Создаем клиента для бэкенда
curl -X POST http://localhost:8081/admin/realms/asset-management/clients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "asset-backend",
    "publicClient": false,
    "secret": "backend-secret",
    "serviceAccountsEnabled": true
  }'

# Создаем роли
curl -X POST http://localhost:8081/admin/realms/asset-management/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "admin"}'

curl -X POST http://localhost:8081/admin/realms/asset-management/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "user"}'

# Создаем пользователя admin
curl -X POST http://localhost:8081/admin/realms/asset-management/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "enabled": true,
    "credentials": [{
      "type": "password",
      "value": "admin123",
      "temporary": false
    }]
  }'

echo "Keycloak setup completed!"