# Task Manager

[![CI](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=coverage)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)

Веб-приложение для управления задачами на Spring Boot.

**Демо:** https://task-manager-c4wi.onrender.com

## Стек

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Flyway
- Spring Boot Actuator
- H2 (dev)
- PostgreSQL (prod)
- Testcontainers
- Gradle (Kotlin DSL)
- JUnit 5
- JaCoCo
- Spotless (google-java-format, AOSP)
- SonarQube Cloud
- GitHub Actions
- Docker

## Требования

- JDK 21+
- Docker - для запуска интеграционных тестов и сборки образа
- Make (опционально, для удобства)

## Запуск

### Локально

```bash
./gradlew bootRun
```

Приложение: http://localhost:8080/welcome

По умолчанию активируется профиль `development` — приложение стартует
на in-memory **H2** (PostgreSQL compatibility mode), **Flyway** накатывает
миграции при старте.

**H2 Console** доступна по адресу http://localhost:8080/h2-console  
(JDBC URL — как в `application-development.yml`, логин `sa`, пароль пустой).

При старте создаётся администратор из `app.admin.*` (см. раздел
«Администратор по умолчанию»).

### Через Make

```bash
make run      # запуск
make test     # тесты
make lint     # проверка форматирования
make format   # автоформатирование
```

### Docker

```bash
docker build -t task-manager .
docker run --rm -p 8080:8080 task-manager
```

## Профили

Конфигурация разбита по профилям:

| Профиль | Файл | Назначение |
|---------|------|------------|
| `development` | `application-development.yml` | Локальная разработка: H2 in-memory, H2 Console, `show-sql` |
| `production` | `application-production.yml` | Прод: PostgreSQL, креды из переменных окружения, `show-sql` выключен |

Профиль `development` активируется по умолчанию (`spring.profiles.default`).
В проде профиль задаётся явно через переменную окружения:

```bash
SPRING_PROFILES_ACTIVE=production ./gradlew bootRun
```

или в Docker:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e JDBC_DATABASE_URL="jdbc:postgresql://host:5432/db" \
  -e DB_USER=... \
  -e DB_PASSWORD=... \
  -e ADMIN_EMAIL=... \
  -e ADMIN_PASSWORD=... \
  task-manager
```

## Переменные окружения (production)

Профиль **production** не содержит дефолтов — приложение упадёт на
старте, если обязательная переменная не задана.

| Переменная | Обязательна | Описание |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | да | Должна быть `production` |
| `JDBC_DATABASE_URL` | да | JDBC URL, напр. `jdbc:postgresql://host:5432/db?sslmode=require` |
| `DB_USER` | да | Пользователь БД |
| `DB_PASSWORD` | да | Пароль БД |
| `ADMIN_EMAIL` | да | Email администратора, создаётся при старте |
| `ADMIN_PASSWORD` | да | Пароль администратора, создаётся при старте |
| `PORT` | нет | Порт приложения, по умолчанию `8080` |

## Администратор по умолчанию

При старте приложения `DataInitializer` создаёт пользователя-админа
из настроек `app.admin.*`. Создание идёт через `UserService.create`,
поэтому:

- email нормализуется (trim + lower case);
- пароль хешируется **BCrypt**;
- повторный запуск не падает — при существующем email
  `DuplicateEmailException` перехватывается и логируется.

В профиле **development** используются значения
`hexlet@example.com` / `qwerty` (см. `application-development.yml`).
В проде значения обязаны прийти из `ADMIN_EMAIL` и `ADMIN_PASSWORD` —
пароль должен быть сильным.

## Миграции (Flyway)

SQL-миграции лежат в `src/main/resources/db/migration/` и накатываются
автоматически при старте приложения. Именование: `V<n>__<description>.sql`.

Схемой управляет Flyway, Hibernate работает в режиме `validate` —
если маппинг сущностей разойдётся с миграциями, приложение не стартует.

## Разработка

### Тесты

```bash
./gradlew test
```

Интеграционные тесты (`UserRepositoryTest` и подобные) используют
Testcontainers: поднимают реальный PostgreSQL в Docker-контейнере,
накатывают Flyway-миграции и работают с настоящей БД.

**Требуется запущенный Docker.** Если Docker недоступен, такие тесты
упадут с `Could not find a valid Docker environment`.

В CI (GitHub Actions на `ubuntu-latest`) Docker доступен из коробки,
поэтому интеграционные тесты проходят без дополнительной настройки.

Срезовые тесты (`AppApplicationTests`, `WelcomeControllerTest`) идут
на H2 и Docker не требуют.

### Покрытие (JaCoCo)

```bash
./gradlew test jacocoTestReport
```
HTML-отчёт: `build/reports/jacoco/test/html/index.html`

### Форматирование (Spotless)

```bash
./gradlew spotlessApply   # применить
./gradlew spotlessCheck   # проверить
```

## CI/CD

- CI — GitHub Actions: сборка, тесты, покрытие, анализ SonarQube.
- Деплой — Render (Docker).
- Качество — SonarCloud.

## API

Базовый путь для пользователей — `/api/users`.

| Метод | Путь | Тело запроса | Успешный ответ | Описание |
|---|---|---|---|---|
| `GET` | `/welcome` | — | `200` text | Приветствие |
| `GET` | `/actuator/health` | — | `200` JSON | Статус приложения |
| `GET` | `/api/users` | — | `200` JSON-массив, заголовок `X-Total-Count` | Список пользователей |
| `GET` | `/api/users/{id}` | — | `200` JSON | Пользователь по id |
| `POST` | `/api/users` | JSON | `201` JSON, заголовок `Location` | Создание пользователя |
| `PUT` | `/api/users/{id}` | JSON | `200` JSON | Частичное обновление (merge patch) |
| `DELETE` | `/api/users/{id}` | — | `204` | Удаление пользователя |

### Модель пользователя

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "createdAt": "2026-10-09"
}
```

`firstName` и `lastName` — nullable. `passwordDigest` в ответах
никогда не возвращается.

### Создание (`POST /api/users`)

Тело:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "qwerty123"
}
```

Валидация: `email` обязателен и валиден, `password` минимум 3 символа.
Email нормализуется (trim + lower case). Дубликат email → `409`.

### Частичное обновление (`PUT /api/users/{id}`)

Тело — JSON с любым подмножеством полей (`application/json`).
Семантика — **merge patch**: указанные поля перезаписываются,
отсутствующие не трогаются.

| Поле | Отсутствует | `null` | Значение |
|---|---|---|---|
| `email` | не менять | `400` | нормализуется и заменяется |
| `password` | не менять | `400` | хешируется и заменяется |
| `firstName` | не менять | очищает | заменяется |
| `lastName` | не менять | очищает | заменяется |

Примеры:

```json
{ "email": "new@example.com", "password": "newpassword" }
```

```json
{ "firstName": null }
```

### Коды ошибок

| Код | Когда |
|---|---|
| `400` | Валидация, невалидный JSON, явный `null` для обязательного поля |
| `404` | Пользователь не найден |
| `405` | Неподдерживаемый HTTP-метод для эндпоинта |
| `406` | Неприемлемый `Accept` |
| `409` | Дубликат email |
| `415` | Неподдерживаемый `Content-Type` |
| `500` | Внутренняя ошибка |

Тело ошибки валидации — `{"field": "message"}`.

