# ---- Stage 0: frontend ----
FROM node:22-slim AS frontend
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
RUN npx build-frontend

# ---- Stage 1: build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Сначала копируем только файлы сборки — это кэширует зависимости
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Делаем wrapper исполняемым и прогреваем кэш зависимостей
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

# Теперь копируем исходники
COPY src src
COPY --from=frontend /app/src/main/resources/static src/main/resources/static

# Собираем исполняемый JAR без тестов
RUN ./gradlew --no-daemon bootJar -x test

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Непривилегированный пользователь
RUN useradd -r -u 1001 appuser
USER appuser

# Копируем только собранный JAR
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]