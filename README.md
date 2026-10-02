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
- Spring Boot Actuator
- Gradle (Kotlin DSL)
- JUnit 5
- JaCoCo
- Spotless (google-java-format, AOSP)
- SonarQube Cloud
- GitHub Actions
- Docker

## Требования

- JDK 21+
- Docker (опционально)
- Make (опционально, для удобства)

## Запуск

### Локально

```bash
./gradlew bootRun
```

Приложение: http://localhost:8080/welcome

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

## Разработка

### Тесты

```bash
./gradlew test
```

## Покрытие (JaCoCo)

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