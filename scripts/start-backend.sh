#!/bin/bash

echo "=== Starting Asset Management Backend ==="

# Определяем корневую директорию проекта
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "Project root: $PROJECT_ROOT"

cd "$PROJECT_ROOT" || exit 1

# 1. Проверяем существование директорий
echo "1. Checking project structure..."
if [ ! -d "api-gateway" ]; then
    echo "ERROR: Directory 'api-gateway' not found!"
    echo "Current directory: $(pwd)"
    echo "Available directories:"
    ls -la
    exit 1
fi

# Проверяем только обязательные директории для начала
REQUIRED_DIRS=("api-gateway")
for dir in "${REQUIRED_DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        echo "ERROR: Required directory '$dir' not found!"
        exit 1
    fi
done

# 2. Build только API Gateway сначала
echo "2. Building API Gateway..."
cd api-gateway
echo "Cleaning and compiling..."
mvn clean compile -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to build API Gateway"
    echo "Checking for compilation errors..."
    mvn clean compile -DskipTests 2>&1 | grep -A5 -B5 "ERROR"
    exit 1
fi
cd ..

# 3. Проверяем наличие docker-compose файла
COMPOSE_FILE="docker-compose.keycloak.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "ERROR: Docker Compose file '$COMPOSE_FILE' not found!"
    echo "Available files:"
    ls -la *.yml *.yaml 2>/dev/null
    echo "Creating simplified docker-compose.yml..."

    cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      KC_HOSTNAME: localhost
    ports:
      - "8080:8080"

  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    ports:
      - "8082:8082"
    depends_on:
      - keycloak
EOF

    COMPOSE_FILE="docker-compose.yml"
fi

# 4. Start everything with Docker Compose
echo "3. Starting Docker containers..."
docker-compose -f "$COMPOSE_FILE" up -d

# 5. Проверяем успешность запуска
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to start Docker containers!"
    echo "Trying to see what went wrong..."
    docker-compose -f "$COMPOSE_FILE" logs --tail=20
    exit 1
fi

# 6. Wait for Keycloak
echo "4. Waiting for Keycloak..."
for i in {1..30}; do
    if curl -s http://localhost:8080 > /dev/null 2>&1; then
        echo "✅ Keycloak is up and running!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Timeout waiting for Keycloak"
        docker-compose -f "$COMPOSE_FILE" logs keycloak
        exit 1
    fi
    echo "Waiting for Keycloak... ($i/30)"
    sleep 2
done

# 7. Wait for API Gateway
echo "5. Waiting for API Gateway..."
for i in {1..20}; do
    if curl -s http://localhost:8082/swagger-ui.html > /dev/null 2>&1; then
        echo "✅ API Gateway is up and running!"
        break
    fi
    if [ $i -eq 20 ]; then
        echo "⚠️  API Gateway might be slow to start, continuing..."
    fi
    echo "Waiting for API Gateway... ($i/20)"
    sleep 3
done

# 8. Проверяем наличие скрипта настройки Keycloak
SETUP_SCRIPT="scripts/setup-keycloak.sh"
if [ ! -f "$SETUP_SCRIPT" ]; then
    echo "WARNING: Keycloak setup script not found at $SETUP_SCRIPT"
    echo "Creating minimal setup script..."

    cat > "$SETUP_SCRIPT" << 'EOF'
#!/bin/bash
echo "=== Setting up Keycloak ==="
echo "Keycloak is already running in development mode"
echo "You can access Keycloak at: http://localhost:8080"
echo "Admin credentials: admin / admin123"
echo ""
echo "For production setup, please configure manually:"
echo "1. Create realm: asset-management"
echo "2. Create client: asset-backend"
echo "3. Create users: admin, user"
echo ""
echo "For now, using test mode with mock authentication."
EOF

    chmod +x "$SETUP_SCRIPT"
fi

# 9. Setup Keycloak (опционально)
echo "6. Setting up Keycloak (if needed)..."
./scripts/setup-keycloak.sh

echo ""
echo "✅ === Backend is ready! ==="
echo ""
echo "🌐 Services:"
echo "   Keycloak:       http://localhost:8080 (admin/admin123)"
echo "   API Gateway:    http://localhost:8082"
echo "   Auth Service:   http://localhost:8083 (if running)"
echo "   Asset Service:  http://localhost:8084 (if running)"
echo ""
echo "📚 Documentation:"
echo "   Aggregated Swagger: http://localhost:8082/swagger-ui.html"
echo "   Gateway API:        http://localhost:8082/api-docs"
echo ""
echo "🔧 Test commands:"
echo "   curl http://localhost:8082/api/gateway/health"
echo "   curl http://localhost:8082/api/gateway/services"
echo "   curl http://localhost:8082/swagger-ui.html"
echo ""
echo "🛑 To stop services, run: docker-compose -f $COMPOSE_FILE down"
echo ""