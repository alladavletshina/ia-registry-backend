#!/bin/bash
# scripts/test-swagger-gateway.sh

echo "=== Testing Swagger through Gateway ==="
echo ""

echo "1. Gateway Swagger UI:"
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/swagger-ui.html
echo " (200 = OK)"
echo ""

echo "2. Gateway OpenAPI Spec:"
curl -s http://localhost:8082/api-docs | jq -r '.info.title'
echo ""

echo "3. Auth Service through Gateway:"
curl -s http://localhost:8082/auth-api-docs | jq -r '.info.title'
echo ""

echo "4. Asset Service through Gateway:"
curl -s http://localhost:8082/asset-api-docs | jq -r '.info.title'
echo ""

echo "=== Direct access ==="
echo "5. Direct Auth Service:"
curl -s http://localhost:8083/api-docs | jq -r '.info.title'
echo ""

echo "6. Direct Asset Service:"
curl -s http://localhost:8084/api-docs | jq -r '.info.title'
echo ""

echo "=== SUCCESS! All Swagger endpoints should return service titles ==="