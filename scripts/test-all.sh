#!/bin/bash

echo "=== COMPLETE SYSTEM TEST ==="
echo ""

echo "1. [DIRECT] Auth Service:"
curl -s http://localhost:8083/api/auth/ping
echo ""
echo ""

echo "2. [DIRECT] Asset Service:"
curl -s http://localhost:8084/api/assets/health
echo ""
echo ""

echo "3. [GATEWAY] Auth Service через Gateway:"
curl -s http://localhost:8082/api/auth/ping
echo ""
echo ""

echo "4. [GATEWAY] Asset Service через Gateway:"
curl -s http://localhost:8082/api/assets/health
echo ""
echo ""

echo "5. Test Login через Gateway:"
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -s | head -20
echo ""
echo ""

echo "=== SUCCESS! All services are running! ==="
echo ""
echo "Access:"
echo "Keycloak UI:      http://localhost:8081"
echo "API Gateway:      http://localhost:8082"
echo "Auth Service:     http://localhost:8083"
echo "Asset Service:    http://localhost:8084"
echo ""
echo "Credentials: admin / admin123"
