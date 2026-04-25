# Territory Wars — Полное описание проекта

> Документ написан для быстрого ввода в контекст нового чата с ИИ.
> Последнее обновление: 2026-04-24 (сессия 5)

---

## Что это за проект

**Territory Wars** — мобильная геолокационная игра. Игрок физически ходит по улицам,
прокладывает маршрут (GPS), и когда возвращается к стартовой точке — замкнутый полигон
превращается в захваченную территорию, которая отображается на карте у всех игроков.
Территории можно отвоёвывать у других игроков (полностью или частично через PostGIS).

---

## Компоненты системы

```
TerritoryWars/
├── app/          — Android-приложение (Kotlin, Jetpack Compose)
├── server/       — Node.js API + WebSocket сервер
└── admin/        — Веб-панель администратора (React + Vite + Tailwind)

C:\Clod\Tests\   — тест-кейсы и документация (НЕ часть сборки)
├── maestro/      — Maestro E2E тесты (.yaml)
├── androidTest/  — Android инструментальные тесты
└── docs/         — бизнес-требования, скриншоты экранов
```

---

## Подключение к серверу (SSH)

```bash
# Ключ лежит здесь:
C:\Users\Илья\Downloads\runterritory-api.pem

# Подключение:
ssh -i "C:/Users/Илья/Downloads/runterritory-api.pem" -o StrictHostKeyChecking=no root@93.183.74.141

# Загрузка файла:
scp -i "C:/Users/Илья/Downloads/runterritory-api.pem" -o StrictHostKeyChecking=no <local> root@93.183.74.141:<remote>

# Пересборка API после изменений:
cd /opt/territorywars && docker compose build api && docker compose up -d api
```

---

## Серверная часть (`server/`)

### Стек
- **Node.js** + **TypeScript** + **Express**
- **Prisma ORM** + **PostgreSQL 16** + расширение **PostGIS 3.4**
- **Socket.IO** — WebSocket для real-time событий
- **Redis 7** — кеширование (пока используется минимально)
- **Docker Compose** — поднимает: PostgreSQL (`tw_postgres`), Redis (`tw_redis`), API (`tw_api`), Nginx (`tw_nginx`)
- **Nginx** — reverse proxy, раздаёт статику, проксирует /api/ и WebSocket

### Адрес боевого сервера
```
http://93.183.74.141          — Nginx
http://93.183.74.141/api/     — REST API
ws://93.183.74.141            — WebSocket (Socket.IO)
```

### Конфиг сборки Android
В `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "BASE_URL", "\"http://93.183.74.141/api/\"")
buildConfigField("String", "WS_URL",   "\"ws://93.183.74.141\"")
```

### REST API эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/auth/register` | Регистрация |
| POST | `/api/auth/login` | Вход |
| POST | `/api/auth/refresh` | Обновление access token |
| POST | `/api/auth/logout` | Выход |
| GET  | `/api/auth/check-username?username=` | Проверка занятости ника |
| GET  | `/api/auth/check-email?email=` | Проверка занятости email |
| GET  | `/api/users/me` | Текущий пользователь |
| GET  | `/api/users/:id` | Пользователь по ID |
| PATCH | `/api/users/me` | Обновить профиль |
| GET  | `/api/users/:id/territories` | Территории пользователя |
| GET  | `/api/territories?bbox=lat1,lng1,lat2,lng2` | Территории в bbox |
| GET  | `/api/territories/my` | Мои территории |
| GET  | `/api/territories/:id` | Территория по ID |
| POST | `/api/territories/capture` | **Захват территории** |
| GET  | `/api/clans` | Список кланов |
| POST | `/api/clans` | Создать клан |
| GET  | `/api/clans/:id` | Клан по ID |
| PUT  | `/api/clans/:id` | Обновить клан (лидер) |
| DELETE | `/api/clans/:id` | Удалить клан (лидер) |
| POST | `/api/clans/:id/join` | Вступить в клан |
| POST | `/api/clans/:id/leave` | Покинуть клан |
| DELETE | `/api/clans/:id/members/:userId` | Исключить участника (кик) |
| POST | `/api/clans/:id/request` | Отправить заявку на вступление |
| GET  | `/api/clans/:id/requests` | Список заявок (лидер) |
| POST | `/api/clans/:id/requests/:userId/accept` | Принять заявку (лидер) |
| DELETE | `/api/clans/:id/requests/:userId` | Отклонить заявку (лидер) |
| GET  | `/api/clans/:id/activity?limit=30` | История захватов клана |
| POST | `/api/clans/:id/avatar` | Загрузить аватарку клана (лидер, multipart) |
| GET  | `/api/leaderboard` | Рейтинг игроков |
| GET  | `/api/cities` | Список городов |
| GET  | `/api/admin/*` | Административные эндпоинты |
| GET  | `/health` | Health-check |

### Логика захвата территории (`server/src/routes/territories.ts`)

**POST `/api/territories/capture`** — ключевой эндпоинт, принимает массив GPS-точек:

1. **Anti-cheat**: скорость между точками > 12 м/с → отклонение
2. **Замыкание полигона**: добавляет первую точку в конец если не замкнут
3. **ST_MakeValid + ST_Dump**: самопересекающийся маршрут автоматически разбивается на несколько валидных полигонов (например, области A и B при маршруте-восьмёрке). Каждый кусок >= 100 м² и <= 5 км² захватывается отдельно.
4. **Захват чужих территорий** для каждого куска:
   - Находит вражеские территории через `ST_Intersects`
   - `ST_Dump(ST_MakeValid(ST_Difference(...)))` — остатки врага
   - Если кусков нет → **полный захват**: DELETE + `emitTerritoryDeleted()`
   - Если есть кусок → **частичный захват**: UPDATE polygon + `emitTerritoryUpdate()`
5. **Слияние со своими**: если кусок перекрывает свои — `ST_Union` всех → одна запись. Сохраняется настоящая объединённая геометрия (`ST_AsText(ST_Union(...)) AS merged_wkt`).
6. **Создание новой** если нет пересечений
7. **Broadcast**: `emitTerritoryUpdate()` для каждого созданного куска через Socket.IO
8. **Ответ**: `{ success, territory (первый/наибольший), merged, conquered, error }`

### Raw SQL и типы UUID в PostgreSQL

**ВАЖНО**: Prisma передаёт строковые параметры как `text`. PostgreSQL не имеет оператора `text = uuid`.
Правильный способ сравнения в `$executeRaw` / `$queryRaw`:

```typescript
// ✅ Правильно — кастуем КОЛОНКУ к text:
WHERE clan_id::text = ${clanId}
WHERE id::text = ${userId}

// ✅ Правильно — кастуем ПАРАМЕТР к uuid (для SET):
SET clan_id = ${clanId}::uuid

// ❌ Неправильно — вызывает ошибку "operator does not exist: text = uuid":
WHERE clan_id = ${clanId}::uuid
```

### WebSocket (`server/src/ws/socket.ts`)

```
Аутентификация: JWT передаётся в socket.handshake.auth.token
Комнаты: user:{userId}, bbox:{lat}_{lng}_{lat}_{lng}
События клиент → сервер: subscribe_bbox, location_update
События сервер → клиент: territory_updated, territory_deleted, player_moved
```

Функции для broadcast (вызываются из routes):
- `emitTerritoryUpdate(territory)` — всем клиентам
- `emitTerritoryDeleted(territoryId)` — всем клиентам

### База данных — Prisma Schema

```
User             — id, email, username, passwordHash, color, cityId, clanId,
                   totalAreaM2, territoriesCount, capturesCount, takeoversCount,
                   distanceWalkedM, fcmToken
City             — id, name, region, population, lat, lng
Territory        — id, ownerId, clanId, polygon(geometry), areaM2, perimeterM,
                   capturedAt, updatedAt
                 — ВАЖНО: колонка polygon НЕ в схеме Prisma (raw SQL), тип: geometry(Polygon,4326)
Clan             — id, name, tag, leaderId, color, avatarUrl?, description, maxMembers,
                   totalAreaM2, territoriesCount
                 — Миграция: server/prisma/migrations/add_clan_avatar.sql
ClanMember       — userId, clanId, role (LEADER/MEMBER), joinedAt
ClanJoinRequest  — id, userId, clanId, createdAt
                 — @@unique([userId, clanId]) — один запрос от пользователя на клан
                 — Миграция: server/prisma/migrations/add_clan_join_requests.sql
RefreshToken     — id, userId, tokenHash, expiresAt
Admin            — id, username, passwordHash
```

**Важно**: колонка `polygon` в таблице `territories` — PostGIS-тип `geometry(Polygon, 4326)`.
Она не отражена в `schema.prisma` (нет нативной поддержки). Все запросы к ней — `$queryRaw`.
Миграция для колонки: `server/prisma/migrations/add_postgis_polygon.sql`

### JWT авторизация
- Access token: 15 минут, secret из env `JWT_ACCESS_SECRET`
- Refresh token: 30 дней, хранится в БД (хеш SHA-256), возвращается клиенту как plain token
- Middleware `requireAuth` — декодирует JWT, кладёт `req.user = { userId, email }`
- `adminAuth` — отдельный middleware для `/api/admin/*`

---

## Android-приложение (`app/`)

### Стек
- **Kotlin** + **Jetpack Compose** (Material3)
- **Hilt** (DI)
- **Retrofit** + **OkHttp** + **Gson** (HTTP)
- **Socket.IO Android client** v2.1.0 (WebSocket)
- **Yandex MapKit** (карта, рендер полигонов)
- **Google Fused Location Provider** (GPS)
- **DataStore Preferences** (токены, настройки)
- **Room** (локальная БД, используется минимально)
- **Firebase Messaging** (push-уведомления)
- **Coil** (загрузка изображений)
- **Accompanist Permissions** (запрос разрешений)
- **Timber** (логирование)

### Экраны (навигация — Compose Navigation)

| Screen | Route | Описание |
|--------|-------|----------|
| Splash | `splash` | Загрузка, проверка токена |
| Login  | `login` | Вход |
| Register | `register` | Регистрация |
| Map    | `map` | **Главный экран** — карта + захват |
| Profile | `profile` | Профиль игрока |
| EditProfile | `edit_profile` | Редактировать профиль |
| Leaderboard | `leaderboard` | Рейтинг |
| Clan   | `clan` | Клан |

### Структура пакетов

```
com.territorywars/
├── data/
│   ├── local/
│   │   ├── TokenDataStore.kt   — accessToken, refreshToken, playerMarker
│   │   ├── AppDatabase.kt      — Room DB
│   │   └── TerritoryDao.kt     — DAO
│   └── remote/
│       ├── AuthInterceptor.kt  — добавляет Bearer token к запросам
│       ├── TokenAuthenticator.kt — обновляет токен при 401
│       ├── SocketManager.kt    — Socket.IO singleton, SharedFlow событий
│       └── api/
│           ├── AuthApi.kt
│           ├── TerritoryApi.kt
│           ├── UserApi.kt
│           ├── ClanApi.kt
│           ├── LeaderboardApi.kt
│           └── CityApi.kt
│       └── dto/
│           ├── TerritoryDto.kt  — CaptureRequest, CaptureResponse, RoutePointDto
│           ├── AuthDto.kt
│           ├── UserDto.kt
│           └── CityDto.kt
├── di/
│   ├── NetworkModule.kt   — OkHttp, Retrofit, все API
│   └── AppModule.kt       — FusedLocationProviderClient, др.
├── domain/model/
│   ├── Territory.kt
│   ├── GeoPoint.kt        — lat, lng
│   ├── RoutePoint.kt      — lat, lng, timestamp, accuracy
│   ├── Clan.kt
│   ├── ClanMember.kt      — userId, username, color, role, totalAreaM2, joinedAt
│   ├── ClanLeaderboardEntry.kt
│   └── LeaderboardEntry.kt
├── presentation/
│   ├── map/
│   │   ├── MapScreen.kt       — UI карты, кнопки захвата, AppBottomNav
│   │   ├── MapViewModel.kt    — вся логика карты + clanRequestsBadge
│   │   ├── YandexMapView.kt   — AndroidView обёртка для MapKit
│   │   └── PlayerMarkerIcon.kt
│   ├── auth/ (Login, Register + ViewModels)
│   ├── profile/
│   ├── clan/
│   │   ├── ClanScreen.kt      — экран клана (мой клан / топ кланов / заявки)
│   │   └── ClanViewModel.kt   — ClanState, ClanJoinRequestItem
│   ├── leaderboard/
│   ├── splash/
│   ├── components/         — AppTextField, PrimaryButton, TerritoryLogo
│   ├── theme/              — Color, Type, Theme
│   └── navigation/NavGraph.kt
├── service/
│   ├── GpsTrackingService.kt   — Foreground service для GPS во время захвата
│   └── FirebaseMessagingService.kt — FCM push
├── MainActivity.kt
└── TerritoryWarsApp.kt    — Hilt Application, инициализация Timber + YandexMapKit
```

### MapScreen — компоновка UI

```
Box(fillMaxSize) {
  YandexMapView                          — карта на весь экран
  GPS permission banner                  — сверху, если нет разрешения
  LegendPill "Ваши"                      — верхний правый угол (только не в захвате)
  GlassFab (GPS кнопка)                  — правый нижний угол
    bottom = если захват: 140dp, иначе: 136dp  (всегда выше кнопок)
  Мини-статистика (Дистанция + Время)    — нижний центр, bottom=76dp (только в захвате, выезжает снизу)
  Row [Старт] [Завершить]                — ВСЕГДА видны, нижний центр
    bottom = захват: 12dp, иначе: 68dp (над навбаром)
    Старт:     активен (градиент) когда не захватываем, серый когда захватываем
    Завершить: серый когда не захватываем, красный (border + bg) когда захватываем
  AppBottomNav                           — нижний центр (только не в захвате)
  AppSnackbar                            — верхний центр (уведомления)
}
```

### AppBottomNav — кастомный компактный бар

```kotlin
// НЕ используем NavigationBar из Material3 (он слишком высокий ~80dp)
// Кастомный Row:
Row(height = 60.dp) {
  // 4 вкладки: Карта, Профиль, Рейтинг, Клан
  // Иконки 24dp, подпись 10sp
  // Клан: BadgedBox если clanRequestsBadge > 0
}
```

### MapViewModel — ключевые константы

```kotlin
private const val BBOX_DEBOUNCE_MS = 500L
private const val GPS_INTERVAL_CAPTURING_MS = 2000L
private const val GPS_INTERVAL_IDLE_MS = 5000L
private const val MAX_SPEED_KMH = 16.0          // порог скоростного античита
private const val SPEED_GRACE_PERIOD_MS = 120_000L // 2 минуты до принудительного завершения
```

### Процесс захвата (клиент)

1. Кнопка **Старт** — всегда видна. `startCapture()` → GPS 2 сек, запускает `GpsTrackingService`
2. `onNewLocation()` — при точности > 30м пропускает; накапливает `routePoints`, считает `routeDistanceM`; вызывает `checkSpeedViolation()`
3. Кнопка **Завершить** — всегда видна, краснеет когда `isCapturing = true`
4. `finishCapture()` — минимум 4 точки → отправляет `POST /territories/capture { route_points: [...] }`
5. Ответ парсится: `conquered`, `merged` → уведомление
6. Дополнительные территории (при самопересечении) приходят через WebSocket

### Скоростной античит (клиент)

`checkSpeedViolation()` вызывается при каждом GPS-обновлении во время захвата:
- Считает среднюю скорость за последние **30 секунд** GPS-точек
- Если скорость > 16 км/ч → показывает диалог-предупреждение (нельзя закрыть свайпом)
- Пользователь нажимает **ОК** → запускается 2-минутный grace period
- Если за 2 мин скорость упала → нарушение снято, захват продолжается
- Если 2 мин прошло и скорость всё ещё > 16 км/ч → захват принудительно отменяется, территория **не засчитывается**

```
MapState поля:
  speedWarningVisible: Boolean     — показать диалог-предупреждение
  speedViolationActive: Boolean    — идёт 2-минутный отсчёт
  speedViolationStartMs: Long      — когда начался отсчёт
  speedViolationCancelled: Boolean — показать финальный диалог отмены
```

### Клановая система

**ClanState** (в `ClanViewModel`):
```kotlin
data class ClanState(
  val myClan: Clan?,
  val myUserId: String?,
  val members: List<ClanMember>,
  val joinRequests: List<ClanJoinRequestItem>, // видны только лидеру
  val activityItems: List<ClanActivityItem>,   // история захватов (вкладка История)
  val selectedTab: ClanTab,                    // MEMBERS | TOP | HISTORY
  val topClans: List<ClanLeaderboardEntry>,    // когда не в клане
  val isLoading, isActivityLoading, isActionLoading,
  val error, actionError, successMessage,
  val isCreating: Boolean,
  val createForm: CreateClanForm
)
```

**Функции ClanViewModel**:
- `loadData()` — загружает клан, участников, заявки (если лидер)
- `selectTab(tab)` — переключает вкладку; при первом открытии История запускает `loadActivity()`
- `loadActivity()` — `GET /clans/:id/activity` → `activityItems`
- `uploadClanAvatar(uri)` — multipart загрузка аватарки клана (`POST /clans/:id/avatar`)
- `createClan()`, `joinClan()`, `leaveClan()`, `deleteClan()`
- `kickMember(userId)`, `requestJoinClan(clanId)`
- `acceptJoinRequest(userId)`, `declineJoinRequest(userId)`

**ClanScreen — режимы**:
1. Нет клана → список топ-кланов. Нажатие → диалог заявки
2. Есть клан → шапка (тег, имя, площадь, участники) + **3 вкладки**:
   - **Участники** — список участников, заявки (лидеру), кнопки Выйти/Удалить
   - **Топ** — участники отсортированы по `totalAreaM2` DESC, медали 🥇🥈🥉
   - **История** — последние 30 захватов территорий с инициалами, именем, датой, площадью

### SocketManager — real-time обновления

```kotlin
// Singleton, инжектируется в MapViewModel
class SocketManager @Inject constructor(private val tokenDataStore: TokenDataStore) {
    val territoryEvents: SharedFlow<TerritoryEvent>  // Updated | Deleted
    fun connect() { /* подключается с JWT, слушает territory_updated / territory_deleted */ }
    fun disconnect()
}
```

URL WebSocket: `BuildConfig.BASE_URL.removeSuffix("api/").trimEnd('/')`

### Полигоны на карте (Yandex MapKit)

`YandexMapView.kt` — AndroidView с MapKit. Полигоны рисуются через MapObjects API.
Цвет полигона: `ownerColor` (hex), заливка 30% прозрачность, граница 100%.
Если у пользователя есть `clanColor` — используется он.

### Ключ Yandex MapKit
```
aa464463-dc33-4ba3-8537-d4c089f4cc05
```
Прописан в `app/build.gradle.kts` как `YANDEX_MAPKIT_API_KEY`.

---

## Административная панель (`admin/`)

- **React + Vite + TypeScript + Tailwind CSS**
- Страницы: Dashboard, Users, Territories, Clans, Login
- Доступна по `http://93.183.74.141/admin/`
- Аутентификация: отдельная таблица `admins` (не связана с User)
- Статика собирается в `admin/dist/`, раздаётся через Nginx

---

## Инфраструктура деплоя

```
Сервер: 93.183.74.141 (VPS)
Удалённая папка: /opt/territorywars
Docker Compose: server/docker-compose.yml
  - tw_postgres (PostGIS)
  - tw_redis
  - tw_api (Node.js, порт 3000)
  - tw_nginx (порт 80)

Скрипты:
  server/deploy.sh           — сборка и перезапуск контейнеров
  server/deploy-to-server.sh — rsync + deploy на сервер
  deploy-admin.sh            — сборка и деплой admin панели
```

### Деплой изменений на сервер (быстрый способ)
```bash
# 1. Загрузить изменённый файл
scp -i "C:/Users/Илья/Downloads/runterritory-api.pem" -o StrictHostKeyChecking=no \
  server/src/routes/clans.ts root@93.183.74.141:/opt/territorywars/src/routes/clans.ts

# 2. Пересобрать и перезапустить API
ssh -i "C:/Users/Илья/Downloads/runterritory-api.pem" -o StrictHostKeyChecking=no root@93.183.74.141 \
  "cd /opt/territorywars && docker compose build api && docker compose up -d api"
```

### Nginx конфиг (ключевое)
- `/api/*` → `proxy_pass http://tw_api:3000`
- `/socket.io/*` → proxy + WebSocket upgrade headers
- `/uploads/*` → статика
- `/admin/*` → статика из `admin/dist/`
- `/` → 404 или редирект

---

## Сборка APK

Открыть в **Android Studio**, нажать **≡ (гамбургер-меню) → Build → Build Bundle(s) / APK(s) → Build APK(s)**.
APK окажется в `app/build/outputs/apk/debug/app-debug.apk`.
**НЕ использовать** `app/build/intermediates/` — там неполный файл.

Gradle: `./gradlew assembleDebug` из папки `TerritoryWars/`.

---

## Технический долг

### DEBT-01 — Отсутствие транзакции в capture endpoint (критично)

**Файл:** `server/src/routes/territories.ts` → `POST /territories/capture`

**Проблема:** Весь capture-цикл выполняется без `prisma.$transaction()`. WebSocket-события
(`emitTerritoryUpdate`, `emitTerritoryDeleted`) отправляются клиентам прямо внутри цикла
по вражеским территориям — до того, как все изменения гарантированно зафиксированы в БД.
При падении сервера на середине операции клиенты получат частичное состояние.

**Как исправить:**
```typescript
// Обернуть всю логику изменения данных в транзакцию
const result = await prisma.$transaction(async (tx) => { ... });
// WebSocket emit — ПОСЛЕ завершения транзакции
emitTerritoryUpdate(result.territory);
```
**Сложность:** средняя — нужно пробросить `tx` во все вложенные операции внутри цикла.

---

### DEBT-02 — Монолитный capture endpoint (сопровождаемость)

**Файл:** `server/src/routes/territories.ts`

**Проблема:** `POST /territories/capture` одновременно делает: валидацию маршрута, античит,
PostGIS-геометрию, цикл по вражеским территориям, merge своих, статистику пользователей,
статистику кланов, broadcast WebSocket. Такой узел регрессионно опасен и сложен для тестирования.

**Как исправить:** вынести в сервис `TerritoryService` с методами:
`validateRoute()`, `processEnemyOverlaps()`, `mergeOwnTerritories()`, `updateStats()`

---

### DEBT-03 — Prisma + raw SQL для PostGIS (прозрачность)

**Файл:** `server/src/routes/territories.ts`, `server/src/routes/clans.ts`

**Проблема:** Колонка `polygon` не отражена в `schema.prisma` (PostGIS-тип не поддерживается
Prisma). Все запросы к ней идут через `$queryRaw`. Это вынужденный компромисс, но он:
- ослабляет типизацию
- переносит SQL-правила в route-слой
- привёл к ошибке `text = uuid` (уже задокументирована и исправлена)

**Как исправить:** вынести все PostGIS-запросы в `TerritoryRepository` — хотя бы изолировать
сырой SQL от бизнес-логики маршрутов.

---

## Известные баги (актуально на 2026-04-23)

| ID | Файл | Описание |
|----|------|----------|
| BUG-06 | `ClanViewModel.kt` | Двойной `response.body()!!` — риск NPE |
| BUG-07 | Profile/Leaderboard vs Clan | Несовместимые единицы площади (км² vs га) |
| BUG-08 | `TokenAuthenticator.kt` | Создаёт новый Retrofit/OkHttp на каждый 401 |
| BUG-09 | `ClanScreen.kt` | Показывает `members.size` вместо `clan.membersCount` |

---

## Реализованные фичи (статус на 2026-04-23)

- [x] Регистрация / вход / обновление токена
- [x] GPS-захват территорий (обход по периметру)
- [x] PostGIS полигоны, валидация площади
- [x] Anti-cheat по скорости GPS
- [x] Слияние (merge) своих перекрывающихся территорий (исправлен баг: теперь сохраняется настоящая union-геометрия)
- [x] **Захват чужих территорий** — полный и частичный (ST_Difference + ST_Dump)
- [x] **Захват самопересекающихся маршрутов** — ST_MakeValid разбивает на несколько областей, каждая захватывается отдельно
- [x] Real-time обновления через Socket.IO (territory_updated / territory_deleted)
- [x] SocketManager на клиенте — SharedFlow событий
- [x] Клановая система (создание, вступление, выход, исключение, удаление клана лидером)
- [x] Заявки на вступление в клан: игрок → запрос → FCM push лидеру → лидер принимает/отклоняет в экране Клан
- [x] Бейдж на вкладке «Клан» (BadgedBox) — количество непрочитанных заявок
- [x] При принятии заявки: территории игрока автоматически переходят в клан
- [x] Рейтинг игроков
- [x] Профиль с аватаром, цветом, статистикой
- [x] GpsTrackingService (foreground service)
- [x] Firebase push-уведомления (FCM)
- [x] Административная веб-панель
- [x] Компактный bottom nav (60dp, иконки 24dp, кастомный Row вместо Material3 NavigationBar)
- [x] Кнопки захвата **Старт** / **Завершить** — всегда видны; Завершить краснеет при захвате
- [x] Мини-статистика (Дистанция + Время) выезжает снизу во время захвата
- [x] GPS кнопка всегда выше кнопок (136dp idle / 140dp capturing)
- [x] Вкладки в экране Клана: Участники / Топ / История
- [x] Исправлен `express-rate-limit` — добавлен `app.set('trust proxy', 1)` для работы за Nginx
- [x] Push «Ваша территория перезахвачена» — полный захват (`notifyTerritoryTakeover`) и частичный (`notifyPartialTakeover`)
- [x] Аватарка пользователя — загрузка через галерею в ProfileScreen, отображение везде через `UserAvatar` composable
- [x] Аватарка клана — лидер загружает в ClanScreen; поле `avatar_url` в БД и API
- [x] **UserAvatar** composable — единый компонент: инициалы (base) + AsyncImage сверху с crossfade
- [x] Аватарки пользователей в рейтинге (Podium, PlayerRow) и экране клана (MemberRow, TopMemberRow, JoinRequestRow)
- [x] Аватарка клана в рейтинге (ClanRow)
- [x] **Скоростной античит** — при средней скорости > 16 км/ч предупреждение + 2-минутный grace period; принудительная отмена если скорость не снизилась

---

## Важные технические детали

### Формат полигона
- В БД: `geometry(Polygon, 4326)` WKT — `POLYGON((lng lat, ...))`
- В API JSON: `polygon: [[lng, lat], ...]` — **сначала longitude, потом latitude**
- В Android domain model `GeoPoint(lat, lng)` — **сначала latitude**
- `TerritoryDto.toDomain()`: `GeoPoint(lat = it[1], lng = it[0])`

### Координатный порядок — частый источник ошибок
WKT и GeoJSON используют `[lng, lat]`, Yandex MapKit использует `Point(lat, lng)`.
При рендере полигонов обязательно переставлять координаты.

### ST_Difference и MULTIPOLYGON
`ST_Difference` может вернуть MULTIPOLYGON. В колонку `geometry(Polygon, 4326)` нельзя записать мультиполигон.
Решение: `ST_Dump` разбивает на части, фильтруем только `ST_Polygon` с площадью >= 100 м².

### Самопересекающиеся маршруты (figure-8, петли)
Путь вида «восьмёрка» создаёт самопересекающийся полигон.
`ST_IsValid` = false → раньше отклонялось.
Теперь: `ST_MakeValid(ST_GeomFromText(wkt))` → MULTIPOLYGON → `ST_Dump` → несколько POLYGON → каждый обрабатывается как отдельный захват.

### UUID vs text в Prisma raw SQL
Prisma отправляет строковые параметры как `text`. Сравнение с UUID-колонкой нужно делать кастом колонки:
```sql
WHERE clan_id::text = ${clanId}   -- ✅
WHERE id::text = ${userId}        -- ✅
SET clan_id = ${clanId}::uuid     -- ✅ (для SET)
WHERE clan_id = ${clanId}::uuid   -- ❌ ошибка "operator does not exist: text = uuid"
```

### TokenDataStore (DataStore Preferences)
```kotlin
val accessToken: Flow<String?>
val refreshToken: Flow<String?>
val playerMarker: Flow<Int>    // id маркера игрока на карте
```

### Аутентификация HTTP (Android)
- `AuthInterceptor` добавляет `Authorization: Bearer <accessToken>` к каждому запросу
- `TokenAuthenticator` — при 401 вызывает `/auth/refresh`, получает новый access token

### Хук READ-BEFORE-EDIT
В проекте настроен хук, который срабатывает перед каждым Edit. Это только напоминание — не блокировка. Файл будет отредактирован если он уже читался в сессии.

---

## Пример типичного запроса

```
POST /api/territories/capture
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "route_points": [
    { "lat": 55.751, "lng": 37.618, "timestamp": 1700000000000, "accuracy": 5.0 },
    ...
  ]
}

Response (одна область):
{ "success": true, "territory": {...}, "merged": false, "conquered": 0, "error": null }

Response (самопересекающийся маршрут — 2 области):
{ "success": true, "territory": {...область A...}, "merged": true, "conquered": 0, "error": null }
// Область B приходит отдельно через WebSocket: territory_updated
```

---

---

## Незавершённые задачи (из плана сессии 5)

| # | Фича | Статус | Затронутые файлы |
|---|------|--------|-----------------|
| 1 | **Цвет полигонов клана** — лидер выбирает цвет; полигоны клана перекрашиваются | ⏳ | `ClanScreen.kt`, `YandexMapView.kt` |
| 2 | **Перекраска при вступлении** — территории принимают `clanColor` | ⏳ | `YandexMapView.kt` (поле `clan_color` уже есть в DTO) |

### Технические заметки

**Цвет полигонов клана:**
- В `TerritoryDto` уже есть `clan_color` (возвращается сервером)
- В `YandexMapView.kt` проверить: если `clanColor != null` → использовать его вместо `ownerColor`
- Лидер может менять `color` клана через `PUT /clans/:id` (уже реализован)

---

## GitHub репозиторий

```
https://github.com/Kembri90-code/TerritoryWars5
Ветка: main
```
