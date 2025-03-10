# Task Management System

REST API система управления задачами с использованием Spring Boot.

Подробное описание архитектуры системы доступно в [ARCHITECTURE.md](ARCHITECTURE.md).

## 🚀 Технологии

- **Java 17**
- **Spring Boot 3.x**
- **Spring Security (JWT-аутентификация)**
- **PostgreSQL**
- **Spring Boot Actuator** (мониторинг)
- **Prometheus & Grafana** (метрики и визуализация)
- **Caffeine Cache** (кэширование)
- **Docker & Docker Compose** (развертывание)

---

## ⚡ Запуск приложения

### 🔧 Development среда

1. **Установите**:
    - Docker
    - Docker Compose
    - Java 17 (если запуск в локальном режиме)

2. **Клонируйте репозиторий**:
   ```sh
   git clone <repository-url>
   cd task-management
   ```

3. **Запустите dev-среду через Docker Compose**:
   ```sh
   # Запуск сервисов разработки (БД, Prometheus, Grafana)
   docker compose -f docker-compose.dev.yml up -d
   
   # Запуск Spring Boot в dev-профиле
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

🛠 Dev среда будет доступна:
- **PostgreSQL**: `localhost:5433`
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
- **Grafana**: [http://localhost:3000](http://localhost:3000) (логин: admin, пароль: admin)

### 🔥 Production среда

1. **Соберите приложение**:
   ```sh
   ./mvnw clean package -DskipTests
   ```

2. **Запустите сервисы**:
   ```sh
   docker compose up -d
   ```

🌍 Production среда доступна:
- **Приложение**: [http://localhost:8080](http://localhost:8080)
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
- **Grafana**: [http://localhost:3000](http://localhost:3000) (логин: admin, пароль: admin)

---

## 📊 Мониторинг и Метрики

### 🔍 **Actuator Endpoints**

| Endpoint | Описание |
|----------|----------|
| `/actuator/health` | Проверка состояния сервиса |
| `/actuator/metrics` | Доступ к метрикам приложения |
| `/actuator/prometheus` | Метрики в формате Prometheus |
| `/actuator/info` | Информация о приложении |
| `/actuator/caches` | Данные о кэше |

🔒 **Доступ к Actuator endpoints** защищен:
- **Username**: `actuator`
- **Password**: `actuator-secret`

### 📊 **Grafana**
1. Войдите в Grafana: [http://localhost:3000](http://localhost:3000)
2. Используйте логин `admin` и пароль `admin`
3. **Добавьте источник данных**:
    - **URL**: `http://prometheus:9090` (prod) или `http://localhost:9090` (dev)
    - **Access**: `Browser`
4. **Импортируйте дашборды**:
    - JVM (Micrometer) **ID: 4701**
    - Spring Boot 3.x Statistics **ID: 17320**

---

## ⚡ Кэширование (Caffeine Cache)

В приложении используется **Caffeine Cache** для ускорения работы.

**📌 Кэш пользователей (users):**
- Максимум: **500 записей**
- Время жизни: **10 минут**
- Очистка при добавлении нового пользователя

**📌 Кэш задач (tasks):**
- Максимум: **500 записей**
- Время жизни: **10 минут**
- Очистка при создании/удалении задачи

📊 **Мониторинг кэша**:
- Доступен через Actuator: [http://localhost:8080/actuator/caches](http://localhost:8080/actuator/caches)
- Метрики в Prometheus/Grafana: **Hits/Misses, размер кэша**

---

## 📌 API Endpoints

📄 **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### 🔹 Основные API:
- **GET** `/api/tasks` – Получение списка задач
- **POST** `/api/tasks` – Создание задачи
- **GET** `/api/tasks/{id}` – Получение задачи по ID
- **PUT** `/api/tasks/{id}` – Обновление задачи
- **DELETE** `/api/tasks/{id}` – Удаление задачи
- **PATCH** `/api/tasks/{id}/status` – Обновление статуса
- **PATCH** `/api/tasks/{id}/assign` – Назначение исполнителя

---

## ⚙️ Разработка

### 📂 **Структура проекта**
```
task-management/
├── src/
│   ├── main/java/  # Код приложения
│   ├── main/resources/  # Конфигурации
│   └── test/  # Тесты
├── docker-compose.yml  # Продакшен среда
├── docker-compose.dev.yml  # Dev среда
├── Dockerfile
└── pom.xml
```

### 🔹 **Профили приложения**
- `dev` – Локальная разработка
- `prod` – Продакшен-среда

### 🛠 Полезные команды
```sh
# Запуск всех тестов
./mvnw clean test

# Сборка приложения
./mvnw clean package

# Запуск приложения в dev-режиме
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Логи контейнеров
docker compose logs -f

# Остановка всех контейнеров
docker compose down

# Полное удаление с volume
docker compose down -v
```

