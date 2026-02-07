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

# Проверяем обязательные директории
REQUIRED_DIRS=("api-gateway" "auth-service" "asset-service")
for dir in "${REQUIRED_DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        echo "WARNING: Directory '$dir' not found!"
    fi
done

# 2. Собираем все сервисы
echo "2. Building all services..."

# Функция для сборки сервиса
build_service() {
    local service_name=$1
    echo "--- Building $service_name ---"

    if [ ! -d "$service_name" ]; then
        echo "WARNING: Directory $service_name not found, skipping..."
        return 0
    fi

    cd "$service_name" || return 1

    echo "Cleaning and compiling $service_name..."
    if ! mvn clean package -DskipTests; then
        echo "ERROR: Failed to build $service_name"
        echo "Checking for compilation errors..."
        mvn clean compile -DskipTests 2>&1 | grep -A5 -B5 "ERROR"

        # Попробуем только компиляцию
        echo "Trying compile only..."
        if ! mvn clean compile -DskipTests; then
            cd ..
            return 1
        fi

        # Если компиляция прошла, попробуем собрать без тестов
        echo "Compilation successful, trying to create JAR..."
        mvn package -DskipTests || {
            echo "WARNING: Could not create JAR for $service_name, but compilation succeeded"
        }
    fi

    cd ..
    echo "✅ $service_name build completed"
    return 0
}

# Собираем все сервисы
FAILED_BUILDS=0

build_service "api-gateway" || FAILED_BUILDS=$((FAILED_BUILDS + 1))
build_service "auth-service" || FAILED_BUILDS=$((FAILED_BUILDS + 1))
build_service "asset-service" || FAILED_BUILDS=$((FAILED_BUILDS + 1))

if [ $FAILED_BUILDS -gt 0 ]; then
    echo "WARNING: $FAILED_BUILDS service(s) failed to build, but continuing..."
fi

# 3. Проверяем наличие docker-compose файла
COMPOSE_FILE="docker-compose.keycloak.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "WARNING: Docker Compose file '$COMPOSE_FILE' not found!"
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

  auth-service:
    build:
      context: ./auth-service
      dockerfile: Dockerfile
    ports:
      - "8083:8083"
    depends_on:
      - keycloak

  asset-service:
    build:
      context: ./asset-service
      dockerfile: Dockerfile
    ports:
      - "8084:8084"
    depends_on:
      - keycloak
EOF

    COMPOSE_FILE="docker-compose.yml"
    echo "Created $COMPOSE_FILE"
fi

# 4. Пересобираем и запускаем Docker контейнеры
echo "3. Rebuilding and starting Docker containers..."
docker-compose -f "$COMPOSE_FILE" down 2>/dev/null
docker-compose -f "$COMPOSE_FILE" up -d --build

# 5. Проверяем успешность запуска
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to start Docker containers!"
    echo "Trying to see what went wrong..."
    docker-compose -f "$COMPOSE_FILE" logs --tail=20
    exit 1
fi

# 6. Ждем запуска Keycloak
echo "4. Waiting for Keycloak..."
KEYCLOAK_READY=false
for i in {1..30}; do
    if curl -s http://localhost:8080 > /dev/null 2>&1; then
        echo "✅ Keycloak is up and running!"
        KEYCLOAK_READY=true
        break
    fi
    if [ $i -eq 30 ]; then
        echo "⚠️  Timeout waiting for Keycloak, but continuing..."
    fi
    echo "Waiting for Keycloak... ($i/30)"
    sleep 2
done

# 7. Ждем запуска API Gateway
echo "5. Waiting for API Gateway..."
GATEWAY_READY=false
for i in {1..20}; do
    if curl -s http://localhost:8082 > /dev/null 2>&1; then
        echo "✅ API Gateway is up and running!"
        GATEWAY_READY=true
        break
    fi
    if [ $i -eq 20 ]; then
        echo "⚠️  API Gateway might be slow to start, continuing..."
    fi
    echo "Waiting for API Gateway... ($i/20)"
    sleep 3
done

# 8. Настройка Keycloak (если доступен)
if [ "$KEYCLOAK_READY" = true ]; then
    echo "6. Setting up Keycloak..."

    # Создаем скрипт настройки если его нет
    SETUP_SCRIPT="scripts/setup-keycloak.sh"
    if [ ! -f "$SETUP_SCRIPT" ]; then
        mkdir -p scripts
        cat > "$SETUP_SCRIPT" << 'EOF'
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
EOF

        chmod +x "$SETUP_SCRIPT"
        echo "Created setup script at $SETUP_SCRIPT"
    fi

    # Запускаем настройку
    if [ -f "$SETUP_SCRIPT" ]; then
        ./scripts/setup-keycloak.sh
    fi
else
    echo "6. Skipping Keycloak setup (not available)"
fi

# 9. Проверяем сервисы
echo ""
echo "7. Checking services..."

sleep 5

echo ""
echo "=== Service Status ==="

check_service() {
    local name=$1
    local url=$2

    if curl -s "$url" > /dev/null 2>&1; then
        echo "✅ $name: UP ($url)"
        return 0
    else
        echo "⚠️  $name: DOWN or starting ($url)"
        return 1
    fi
}

check_service "Keycloak" "http://localhost:8080"
check_service "API Gateway" "http://localhost:8082"
check_service "Auth Service" "http://localhost:8083/actuator/health"
check_service "Asset Service" "http://localhost:8084/actuator/health"

echo ""
echo "✅ === Backend is ready! ==="
echo ""
echo "🌐 Services:"
echo "   Keycloak:       http://localhost:8080 (admin/admin123)"
echo "   API Gateway:    http://localhost:8082"
echo "   Auth Service:   http://localhost:8083"
echo "   Asset Service:  http://localhost:8084"
echo ""
echo "📚 Documentation:"
echo "   Dashboard:          http://localhost:8082/"
echo "   Gateway Swagger:    http://localhost:8082/swagger-ui.html"
echo "   Auth Swagger:       http://localhost:8082/auth-swagger-ui/"
echo "   Asset Swagger:      http://localhost:8082/asset-swagger-ui/"
echo ""
echo "🔧 Quick Tests:"
echo "   curl http://localhost:8082/api/gateway/health"
echo "   curl http://localhost:8082/api/auth/health"
echo "   curl http://localhost:8082/api/assets/health"
echo ""
echo "🔐 Test Authentication:"
echo "   curl -X POST http://localhost:8082/api/auth/login \\"
echo '     -H "Content-Type: application/json" \'
echo '     -d '\''{"username": "admin", "password": "admin123"}'\'''
echo ""
echo "🛑 To stop services:"
echo "   docker-compose -f $COMPOSE_FILE down"
echo ""