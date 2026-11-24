# Архітектура RemotePad

## Ідея

Смартфон надсилає події введення (рух курсора, кліки, прокрутку, текст, клавіші), а ПК їх виконує через `java.awt.Robot`. Обидві частини працюють в одній мережі Wi-Fi, тож хмарний посередник не потрібен і затримка мінімальна.

## Обмін даними

1. Пошук: клієнт шле широкомовний UDP-рядок `REMOTEPAD_DISCOVER` на порт 8080, сервер відповідає `REMOTEPAD_HOST;<ім'я>;<порт>`.
2. Авторизація: клієнт відкриває WebSocket `ws://<IP>:8080/control` і шле `AUTH_REQUEST` з PIN. Сервер повертає `AUTH_RESPONSE` з випадковим токеном. Після п'яти хибних PIN з'єднання закривається.
3. Керування: кожен пакет `InputEventPacket` (JSON) містить токен; без чинного токена сервер нічого не виконує.
4. Контроль зв'язку: раз на 2 с клієнт шле `PING` зі своїм часом, сервер повертає його в `PONG`, клієнт рахує RTT.

Формат пакетів описано один раз у `shared-protocol`, його підключають обидва модулі.

## Клієнт (`mobile-client`)

```text
ui/        екрани Compose і ViewModel (connect, control, settings, theme)
input/     GestureTranslator, MoveAccumulator, KeyChordParser, PinValidator
network/   RemotePadConnection, WebSocketConnection, HostDiscovery, RttTracker
data/      Room (HostPCProfile, DeviceSession, MacroPreset) і DataStore (UserPreference)
AppContainer.kt   ручне створення залежностей
```

- Екрани поділено на обгортку з ViewModel і функцію `...Content` без стану. Друга використовується в `ScreenshotTest`.
- Рух пальця не відправляється одразу: `MoveAccumulator` підсумовує зміщення, а ViewModel раз на 8 мс шле один пакет. Після розриву зв'язку буфер очищується, щоб курсор не стрибнув.
- `WebSocketConnection` після несподіваного розриву до трьох разів пробує підключитися з тими самими даними.
- Класи з `input/` і `RttTracker` не залежать від Android і покриті юніт-тестами.

## Сервер (`pc-server`)

```text
auth/    PinAuthenticator (порівняння за сталий час), SessionRegistry (UUID-токени, ліміт пристроїв)
input/   InputDevice, AwtRobotInputDevice, CursorSmoother (EMA зі збереженням дробового залишку)
net/     ControlServer (Ktor), PacketHandler (логіка без мережі), DiscoveryResponder (UDP)
```

`PacketHandler` не знає про мережу і Robot, тому перевіряється тестами з `FakeInputDevice`.

## Діаграми

Вихідні файли PlantUML лежать у `docs/diagrams`: варіанти використання, компоненти, послідовність, класи клієнта, ER-модель. PNG рендерить CI (job `diagrams`, артефакт `diagrams`).

## Запуск і перевірка

Див. [README.md](README.md). CI (`.github/workflows/build.yml`) запускає `./gradlew test :pc-server:installDist :mobile-client:assembleDebug` і публікує APK та звіти тестів як артефакти.

## Результати

Застосунок збирається в APK, сервер у дистрибутив `pc-server/build/install`. Усі юніт-тести трьох модулів і тести знімків екранів проходять у GitHub Actions. Затримку на реальній мережі ще треба виміряти, це заплановано на наступні пункти курсової.
