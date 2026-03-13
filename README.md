# FDM‑Infrastructure (Docker Compose для FDM)

## Оглавление
1. [Общее описание](#общее-описание)
2. [Архитектура и список сервисов](#архитектура-и-список-сервисов)
3. [Требования / Предустановки](#требования--предустановки)
4. [Быстрый старт (Docker Compose)](#быстрый-старт-docker-compose)
5. [Режимы аутентификации](#режимы-аутентификации)
6. [Конфигурация (переменные окружения)](#конфигурация-переменные-окружения)
7. [Управление средой (остановка, очистка)](#управление-средой)
8. [Полезные URL и порты](#полезные-url-и-порты)
9. [Postman](#postman)
10. [Лицензия](#лицензия)

---

## 1 Общее описание
`fdm-infrastructure` — репозиторий с **единым `docker-compose.yml`** для поднятия FDM‑системы (gateway + микросервисы + инфраструктура).
Сервисы находятся в одной Docker‑сети `fdm-network` и используют общую инфраструктуру:

| Компонент | Что делает                                       | Образ |
|-----------|--------------------------------------------------|-------|
| **PostgreSQL** (`postgres`) | База бизнес‑данных `fdm_db`                      | `postgres:15‑alpine` |
| **RabbitMQ** (`rabbitmq`) | Асинхронный брокер сообщений                     | `rabbitmq:3‑management‑alpine` |
| **Neo4j** (`neo4j`) | Графовая БД для архитектурных моделей            | `neo4j:5‑community` |
| **Gateway** (`gateway`) | API‑шлюз, маршрутизация запросов к микросервисам | локальная сборка (`services/gateway/Dockerfile`) |
| **FDM‑Auth‑Backend** (`fdm-auth-backend`) | Аутентификация/авторизация                       | локальная сборка (`services/fdm-auth-service/Dockerfile`) |
| **Capability‑Backend** (`capability-backend`) | Управление capability‑моделями                   | локальная сборка (`services/capability-service/Dockerfile`) |
| **Architect‑Graph‑Service** (`architect-graph-service`) | Работа с Neo4j‑графом (архитектуры)              | локальная сборка (`services/architect-graph-service/Dockerfile`) |
| **Products‑Service** (`products-service`) | CRUD‑операции над продуктами                     | локальная сборка (`services/products-service/Dockerfile`) |
| **Techradar‑Backend** (`techradar-backend`) | Хранилище технологий и моделей                   | локальная сборка (`services/techradar-service/Dockerfile`) |
| **Structurizr‑Backend** (`structurizr_backend`) | API для построения диаграмм (FastAPI)            | локальная сборка (`services/structurizr_backend/Dockerfile`) |

Аутентификация/авторизация может работать в **двух режимах** (см. раздел ниже):

- **DEMO_AUTH**: быстрый старт “из коробки”, без внешнего IdP и без обязательных заголовков.
- **AUTHENTIC_AUTH**: включаем Authentik (IdP) и связанные сервисы (Redis + Authentik Postgres) через профили Docker Compose.

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
git clone https://github.com/tech-beeline/fdm-infrastructure.git
cd fdm-infrastructure
```

### 4.2 Запуск в DEMO режиме (рекомендуется для первого поднятия)
В этом режиме gateway работает с `app.demo-auth=true` и “подкладывает” тестового пользователя, поэтому можно делать запросы без токенов/спец‑заголовков.

По умолчанию DEMO уже включён в `docker-compose.yml` для сервиса `gateway`, поэтому достаточно:

```bash
docker compose up -d --build
```

Если нужно **явно** переопределить (например, при локальных экспериментах), можно запустить так:

```powershell
$env:DEMO_AUTH="true"
$env:AUTHENTIC_AUTH="false"
docker compose up -d --build
```

После старта в БД автоматически появятся тестовые данные:

- **Пользователь по умолчанию**: `defaultUser` (создаётся миграцией `services/fdm-auth-service/.../V0007__Add_role.sql`)
- **Продукт по умолчанию**: `default product` с alias `dflt` и привязкой к user_id=0 (миграция `services/products-service/.../V0031__add_default_product.sql`)

### 4.3 Запуск в AUTHENTIC режиме (Authentik IdP)
Этот режим поднимает Authentik и его зависимости **через профили Docker Compose** (в `docker-compose.yml` профиль задаётся значением переменной `AUTHENTIC_AUTH`).

PowerShell (Windows):

```powershell
$env:DEMO_AUTH="false"
$env:AUTHENTIC_AUTH="true"
docker compose --profile true up -d --build
```

Примечания:

- В AUTHENTIC режиме дополнительно стартуют `redis`, `authentik-postgres`, `authentik-server`, `authentik-worker`.
- Если вы запускаете без `--profile true`, эти контейнеры не поднимутся.

### 4.4 Проверка «здоровья»
```bash
docker compose ps
# все сервисы должны иметь статус "healthy" или "Up"
```
Для Spring‑сервисов доступен `/actuator/health` (порты см. в compose):

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8081/actuator/health   # fdm‑auth‑backend
curl -s http://localhost:8082/actuator/health   # capability‑backend
...
```

---

## 5 Режимы аутентификации

### 5.1 DEMO_AUTH
Используй, когда нужно быстро поднять систему и выполнять тестовые запросы к API без внешней авторизации.

- Включается переменной окружения **`DEMO_AUTH=true`** (см. `services/gateway/src/main/resources/application.yml`).
- Gateway инжектит тестового пользователя и проксирует запросы на бэкенды.

### 5.2 AUTHENTIC_AUTH (Authentik)
Используй, когда нужна интеграция с IdP (OAuth2/OIDC) и проверка токенов.

- Включается переменной окружения **`AUTHENTIC_AUTH=true`**.
- Поднимает Authentik‑стек через профиль Compose: запускать с `docker compose --profile true ...`.

---

## 6 Конфигурация (переменные окружения)

| Сервис | Пример переменной | Описание |
|--------|-------------------|----------|
| **gateway** | `SPRING_PROFILES_ACTIVE=local` | Профиль Spring Boot |
| **gateway** | `DEMO_AUTH=true/false` | Режим demo‑аутентификации |
| **gateway** | `AUTHENTIC_AUTH=true/false` | Режим с Authentik |
| **gateway** | `INTEGRATION_AUTHENTIC_AUTH_URL=...` | URL приложения/issuer в Authentik |
| **fdm‑auth‑backend** | `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fdm_db` | Подключение к основной БД |
| **capability‑backend** | `SPRING_RABBITMQ_HOST=fdm-rabbitmq` | Хост RabbitMQ |
| **architect‑graph‑service** | `SPRING_NEO4J_URI=bolt://neo4j:7687` | Подключение к Neo4j |
| **postgres** | `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` | Данные для основной БД (`fdm_db`) |
| **structurizr_backend** | Читаются из `services/structurizr_backend/.env` | Пример: `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fdm_db` |

### Где задавать переменные

- **PowerShell**: `$env:VAR="value"` перед запуском `docker compose ...`
- **`.env` в корне репозитория**: Docker Compose автоматически подхватит значения

> Для включения Authentik‑профиля используйте `docker compose --profile true ...` (см. выше).

---

## 7 Управление средой (остановка, очистка)

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

---

## 8 Полезные URL и порты

| Компонент | URL | Примечание |
|---|---|---|
| **Gateway** | `http://localhost:8080` | единая точка входа |
| **RabbitMQ UI** | `http://localhost:15672` | `guest/guest` |
| **Neo4j Browser** | `http://localhost:7474` | `neo4j/password` |
| **Structurizr Backend** | `http://localhost:8086/docs` | OpenAPI UI |
| **Authentik UI** | `http://localhost:5000` | только при `AUTHENTIC_AUTH=true` + `--profile true` |

---

## 9 Postman

В папке `postman/` лежит **Postman‑коллекция для Gateway**, чтобы быстро выполнять тестовые запросы к API.

- Импорт: Postman → **Import** → выбрать файл коллекции из `postman/`
- Рекомендуется завести переменную окружения `baseUrl` (например `http://localhost:8080`) и использовать её в запросах

---

## 10 Лицензия
Проект распространяется под лицензией **Apache License 2.0**.  
Смотрите файл `LICENSE` в корне репозитория для подробностей.

---

## 📌 Краткий чек‑лист

| Шаг | Команда | Что проверяется |
|-----|---------|-----------------|
| **Клонирование** | `git clone … && cd fdm-infrastructure` | репозиторий |
| **DEMO старт** | `docker compose up -d --build` | быстрый запуск |
| **AUTHENTIC старт** | `docker compose --profile true up -d --build` | запуск с Authentik |
| **База готова** | `docker exec -it fdm-postgres psql -U postgres -d fdm_db -c "\dt"` | таблицы созданы |
| **UI** | открыть `http://localhost:8080` | Swagger / API |
| **Остановка** | `docker compose down -v` | чистая среда |

Если какой‑то сервис не проходит health‑check — смотрите логи:

```bash
docker compose logs -f <service-name>
```

Например:

```bash
docker compose logs -f gateway
docker compose logs -f fdm-auth-backend
docker compose logs -f products-service
```