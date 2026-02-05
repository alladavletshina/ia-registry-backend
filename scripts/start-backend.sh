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

if [ ! -d "auth-service" ]; then
    echo "ERROR: Directory 'auth-service' not found!"
    exit 1
fi

if [ ! -d "asset-service" ]; then
    echo "ERROR: Directory 'asset-service' not found!"
    exit 1
fi

# 2. Build all services
echo "2. Building services..."
cd api-gateway && mvn clean package -DskipTests && cd ..
cd auth-service && mvn clean package -DskipTests && cd ..
cd asset-service && mvn clean package -DskipTests && cd ..

# 3. Проверяем наличие docker-compose файла
COMPOSE_FILE="docker-compose.keycloak.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "ERROR: Docker Compose file '$COMPOSE_FILE' not found!"
    echo "Available files:"
    ls -la *.yml *.yaml 2>/dev/null || echo "No YAML files found"
    exit 1
fi

# 4. Start everything with Docker Compose
echo "3. Starting Docker containers..."
docker-compose -f "$COMPOSE_FILE" up -d

# 5. Проверяем успешность запуска
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to start Docker containers!"
    echo "Trying to see what went wrong..."
    docker-compose -f "$COMPOSE_FILE" logs
    exit 1
fi

# 6. Wait for Keycloak
echo "4. Waiting for Keycloak..."
for i in {1..30}; do
    if curl -s http://localhost:8080 > /dev/null; then
        echo "Keycloak is up and running!"
        break
    fi
    echo "Waiting for Keycloak... ($i/30)"
    sleep 2
done

# 7. Проверяем наличие скрипта настройки Keycloak
SETUP_SCRIPT="scripts/setup-keycloak.sh"
if [ ! -f "$SETUP_SCRIPT" ]; then
    echo "WARNING: Keycloak setup script not found at $SETUP_SCRIPT"
    echo "Creating minimal setup script..."
    # Создаем простой скрипт
    cat > "$SETUP_SCRIPT" << 'EOF'
#!/bin/bash
echo "Skipping Keycloak setup (script not fully implemented)"
echo "You can access Keycloak at http://localhost:8080"
echo "Default admin credentials: admin / admin123"
EOF
    chmod +x "$SETUP_SCRIPT"
fi

# 8. Setup Keycloak
echo "5. Setting up Keycloak..."
./scripts/setup-keycloak.sh

echo ""
echo "=== Backend is ready! ==="
echo ""
echo "Services:"
echo "- Keycloak: http://localhost:8081"  # Изменено с 8080 на 8081 согласно docker-compose
echo "- API Gateway: http://localhost:8082"
echo "- Auth Service: http://localhost:8083"
echo "- Asset Service: http://localhost:8084"
echo ""
echo "Test credentials: admin / admin123"
echo ""
echo "To stop services, run: docker-compose -f $COMPOSE_FILE down"