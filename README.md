# FDM‑Infrastructure (Full‑stack микросервисная платформа)

## Оглавление
1. [Общее описание](#общее-описание)
2. [Архитектура и список сервисов](#архитектура-и-список-сервисов)
3. [Требования / Предустановки](#требования--предустановки)
4. [Быстрый старт (Docker Compose)](#быстрый-старт-docker-compose)
5. [Сборка и запуск отдельных сервисов](#сборка-и-запуск-отдельных-сервисов)
6. [Конфигурация (переменные окружения)](#конфигурация-переменные-окружения)
7. [Управление средой (остановка, очистка, миграции)](#управление-средой)
8. [Лицензия](#лицензия)

---

## 1 Общее описание
fdm-infrastructure — коллекция микросервисов, реализованных на **Spring Boot (Java 17)**, **FastAPI (Python)** и **Neo4j**.  
Все сервисы находятся в одной Docker‑сети `fdm-network` и используют общую инфраструктуру:

| Компонент | Что делает                                       | Образ |
|-----------|--------------------------------------------------|-------|
| **PostgreSQL** (`postgres`) | База бизнес‑данных `fdm_db`                      | `postgres:15‑alpine` |
| **RabbitMQ** (`rabbitmq`) | Асинхронный брокер сообщений                     | `rabbitmq:3‑management‑alpine` |
| **Redis** (`redis`) | Кеш/Session‑store (используется Authentik)       | `redis:7‑alpine` |
| **Neo4j** (`neo4j`) | Графовая БД для архитектурных моделей            | `neo4j:5‑community` |
| **Authentik** (`authentik‑postgres`, `authentik‑server`, `authentik‑worker`) | Open‑Source IAM (SSO, OAuth2)                    | `ghcr.io/goauthentik/server:2025.10.2` |
| **Gateway** (`gateway`) | API‑шлюз, маршрутизация запросов к микросервисам | локальная сборка (`services/gateway/Dockerfile`) |
| **FDM‑Auth‑Backend** (`fdm-auth-backend`) | Аутентификация/авторизация                       | локальная сборка (`services/fdm-auth-service/Dockerfile`) |
| **Capability‑Backend** (`capability-backend`) | Управление capability‑моделями                   | локальная сборка (`services/capability-service/Dockerfile`) |
| **Architect‑Graph‑Service** (`architect-graph-service`) | Работа с Neo4j‑графом (архитектуры)              | локальная сборка (`services/architect-graph-service/Dockerfile`) |
| **Products‑Service** (`products-service`) | CRUD‑операции над продуктами                     | локальная сборка (`services/products-service/Dockerfile`) |
| **Techradar‑Backend** (`techradar-backend`) | Хранилище технологий и моделей                   | локальная сборка (`services/techradar-service/Dockerfile`) |
| **Structurizr‑Backend** (`structurizr_backend`) | API для построения диаграмм (FastAPI)            | локальная сборка (`services/structurizr_backend/Dockerfile`) |

Все сервисы используют **Spring Boot JPA + Flyway** для миграций схемы, а аутентификация реализована через **Authentik** (OAuth2/OpenID Connect).

---

## 2 Архитектура и список сервисов
```text
+-------------------+          +-------------------+          +-------------------+
|  Authentik Server | <----->  |   Authentik DB   |          |      Redis        |
|  (SSO/OIDC)       |          | (Postgres)        |          |   (Cache)         |
+-------------------+          +-------------------+          +-------------------+
          |                                 |
          |  (OAuth2 tokens)                |
          v                                 v
+-------------------+          +-------------------+          +-------------------+
|   Gateway (API)   | <------> |   RabbitMQ       | <------> |  PostgreSQL (fdm) |
|   (Zuul/Edge)    |          |   (messaging)    |          |   (fdm_db)        |
+-------------------+          +-------------------+          +-------------------+
          |                                 |
          |   REST/GraphQL                  |
          v                                 v
+-------------------+   +-------------------+   +-------------------+
|  Capability       |   |  Products         |   |  TechRadar        |
|  Backend          |   |  Service          |   |  Backend          |
+-------------------+   +-------------------+   +-------------------+
          |                                 |
          |    Neo4j (Graph)                |
          v                                 v
+-------------------+   +-------------------+
|  Architect‑Graph  |   |  Structurizr      |
|  Service          |   |  Backend (FastAPI)|
+-------------------+   +-------------------+
```

* **Gateway** – единственная точка входа (`http://localhost:8080`).
* **RabbitMQ** – используется всеми сервисами для асинхронных задач и событий.
* **PostgreSQL** – совместно используется несколькими сервисами (auth, capability, products, techradar) – у каждого сервисa отдельная схема.
* **Neo4j** – хранит графовую модель архитектуры, к которой обращается `architect-graph-service`.

---

## 3 Требования / Предустановки
| Что | Минимальная версия |
|-----|--------------------|
| **Docker Engine** | 20.10+ |
| **Docker Compose** (v2‑плагин) | 2.0+ |
| **Java** | 17 (JDK) – только если собираете сервисы без Docker |
| **Maven** | 3.9+ (или Gradle) |
| **Python** | 3.9+ (для `structurizr_backend`) |
| **Git** | любой |

> **Важно** – Убедитесь, что порты `5433`, `5434`, `5672`, `15672`, `6379`, `7474`, `7687`, `8080‑8086` свободны. При конфликте измените их в `docker‑compose.yml`.

---

## 4 Быстрый старт (Docker Compose)

### 4.1 Клонирование репозитория
```bash
git clone github	https://github.com/tech-beeline/fdm-infrastructure.git
cd fdm-infrastructure
```

### 4.2 Подготовка `.env` (опционально)
`structurizr_backend` уже имеет файл `services/structurizr_backend/.env`.  
Если нужно переопределить переменные, создайте `.env` в корне проекта – Docker‑Compose автоматически подхватит его.

```bash
cp services/structurizr_backend/.env.example services/structurizr_backend/.env
# отредактируйте при необходимости
```

### 4.3 Запуск инфраструктуры и всех сервисов
```bash
docker compose up -d          # поднимет все сервисы в фоне
```
Docker Compose выполнит:
* Инициализацию баз (Postgres, Neo4j) и выполнит Flyway‑миграции.
* Запуск `init.sh` для RabbitMQ (создание очередей).
* Запуск Authentik + Redis.
* Сборку и старт всех микросервисов (gateway, auth‑backend, capability‑backend, …).

### 4.4 Проверка «здоровья»
```bash
docker compose ps
# все сервисы должны иметь статус "healthy" или "Up"
```
Для каждого Spring‑сервиса доступен `/actuator/health`:

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8081/actuator/health   # fdm‑auth‑backend
curl -s http://localhost:8082/actuator/health   # capability‑backend
...
```

### 4.5 Доступ к UI
| Сервис | URL | Что увидеть |
|--------|-----|--------------|
| **Gateway** | `http://localhost:8080` | Swagger UI (если включён) |
| **RabbitMQ** | `http://localhost:15672` | Management UI (guest/guest) |
| **Neo4j** | `http://localhost:7474` | Neo4j Browser (логин `neo4j`, пароль `password`) |
| **Authentik** | `http://localhost:5000` | UI Authentik |
| **Structurizr Backend** | `http://localhost:8086/docs` | OpenAPI UI (Swagger) |

---

## 5 Сборка и запуск отдельных сервисов
Каждый сервис находится в `services/<service‑name>`. По‑умолчанию используется **Maven** (или **Gradle** – смотрите `pom.xml`/`build.gradle`).

### Пример: `capability-backend`
```bash
cd services/capability-service
docker build -t capability-backend:dev .
docker compose -f docker-compose.yml up -d capability-backend
```

### FastAPI‑сервис (`structurizr_backend`)
```bash
cd services/structurizr_backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn src.main:app --host 0.0.0.0 --port 8080
```
Но в проде обычно используется образ из `docker‑compose.yml`.

---

## 6 Конфигурация (переменные окружения)

| Сервис | Пример переменной | Описание |
|--------|-------------------|----------|
| **gateway** | `SPRING_PROFILES_ACTIVE=local` | Профиль Spring Boot |
| **gateway** | `AUTHENTIK_HOST=http://authentik-server:9000` | URL Authentik‑сервера |
| **fdm‑auth‑backend** | `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fdm_db` | Подключение к основной БД |
| **capability‑backend** | `SPRING_RABBITMQ_HOST=fdm-rabbitmq` | Хост RabbitMQ |
| **architect‑graph‑service** | `SPRING_NEO4J_URI=bolt://neo4j:7687` | Подключение к Neo4j |
| **postgres** | `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` | Данные для основной БД (`fdm_db`) |
| **authentik‑postgres** | `POSTGRES_USER=authentik` … | Данные для БД Authentik |
| **redis** | (нет переменных, используется стандартный порт 6379) | |
| **structurizr_backend** | Читаются из `services/structurizr_backend/.env` | Пример: `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fdm_db` |

*Чтобы переопределить переменную, укажите её в корневом `.env`‑файле или в `docker‑compose.override.yml`.*

---


## 7 Управление средой (остановка, очистка, миграции)

### Остановка всех сервисов
```bash
docker compose down          # остановит контейнеры, но оставит тома
```

### Полная очистка (удалить тома и сети)
```bash
docker compose down -v       # удалит контейнеры + тома (БД, очередь, Neo4j и т.п.)
docker network rm fdm-network   # если сеть не удалилась автоматически
```

### Перезапуск отдельного сервиса
```bash
docker compose restart capability-backend
```

### Просмотр логов
```bash
docker compose logs -f gateway
docker compose logs -f capability-backend
```

### Выполнение миграций вручную
Flyway запускается автоматически при старте сервиса.  
Для ручной проверки:

```bash
docker exec -it fdm-postgres bash
psql -U postgres -d fdm_db -c "SELECT * FROM flyway_schema_history;"
```

---

## 8 Лицензия
Проект распространяется под лицензией **Apache License 2.0**.  
Смотрите файл `LICENSE` в корне репозитория для подробностей.

---

## 📌 Краткий чек‑лист для разработки

| Шаг | Команда | Что проверяется |
|-----|---------|-----------------|
| **Клонирование** | `git clone … && cd fdm-platform` | репозиторий |
| **Подготовка** | `docker compose pull && docker compose up -d` | инфраструктура |
| **База готова** | `docker exec -it fdm-postgres psql -U postgres -d fdm_db -c "\dt"` | таблицы созданы |
| **UI** | открыть `http://localhost:8080` | Swagger / API |
| **Остановка** | `docker compose down -v` | чистая среда |

Если какой‑то сервис не проходит health‑check — смотрите логи:

```bash
docker compose logs -f <service-name>