#!/bin/bash

# Конфигурация
PROJECT_NAME="mos_project"
PROJECT_PATH=$(pwd)  # Текущая директория
OUTPUT_DIR="./code_backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_FILE="$OUTPUT_DIR/${PROJECT_NAME}_code_${TIMESTAMP}.txt"
LOG_FILE="$OUTPUT_DIR/backup_log_${TIMESTAMP}.txt"

# Создаем директорию для бэкапов
mkdir -p "$OUTPUT_DIR"

# Файлы для исключения
EXCLUDE_PATTERNS=(
    "*.class"
    "*.jar"
    "*.war"
    "*.ear"
    "*.log"
    "*.tmp"
    "*.temp"
    "*.bak"
    "*.swp"
    "*.swo"
    "*.pyc"
    "*.pyo"
    "Dockerfile"
)

# Директории для исключения
EXCLUDE_DIRS=(
    ".git"
    ".svn"
    ".idea"
    ".vscode"
    "node_modules"
    "target"
    "build"
    "dist"
    "out"
    "__pycache__"
    "*.metadata"
    "bin"
    "lib"
)

# Функция для логирования
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Функция для проверки файла
should_exclude() {
    local file="$1"

    # Проверка по расширению
    for pattern in "${EXCLUDE_PATTERNS[@]}"; do
        if [[ "$file" == $pattern ]]; then
            return 0
        fi
    done

    # Проверка по директории
    for dir in "${EXCLUDE_DIRS[@]}"; do
        if [[ "$file" == *"/$dir/"* ]] || [[ "$file" == "./$dir/"* ]]; then
            return 0
        fi
    done

    return 1
}

# Начало работы
log "Начинаем сохранение кода проекта: $PROJECT_NAME"
log "Путь к проекту: $PROJECT_PATH"
log "Выходной файл: $OUTPUT_FILE"

# Создаем заголовок
{
    echo "=================================================="
    echo "БЭКАП КОДА ПРОЕКТА: $PROJECT_NAME"
    echo "Путь: $PROJECT_PATH"
    echo "Дата: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Версия скрипта: 1.0"
    echo "=================================================="
    echo ""
    echo "СТРУКТУРА ПРОЕКТА:"
    echo "=================="
    find . -type f -name "*.java" -o -name "*.kt" -o -name "*.py" \
        -o -name "*.js" -o -name "*.ts" -o -name "*.html" -o -name "*.css" \
        -o -name "*.xml" -o -name "*.yml" -o -name "*.yaml" \
        -o -name "*.properties" -o -name "*.sh" -o -name "*.sql" \
        -o -name "*.md" -o -name "*.gradle" -o -name "*.kts" \
        | grep -v -E "$(IFS=\|; echo "${EXCLUDE_DIRS[*]}")" \
        | sort
    echo ""
    echo "=================================================="
    echo ""
} > "$OUTPUT_FILE"

# Счетчики
TOTAL_FILES=0
PROCESSED_FILES=0
SKIPPED_FILES=0

# Обрабатываем файлы
find . -type f \( -name "*.java" -o -name "*.kt" -o -name "*.py" \
    -o -name "*.js" -o -name "*.ts" -o -name "*.html" -o -name "*.css" \
    -o -name "*.xml" -o -name "*.yml" -o -name "*.yaml" \
    -o -name "*.properties" -o -name "*.sh" -o -name "*.sql" \
    -o -name "*.md" -o -name "*.gradle" -o -name "*.kts" \) \
    | while read -r file; do

    TOTAL_FILES=$((TOTAL_FILES + 1))

    # Проверяем, нужно ли исключить файл
    if should_exclude "$file"; then
        log "Пропускаем: $file"
        SKIPPED_FILES=$((SKIPPED_FILES + 1))
        continue
    fi

    # Добавляем файл в выходной файл
    log "Обрабатываем: $file"
    {
        echo ""
        echo "===================================================================="
        echo "ФАЙЛ: $file"
        echo "РАЗМЕР: $(du -h "$file" | cut -f1)"
        echo "ПОСЛЕДНЕЕ ИЗМЕНЕНИЕ: $(stat -c %y "$file" 2>/dev/null || echo "N/A")"
        echo "===================================================================="
        echo ""
        cat "$file"
        echo ""
        echo "=== КОНЕЦ ФАЙЛА ==="
    } >> "$OUTPUT_FILE"

    PROCESSED_FILES=$((PROCESSED_FILES + 1))
done

# Добавляем статистику
{
    echo ""
    echo "=================================================="
    echo "СТАТИСТИКА:"
    echo "  Всего найдено файлов: $TOTAL_FILES"
    echo "  Обработано файлов: $PROCESSED_FILES"
    echo "  Пропущено файлов: $SKIPPED_FILES"
    echo "  Размер выходного файла: $(du -h "$OUTPUT_FILE" | cut -f1)"
    echo "=================================================="
} >> "$OUTPUT_FILE"

log "Сохранение завершено!"
log "Статистика:"
log "  Всего файлов: $TOTAL_FILES"
log "  Обработано: $PROCESSED_FILES"
log "  Пропущено: $SKIPPED_FILES"
log "Выходной файл: $OUTPUT_FILE ($(du -h "$OUTPUT_FILE" | cut -f1))"

# Создаем архив (опционально)
tar -czf "${OUTPUT_FILE%.txt}.tar.gz" "$OUTPUT_FILE" "$LOG_FILE" 2>/dev/null
if [ $? -eq 0 ]; then
    log "Создан архив: ${OUTPUT_FILE%.txt}.tar.gz"
fi

# Чистка старых бэкапов (сохраняем только последние 5)
ls -t "$OUTPUT_DIR"/*.txt 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null
ls -t "$OUTPUT_DIR"/*.tar.gz 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null

log "Готово!"