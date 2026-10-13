# Task Manager

[![CI](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=coverage)](...)

Веб-приложение для управления задачами на Spring Boot с SPA-фронтендом.

**Демо:** https://task-manager-c4wi.onrender.com

## Стек

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Security OAuth2 Resource Server (JWT, RS256)
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
- Node.js 22 (сборка фронтенда)
- Vite (сборщик SPA, приходит вместе с фронтенд-пакетом)

## Требования

- JDK 21+
- Node.js 22+ и npm
- Docker — для запуска интеграционных тестов и сборки образа
- Make (опционально, для удобства)
- На Windows: Git Bash или WSL для запуска `make` и bash-скриптов npm

## Запуск

### Локально

Собрать фронтенд и поднять приложение одной командой:

```bash
make setup
./gradlew bootRun
```

Или по шагам:

```bash
npm ci
npx build-frontend
./gradlew bootRun
```

Приложение: http://localhost:8080/

По умолчанию активируется профиль `development` — приложение стартует
на in-memory **H2** (PostgreSQL compatibility mode), **Flyway** накатывает
миграции при старте.

**H2 Console** доступна по адресу http://localhost:8080/h2-console
(JDBC URL — как в `application-development.yml`, логин `sa`, пароль пустой).

При старте создаётся администратор из `app.admin.*` (см. раздел
«Администратор по умолчанию»). JWT-ключи для профиля `development`
генерируются эфемерно при каждом запуске — это нормально для локальной
разработки.

### Через Make

```bash
make frontend   # сборка SPA в src/main/resources/static
make setup      # frontend + ./gradlew build
make run        # запуск
make test       # тесты
make lint       # проверка форматирования
make format     # автоформатирование
```

`make` под Windows запускайте из **Git Bash** или **WSL**, не из PowerShell —
цель `frontend` вызывает bash-скрипт `build-frontend` из npm-пакета.

### Docker

```bash
docker build -t task-manager .
docker run --rm -p 8080:8080 task-manager
```

Образ собирается в две стадии: `node:22-slim` собирает SPA, затем Gradle-стадия
накладывает готовый бандл из `src/main/resources/static` поверх исходников.

## Профили

Конфигурация разбита по профилям:

| Профиль | Файл | Назначение |
|---------|------|------------|
| `development` | `application-development.yml` | Локальная разработка: H2 in-memory, H2 Console, `show-sql`, эфемерные JWT-ключи |
| `production` | `application-production.yml` | Прод: PostgreSQL, креды из переменных окружения, `show-sql` выключен, JWT-ключ из `JWT_PRIVATE_KEY` |

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
  -e JWT_PRIVATE_KEY="$(cat jwt-private.pem)" \
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
| `JWT_PRIVATE_KEY` | да | PEM PKCS#8 приватного RSA-ключа (см. «Аутентификация») |
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

## Аутентификация

Все защищённые эндпоинты требуют заголовок `Authorization: Bearer <JWT>`.
JWT подписан **RS256** (RSA + SHA-256), TTL — 1 час (настраивается через `app.jwt.ttl`, короткий формат Spring
или ISO-8601, напр. `30m`, `2h` или `PT30M`).

### Получение токена

```http
POST /api/login
Content-Type: application/json

{ "username": "hexlet@example.com", "password": "qwerty" }
```

Успех → `200`, тело — сам токен строкой (без JSON-обёртки).

Ошибка (неверный пароль, неизвестный email, пустые поля, пустое тело) →
`401` с телом `Invalid credentials`. Тело ответа **одинаковое** для всех
сценариев — это защита от перечисления пользователей.

### Использование токена

```bash
curl -X POST http://localhost:8080/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"hexlet@example.com","password":"qwerty"}'
# → eyJhbGciOiJSUzI1NiJ9...

curl http://localhost:8080/api/users \
  -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...'
# → 200, массив пользователей
```

### Claims токена

| Claim | Значение |
|---|---|
| `iss` | `hexlet-task-manager` (константа `JwtService.ISSUER`) |
| `sub` | id пользователя (строка) |
| `iat` | время выпуска |
| `exp` | `iat + TTL` (по умолчанию 1 час) |
| `email` | email пользователя |

`JwtDecoder` валидирует подпись, `exp` (с допуском 60 секунд) и `iss`.
Токен, подписанный другим ключом или с чужим `iss`, отвергается → `401`.

### Ключи (RS256)

| Профиль | Источник ключа |
|---|---|
| `development`, `test` | Эфемерная пара, генерируется при старте (`JwtKeyConfig.devKeyPair`) |
| `production` | `PemKeyParser.parse(${app.jwt.private-key})` — из `JWT_PRIVATE_KEY` |

Публичный ключ отдельно не хранится — он выводится из приватного
(`RSAPrivateCrtKey.getModulus()` + `getPublicExponent()`).

### Генерация ключа для прода

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
```

Если `openssl` не установлен (типичный случай на Windows без Git Bash),
сгенерируйте ключ через PowerShell + .NET:

```powershell
$rsa = [System.Security.Cryptography.RSA]::Create(2048)
$privBytes = $rsa.ExportPkcs8PrivateKey()

$b64 = [Convert]::ToBase64String($privBytes)
$chunks = for ($i = 0; $i -lt $b64.Length; $i += 64) {
    $b64.Substring($i, [Math]::Min(64, $b64.Length - $i))
}
$pem = "-----BEGIN PRIVATE KEY-----`n" + ($chunks -join "`n") + "`n-----END PRIVATE KEY-----"
$pem | Set-Content -Encoding ascii -NoNewline jwt-private.pem
```

Файл `jwt-private.pem` **никогда не коммитить в репозиторий**. Убедитесь,
что в `.gitignore` есть:

```
*.pem
```

Содержимое файла целиком (включая `-----BEGIN PRIVATE KEY-----` и
`-----END PRIVATE KEY-----`) вставьте в переменную `JWT_PRIVATE_KEY`
на Render **до** деплоя.

`PemKeyParser` принимает как реальные переводы строк, так и литеральные
`\n` (некоторые платформы отдают многострочные env-переменные именно так).

### Смена ключа

При смене `JWT_PRIVATE_KEY` **все ранее выданные токены перестают работать**
— пользователи должны залогиниться заново. Это ожидаемое поведение.

## Авторизация

Регистрация (`POST /api/users`) открыта. Изменение и удаление пользователя
разрешены только самому пользователю: `sub` токена должен совпадать с `id`.

```java
@PutMapping("/{id}")
@PreAuthorize("#id.toString() == authentication.name")
public UserDTO update(...) { ... }
```

Проверка `@PreAuthorize` выполняется **до** входа в метод. Поэтому чужой
`PUT /api/users/999999` (несуществующий id) возвращает `403`, а не `404`
— сначала проверяются права, потом ищется ресурс. Это предотвращает
раскрытие информации о существовании объектов.

## Фронтенд

SPA на React, подключается как npm-пакет
[`@hexlet/java-task-manager-frontend`](https://www.npmjs.com/package/@hexlet/java-task-manager-frontend).
Исходников фронтенда в этом репозитории нет — есть только `package.json`
и `package-lock.json`, которые фиксируют версию пакета. Весь код и сборка
живут внутри пакета.

### Как собрать

Полная сборка бэкенда с фронтендом:

```bash
make setup
```

Цель `setup` делает две вещи: `make frontend` (собирает SPA) и
`./gradlew build` (компилирует и тестирует Java-часть, используя уже
готовую статику).

Цель `frontend` под капотом — ровно две команды:

```bash
npm ci
npx build-frontend
```

- `npm ci` ставит зависимости строго по `package-lock.json` (в отличие от
  `npm install`, не обновляет lockfile и не разрешает новые версии).
- `npx build-frontend` вызывает bash-скрипт из пакета, который запускает
  Vite и кладёт результат в `src/main/resources/static/`.

**Требуется bash.** Под Windows `make frontend` запускайте из Git Bash
или WSL. Из PowerShell скрипт не отработает — там нет bash. Если `make`
не установлен, выполните две команды напрямую в Git Bash.

### Режим разработки

Для активной работы над фронтендом есть dev-сервер Vite с горячей
перезагрузкой и проксированием API-запросов на бэкенд:

```bash
# в одном терминале — backend
./gradlew bootRun

# в другом — dev-сервер фронтенда
npx start-frontend
```

Dev-сервер поднимается на отдельном порту (обычно 5173) и проксирует
все запросы `/api/**` на `http://localhost:8080`. Это значит, что
SPA в браузере обращается к своему origin, а запросы до бэкенда доходят
как если бы он был на том же порту. CORS-настройки не нужны.

Почему это удобно: изменения в исходниках фронтенда подхватываются
мгновенно без пересборки бандла. Продакшн-сборка (`make frontend`)
делается редко — только перед `./gradlew build`, перед `docker build`
или в CI.

### Почему бандл не коммитится

`src/main/resources/static/` в `.gitignore`. Причины:

- это **артефакт**, а не исходник. Артефакты в репозитории — источник
  конфликтов: каждый `make setup` перегенерирует файлы с новыми хешами
  в именах (`index-a1b2c3.js`), и в диффах будет шум вместо полезных
  изменений;
- бандл весит сотни килобайт и раздувает репозиторий;
- воспроизводимость: сборка должна получаться из `package-lock.json`
  одной командой, а не подтягиваться из истории git;
- в CI и Docker бандл собирается автоматически на стадии `node:22-slim`,
  и локальный устаревший вариант там не нужен. В `.dockerignore` тоже
  есть `src/main/resources/static`, чтобы старый бандл не попадал
  в контекст сборки.

### Где лежит токен

JWT **не хранится на сервере**. После успешного `POST /api/login` фронтенд
получает токен в теле ответа и сохраняет его **в `localStorage` браузера**.
Заголовок `Authorization: Bearer <token>` ставит собственный `httpClient`
в `dataProvider.ts` пакета `@hexlet/java-task-manager-frontend` — axios-
interceptor там не используется.

Схема:

1. Пользователь вводит email и пароль в форме логина.
2. SPA делает `POST /api/login`, получает JWT строкой.
3. Токен кладётся в `localStorage` вместе с **логином и паролем
   в открытом виде** под ключом `user` — это известный компромисс
   учебного пакета, а не рекомендация для прода. В реальном продукте
   так делать нельзя: любой XSS прочитает и токен, и пароль.
4. При каждом защищённом запросе `httpClient` из `dataProvider.ts`
   подставляет заголовок `Authorization`.
5. Когда токен истекает (TTL 1 час), бэкенд отвечает `401`, SPA
   редиректит на страницу логина.

Почему **localStorage**, а не `cookie`:

- cookie отправляются браузером автоматически при каждом запросе на тот
  же домен — это открывает CSRF-атаки. Для защиты пришлось бы
  возвращать CSRF-токен.
- localStorage читается только из JS на том же origin. Запросы с чужих
  сайтов токен не увидят и не подставят.
- на бэкенде поэтому `csrf.disable()` — классический CSRF-вектор
  отсутствует по построению.

Обратная сторона: localStorage уязвим для XSS. Для продакшена правильнее
HttpOnly cookie + CSRF-токен — тогда пароль в открытом виде не хранится
на клиенте вообще.

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

Интеграционные тесты (`UserRepositoryTest`, `UserControllerTest`,
`SecurityTest`, `AuthControllerTest` и другие) используют Testcontainers:
поднимают реальный PostgreSQL в Docker-контейнере, накатывают
Flyway-миграции и работают с настоящей БД.

**Требуется запущенный Docker.** Если Docker недоступен, такие тесты
упадут с `Could not find a valid Docker environment`.

В CI (GitHub Actions на `ubuntu-latest`) Docker доступен из коробки,
поэтому интеграционные тесты проходят без дополнительной настройки.

Срезовые тесты (`AppApplicationTests`, `WelcomeControllerTest`,
`PemKeyParserTest`, `UserServiceTest`, `UserMapperTest`) идут на H2 или
вообще без Spring-контекста и Docker не требуют.

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

- CI — GitHub Actions: сборка и тесты бэкенда, покрытие, анализ SonarQube.
  Сборка фронтенда в CI не выполняется: тесты бэкенда статику не используют.
- Деплой — Render (Docker, многостадийная сборка).
- Качество — SonarCloud.

## API

Базовый путь для пользователей — `/api/users`. Защищённые эндпоинты
требуют заголовок `Authorization: Bearer <JWT>`.

| Метод | Путь | Тело запроса | Успешный ответ | Auth | Описание |
|---|---|---|---|---|---|
| `GET` | `/` | — | `200` HTML (SPA) | — | SPA-страница |
| `GET` | `/assets/**` | — | `200` JS/CSS | — | Статика SPA |
| `GET` | `/welcome` | — | `200` text | — | Приветствие |
| `GET` | `/actuator/health` | — | `200` JSON | — | Статус приложения |
| `POST` | `/api/login` | JSON | `200` text (JWT) | — | Аутентификация, возвращает JWT |
| `GET` | `/api/users` | — | `200` JSON-массив, заголовок `X-Total-Count` | Bearer | Список пользователей |
| `GET` | `/api/users/{id}` | — | `200` JSON | Bearer | Пользователь по id |
| `POST` | `/api/users` | JSON | `201` JSON, заголовок `Location` | — | Регистрация |
| `PUT` | `/api/users/{id}` | JSON | `200` JSON | Bearer + owner | Частичное обновление (merge patch) |
| `DELETE` | `/api/users/{id}` | — | `204` | Bearer + owner | Удаление пользователя |

`owner` = `sub` токена должен совпадать с `id` в пути.

### Аутентификация (`POST /api/login`)

Тело:

```json
{ "username": "hexlet@example.com", "password": "qwerty" }
```

Успех → `200`, тело — JWT строкой. Ошибка → `401`,
тело `Invalid credentials`.

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
отсутствующие не трогаются. Только для самого пользователя.

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
| `401` | Нет токена, токен просрочен, подпись/issuer не сходятся, неверные креды |
| `403` | Токен валиден, но нет прав на операцию (чужой пользователь) |
| `404` | Пользователь не найден |
| `405` | Неподдерживаемый HTTP-метод для эндпоинта |
| `406` | Неприемлемый `Accept` |
| `409` | Дубликат email |
| `415` | Неподдерживаемый `Content-Type` |
| `500` | Внутренняя ошибка |

Тело ошибки валидации — `{"field": "message"}`.
Тело `401` для `/api/login` — `Invalid credentials`.
Тело `403` — `Access denied`.