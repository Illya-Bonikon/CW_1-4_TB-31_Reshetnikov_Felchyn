# Архітектура RemotePad

## Ідея

Смартфон надсилає події введення (рух курсора, кліки, прокрутку, текст, клавіші), а ПК їх виконує через `java.awt.Robot`. Обидві частини працюють в одній мережі Wi-Fi, тож хмарний посередник не потрібен і затримка мінімальна.

## Обмін даними

1. Пошук, два способи паралельно: широкомовний UDP-рядок `REMOTEPAD_DISCOVER` на порт 8080 (відповідь `REMOTEPAD_HOST;<ім'я>;<порт>`) і mDNS/DNS-SD: сервер оголошує `_remotepad._tcp` через JmDNS, телефон шукає через Android `NsdManager`. Результати зводяться в `CompositeHostDiscovery`.
2. Авторизація: клієнт відкриває WebSocket `ws://<IP>:8080/control` і шле `AUTH_REQUEST` з PIN. Сервер повертає `AUTH_RESPONSE` з випадковим токеном. Після п'яти хибних PIN з'єднання закривається.
3. Керування: кожен пакет `InputEventPacket` (JSON) містить токен; без чинного токена сервер нічого не виконує.
4. Контроль зв'язку: раз на 2 с клієнт шле `PING` зі своїм часом, сервер повертає його в `PONG`, клієнт рахує RTT.
5. Сповіщення: клієнт додає до `AUTH_REQUEST` токен FCM. Якщо з'єднання обірвалося без пакета `DISCONNECT` або сервер зупиняється, сервер надсилає push через Firebase Admin SDK. Ключі Firebase зберігаються поза репозиторієм (див. README).

Формат пакетів описано один раз у `shared-protocol`, його підключають обидва модулі.

## Клієнт (`mobile-client`)

```text
ui/        екрани Compose і ViewModel (connect, control, settings, theme)
input/     GestureTranslator, MoveAccumulator, KeyChordParser, PinValidator
network/   RemotePadConnection, WebSocketConnection, HostDiscovery, RttTracker
data/      репозиторії (ConnectionHistoryRepository, MacroRepository, SettingsRepository)
data/local Room: сутності, DAO з повним CRUD, Migrations (версія схеми 2)
push/      токен FCM, прийом і показ сповіщень
AppContainer.kt   ручне створення залежностей
```

- Екрани поділено на обгортку з ViewModel і функцію `...Content` без стану. Друга використовується в `ScreenshotTest`.
- Рух пальця не відправляється одразу: `MoveAccumulator` підсумовує зміщення, а ViewModel раз на 8 мс шле один пакет. Після розриву зв'язку буфер очищується, щоб курсор не стрибнув.
- `WebSocketConnection` після несподіваного розриву до трьох разів пробує підключитися з тими самими даними.
- Класи з `input/` і `RttTracker` не залежать від Android і покриті юніт-тестами.

## База даних

Room, файл `remotepad.db`, версія 2. Схема кожної версії експортується в `mobile-client/schemas` (Gradle-плагін Room) і комітиться. Кожна зміна схеми отримує явну міграцію в `Migrations.ALL`; `fallbackToDestructiveMigration` не використовується. `MigrationTest` (Robolectric) перевіряє перехід 1 → 2 на справжній схемі версії 1.

## Сервер (`pc-server`)

```text
auth/    PinAuthenticator (порівняння за сталий час), SessionRegistry (UUID-токени, ліміт пристроїв)
input/   InputDevice, AwtRobotInputDevice, CursorSmoother (EMA зі збереженням дробового залишку)
net/     ControlServer (Ktor), PacketHandler (логіка без мережі), DiscoveryResponder (UDP)
discovery/ MdnsAdvertiser (JmDNS)
push/    PushNotifier, FcmPushNotifier (Firebase Admin SDK), NoopPushNotifier
```

`PacketHandler` не знає про мережу і Robot, тому перевіряється тестами з `FakeInputDevice`.

## Діаграми

Вихідні файли PlantUML лежать у `docs/diagrams`: варіанти використання, компоненти, послідовність, класи клієнта, ER-модель. PNG рендерить CI (job `diagrams`, артефакт `diagrams`).

## Запуск і перевірка

Див. [README.md](README.md). CI (`.github/workflows/build.yml`) запускає `./gradlew test :pc-server:installDist :mobile-client:assembleDebug` і публікує APK та звіти тестів як артефакти.

## Тести

- юніт-тести в усіх трьох модулях;
- `ScreenshotTest` (Paparazzi): рендер усіх екранів, у тому числі в альбомній орієнтації;
- `LoopbackLatencyTest`: справжній `WebSocketConnection` проти справжнього `ControlServer` на 127.0.0.1. Тест перевіряє авторизацію і доставку подій, а також міряє затримку та пропускну здатність і пише результат у `mobile-client/build/measurements/loopback.json`;
- покриття коду клієнта: `./gradlew :mobile-client:createDebugUnitTestCoverageReport` (JaCoCo).

## Результати

Застосунок збирається в APK, сервер у дистрибутив `pc-server/build/install`. Усі тести проходять у GitHub Actions; заміри затримки, покриття і знімки екранів публікуються як артефакти `measurements` і `screenshots`. Заміри на loopback показують лише програмну частину затримки, час мережі Wi-Fi до них не входить.
