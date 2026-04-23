# Territory Wars — Полное описание проекта

> Документ написан для быстрого ввода в контекст нового чата с ИИ.
> Последнее обновление: 2026-04-23

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
| GET  | `/api/leaderboard` | Рейтинг игроков |
| GET  | `/api/cities` | Список городов |
| GET  | `/api/admin/*` | Административные эндпоинты |
| GET  | `/health` | Health-check |

### Логика захвата территории (`server/src/routes/territories.ts`)

**POST `/api/territories/capture`** — ключевой эндпоинт, принимает массив GPS-точек:

1. **Anti-cheat**: скорость между точками > 12 м/с → отклонение
2. **Замыкание полигона**: добавляет первую точку в конец если не замкнут
3. **Валидация через PostGIS**: `ST_IsValid`, мин. площадь 100 м², макс. 5 км²
4. **Захват чужих территорий** (реализован):
   - Находит все вражеские территории через `ST_Intersects`
   - Для каждой: `ST_Dump(ST_MakeValid(ST_Difference(...)))` — получает оставшиеся куски
   - Фильтрует: только `ST_Polygon` с площадью >= 100 м², берёт наибольший
   - Если кусков нет → **полный захват**: `DELETE`, обновить статистику врага, `emitTerritoryDeleted()`
   - Если есть кусок → **частичный захват**: `UPDATE polygon` врага, обновить статистику, `emitTerritoryUpdate()`
5. **Слияние со своими**: если новый полигон перекрывает свои — `ST_Union` всех + создать новую запись
6. **Создание новой** если нет пересечений
7. **Broadcast**: `emitTerritoryUpdate()` всем клиентам через Socket.IO
8. **Ответ**: `{ success, territory, merged, conquered: число_захваченных, error }`

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
Clan             — id, name, tag, leaderId, color, description, maxMembers,
                   totalAreaM2, territoriesCount
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
| ClanCreate | `clan_create` | Создать клан |
| ClanDetail | `clan_detail/{clanId}` | Детали клана |

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
│   ├── Territory.kt       — id, ownerId, ownerUsername, ownerColor, clanId, clanColor,
│   │                        polygon: List<GeoPoint>, areaM2, perimeterM, capturedAt, updatedAt
│   ├── GeoPoint.kt        — lat, lng
│   ├── RoutePoint.kt      — lat, lng, timestamp, accuracy
│   ├── Clan.kt
│   ├── User.kt
│   └── LeaderboardEntry.kt
├── presentation/
│   ├── map/
│   │   ├── MapScreen.kt       — UI карты, кнопки захвата
│   │   ├── MapViewModel.kt    — вся логика карты
│   │   ├── YandexMapView.kt   — AndroidView обёртка для MapKit
│   │   └── PlayerMarkerIcon.kt
│   ├── auth/ (Login, Register + ViewModels)
│   ├── profile/
│   ├── clan/
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

### MapViewModel — ключевые константы

```kotlin
private const val CLOSE_RADIUS_M = 15.0          // радиус "закрытия" маршрута
private const val MIN_POINTS = 20                 // минимум GPS-точек для захвата
private const val BBOX_DEBOUNCE_MS = 500L         // дебаунс загрузки территорий
private const val GPS_INTERVAL_CAPTURING_MS = 2000L
private const val GPS_INTERVAL_IDLE_MS = 5000L
```

### Процесс захвата (клиент)

1. `startCapture()` — переключает GPS на 2 сек интервал, запускает `GpsTrackingService`
2. `onNewLocation()` — при точности > 30м пропускает точку; накапливает `routePoints`
3. `canFinishCapture = routePoints.size >= MIN_POINTS`
4. `finishCapture()` — отправляет `POST /territories/capture { route_points: [...] }`
5. Ответ парсится: `conquered`, `merged` → показывается уведомление

### SocketManager — real-time обновления

```kotlin
// Singleton, инжектируется в MapViewModel
class SocketManager @Inject constructor(private val tokenDataStore: TokenDataStore) {
    val territoryEvents: SharedFlow<TerritoryEvent>  // Updated | Deleted
    fun connect() { /* подключается с JWT, слушает territory_updated / territory_deleted */ }
    fun disconnect()
}

sealed class TerritoryEvent {
    data class Updated(val territory: Territory) : TerritoryEvent()
    data class Deleted(val id: String) : TerritoryEvent()
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

### Nginx конфиг (ключевое)
- `/api/*` → `proxy_pass http://tw_api:3000`
- `/socket.io/*` → proxy + WebSocket upgrade headers
- `/uploads/*` → статика
- `/admin/*` → статика из `admin/dist/`
- `/` → 404 или редирект

---

## Сборка APK

Открыть в **Android Studio**, меню **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
APK окажется в `app/build/outputs/apk/debug/app-debug.apk`.
Gradle: `./gradlew assembleDebug` из папки `TerritoryWars/`.

---

## Известные баги (на 2026-04-23)

| ID | Файл | Описание |
|----|------|----------|
| BUG-06 | `ClanViewModel.kt:168-180` | Двойной `response.body()!!` — риск NPE |
| BUG-07 | Profile/Leaderboard vs Clan | Несовместимые единицы площади (км² vs га) |
| BUG-08 | `TokenAuthenticator.kt` | Создаёт новый Retrofit/OkHttp на каждый 401 |
| BUG-09 | `ClanScreen.kt:321` | Показывает `members.size` вместо `clan.membersCount` |

---

## Реализованные фичи (статус на 2026-04-23)

- [x] Регистрация / вход / обновление токена
- [x] GPS-захват территорий (обход по периметру)
- [x] PostGIS полигоны, валидация площади
- [x] Anti-cheat по скорости GPS
- [x] Слияние (merge) своих перекрывающихся территорий
- [x] **Захват чужих территорий** — полный и частичный (ST_Difference + ST_Dump)
- [x] Real-time обновления через Socket.IO (territory_updated / territory_deleted)
- [x] SocketManager на клиенте — SharedFlow событий
- [x] Клановая система (создание, вступление, выход, исключение, удаление клана лидером)
- [x] Заявки на вступление в клан: игрок → запрос → FCM push лидеру → лидер принимает/отклоняет в экране Клан
- [x] Бейдж на вкладке «Клан» (BadgedBox) — показывает количество непрочитанных заявок
- [x] Принятие заявки объединяет территории вступившего с территориями клана (UPDATE territories SET clan_id)
- [x] Рейтинг игроков
- [x] Профиль с аватаром, цветом, статистикой
- [x] GpsTrackingService (foreground service)
- [x] Firebase push-уведомления (FCM)
- [x] Административная веб-панель

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

### TokenDataStore (DataStore Preferences)
```kotlin
val accessToken: Flow<String?>
val refreshToken: Flow<String?>
val playerMarker: Flow<Int>    // id маркера игрока на карте
```

### Аутентификация HTTP (Android)
- `AuthInterceptor` добавляет `Authorization: Bearer <accessToken>` к каждому запросу
- `TokenAuthenticator` — при 401 вызывает `/auth/refresh`, получает новый access token

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

Response:
{
  "success": true,
  "territory": { "id": "...", "polygon": [[lng,lat],...], "area_m2": 3500, ... },
  "merged": false,
  "conquered": 1,
  "error": null
}
```
