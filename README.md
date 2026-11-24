# RemotePad

Курсова робота з дисципліни «Розробка програмного забезпечення мобільних пристроїв», група ТВ-31, НТУУ «КПІ ім. Ігоря Сікорського».

RemotePad перетворює Android-смартфон на бездротові тачпад, клавіатуру і пульт для презентацій. Телефон і ПК мають бути в одній локальній мережі.

- Решетніков Ілля Володимирович: клієнтська частина, модуль `mobile-client`.
- Фельчин Микола Борисович: серверна частина, модуль `pc-server`.

## Структура

```text
shared-protocol/   спільні моделі пакетів, коди клавіш, кодування JSON
mobile-client/     Android-застосунок (Kotlin, Jetpack Compose, Room, DataStore, OkHttp)
pc-server/         сервер для ПК (Kotlin, Ktor WebSocket, UDP-пошук, java.awt.Robot)
docs/              звіти до пунктів 1-4 і діаграми PlantUML (docs/diagrams)
```

Докладніше про будову системи: [ARCHITECTURE.md](ARCHITECTURE.md).

## Запуск

Потрібен JDK 17. Для клієнта ще Android SDK (найпростіше відкрити корінь репозиторію в Android Studio).

Сервер на ПК:

```bash
./gradlew :pc-server:run                   # порт 8080, випадковий PIN
./gradlew :pc-server:run --args="8080 4812"  # свій порт і PIN
```

Сервер виводить свої адреси в локальній мережі та PIN.

Клієнт:

```bash
./gradlew :mobile-client:installDebug   # на підключений телефон
./gradlew :mobile-client:assembleDebug  # лише APK у mobile-client/build/outputs/apk/debug
```

Після запуску застосунок сам шукає сервер у мережі. Якщо пошук заблоковано брандмауером, IP-адресу можна ввести вручну.

## Тести

```bash
./gradlew test
```

Юніт-тести є в усіх трьох модулях. У `mobile-client` тест `ScreenshotTest` рендерить екрани через Paparazzi без емулятора. GitHub Actions на кожен push запускає тести, збирає APK і сервер та рендерить діаграми з `docs/diagrams`.
