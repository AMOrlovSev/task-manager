# Task Manager

[![CI](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=coverage)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)

Веб-приложение для управления задачами на Spring Boot с SPA-фронтендом.

**Демо:** https://task-manager-c4wi.onrender.com

**API-документация:** https://task-manager-c4wi.onrender.com/docs

## Стек

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Security OAuth2 Resource Server (JWT, RS256)
- Springdoc OpenAPI (Swagger UI)
- Flyway
- Spring Boot Actuator
- H2 (dev)
- PostgreSQL (prod)
- Testcontainers
- Gradle (Kotlin DSL)
- JUnit 5
- JaCoCo
- Spotless (google-java-format)
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

**Swagger UI:** http://localhost:8080/docs

По умолчанию активируется профиль `development` — приложение стартует
на in-memory **H2** (PostgreSQL compatibility mode), **Flyway** накатывает
миграции при старте.

**H2 Console** доступна по адресу http://localhost:8080/h2-console
(JDBC URL — как в `application-development.yml`, логин `sa`, пароль пустой).

При старте создаётся администратор из `app.admin.*`, дефолтные статусы
задач и дефолтные метки (см. разделы «Администратор по умолчанию»,
«Статусы задач» и «Метки»). JWT-ключи для профиля `development`
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

## API-документация

Интерактивная документация генерируется **springdoc-openapi** (линия 3.x,
совместима со Spring Boot 4).

| Ресурс | URL (локально) | URL на Render |
|---|---|---|
| Swagger UI | http://localhost:8080/docs | https://task-manager-c4wi.onrender.com/docs |
| Спецификация JSON | http://localhost:8080/api-docs | https://task-manager-c4wi.onrender.com/api-docs |
| Спецификация YAML | http://localhost:8080/api-docs.yaml | https://task-manager-c4wi.onrender.com/api-docs.yaml |

Пути кастомизированы: `/docs` и `/api-docs` вместо дефолтных
`/swagger-ui.html` и `/v3/api-docs`. Старый `/v3/api-docs` больше
не обслуживается.

### Авторизация в Swagger UI

Документация публична, но большинство эндпоинтов `/api/**` защищены
JWT. Чтобы выполнять защищённые операции прямо из UI:

1. Раскройте `POST /api/login` → **Try it out**.
2. Введите креды администратора (dev: `hexlet@example.com` / `qwerty`).
3. **Execute** → скопируйте токен из тела ответа.
4. Нажмите кнопку **Authorize** в правом верхнем углу.
5. Вставьте токен **без** префикса `Bearer` — UI добавит его сам.
6. **Authorize** → **Close**.

Теперь у защищённых операций замочек закрыт, а в запросы автоматически
подставляется `Authorization: Bearer …`.

Публичные операции — `POST /api/login`, `POST /api/users`,
`GET /api/task_statuses`, `GET /api/task_statuses/{id}` — замочка
не имеют и работают без токена.

### Что видно в спецификации

- `securitySchemes.bearerAuth` — глобальная bearer-схема;
- теги по контроллерам (`Authentication`, `Users`, `Tasks`,
  `Task statuses`, `Labels`);
- заголовок `X-Total-Count` у списков;
- явные схемы тел запросов для `PUT` (через
  `@io.swagger.v3.oas.annotations.parameters.RequestBody`);
- `@Schema` с описаниями и примерами у ключевых DTO
  (`TaskCreateDTO`, `TaskDTO` и др.).

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
JWT подписан **RS256** (RSA + SHA-256), TTL — 1 час (настраивается
через `app.jwt.ttl`, короткий формат Spring или ISO-8601, напр. `30m`,
`2h` или `PT30M`).

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

Статусы задач (`/api/task_statuses/**`) читают все, включая анонимных
клиентов — `GET` открыт. Создание, изменение и удаление требуют Bearer
токена. Специальных прав «только владелец» у статусов нет: их может
менять любой аутентифицированный пользователь.

Все пять маршрутов задач (`/api/tasks/**`) требуют Bearer токен — это
следует из общего правила `anyRequest().authenticated()`. Отдельного
`@PreAuthorize` на задачах нет: любой аутентифицированный пользователь
может создать, изменить и удалить задачу.

Метки (`/api/labels/**`) — **все пять маршрутов, включая `GET`**,
требуют Bearer токен. В отличие от статусов, анонимный клиент метки
не видит: `SecurityConfig` не содержит для них `permitAll`, действует
общее правило `anyRequest().authenticated()`. Специальных прав
«только владелец» у меток нет: менять может любой аутентифицированный
пользователь.

## Принятые решения по безопасности

- **Документация публична, API — нет.** `/docs`, `/swagger-ui/**` и
  `/api-docs/**` открыты без токена (`permitAll` в `SecurityConfig`),
  а все `/api/**` (кроме явно публичных `POST /api/login`,
  `POST /api/users`, `GET /api/task_statuses`) требуют JWT. Это
  осознанное решение для учебного проекта на Render: ревьюеру и
  пользователю не нужен токен, чтобы прочитать контракт API, но данные
  остаются защищены.
- **CSRF выключен.** Классический CSRF-вектор отсутствует: JWT хранится
  в `localStorage`, а не в cookie, и не отправляется автоматически.
- **Тело `401` на `/api/login` одинаково для всех сценариев**
  (`Invalid credentials`) — защита от перечисления пользователей.

## Статусы задач

Каждая задача в таск-менеджере имеет статус: новый, в работе,
на тестировании, завершён. Статус описывается сущностью `TaskStatus`
с полями `name`, `slug` и `createdAt`. `name` и `slug` уникальны,
оба обязательны, минимум один символ. `createdAt` проставляется
автоматически через `@CreatedDate`.

### Дефолтные статусы

`DataInitializer` создаёт при старте пять статусов, если их ещё нет:

| name | slug |
|---|---|
| Draft | `draft` |
| ToReview | `to_review` |
| ToBeFixed | `to_be_fixed` |
| ToPublish | `to_publish` |
| Published | `published` |

Перед созданием проверяется наличие статуса с таким `slug` или `name`;
если он есть, статус пропускается и событие логируется. Повторный
запуск не падает.

### Модель статуса

```json
{
  "id": 2,
  "name": "ToReview",
  "slug": "to_review",
  "createdAt": "2026-10-30"
}
```

### Получение по слагу

В репозитории есть метод `findBySlug`. Слаг уникален, поэтому это
однозначный идентификатор статуса — на его основе можно строить
человекочитаемые URL (например, `/statuses/to_review`).

### Обновление статуса

Тот же merge-patch, что и у пользователей: в теле `PUT /api/task_statuses/{id}`
передаётся любое подмножество полей. Отсутствующие не трогаются,
`name` и `slug` не могут быть `null` или состоять из пробелов — валидируется
через `@NotBlank`. Пробелы в начале и конце обрезаются сервисом
до сохранения. Дубликаты `name`/`slug` возвращают `409`.

| Поле | Отсутствует | `null` или пробелы | Значение |
|---|---|---|---|
| `name` | не менять | `400` | trim + замена, дубликат → `409` |
| `slug` | не менять | `400` | trim + замена, дубликат → `409` |

## Метки

Метки — это теги для задач: «feature», «bug», «backend», «frontend» и т.п.
Задача может иметь ноль или больше меток. Одна метка может быть
привязана ко многим задачам.

Связь — **many-to-many**. Владелец связи — `Task` (поле `labels`,
`@JoinTable("tasks_labels")`). У `Label` обратной коллекции нет. На
`@ManyToMany` **не стоит** `cascade`: `CascadeType.REMOVE` или `ALL`
удалил бы метку при удалении задачи. Строки связующей таблицы Hibernate
сносит сам при удалении владельца.

Метка описывается сущностью `Label` с полями `name`, `createdAt`,
`updatedAt`. Наружу `updatedAt` не отдаётся. `name` уникально
(регистрозависимо), от 3 до 1000 символов.

### Дефолтные метки

`DataInitializer` создаёт при старте две метки, если их ещё нет:

| name |
|---|
| `feature` |
| `bug` |

Проверка идёт по имени, повторный запуск не плодит дубликаты. Если
метка с таким именем уже есть, она пропускается и событие логируется.

### Модель метки

```json
{
  "id": 3,
  "name": "feature",
  "createdAt": "2026-10-30"
}
```

### Создание метки (`POST /api/labels`)

Тело:

```json
{ "name": "backend" }
```

Валидация: `name` обязателен, от 3 до 1000 символов. Пробелы в начале
и конце обрезаются на уровне DTO (`LabelCreateDTO` — компактный
конструктор делает `strip()`), поэтому валидатор видит уже нормализованное
значение. Имя `" ab "` после `strip` даёт длину 2 → `400`. Дубликат
имени → `409`.

### Обновление метки (`PUT /api/labels/{id}`)

Тот же merge-patch. `name` обрезается на уровне `LabelUpdateDTO` в
сеттере `setName`, поэтому в сервисе `trim` не нужен.

| Поле | Отсутствует | `null` или пробелы | Значение |
|---|---|---|---|
| `name` | не менять | `400` | strip + замена, дубликат → `409` |

Если `name` совпадает с текущим — `200` (no-op), не `409`.

### Удаление метки (`DELETE /api/labels/{id}`)

Используемую метку удалить нельзя: `409`. Перед удалением сервис
проверяет `taskRepository.existsByLabelsId(id)` и бросает
`ResourceInUseException`. На уровне БД это дублирует внешний ключ
`fk_tasks_labels_label`: он запрещает удаление связанной метки, а
`fk_tasks_labels_task` стоит с `on delete cascade`, поэтому удаление
задачи снимает её связи, а метки остаются.

Чтобы удалить метку, сначала уберите её из всех задач
(`PUT /api/tasks/{id}` с `taskLabelIds: []`), потом удаляйте.

### Индексы

- PK `tasks_labels (task_id, label_id)` покрывает поиск по `task_id`.
- Отдельный `idx_tasks_labels_label_id` нужен для проверки
  «метка используется» и будущей фильтрации задач по меткам.

### Принятые решения

- Владелец связи — `Task`. `Label` — независимая сущность, на неё
  ссылаются через `tasks_labels`.
- Нет `cascade` на `@ManyToMany`. Иначе удаление задачи каскадно
  снесло бы метки, чего быть не должно.
- `name` обрезается на уровне DTO, а не в сервисе: валидатор
  (`@Size(min = 3)`) видит уже нормализованное значение.
- Уникальность `name` — регистрозависимая, как у статусов.
- Замена набора меток при `PUT /api/tasks/{id}` — атомарная: либо
  применяется весь запрошенный набор, либо `400` (если хотя бы один id
  не найден). Дубликаты в массиве молча схлопываются.

## Задачи

Каждая задача привязана к статусу и (опционально) к исполнителю и к
набору меток. Сущность `Task` — `name`, `description`, `index`, `createdAt`,
`updatedAt` плюс две `@ManyToOne(LAZY)` связи (`taskStatus` — обязательная,
`assignee` — nullable) и одна `@ManyToMany(LAZY)` (`labels`, владелец
связи).

### Модель и соответствие JSON

В JSON имена отличаются от полей сущности — это удобно для
`ra-data-json-server` на фронтенде:

| Поле сущности | Поле JSON |
|---|---|
| `name` | `title` |
| `description` | `content` |
| `index` | `index` |
| `taskStatus.slug` | `status` |
| `assignee.id` | `assignee_id` |
| `labels` (id, сортированные по возрастанию) | `taskLabelIds` |
| `createdAt` | `createdAt` |

Пример ответа:

```json
{
  "id": 1,
  "index": 0,
  "createdAt": "2026-10-30",
  "assignee_id": 2,
  "title": "Task 1",
  "content": "Description",
  "status": "draft",
  "taskLabelIds": [1, 3]
}
```

`taskLabelIds` всегда присутствует в ответе и отсортирован по возрастанию
— чтобы ответ был детерминированным.

### Создание задачи (`POST /api/tasks`)

Тело:

```json
{
  "title": "Task 1",
  "content": "Description",
  "index": 0,
  "status": "draft",
  "assignee_id": 2,
  "taskLabelIds": [1, 3]
}
```

Валидация: `title` обязателен и не длиннее 255 символов, `content`
не длиннее 4000, `status` обязателен и должен существовать (по slug),
`assignee_id` — nullable, если передан, должен существовать.
`taskLabelIds` — nullable, массив опциональных id (элемент `null`
запрещён валидацией `List<@NotNull Long>`), длина массива не более 100.

Если статус или исполнитель не найдены — `400` с ключом `status` или
`assignee_id` соответственно. Если хотя бы один id метки не найден —
`400` с ключом `taskLabelIds` и перечислением отсутствующих id.
`404` здесь не возвращается: 404 означает «нет задачи по URL», а
неверное значение в теле — это 400.

### Частичное обновление задачи (`PUT /api/tasks/{id}`)

Тело — любое подмножество полей, семантика merge patch. Kanban-фронтенд
шлёт `{"index": N}` при перетаскивании карточки и
`{"index": N, "status": "slug"}` при перетаскивании в другую колонку.
Неизвестные поля (например, `labels`, `assigneeId` в camelCase)
игнорируются.

| Поле | Отсутствует | `null` | Значение |
|---|---|---|---|
| `title` | не менять | `400` | trim + замена, `max 255` |
| `content` | не менять | очищает | замена, `max 4000` |
| `index` | не менять | очищает | замена |
| `status` | не менять | `400` | slug lookup, не найден → `400` с ключом `status` |
| `assignee_id` | не менять | снимает исполнителя | id lookup, не найден → `400` с ключом `assignee_id` |
| `taskLabelIds` | не менять | очищает | замена, не найден id → `400` с ключом `taskLabelIds` |

Поведение `taskLabelIds` в PUT:

| Значение в запросе | Поведение |
|---|---|
| Ключ отсутствует | Метки не меняются |
| `null` | Метки очищаются |
| `[]` | Метки очищаются |
| `[1, 2]` | Метки заменяются на `{1, 2}` (порядок и дубликаты не важны) |
| `[1, null]` | `400`, ключ `taskLabelIds` |
| `[999]` (неизвестный id) | `400`, ключ `taskLabelIds`, перечисление отсутствующих id |

Важно для Kanban: `PUT {"index": N}` не должен стирать метки. Это
достигается тем, что `TaskService.update` сравнивает запрошенный набор
id с текущим и перепривязывает, только если ключ присутствует в патче
и набор отличается от текущего. Отсутствие ключа трактуется как
«не трогать», а не как «очистить».

### Удаление связанных ресурсов

`DELETE /api/users/{id}` и `DELETE /api/task_statuses/{id}` возвращают
`409`, если на них ссылается хотя бы одна задача. `DELETE /api/labels/{id}`
возвращает `409`, если на неё ссылается хотя бы одна задача. Две линии
защиты: явная проверка в сервисе (`ResourceInUseException`) и внешние
ключи БД (`fk_tasks_status`, `fk_tasks_assignee`, `fk_tasks_labels_label`)
как страховка.

При удалении задачи строки связующей таблицы `tasks_labels` уходят
автоматически (`on delete cascade` по `task_id`), а сами метки
остаются.

### Принятые решения

- Несуществующий `status`/`assignee_id`/`taskLabelIds` в теле → `400`,
  не `404`. `404` означает «нет задачи по URL».
- Связи загружаются `LAZY`, маппинг в DTO выполняется внутри транзакции
  сервиса (`open-in-view: false`).
- `PatchSupport.apply` заменяет верхний уровень патча целиком через
  `ObjectNode.setAll`, а не сливает через `jsonMapper.updateValue`.
  Иначе массивы могли бы дописываться, и `taskLabelIds` не заменялся бы
  целиком — а именно замену ждёт фронтенд.
- N+1 на `GET /api/tasks` вырос: теперь на список задач выполняются
  отдельные запросы за метками каждой задачи. Отложено до шага
  фильтрации; при добавлении фильтра по меткам будем чинить через
  `@EntityGraph`/`join fetch` вместе со статусами и исполнителями.
- `DataIntegrityViolationException` отвечает нейтральным
  `"Conflict with existing data"` — покрывает и дубликаты, и FK,
  и нарушение длины.

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

### Метки на фронтенде

Форма задачи работает с полем `taskLabelIds` (массив id меток). Для
отрисовки самих меток фронтенду нужны только `name` и `createdAt`
из `GET /api/labels`. При сохранении формы `dataProvider.ts` шлёт
`taskLabelIds` как есть; при перетаскивании карточки в Kanban — только
`{"index": N}` или `{"index": N, "status": "slug"}`, метки при этом
не трогаются.

## Миграции (Flyway)

SQL-миграции лежат в `src/main/resources/db/migration/` и накатываются
автоматически при старте приложения. Именование: `V<n>__<description>.sql`.

Текущие миграции:

- `V1__create_users.sql` — таблица `users`
- `V2__make_user_names_nullable.sql` — снимает `not null` с `first_name`
  и `last_name`
- `V3__create_task_statuses.sql` — таблица `task_statuses` с уникальными
  индексами на `name` и `slug`
- `V4__create_tasks.sql` — таблица `tasks` со связями на `task_statuses`
  и `users`, индексы на `status_id` и `assignee_id`
- `V5__create_labels.sql` — таблицы `labels` (уникальный `name`)
  и `tasks_labels` (PK `(task_id, label_id)`, FK с `on delete cascade`
  только по `task_id`, индекс `idx_tasks_labels_label_id`)

Схемой управляет Flyway, Hibernate работает в режиме `validate` —
если маппинг сущностей разойдётся с миграциями, приложение не стартует.

## Разработка

### Тесты

```bash
./gradlew test
```

Интеграционные тесты используют Testcontainers: поднимают реальный
PostgreSQL в Docker-контейнере, накатывают Flyway-миграции и работают
с настоящей БД. К ним относятся:

- `UserRepositoryTest`, `UserControllerTest`, `AuthControllerTest`,
  `SecurityTest`
- `TaskStatusControllerTest`, `TaskStatusRepositoryTest`
- `TaskControllerTest`, `TaskRepositoryTest`
- `LabelControllerTest`, `LabelRepositoryTest`

**Требуется запущенный Docker.** Если Docker недоступен, такие тесты
упадут с `Could not find a valid Docker environment`.

В CI (GitHub Actions на `ubuntu-latest`) Docker доступен из коробки,
поэтому интеграционные тесты проходят без дополнительной настройки.

Срезовые тесты идут на H2 или вообще без Spring-контекста и Docker
не требуют:

- `AppApplicationTests`, `WelcomeControllerTest`
- `OpenApiTest` — спецификация, пути, security-метаданные
- `PemKeyParserTest`
- `UserServiceTest`, `UserMapperTest`
- `TaskStatusServiceTest`, `TaskStatusMapperTest`
- `TaskServiceTest`, `TaskMapperTest`
- `LabelServiceTest`, `LabelMapperTest`
- `PatchSupportTest`

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

- CI — GitHub Actions: сборка и тесты бэкенда, покрытие, анализ SonarQube,
  проверка `docker build` (Dockerfile двухстадийный, стадия `node:22-slim`
  собирает фронтенд; проверка ловит поломку стадии на PR, а не на Render).
  Сборка фронтенда отдельным шагом в CI не выполняется: тесты бэкенда
  статику не используют.
- Деплой — Render (Docker, многостадийная сборка).
- Качество — SonarCloud.

## API

Базовые пути: `/api/users`, `/api/task_statuses`, `/api/labels`, `/api/tasks`.
Защищённые эндпоинты требуют заголовок `Authorization: Bearer <JWT>`.

**Интерактивная документация:** `/docs` (Swagger UI),
`/api-docs` (JSON), `/api-docs.yaml` (YAML). См. раздел
«API-документация».

| Метод | Путь | Тело запроса | Успешный ответ | Auth | Описание |
|---|---|---|---|---|---|
| `GET` | `/` | — | `200` HTML (SPA) | — | SPA-страница |
| `GET` | `/assets/**` | — | `200` JS/CSS | — | Статика SPA |
| `GET` | `/docs` | — | `302` → `/swagger-ui/index.html` | — | Swagger UI (точка входа) |
| `GET` | `/swagger-ui/**` | — | `200` JS/CSS | — | Статика Swagger UI |
| `GET` | `/api-docs` | — | `200` JSON | — | OpenAPI-спецификация |
| `GET` | `/api-docs.yaml` | — | `200` YAML | — | OpenAPI-спецификация в YAML |
| `GET` | `/welcome` | — | `200` text | — | Приветствие |
| `GET` | `/actuator/health` | — | `200` JSON | — | Статус приложения |
| `POST` | `/api/login` | JSON | `200` text (JWT) | — | Аутентификация, возвращает JWT |
| `GET` | `/api/users` | — | `200` JSON-массив, `X-Total-Count` | Bearer | Список пользователей |
| `GET` | `/api/users/{id}` | — | `200` JSON | Bearer | Пользователь по id |
| `POST` | `/api/users` | JSON | `201` JSON, `Location` | — | Регистрация |
| `PUT` | `/api/users/{id}` | JSON | `200` JSON | Bearer + owner | Частичное обновление (merge patch) |
| `DELETE` | `/api/users/{id}` | — | `204` | Bearer + owner | Удаление пользователя |
| `GET` | `/api/task_statuses` | — | `200` JSON-массив, `X-Total-Count` | — | Список статусов |
| `GET` | `/api/task_statuses/{id}` | — | `200` JSON | — | Статус по id |
| `POST` | `/api/task_statuses` | JSON | `201` JSON, `Location` | Bearer | Создание статуса |
| `PUT` | `/api/task_statuses/{id}` | JSON | `200` JSON | Bearer | Частичное обновление (merge patch) |
| `DELETE` | `/api/task_statuses/{id}` | — | `204` | Bearer | Удаление статуса |
| `GET` | `/api/labels` | — | `200` JSON-массив, `X-Total-Count` | Bearer | Список меток |
| `GET` | `/api/labels/{id}` | — | `200` JSON | Bearer | Метка по id |
| `POST` | `/api/labels` | JSON | `201` JSON, `Location` | Bearer | Создание метки |
| `PUT` | `/api/labels/{id}` | JSON | `200` JSON | Bearer | Частичное обновление (merge patch) |
| `DELETE` | `/api/labels/{id}` | — | `204` | Bearer | Удаление метки |
| `GET` | `/api/tasks` | — | `200` JSON-массив, `X-Total-Count` | Bearer | Список задач |
| `GET` | `/api/tasks/{id}` | — | `200` JSON | Bearer | Задача по id |
| `POST` | `/api/tasks` | JSON | `201` JSON, `Location` | Bearer | Создание задачи |
| `PUT` | `/api/tasks/{id}` | JSON | `200` JSON | Bearer | Частичное обновление (merge patch) |
| `DELETE` | `/api/tasks/{id}` | — | `204` | Bearer | Удаление задачи |

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

### Создание пользователя (`POST /api/users`)

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

### Частичное обновление пользователя (`PUT /api/users/{id}`)

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

### Модель статуса

```json
{
  "id": 2,
  "name": "ToReview",
  "slug": "to_review",
  "createdAt": "2026-10-30"
}
```

### Создание статуса (`POST /api/task_statuses`)

Тело:

```json
{ "name": "New", "slug": "new" }
```

Валидация: `name` и `slug` обязательны, минимум 1 символ. Дубликат
`name` или `slug` → `409`. Требует Bearer токен.

### Частичное обновление статуса (`PUT /api/task_statuses/{id}`)

Семантика и правила — см. раздел «Статусы задач → Обновление статуса».

### Удаление статуса (`DELETE /api/task_statuses/{id}`)

Успех → `204`. Несуществующий id → `404`. Без токена → `401`.
Если на статус ссылается задача → `409`.

### Модель метки

```json
{
  "id": 3,
  "name": "feature",
  "createdAt": "2026-10-30"
}
```

### Создание метки (`POST /api/labels`)

Тело:

```json
{ "name": "backend" }
```

Валидация: `name` обязателен, от 3 до 1000 символов. Пробелы обрезаются
на уровне DTO до валидации. Дубликат имени → `409`. Требует Bearer токен.

### Частичное обновление метки (`PUT /api/labels/{id}`)

Семантика и правила — см. раздел «Метки → Обновление метки».

### Удаление метки (`DELETE /api/labels/{id}`)

Успех → `204`. Несуществующий id → `404`. Без токена → `401`.
Если на метку ссылается задача → `409`.

### Модель задачи и правила создания/обновления

См. раздел «Задачи» выше. `taskLabelIds` в ответе всегда массив,
отсортированный по возрастанию; `null` в запросе очищает набор,
отсутствие ключа — не трогает.

### Коды ошибок

| Код | Когда |
|---|---|
| `400` | Валидация, невалидный JSON, явный `null` для обязательного поля, несуществующий `status`/`assignee_id`/`taskLabelIds` в теле |
| `401` | Нет токена, токен просрочен, подпись/issuer не сходятся, неверные креды |
| `403` | Токен валиден, но нет прав на операцию (чужой пользователь) |
| `404` | Ресурс по URL не найден |
| `405` | Неподдерживаемый HTTP-метод для эндпоинта |
| `406` | Неприемлемый `Accept` |
| `409` | Дубликат email/name/slug, ресурс используется другой сущностью, конфликт ограничений БД |
| `415` | Неподдерживаемый `Content-Type` |
| `500` | Внутренняя ошибка |

Тело ошибки валидации — `{"field": "message"}`.
Тело `401` для `/api/login` — `Invalid credentials`.
Тело `403` — `Access denied`.