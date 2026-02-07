#!/bin/bash

echo "=== Setting up Keycloak ==="
sleep 5

# Проверяем доступность Keycloak
if ! curl -s http://localhost:8080 > /dev/null; then
    echo "Keycloak is not available, skipping setup..."
    exit 0
fi

echo "Keycloak is running, attempting to configure..."

# Получаем токен администратора
TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123&grant_type=password&client_id=admin-cli" 2>/dev/null)

if echo "$TOKEN_RESPONSE" | grep -q "access_token"; then
    TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token', ''))")

    if [ -n "$TOKEN" ]; then
        echo "Creating realm 'asset-management'..."
        curl -X POST http://localhost:8080/admin/realms \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{"realm": "asset-management", "enabled": true}' 2>/dev/null || echo "Realm might already exist"

        echo "Creating client 'asset-backend'..."
        curl -X POST http://localhost:8080/admin/realms/asset-management/clients \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{
            "clientId": "asset-backend",
            "publicClient": false,
            "secret": "backend-secret",
            "directAccessGrantsEnabled": true,
            "enabled": true
          }' 2>/dev/null || echo "Client might already exist"

        echo "Creating admin user..."
        curl -X POST http://localhost:8080/admin/realms/asset-management/users \
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
          }' 2>/dev/null || echo "User might already exist"

        echo "✅ Keycloak setup attempted"
    fi
else
    echo "⚠️  Could not get admin token, Keycloak might be in dev mode"
    echo "Using default development configuration"
fi

echo ""
echo "Access Keycloak at: http://localhost:8080"
echo "Admin credentials: admin / admin123"
echo ""
echo "Test users:"
echo "  - admin / admin123"
echo "  - user / user123 (if created)"
