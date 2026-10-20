# Task Manager

[![CI](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=coverage)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)

REST API менеджера задач на Spring Boot с SPA-фронтендом: задачи, статусы,
метки, пользователи, JWT-аутентификация, фильтрация.

- **Демо:** https://task-manager-c4wi.onrender.com
- **Swagger UI:** https://task-manager-c4wi.onrender.com/docs

## Вход в приложение

Во всех профилях при старте создаётся администратор (если его ещё нет):

| Профиль | Email | Пароль |
|---|---|---|
| `development`, `test` | `hexlet@example.com` | `qwerty` |
| `production` | значение `ADMIN_EMAIL` | значение `ADMIN_PASSWORD` |

Войти можно на странице SPA или через `POST /api/login`. На демо-стенде
(Render) используются данные из переменных окружения сервиса.

## Стек

| Область | Технологии |
|---|---|
| Бэкенд | Java 21, Spring Boot 4 (Web MVC, Data JPA, Security, OAuth2 Resource Server, Actuator, Validation), MapStruct, Lombok |
| База данных | PostgreSQL (prod), H2 (dev), Flyway |
| Документация API | springdoc-openapi (Swagger UI) |
| Тесты и качество | JUnit 5, Testcontainers, JaCoCo, Spotless (google-java-format), SonarQube Cloud |
| Сборка и деплой | Gradle (Kotlin DSL), Docker (multi-stage), GitHub Actions, Render |
| Фронтенд | React-SPA из npm-пакета `@hexlet/java-task-manager-frontend` (сборка на Node.js 22) |

## Быстрый старт

Нужны: JDK 21+, Node.js 22+ с npm, Docker (для интеграционных тестов и образа).
Make опционален; на Windows запускайте его из Git Bash или WSL.

```bash
make setup          # собрать фронтенд и бэкенд (npm ci + build-frontend + gradlew build)
./gradlew bootRun   # запустить: http://localhost:8080
```

Без Make: `npm ci && npx build-frontend && ./gradlew bootRun`.

| Адрес | Что |
|---|---|
| http://localhost:8080 | SPA |
| http://localhost:8080/docs | Swagger UI |
| http://localhost:8080/h2-console | консоль H2 (только `development`, логин `sa`, пароль пустой) |

По умолчанию активен профиль `development`: H2 in-memory, Flyway накатывает
миграции, при старте создаются администратор `hexlet@example.com` / `qwerty`,
статусы и метки по умолчанию, JWT-ключи генерируются на каждый запуск.
Только в `development` дополнительно создаются демо-данные (5 пользователей
с паролем `password123`, 10 меток, 30 задач) — удобно проверять пагинацию и
Kanban. Они выключены в `production` и в тестах (`app.mock-data.enabled`).

Другие команды: `make test`, `make lint`, `make format`, `make frontend`.
Режим разработки фронтенда с hot reload: `./gradlew bootRun` и рядом
`npx start-frontend` (dev-сервер проксирует `/api/**` на бэкенд).

### Docker

```bash
docker build -t task-manager .
docker run --rm -p 8080:8080 task-manager
```

Стадия `node:22-slim` собирает SPA, Gradle-стадия кладёт её в
`src/main/resources/static`, финальный образ — JRE под непривилегированным
пользователем.

### Production

Профиль `production` не имеет значений по умолчанию: без обязательной
переменной приложение не стартует.

| Переменная | Описание |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `production` |
| `JDBC_DATABASE_URL`, `DB_USER`, `DB_PASSWORD` | подключение к PostgreSQL |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | администратор, создаётся при старте |
| `JWT_PRIVATE_KEY` | приватный RSA-ключ в PEM PKCS#8 (целиком, с `BEGIN`/`END`) |
| `PORT` | порт, по умолчанию `8080` (необязательно) |

Ключ: `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem`
(на Windows `openssl` есть в Git Bash). Файл `*.pem` в репозиторий не
коммитится. При смене ключа все выданные токены перестают работать.

## Архитектура

```
Controller ──► Service ──► Repository ──► PostgreSQL (Flyway)
   │              │
   DTO         Mapper (MapStruct)
```

- `controller` — REST-эндпоинты, валидация входа (`@Valid`), без бизнес-логики.
- `service` — бизнес-правила и транзакции; маппинг в DTO происходит внутри
  транзакции (`open-in-view: false`, связи `LAZY`).
- `repository` — Spring Data JPA; фильтры задач на `JpaSpecificationExecutor`.
- `handler` — `GlobalExceptionHandler`: одно место, где исключения
  превращаются в HTTP-коды.
- `component/PatchSupport` — частичное обновление через `PUT` (см. ниже).
- Схемой управляет Flyway (`V1`–`V5`), Hibernate работает в режиме
  `validate`: если сущности разойдутся с миграциями, приложение не стартует.

### Модель данных

- **User** — email (уникален, нормализуется), пароль хранится как BCrypt-хеш.
- **TaskStatus** — `name` и `slug`, оба уникальны. По умолчанию: `draft`,
  `to_review`, `to_be_fixed`, `to_publish`, `published`.
- **Label** — `name` (3–1000 символов, уникален). По умолчанию: `feature`, `bug`.
- **Task** — название, описание, `index`, обязательный статус, необязательные
  исполнитель и набор меток (many-to-many, владелец связи — `Task`).

### Принятые решения

- **Частичное обновление через `PUT`.** Контракт фронтенда: в теле любое
  подмножество полей. Отсутствующее поле не меняется, `null` очищает
  необязательное поле (`content`, `index`, `assignee_id`, `taskLabelIds`),
  `null` для обязательного — `400`. Верхний уровень патча заменяет значения
  целиком (`ObjectNode.setAll`), чтобы массивы заменялись, а не дописывались.
- **Коды ответов.** Неизвестная ссылка в теле (`status`, `assignee_id`,
  `taskLabelIds`) — `400`, а не `404`: `404` значит «нет ресурса по URL».
  Дубликат уникального значения и удаление используемого ресурса — `409`.
  Проверки в сервисах дублируются внешними ключами и уникальными индексами БД.
- **Метки.** Нет `cascade` на `@ManyToMany`: удаление задачи не удаляет метки.
  Метку, привязанную к задаче, удалить нельзя (`409`).
- **N+1.** Полный список задач грузится одним запросом (`@EntityGraph` на
  статус и метки). В режиме страниц граф только на статус, а метки подтягиваются
  пачками (`@BatchSize`): fetch join коллекции вместе с `LIMIT` заставил бы
  Hibernate пагинировать в памяти. Число запросов не зависит от размера
  страницы — это закреплено тестом (`TaskQueryCountTest`).
- **Фильтры.** `titleCont` без учёта регистра, спецсимволы `LIKE` экранируются;
  фильтр по метке сделан через `exists`-подзапрос, чтобы в ответе у задачи
  оставались все её метки и не было дублей.
- **Документация публична, API нет.** Swagger UI и спецификация открыты без
  токена, чтобы контракт можно было прочитать; все данные защищены JWT.
- **Ошибки логина.** Все сбои (неверный пароль, неизвестный email, пустые
  поля) дают одинаковый `401 Invalid credentials` — защита от перебора email.
- **Пагинация.** Формат `react-admin` (`_start`, `_end`, `_sort`, `_order`).
  Размер страницы ограничен (`app.pagination.max-size`), поля сортировки
  берутся из белого списка, `id` — второй ключ сортировки для стабильного
  порядка. Подробности — в разделе «Пагинация и сортировка».

## API

Полное описание, схемы и примеры: **Swagger UI** (`/docs`), спецификация -
`/api-docs` (JSON) и `/api-docs.yaml` (YAML). Защищённые эндпоинты требуют
`Authorization: Bearer <JWT>`.

| Ресурс | Методы | Доступ |
|---|---|---|
| `/api/login` | `POST` | открыт |
| `/api/users`, `/api/users/{id}` | `GET`, `POST`, `PUT`, `DELETE` | `POST` (регистрация) открыт, остальное — JWT; `PUT` и `DELETE` только для самого пользователя |
| `/api/task_statuses`, `/api/task_statuses/{id}` | `GET`, `POST`, `PUT`, `DELETE` | `GET` открыт, запись — JWT |
| `/api/labels`, `/api/labels/{id}` | `GET`, `POST`, `PUT`, `DELETE` | JWT |
| `/api/tasks`, `/api/tasks/{id}` | `GET`, `POST`, `PUT`, `DELETE` | JWT |

Также: `/welcome`, `/actuator/health` (открыты). `POST` возвращает `201` и
`Location`, `DELETE` — `204`, списки отдают заголовок `X-Total-Count`.

### Пагинация и сортировка

Поддерживают все четыре списка (`/api/tasks`, `/api/users`,
`/api/task_statuses`, `/api/labels`) в формате `ra-data-json-server`:

| Параметр | Значение |
|---|---|
| `_start`, `_end` | диапазон `[_start, _end)`; размер страницы `_end − _start` |
| `_sort` | поле из белого списка (ниже) |
| `_order` | `ASC` (по умолчанию) или `DESC` |

- Без `_start` и `_end` возвращается весь список; передать только один из них
  нельзя — `400`.
- Размер страницы не больше `app.pagination.max-size` (по умолчанию `100`),
  `_start` кратен размеру страницы; иначе `400`. Неизвестное поле
  сортировки или `_order` — тоже `400`.
- `X-Total-Count` — всего записей после фильтров, до пагинации. Страница
  за пределами списка даёт `200` и `[]`.
- Строки (`title`, `email`, `name`, …) сортируются без учёта регистра,
  `NULL` — в конце.

| Ресурс | Поля `_sort` |
|---|---|
| Задачи | `id`, `title`, `content`, `index`, `createdAt`, `assignee_id`, `status` |
| Пользователи | `id`, `email`, `firstName`, `lastName`, `createdAt` |
| Статусы | `id`, `name`, `slug`, `createdAt` |
| Метки | `id`, `name`, `createdAt` |

### Как попробовать в Swagger UI

1. Откройте `POST /api/login`, выполните с `hexlet@example.com` / `qwerty`
   (локально) и скопируйте токен из ответа.
2. Нажмите **Authorize** и вставьте токен без префикса `Bearer`.

### Задачи

JSON-поля отличаются от сущности (так ждёт `ra-data-json-server`):
`name` → `title`, `description` → `content`, `taskStatus.slug` → `status`,
`assignee.id` → `assignee_id`, `labels` → `taskLabelIds` (id по возрастанию).

```json
{
  "id": 1, "index": 0, "createdAt": "2026-10-30", "assignee_id": 2,
  "title": "Task 1", "content": "Description", "status": "draft",
  "taskLabelIds": [1, 3]
}
```

**Фильтрация** — `GET /api/tasks` с необязательными параметрами. Они
объединяются через AND; пустые значения игнорируются; неизвестный `status`,
`assigneeId` или `labelId` даёт `200` и `[]`; нечисловой id — `400`.
Фильтры работают вместе с пагинацией.

| Параметр | Что делает |
|---|---|
| `titleCont` | подстрока в названии, без учёта регистра |
| `assigneeId` | задачи исполнителя |
| `status` | задачи со статусом по `slug` |
| `labelId` | задачи с меткой |

```bash
curl 'http://localhost:8080/api/tasks?titleCont=create&status=draft&labelId=1' \
  -H "Authorization: Bearer $TOKEN"
```

**Частичное обновление** (`PUT /api/tasks/{id}`):

| Поле | Отсутствует | `null` | Значение |
|---|---|---|---|
| `title` | не менять | `400` | замена (до 255 символов) |
| `status` | не менять | `400` | поиск по `slug`, не найден — `400` |
| `content`, `index` | не менять | очищает | замена |
| `assignee_id` | не менять | снимает исполнителя | замена, не найден — `400` |
| `taskLabelIds` | не менять | очищает | замена набора, `[]` очищает, не найден id — `400` |

Kanban шлёт `{"index": N}` и `{"index": N, "status": "slug"}`; метки при этом
не затрагиваются. Неизвестные поля игнорируются.

### Коды ошибок

| Код | Когда |
|---|---|
| `400` | валидация, невалидный JSON, `null` для обязательного поля, несуществующая ссылка в теле, нечисловой id в query |
| `401` | нет токена, токен просрочен или недействителен, неверные креды |
| `403` | нет прав на операцию (чужой пользователь) |
| `404` | ресурс по URL не найден |
| `409` | дубликат, ресурс используется другой сущностью |
| `405`, `406`, `415` | неподдерживаемые метод, `Accept`, `Content-Type` |
| `500` | внутренняя ошибка |

Тело ошибки валидации — `{"поле": "сообщение"}`; для остальных — короткий текст.

## Безопасность

- **JWT RS256**, время жизни 1 час (`app.jwt.ttl`). Claims: `iss`
  (`hexlet-task-manager`), `sub` (id пользователя), `email`, `iat`, `exp`.
  Декодер проверяет подпись, срок и издателя.
- **Ключ.** В `production` берётся из `JWT_PRIVATE_KEY`, публичный ключ
  выводится из него; в `development` и тестах генерируется при старте.
- **Права.** Правило по умолчанию — `authenticated()`. Открыты только логин,
  регистрация, `GET` статусов, статика SPA, документация и health.
  `PUT`/`DELETE` пользователя проверяются через `@PreAuthorize` до входа в
  метод (чужой несуществующий id даёт `403`, а не `404`). Остальные ресурсы
  может менять любой аутентифицированный пользователь.
- **CSRF отключён:** токен в заголовке, а не в cookie, и сессий нет.
- **Токен на фронтенде.** Пакет Hexlet хранит токен в `localStorage` вместе с
  логином и паролем открытым текстом. Это известный компромисс учебного
  пакета и уязвимость к XSS; в реальном продукте нужен HttpOnly cookie
  с CSRF-токеном.

## Ограничения

- Без `_start`/`_end` списки отдаются целиком, без верхней границы; для
  больших объёмов нужно сделать пагинацию обязательной.
- Нет ролей: любой аутентифицированный пользователь может менять задачи,
  статусы и метки (пользователя — только самого себя).
- Нет refresh-токенов и отзыва токена: токен живёт 1 час.
- Нет ограничения частоты запросов к `/api/login`.

## Тесты и качество

```bash
./gradlew test jacocoTestReport   # тесты и покрытие (build/reports/jacoco)
./gradlew spotlessCheck           # форматирование (spotlessApply — исправить)
```

- Интеграционные тесты работают на реальном PostgreSQL через Testcontainers
  (**нужен запущенный Docker**); сервисные, маппер-тесты, тест
  `PageableFactory` и проверка OpenAPI идут без Docker. Тесты выполняются с
  профилем `test`.
- Покрыто: CRUD всех ресурсов, права и коды ошибок, `PatchSupport`,
  фильтры, пагинация и сортировка, число SQL-запросов, идемпотентность стартовых данных.
- **CI** (GitHub Actions): сборка, тесты, покрытие, анализ SonarQube,
  проверка `docker build`.
- **Деплой:** Render, Docker-образ собирается из репозитория.
