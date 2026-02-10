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
    TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null || \
            echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

    if [ -n "$TOKEN" ]; then
        echo "Creating realm 'information-assets'..."
        curl -X POST http://localhost:8080/admin/realms \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{
            "realm": "information-assets",
            "enabled": true,
            "displayName": "Asset Management",
            "loginTheme": "keycloak",
            "accountTheme": "keycloak",
            "adminTheme": "keycloak",
            "emailTheme": "keycloak"
          }' 2>/dev/null || echo "Realm might already exist"

        echo "Creating client 'asset-backend'..."
        curl -X POST http://localhost:8080/admin/realms/information-assets/clients \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{
            "clientId": "asset-backend",
            "name": "Asset Management Backend",
            "description": "Backend services for asset management",
            "publicClient": false,
            "secret": "backend-secret",
            "directAccessGrantsEnabled": true,
            "serviceAccountsEnabled": true,
            "authorizationServicesEnabled": true,
            "standardFlowEnabled": true,
            "enabled": true,
            "redirectUris": ["http://localhost:8082/*", "http://localhost:3000/*"],
            "webOrigins": ["http://localhost:8082", "http://localhost:3000"]
          }' 2>/dev/null || echo "Client might already exist"

        echo "Creating client 'asset-frontend'..."
        curl -X POST http://localhost:8080/admin/realms/information-assets/clients \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{
            "clientId": "asset-frontend",
            "name": "Asset Management Frontend",
            "description": "React frontend for asset management",
            "publicClient": true,
            "directAccessGrantsEnabled": true,
            "enabled": true,
            "redirectUris": ["http://localhost:3000/*"],
            "webOrigins": ["http://localhost:3000"]
          }' 2>/dev/null || echo "Frontend client might already exist"

        echo "Creating admin user..."
        curl -X POST http://localhost:8080/admin/realms/information-assets/users \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{
            "username": "admin",
            "email": "admin@company.com",
            "firstName": "Admin",
            "lastName": "System",
            "enabled": true,
            "emailVerified": true,
            "credentials": [{
              "type": "password",
              "value": "admin123",
              "temporary": false
            }]
          }' 2>/dev/null || echo "Admin user might already exist"

        echo "Creating regular user..."
        curl -X POST http://localhost:8080/admin/realms/information-assets/users \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{
            "username": "user",
            "email": "user@company.com",
            "firstName": "Regular",
            "lastName": "User",
            "enabled": true,
            "emailVerified": true,
            "credentials": [{
              "type": "password",
              "value": "user123",
              "temporary": false
            }]
          }' 2>/dev/null || echo "Regular user might already exist"

        echo "Creating roles..."
        for role in "admin" "user" "auditor" "manager"; do
            curl -X POST http://localhost:8080/admin/realms/information-assets/roles \
              -H "Authorization: Bearer $TOKEN" \
              -H "Content-Type: application/json" \
              -d "{\"name\": \"$role\"}" 2>/dev/null || echo "Role $role might already exist"
        done

        echo "Assigning admin role to admin user..."
        USER_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
          "http://localhost:8080/admin/realms/information-assets/users?username=admin" | \
          python3 -c "import sys,json; data=json.load(sys.stdin); print(data[0]['id'] if data else '')" 2>/dev/null)

        if [ -n "$USER_ID" ]; then
            curl -X POST "http://localhost:8080/admin/realms/information-assets/users/$USER_ID/role-mappings/realm" \
              -H "Authorization: Bearer $TOKEN" \
              -H "Content-Type: application/json" \
              -d '[{"name":"admin","id":"'$(curl -s -H "Authorization: Bearer $TOKEN" \
                "http://localhost:8080/admin/realms/information-assets/roles/admin" | \
                python3 -c "import sys,json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)'"}]' 2>/dev/null || echo "Failed to assign admin role"
        fi

        echo "✅ Keycloak setup completed"
    fi
else
    echo "⚠️  Could not get admin token, Keycloak might be in dev mode"
    echo "Using default development configuration"
fi

echo ""
echo "Access Keycloak at: http://localhost:8080"
echo "Admin console: http://localhost:8080/admin (admin/admin123)"
echo "Realm: information-assets"
echo ""
echo "Test users:"
echo "  - admin / admin123 (admin role)"
echo "  - user / user123 (user role)"
echo ""
echo "Clients:"
echo "  - asset-backend (backend services, secret: backend-secret)"
echo "  - asset-frontend (React frontend, public client)"