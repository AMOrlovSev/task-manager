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
на in-memory H2 (PostgreSQL compatibility mode), Flyway накатывает
миграции при старте. 
H2 Console доступна по адресу http://localhost:8080/h2-console (JDBC URL — как в `application-development.yml`, логин `sa`, пароль пустой)

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
  task-manager
```

## Переменные окружения (production)

Профиль `production` не содержит дефолтов для кредов — приложение
упадёт на старте, если переменная не задана.

| Переменная | Обязательна | Описание |
|------------|-------------|----------|
| `SPRING_PROFILES_ACTIVE` | да | Должна быть `production` |
| `JDBC_DATABASE_URL` | да | JDBC URL, напр. `jdbc:postgresql://host:5432/db?sslmode=require` |
| `DB_USER` | да | Пользователь БД |
| `DB_PASSWORD` | да | Пароль БД |
| `PORT` | нет | Порт приложения, по умолчанию `8080` |

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

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/welcome` | Приветствие |
| GET | `/actuator/health` | Статус приложения |