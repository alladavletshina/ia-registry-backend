User Management Service (Сервис управления пользователями)
Порт: 8085
База данных: PostgreSQL users

Функционал:
- CRUD для пользователей
- Управление ролями и правами
- История активности пользователей
- Смена пароля, блокировка/разблокировкаcurl http://localhost:8082/api/users/health

Как проверить Swagger UI:

- Swagger UI: http://localhost:8085/swagger-ui.html
- OpenAPI JSON: http://localhost:8085/api-docs
- Через API Gateway (если маршрутизация настроена):
http://localhost:8082/swagger-ui.html – если Gateway перенаправляет запросы к user-service 