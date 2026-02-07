#!/bin/bash

echo "=== Final Keycloak Setup ==="

# 1. Get admin token
echo "1. Getting admin token..."
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123&grant_type=password&client_id=admin-cli" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token', ''))")

if [ -z "$ADMIN_TOKEN" ]; then
    echo "❌ Failed to get admin token"
    exit 1
fi

echo "✅ Admin token obtained"

# 2. Check if client exists
echo "2. Checking client..."
CLIENT_INFO=$(curl -s "http://localhost:8080/admin/realms/asset-management/clients?clientId=asset-backend" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$CLIENT_INFO" | grep -q '"id"'; then
    echo "✅ Client exists"
    CLIENT_ID=$(echo "$CLIENT_INFO" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")
    echo "Client ID: $CLIENT_ID"
    
    # Update client
    echo "Updating client settings..."
    curl -X PUT "http://localhost:8080/admin/realms/asset-management/clients/$CLIENT_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "clientId": "asset-backend",
        "publicClient": false,
        "secret": "backend-secret",
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": false,
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "enabled": true
      }'
else
    echo "❌ Client not found, creating..."
    # Create client
    curl -X POST http://localhost:8080/admin/realms/asset-management/clients \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "clientId": "asset-backend",
        "publicClient": false,
        "secret": "backend-secret",
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": false,
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "enabled": true
      }'
fi

# 3. Create users
echo "3. Creating users..."
# Check if admin user exists
ADMIN_EXISTS=$(curl -s "http://localhost:8080/admin/realms/asset-management/users?username=admin" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$ADMIN_EXISTS" | grep -q '"id"'; then
    echo "✅ Admin user exists"
else
    echo "Creating admin user..."
    curl -X POST http://localhost:8080/admin/realms/asset-management/users \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
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
fi

# Check if regular user exists
USER_EXISTS=$(curl -s "http://localhost:8080/admin/realms/asset-management/users?username=user" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$USER_EXISTS" | grep -q '"id"'; then
    echo "✅ Regular user exists"
else
    echo "Creating regular user..."
    curl -X POST http://localhost:8080/admin/realms/asset-management/users \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "username": "user",
        "enabled": true,
        "email": "user@example.com",
        "firstName": "Regular",
        "lastName": "User",
        "credentials": [{
          "type": "password",
          "value": "user123",
          "temporary": false
        }]
      }'
fi

echo ""
echo "✅ Keycloak setup complete!"
echo ""
echo "Test with:"
echo "curl -X POST http://localhost:8080/realms/asset-management/protocol/openid-connect/token \\"
echo '  -H "Content-Type: application/x-www-form-urlencoded" \'
echo '  -d "client_id=asset-backend&client_secret=backend-secret&username=admin&password=admin123&grant_type=password"'
