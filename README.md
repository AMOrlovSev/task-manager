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
- H2 (in-memory)
- Gradle (Kotlin DSL)
- JUnit 5, AssertJ
- JaCoCo
- Spotless (google-java-format, AOSP)
- SonarQube Cloud
- GitHub Actions (CI)
- Docker (multi-stage build)
- Render (deploy)

## Требования

- JDK 21+
- Docker (опционально)
- Make (опционально, для удобства)

## Запуск

### Локально

```bash
./gradlew bootRun