#!/bin/bash
# scripts/test-aggregated-swagger.sh

echo "=== Testing Aggregated Swagger ==="
echo ""

echo "1. Gateway Swagger UI:"
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/swagger-ui.html
echo " (200 = OK)"
echo ""

echo "2. Aggregated API Docs (should list all services):"
curl -s http://localhost:8082/api-docs | jq -r '.info.title'
echo ""

echo "3. Available Swagger URLs:"
echo "   Main Gateway:     http://localhost:8082/swagger-ui.html"
echo "   Auth via Gateway: http://localhost:8082/auth-swagger-ui/"
echo "   Asset via Gateway: http://localhost:8082/asset-swagger-ui/"
echo ""

echo "4. Service endpoints:"
curl -s http://localhost:8082/api/gateway/services | jq -r '.[] | "   \(.name): \(.url)"'
echo ""

echo "5. Swagger configurations:"
echo "   Auth API Docs:    http://localhost:8082/auth-api-docs"
echo "   Asset API Docs:   http://localhost:8082/asset-api-docs"
echo "   Gateway API Docs: http://localhost:8082/api-docs"
echo ""

echo "=== Успех! ==="
echo "Откройте в браузере: http://localhost:8082/swagger-ui.html"
echo "Вы увидите все сервисы в одном Swagger UI!"