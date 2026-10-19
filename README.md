# Task Manager

[![CI](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AMOrlovSev/task-manager/actions/workflows/ci.yml)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_task-manager&metric=coverage)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_task-manager)

REST API менеджера задач на Spring Boot с SPA-фронтендом: задачи, статусы,
метки, пользователи, JWT-аутентификация, фильтрация.

- **Демо:** https://task-manager-c4wi.onrender.com
- **Swagger UI:** https://task-manager-c4wi.onrender.com/docs

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

При старте в профилях `development` и `production` (если задано `app.mock-data.enabled=true`) 
генерируется 30 демонстрационных задач, 5 пользователей и 10 меток для удобной проверки 
пагинации, сортировки и работы Kanban-доски. В тестах профиль `test` отключает генерацию, 
чтобы не засорять базу.

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

Другие команды: `make test`, `make lint`, `make format`, `make frontend`.
Режим разработки фронтенда с hot reload: `./gradlew bootRun` и рядом
`npx start-frontend` (dev-сервер проксирует `/api/**` на бэкенд).

### Docker

```bash
docker build -t task-manager .
docker run --rm -p 8080:8080 task-manager
```

Стадия `node:22-slim` собирает SPA, Gradle-стадия кладёт её в
`src/main/resources/static`, финальный образ - JRE под непривилегированным
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

- `controller` - REST-эндпоинты, валидация входа (`@Valid`), без бизнес-логики.
- `service` - бизнес-правила и транзакции; маппинг в DTO происходит внутри
  транзакции (`open-in-view: false`, связи `LAZY`).
- `repository` - Spring Data JPA; фильтры задач на `JpaSpecificationExecutor`.
- `handler` - `GlobalExceptionHandler`: одно место, где исключения
  превращаются в HTTP-коды.
- `component/PatchSupport` - частичное обновление через `PUT` (см. ниже).
- Схемой управляет Flyway (`V1`–`V5`), Hibernate работает в режиме
  `validate`: если сущности разойдутся с миграциями, приложение не стартует.

### Модель данных

- **User** - email (уникален, нормализуется), пароль хранится как BCrypt-хеш.
- **TaskStatus** - `name` и `slug`, оба уникальны. По умолчанию: `draft`,
  `to_review`, `to_be_fixed`, `to_publish`, `published`.
- **Label** - `name` (3–1000 символов, уникален). По умолчанию: `feature`, `bug`.
- **Task** - название, описание, `index`, обязательный статус, необязательные
  исполнитель и набор меток (many-to-many, владелец связи - `Task`).

### Принятые решения

- **Частичное обновление через `PUT`.** Контракт фронтенда: в теле любое
  подмножество полей. Отсутствующее поле не меняется, `null` очищает
  необязательное поле (`content`, `index`, `assignee_id`, `taskLabelIds`),
  `null` для обязательного - `400`. Верхний уровень патча заменяет значения
  целиком (`ObjectNode.setAll`), чтобы массивы заменялись, а не дописывались.
- **Коды ответов.** Неизвестная ссылка в теле (`status`, `assignee_id`,
  `taskLabelIds`) - `400`, а не `404`: `404` значит «нет ресурса по URL».
  Дубликат уникального значения и удаление используемого ресурса - `409`.
  Проверки в сервисах дублируются внешними ключами и уникальными индексами БД.
- **Метки.** Нет `cascade` на `@ManyToMany`: удаление задачи не удаляет метки.
  Метку, привязанную к задаче, удалить нельзя (`409`).
- **N+1.** Список задач загружается одним запросом (`@EntityGraph` на
  `findAll(Specification, Sort)`); это закреплено тестом на число запросов
  (`TaskQueryCountTest`, статистика Hibernate).
- **Фильтры.** `titleCont` без учёта регистра, спецсимволы `LIKE` экранируются;
  фильтр по метке сделан через `exists`-подзапрос, чтобы в ответе у задачи
  оставались все её метки и не было дублей.
- **Документация публична, API нет.** Swagger UI и спецификация открыты без
  токена, чтобы контракт можно было прочитать; все данные защищены JWT.
- **Ошибки логина.** Все сбои (неверный пароль, неизвестный email, пустые
  поля) дают одинаковый `401 Invalid credentials` - защита от перебора email.
- **Пагинация.** Смещение (`_start`) должно быть выровнено по размеру страницы: 
  `_start % (_end - _start) == 0`. Размер страницы ограничен `max-size`, 
  чтобы предотвратить выкачивание всей базы. Поля сортировки строго валидируются 
  по белому списку во избежание SQL-инъекций и падений при некорректных именах полей. 
  Для загрузки меток при пагинации используется `@BatchSize`, чтобы избежать 
  проблемы N+1 (Hibernate делает один дополнительный запрос на пачку меток, 
  а не на каждую задачу).



## API

Полное описание, схемы и примеры: **Swagger UI** (`/docs`), спецификация -
`/api-docs` (JSON) и `/api-docs.yaml` (YAML). Защищённые эндпоинты требуют
`Authorization: Bearer <JWT>`.

| Ресурс | Методы | Доступ |
|---|---|---|
| `/api/login` | `POST` | открыт |
| `/api/users`, `/api/users/{id}` | `GET`, `POST`, `PUT`, `DELETE` | `POST` (регистрация) открыт, остальное - JWT; `PUT` и `DELETE` только для самого пользователя |
| `/api/task_statuses`, `/api/task_statuses/{id}` | `GET`, `POST`, `PUT`, `DELETE` | `GET` открыт, запись - JWT |
| `/api/labels`, `/api/labels/{id}` | `GET`, `POST`, `PUT`, `DELETE` | JWT |
| `/api/tasks`, `/api/tasks/{id}` | `GET`, `POST`, `PUT`, `DELETE` | JWT |

Также: `/welcome`, `/actuator/health` (открыты). `POST` возвращает `201` и
`Location`, `DELETE` - `204`, списки отдают заголовок `X-Total-Count`.

### Пагинация и сортировка

Все эндпоинты, возвращающие списки (`GET /api/tasks`, `/api/users`, `/api/task_statuses`, `/api/labels`), поддерживают пагинацию и сортировку в формате `react-admin`.

**Параметры запроса:**
| Параметр | Описание | Пример |
|---|---|---|
| `_start` | Смещение (начиная с 0). Должно быть кратно размеру страницы. | `0` |
| `_end` | Конец диапазона (не включая). Размер страницы = `_end - _start`. | `10` |
| `_sort` | Поле для сортировки (см. белый список ниже). | `id` |
| `_order` | Направление сортировки: `ASC` или `DESC`. | `ASC` |

**Особенности:**
* Если `_start` и `_end` не переданы, возвращается весь список (как раньше).
* Если передан только один из них, возвращается `400 Bad Request`.
* Максимальный размер страницы ограничен настройкой `app.pagination.max-size` (по умолчанию `1000`).
* Если `_order` не передан, используется `ASC`. Если `_sort` не передан, сортировка по `id`.
* Для стабильности результатов к любой сортировке всегда добавляется `id` как второй ключ.
* В ответе всегда присутствует заголовок `X-Total-Count` с общим количеством записей (до применения пагинации, но после фильтрации).

**Белый список полей для сортировки:**
| Ресурс | Доступные поля `_sort` |
|---|---|
| **Tasks** | `id`, `title`, `content`, `index`, `createdAt`, `assignee_id`, `status` |
| **Users** | `id`, `email`, `firstName`, `lastName`, `createdAt` |
| **Task Statuses** | `id`, `name`, `slug`, `createdAt` |
| **Labels** | `id`, `name`, `createdAt` |

*Примечание:* Текстовые поля (`title`, `email`, `name` и т.д.) сортируются без учета регистра (`ignoreCase`).

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

**Фильтрация** - `GET /api/tasks` с необязательными параметрами. Они
объединяются через AND; пустые значения игнорируются; неизвестный `status`,
`assigneeId` или `labelId` даёт `200` и `[]`; нечисловой id - `400`.
`X-Total-Count` равен размеру отфильтрованного списка.

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
| `status` | не менять | `400` | поиск по `slug`, не найден - `400` |
| `content`, `index` | не менять | очищает | замена |
| `assignee_id` | не менять | снимает исполнителя | замена, не найден - `400` |
| `taskLabelIds` | не менять | очищает | замена набора, `[]` очищает, не найден id - `400` |

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

Тело ошибки валидации - `{"поле": "сообщение"}`; для остальных - короткий текст.

## Безопасность

- **JWT RS256**, время жизни 1 час (`app.jwt.ttl`). Claims: `iss`
  (`hexlet-task-manager`), `sub` (id пользователя), `email`, `iat`, `exp`.
  Декодер проверяет подпись, срок и издателя.
- **Ключ.** В `production` берётся из `JWT_PRIVATE_KEY`, публичный ключ
  выводится из него; в `development` и тестах генерируется при старте.
- **Права.** Правило по умолчанию - `authenticated()`. Открыты только логин,
  регистрация, `GET` статусов, статика SPA, документация и health.
  `PUT`/`DELETE` пользователя проверяются через `@PreAuthorize` до входа в
  метод (чужой несуществующий id даёт `403`, а не `404`). Остальные ресурсы
  может менять любой аутентифицированный пользователь.
- **CSRF отключён:** токен в заголовке, а не в cookie, и сессий нет.
- **Токен на фронтенде.** Пакет Hexlet хранит токен в `localStorage` вместе с
  логином и паролем открытым текстом. Это известный компромисс учебного
  пакета и уязвимость к XSS; в реальном продукте нужен HttpOnly cookie
  с CSRF-токеном.

## Тесты и качество

```bash
./gradlew test jacocoTestReport   # тесты и покрытие (build/reports/jacoco)
./gradlew spotlessCheck           # форматирование (spotlessApply - исправить)
```

- Интеграционные тесты работают на реальном PostgreSQL через Testcontainers
  (**нужен запущенный Docker**); сервисные, маппер-тесты и проверка
  OpenAPI идут без Docker.
- Покрыто: CRUD всех ресурсов, права и коды ошибок, `PatchSupport`,
  фильтры, число SQL-запросов, идемпотентность стартовых данных.
- **CI** (GitHub Actions): сборка, тесты, покрытие, анализ SonarQube,
  проверка `docker build`.
- **Деплой:** Render, Docker-образ собирается из репозитория.
