# RemotePad — Система використання смартфона як периферійного пристрою введення для ПК

Даний репозиторій містить монорепозиторій проєкту курсової роботи студентів групи ТВ-31 кафедри ІПЗЕ НТУУ «КПІ ім. Ігоря Сікорського»:
- **Решетніков Ілля Володимирович** — Клієнтська частина (Android / Jetpack Compose / Gestures / UI-UX).
- **Фельчин Микола Борисович** — Серверна частина (Desktop Daemon / Kotlin Ktor / AWT Robot HID / Security).

---

## 1. Архітектура та структура репозиторію

```text
├── shared-protocol/     # Спільний модуль моделей даних, констант та протоколу серіалізації
│   └── src/main/kotlin/com/remotepad/protocol/
│       └── ProtocolModels.kt
├── mobile-client/       # Клієнтський Android-застосунок (Kotlin, Jetpack Compose, Coroutines)
│   └── src/main/kotlin/com/remotepad/client/
│       └── TouchpadScreen.kt
├── pc-server/           # Серверний сервіс для ПК (Kotlin, Ktor, WebSockets/UDP, java.awt.Robot)
│   └── src/main/kotlin/com/remotepad/server/
│       └── RemotePadServer.kt
├── docs/                # Звіти та пояснювальні записки до курсової роботи (ДСТУ 3008:2015)
│   ├── CW_1-4_ТВ-31_РешетніковІВ.docx
│   ├── CW_1-4_ТВ-31_ФельчинМБ.docx
│   ├── CW_1-4_ТВ-31_РешетніковІВ.zip
│   └── CW_1-4_ТВ-31_ФельчинМБ.zip
├── .gitignore
└── README.md
```

---

## 2. Технологічний стек

* **Спільний протокол (`shared-protocol`):** Kotlin Multiplatform ready, `kotlinx.serialization` (JSON / CBOR бінарні пакети), енуми типів подій та кодів дій.
* **Мобільний клієнт (`mobile-client`):** Android 8.0+ (API 26+), Jetpack Compose, Kotlin Coroutines & Flow, Ktor HTTP/WebSocket Client, Android Sensors & Multi-touch Gestures (`PointerInputScope`).
* **ПК-сервер (`pc-server`):** Kotlin JVM 17+, Ktor Asynchronous Server Engine (CIO / Netty), WebSockets / Datagram UDP Sockets, JNA / `java.awt.Robot` для низькорівневої емуляції системного введення (Mouse & Keyboard HID).

---

## 3. Особливості реалізації

1. **Мінімальна затримка (Low Latency):** Використання неблокуючих сокетів та оптимізованого бінарного/компактного JSON-формату для обробки сенсорних пакетів із затримкою < 10 мс у локальній мережі Wi-Fi.
2. **Алгоритм згладжування курсора (Cursor Smoothing):** Сервер реалізує експоненційне згладжування (EMA / Exponential Moving Average) для демпфування тремтіння пальця при мікропереміщеннях.
3. **Безпека підключення:** Двоетапна автентифікація через PIN-код або QR-код з обмеженням кількості сесій та перевіркою валідності IP-адрес клієнтів.
4. **Багатофункціональність:** Режими тачпада (1 палець — переміщення/клік, 2 пальці — скролінг/правий клік, 3 пальці — перемикання вікон), повнорозмірної клавіатури, медіа-пульта та програмованої макро-панелі.

---

## 4. Збірка та запуск

### Запуск сервера на ПК:
```bash
cd pc-server
./gradlew run
```
Сервер автоматично виводить IP-адресу в LAN, порт (за замовчуванням `8080`) та згенерований 4-значний PIN-код для підключення.

### Збірка мобільного клієнта:
```bash
cd mobile-client
./gradlew assembleDebug
```
Встановіть отриманий `.apk` файл на мобільний пристрій або запустіть безпосередньо через Android Studio.
