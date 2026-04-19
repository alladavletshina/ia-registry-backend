#!/bin/bash

echo "=== Starting Asset Management Backend (Deploy version with docker compose) ==="

# Определяем корневую директорию проекта
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "Project root: $PROJECT_ROOT"

cd "$PROJECT_ROOT" || exit 1

# 1. Проверяем существование директорий
echo "1. Checking project structure..."

# Проверяем обязательные директории (ядро системы)
CORE_SERVICES=("api-gateway" "asset-service")
for dir in "${CORE_SERVICES[@]}"; do
    if [ ! -d "$dir" ]; then
        echo "ERROR: Core directory '$dir' not found!"
        echo "Current directory: $(pwd)"
        echo "Available directories:"
        ls -la
        exit 1
    fi
done

# Дополнительные сервисы (не обязательные, но желательные)
ADDITIONAL_SERVICES=("user-service" "task-service" "notification-service" "audit-service" "report-service")
for dir in "${ADDITIONAL_SERVICES[@]}"; do
    if [ ! -d "$dir" ]; then
        echo "INFO: Optional directory '$dir' not found, will skip..."
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

    # Проверяем, какой инструмент сборки используется
    if [ -f "pom.xml" ]; then
        BUILD_TOOL="mvn"
    elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
        BUILD_TOOL="./gradlew"
    else
        echo "ERROR: No build tool detected for $service_name"
        cd ..
        return 1
    fi

    echo "Using $BUILD_TOOL for $service_name..."

    if [ "$BUILD_TOOL" = "mvn" ]; then
        if ! mvn clean package -DskipTests; then
            echo "ERROR: Failed to build $service_name with Maven"
            echo "Checking for compilation errors..."
            mvn clean compile -DskipTests 2>&1 | grep -A5 -B5 "ERROR"
            cd ..
            return 1
        fi
    else
        # Gradle
        if [ ! -x "./gradlew" ]; then
            chmod +x ./gradlew
        fi
        if ! ./gradlew clean build -x test; then
            echo "ERROR: Failed to build $service_name with Gradle"
            cd ..
            return 1
        fi
    fi

    cd ..
    echo "✅ $service_name build completed"
    return 0
}

# Собираем все сервисы
FAILED_BUILDS=0

echo "Building core services..."
build_service "api-gateway" || FAILED_BUILDS=$((FAILED_BUILDS + 1))
build_service "asset-service" || FAILED_BUILDS=$((FAILED_BUILDS + 1))

echo "Building additional services..."
for service in "${ADDITIONAL_SERVICES[@]}"; do
    if [ -d "$service" ]; then
        build_service "$service" || FAILED_BUILDS=$((FAILED_BUILDS + 1))
    fi
done

if [ $FAILED_BUILDS -gt 0 ]; then
    echo "WARNING: $FAILED_BUILDS service(s) failed to build, but continuing..."
fi

# 3. Проверяем наличие docker-compose файла
COMPOSE_FILES=("docker-compose.yml" "docker-compose.keycloak.yml" "docker-compose.full.yml")
COMPOSE_FILE=""

for file in "${COMPOSE_FILES[@]}"; do
    if [ -f "$file" ]; then
        COMPOSE_FILE="$file"
        echo "Found Docker Compose file: $COMPOSE_FILE"
        break
    fi
done

if [ -z "$COMPOSE_FILE" ]; then
    echo "WARNING: No Docker Compose file found!"
    echo "Available files:"
    ls -la *.yml *.yaml 2>/dev/null
    echo "Creating full docker-compose.yml with all services..."

    cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  # Keycloak (Identity Provider)
  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      KC_HOSTNAME: localhost
    ports:
      - "8080:8080"

  # Core Services
  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    ports:
      - "8082:8082"
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

  # Additional Services (will be built if directories exist)
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
      args:
        SKIP_BUILD: ${SKIP_USER_SERVICE:-true}
    ports:
      - "8085:8085"
    depends_on:
      - keycloak
    environment:
      SKIP_SERVICE: ${SKIP_USER_SERVICE:-true}

  task-service:
    build:
      context: ./task-service
      dockerfile: Dockerfile
      args:
        SKIP_BUILD: ${SKIP_TASK_SERVICE:-true}
    ports:
      - "8086:8086"
    depends_on:
      - keycloak
    environment:
      SKIP_SERVICE: ${SKIP_TASK_SERVICE:-true}

  notification-service:
    build:
      context: ./notification-service
      dockerfile: Dockerfile
      args:
        SKIP_BUILD: ${SKIP_NOTIFICATION_SERVICE:-true}
    ports:
      - "8087:8087"
    depends_on:
      - keycloak
    environment:
      SKIP_SERVICE: ${SKIP_NOTIFICATION_SERVICE:-true}

  audit-service:
    build:
      context: ./audit-service
      dockerfile: Dockerfile
      args:
        SKIP_BUILD: ${SKIP_AUDIT_SERVICE:-true}
    ports:
      - "8088:8088"
    depends_on:
      - keycloak
    environment:
      SKIP_SERVICE: ${SKIP_AUDIT_SERVICE:-true}

  report-service:
    build:
      context: ./report-service
      dockerfile: Dockerfile
      args:
        SKIP_BUILD: ${SKIP_REPORT_SERVICE:-true}
    ports:
      - "8089:8089"
    depends_on:
      - keycloak
    environment:
      SKIP_SERVICE: ${SKIP_REPORT_SERVICE:-true}
EOF

    COMPOSE_FILE="docker-compose.yml"
    echo "Created $COMPOSE_FILE with conditional service startup"
fi

# 4. Пересобираем и запускаем Docker контейнеры (используем docker compose)
echo "3. Rebuilding and starting Docker containers..."

# Определяем, какие дополнительные сервисы запускать
export SKIP_USER_SERVICE="true"
export SKIP_TASK_SERVICE="true"
export SKIP_NOTIFICATION_SERVICE="true"
export SKIP_AUDIT_SERVICE="true"
export SKIP_REPORT_SERVICE="true"

# Проверяем существование директорий и изменяем переменные
[ -d "user-service" ] && export SKIP_USER_SERVICE="false"
[ -d "task-service" ] && export SKIP_TASK_SERVICE="false"
[ -d "notification-service" ] && export SKIP_NOTIFICATION_SERVICE="false"
[ -d "audit-service" ] && export SKIP_AUDIT_SERVICE="false"
[ -d "report-service" ] && export SKIP_REPORT_SERVICE="false"

echo "Service startup configuration:"
echo "  user-service: $([ "$SKIP_USER_SERVICE" = "false" ] && echo "ENABLED" || echo "SKIPPED")"
echo "  task-service: $([ "$SKIP_TASK_SERVICE" = "false" ] && echo "ENABLED" || echo "SKIPPED")"
echo "  notification-service: $([ "$SKIP_NOTIFICATION_SERVICE" = "false" ] && echo "ENABLED" || echo "SKIPPED")"
echo "  audit-service: $([ "$SKIP_AUDIT_SERVICE" = "false" ] && echo "ENABLED" || echo "SKIPPED")"
echo "  report-service: $([ "$SKIP_REPORT_SERVICE" = "false" ] && echo "ENABLED" || echo "SKIPPED")"

# Замена docker-compose на docker compose
docker compose -f "$COMPOSE_FILE" down 2>/dev/null
docker compose -f "$COMPOSE_FILE" up -d --build

# 5. Проверяем успешность запуска
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to start Docker containers!"
    echo "Trying to see what went wrong..."
    docker compose -f "$COMPOSE_FILE" logs --tail=20
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

# 7. Ждем запуска сервисов
echo "5. Waiting for services to start..."

SERVICES_TO_CHECK=(
    "8082:API Gateway"
    "8084:Asset Service"
)

# Добавляем дополнительные сервисы, если они запущены
[ "$SKIP_USER_SERVICE" = "false" ] && SERVICES_TO_CHECK+=("8085:User Service")
[ "$SKIP_TASK_SERVICE" = "false" ] && SERVICES_TO_CHECK+=("8086:Task Service")
[ "$SKIP_NOTIFICATION_SERVICE" = "false" ] && SERVICES_TO_CHECK+=("8087:Notification Service")
[ "$SKIP_AUDIT_SERVICE" = "false" ] && SERVICES_TO_CHECK+=("8088:Audit Service")
[ "$SKIP_REPORT_SERVICE" = "false" ] && SERVICES_TO_CHECK+=("8089:Report Service")

for service_info in "${SERVICES_TO_CHECK[@]}"; do
    port=$(echo "$service_info" | cut -d: -f1)
    name=$(echo "$service_info" | cut -d: -f2)

    SERVICE_READY=false
    for i in {1..15}; do
        if curl -s "http://localhost:$port" > /dev/null 2>&1 ||
           curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1 ||
           curl -s "http://localhost:$port/health" > /dev/null 2>&1; then
            echo "✅ $name is up and running! (port $port)"
            SERVICE_READY=true
            break
        fi
        if [ $i -eq 15 ]; then
            echo "⚠️  $name might be slow to start (port $port)..."
        fi
        sleep 2
    done
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

        echo "✅ Keycloak setup attempted"
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
echo "  - asset-backend (backend services)"
echo "  - asset-frontend (React frontend)"
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
check_service "Asset Service" "http://localhost:8084/actuator/health"

[ "$SKIP_USER_SERVICE" = "false" ] && check_service "User Service" "http://localhost:8085/actuator/health"
[ "$SKIP_TASK_SERVICE" = "false" ] && check_service "Task Service" "http://localhost:8086/actuator/health"
[ "$SKIP_NOTIFICATION_SERVICE" = "false" ] && check_service "Notification Service" "http://localhost:8087/actuator/health"
[ "$SKIP_AUDIT_SERVICE" = "false" ] && check_service "Audit Service" "http://localhost:8088/actuator/health"
[ "$SKIP_REPORT_SERVICE" = "false" ] && check_service "Report Service" "http://localhost:8089/actuator/health"

echo ""
echo "✅ === Backend is ready! ==="
echo ""

echo "🌐 Services:"
echo "   Keycloak:           http://localhost:8080 (admin/admin123)"
echo "   API Gateway:        http://localhost:8082"
echo "   Asset Service:      http://localhost:8084"
[ "$SKIP_USER_SERVICE" = "false" ] && echo "   User Service:        http://localhost:8085"
[ "$SKIP_TASK_SERVICE" = "false" ] && echo "   Task Service:        http://localhost:8086"
[ "$SKIP_NOTIFICATION_SERVICE" = "false" ] && echo "   Notification Service: http://localhost:8087"
[ "$SKIP_AUDIT_SERVICE" = "false" ] && echo "   Audit Service:       http://localhost:8088"
[ "$SKIP_REPORT_SERVICE" = "false" ] && echo "   Report Service:      http://localhost:8089"

echo ""
echo "📚 Documentation:"
echo "   Gateway Dashboard:    http://localhost:8082/"
echo "   Gateway Swagger:      http://localhost:8082/swagger-ui.html"
echo "   Asset Swagger:        http://localhost:8082/asset-swagger-ui/"
[ "$SKIP_USER_SERVICE" = "false" ] && echo "   User Swagger:        http://localhost:8082/user-swagger-ui/"
[ "$SKIP_TASK_SERVICE" = "false" ] && echo "   Task Swagger:        http://localhost:8082/task-swagger-ui/"
echo ""
echo "🔧 Quick Tests:"
echo "   curl http://localhost:8082/api/gateway/health"
echo "   curl http://localhost:8082/api/assets/health"
[ "$SKIP_USER_SERVICE" = "false" ] && echo "   curl http://localhost:8082/api/users/health"
[ "$SKIP_TASK_SERVICE" = "false" ] && echo "   curl http://localhost:8082/api/tasks/health"
echo ""
echo "⚙️  Configuration:"
echo "   Core services: ✅ Always started"
[ "$SKIP_USER_SERVICE" = "false" ] || echo "   User Service:     ❌ Not found (skip)"
[ "$SKIP_TASK_SERVICE" = "false" ] || echo "   Task Service:     ❌ Not found (skip)"
[ "$SKIP_NOTIFICATION_SERVICE" = "false" ] || echo "   Notification:     ❌ Not found (skip)"
[ "$SKIP_AUDIT_SERVICE" = "false" ] || echo "   Audit Service:    ❌ Not found (skip)"
[ "$SKIP_REPORT_SERVICE" = "false" ] || echo "   Report Service:   ❌ Not found (skip)"
echo ""
echo "🛑 To stop services:"
echo "   docker compose -f $COMPOSE_FILE down"
echo ""