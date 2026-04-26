# Territory Wars — Полное описание проекта

> Документ написан для быстрого ввода в контекст нового чата с ИИ.
> Последнее обновление: 2026-04-25 (сессия 6)

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
- **Redis 7** — кеширование, DAU-трекинг (`SADD dau:{date} {userId}`), счётчик ошибок (`INCR errors:{date}`)
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
| DELETE | `/api/users/me` | Удалить аккаунт (+ клан если лидер) |
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
| Onboarding | `onboarding` | Обучение для новых игроков (4 слайда, HorizontalPager) |
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
│   ├── onboarding/
│   │   └── OnboardingScreen.kt  — 4 слайда (HorizontalPager), Далее/Начать/Пропустить
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
- Учётные данные: `admin` / `Admin123!`
- Статика собирается в `admin/dist/`, раздаётся через Nginx
- `BrowserRouter` с `basename="/admin"` — обязательно для корректной маршрутизации под `/admin/`

### Admin Dashboard — статистика (Redis)

`GET /api/admin/stats` возвращает:

| Поле | Источник |
|------|----------|
| `total_users` | `COUNT(*)` из `users` |
| `total_territories` | `COUNT(*)` из `territories` |
| `total_clans` | `COUNT(*)` из `clans` |
| `active_sessions` | `SCARD online_users` (Redis) |
| `dau_today` | `SCARD dau:{date}` (Redis) |
| `errors_today` | `GET errors:{date}` (Redis) |
| `dau_7days` | SCARD за 7 последних дней |
| `errors_7days` | GET за 7 последних дней |

**Трекинг DAU** — в middleware `auth.ts` при каждом успешном JWT-запросе:
```typescript
redis.sadd(`dau:${today}`, payload.userId)  // SADD = уникальные пользователи
redis.expire(`dau:${today}`, 30 * 86400)    // хранить 30 дней
```

**Счётчик ошибок** — в `errorHandler.ts` при каждой 500-ошибке:
```typescript
redis.incr(`errors:${today}`)
redis.expire(`errors:${today}`, 30 * 86400)
```

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

### Деплой Admin панели

**ВАЖНО**: `docker-compose.yml` монтирует `../admin/dist:/admin:ro`, что на сервере
разрешается в `/opt/admin/dist/` — НЕ `/opt/territorywars/admin/dist/`.

```bash
# Собрать локально
cd admin && npm run build

# Загрузить на сервер
scp -i "C:/Users/Илья/Downloads/runterritory-api.pem" -o StrictHostKeyChecking=no \
  -r admin/dist/. root@93.183.74.141:/opt/admin/dist/
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

## Известные баги (актуально на 2026-04-25)

| ID | Файл | Описание | Статус |
|----|------|----------|--------|
| BUG-06 | `ClanViewModel.kt` | Двойной `response.body()!!` — риск NPE | ✅ Исправлен: `body()` сохраняется в локальную переменную, остальные заменены на `.orEmpty()` |
| BUG-07 | `ClanScreen.kt` | Несовместимые единицы площади: Клан показывал `га`, Profile/Leaderboard — `км²` | ✅ Исправлен: `formatArea` в ClanScreen приведён к `км²` |
| BUG-08 | `TokenAuthenticator.kt` | Создаёт новый `Retrofit`/`OkHttp` на каждый 401 — утечка ресурсов | ✅ Исправлен: `authApi` вынесен в `lazy` свойство, создаётся один раз |
| BUG-09 | `ClanScreen.kt` | Показывает `members.size` вместо `clan.membersCount` — расхождение после кика | ✅ Исправлен: заменено на `clan.membersCount` |

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
- [x] **Онбординг** — 4 слайда (HorizontalPager) после регистрации: Welcome, Захват, Кланы, Старт; кнопки Далее/Начать/Пропустить
- [x] **Удаление аккаунта** — в ProfileScreen, с подтверждением (AlertDialog); если пользователь лидер — клан и все его данные удаляются; токены очищаются, редирект на Login
- [x] **Admin дашборд** — карточки: Аккаунтов, Активны сегодня (DAU), Ошибок сегодня, Территорий, Кланов; мини-гистограммы за 7 дней для DAU и ошибок
- [x] **Цвет полигонов клана** — `YandexMapView` использует `clanColor ?: ownerColor` (вместо условия `isOwn`)
- [x] **Метки на полигонах** — для территорий >= 500 м² в центроиде отображается bitmap-метка: ник (жирный 13sp) + [тег клана] (11sp) под ним; белый текст с чёрным контуром; API теперь возвращает `clan_tag` во всех territory-эндпоинтах
- [x] **Поглощение территорий** — игрок может обойти чужую территорию снаружи не заходя в неё; `ST_Intersects` находит её как пересечение, `ST_Difference` возвращает пустую геометрию → полный захват (DELETE); работает через существующую capture-логику без дополнительного кода
- [x] **`clan_tag` в territory API** — добавлен `c.tag AS clan_tag` во все 6 SQL-запросов territories; `Territory.kt`, `TerritoryDto.kt`, `SocketManager.kt` обновлены; `TerritoryDao.kt` исправлен (был баг: отсутствовало поле при конструировании объекта)
- [x] **Документация в Confluence** — создано ~30 страниц: Продукт, Архитектура, API Reference, Android, Инфраструктура, Долг; 5 BPMN/flowchart диаграмм в draw.io; реальные скриншоты всех экранов загружены с описанием каждой кнопки
- [x] **Профиль: цвет + маркер** — на экране профиля добавлен выбор цвета территорий (12 цветов) и маркера на карте (Точка/Стрелка/Звезда/Ромб)
- [x] **Система достижений (сервер)** — таблицы `achievements` (20 достижений) и `user_achievements` в БД; `AchievementService.ts` проверяет и выдаёт достижения после каждого захвата; эндпоинты `GET /api/achievements` и `GET /api/achievements/me`
- [x] **Система достижений (Android)** — `AchievementsScreen.kt`: карточка прогресса (очки + прогресс-бар), секция «Выполненные» (цветные) + «Доступные» (серые, по категориям); новая вкладка в ProfileScreen

---

## Тестирование (актуально на 2026-04-26)

### Smoke-тесты (6/6 ✅)
| # | Тест | Результат |
|---|------|-----------|
| 1 | GET /health | 200 ✅ |
| 2 | GET /api/auth/check-username | 200 ✅ |
| 3 | GET /api/cities | 200 ✅ |
| 4 | GET /api/achievements без авторизации | 401 ✅ |
| 5 | GET /api/territories без авторизации | 401 ✅ |
| 6 | GET /api/nonexistent | 404 ✅ |

### Функциональные тесты (14/15 ✅, 1 pre-existing)
Все ключевые эндпоинты работают корректно. Единственный несоответствие: `GET /api/clans` не имеет обработчика списка (pre-existing bug — список кланов доступен через `/api/leaderboard/clans`).

### Интеграционные тесты (14/14 ✅)
| Flow | Описание | Результат |
|------|----------|-----------|
| 1 | Login → Profile | ✅ |
| 2 | Token Refresh | ✅ |
| 3 | Achievements pipeline (20 штук загружены) | ✅ |
| 4 | Capture validation (400 на некорректные данные) | ✅ |
| 5 | Leaderboard players/clans | ✅ |
| 6 | Security (401 на невалидный/отсутствующий токен) | ✅ |

### Известный pre-existing баг
`GET /api/clans` возвращает 404 — у clansRouter нет обработчика `GET /`. Список кланов доступен через `/api/leaderboard/clans`.

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

## Незавершённые задачи

| # | Фича | Статус | Затронутые файлы |
|---|------|--------|-----------------|
| 1 | **Цвет полигонов клана** — полигоны клана перекрашиваются в цвет клана | ✅ | `YandexMapView.kt` |
| 2 | **Перекраска при вступлении** — реал-тайм смена цвета на карте при вступлении в клан | ⏳ | `YandexMapView.kt`, `SocketManager.kt` |

---

## GitHub репозиторий

```
https://github.com/Kembri90-code/TerritoryWars5
Ветка: main
```

---

## Правило: синхронизация с Confluence

**Если фича или изменение описаны в Confluence — обновляй документацию там же.**

Confluence: `https://bergamicadorette.atlassian.net/wiki/home`

| Что изменилось | Какую страницу обновить |
|---|---|
| Новый API эндпоинт | API Reference → соответствующий раздел |
| Изменение экрана / новая кнопка | Продукт → Экраны → нужный экран |
| Новая бизнес-логика / фича | Продукт → Бизнес-логика фич |
| Изменение схемы БД | Архитектура → База данных |
| Изменение деплоя / инфраструктуры | Инфраструктура → соответствующий раздел |
| Новый технический долг / баг | Технический долг и баги |

**Правило**: не считай фичу завершённой пока README me.md и Confluence не обновлены.

---

## Правило: версионирование в GitHub (для отката)

После каждого значимого изменения создавай тег версии в GitHub. Это позволяет откатиться при багах или негативных отзывах.

### Как создать тег версии

```bash
# 1. Убедись что все изменения закоммичены
git add .
git commit -m "feat: описание что сделано"

# 2. Создай тег с номером версии
git tag -a v1.2.0 -m "Добавлены метки на полигонах, clan_tag в API"

# 3. Запушь тег на GitHub
git push origin main
git push origin v1.2.0
```

### Схема нумерации версий

```
v МАЖОР . МИНОР . ПАТЧ
  │        │       └─ Исправление бага (v1.2.1)
  │        └───────── Новая фича (v1.3.0)
  └────────────────── Крупное изменение архитектуры (v2.0.0)
```

### Как откатиться на предыдущую версию

```bash
# Посмотреть все теги
git tag -l

# Откатить код к тегу
git checkout v1.1.0

# Или откатить и сделать новую ветку для хотфикса
git checkout -b hotfix/rollback v1.1.0
```

### Текущие версии

| Версия | Дата | Что включено |
|--------|------|-------------|
| v1.0.0 | 2026-04-20 | Базовый захват, кланы, рейтинг |
| v1.1.0 | 2026-04-23 | Аватарки, push-уведомления, онбординг |
| v1.2.0 | 2026-04-25 | Метки на полигонах, clan_tag, удаление аккаунта, admin дашборд |

**После каждой новой фичи добавляй строку в эту таблицу.**
