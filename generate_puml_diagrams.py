# -*- coding: utf-8 -*-
"""
Генерація діаграм у форматі PlantUML (.puml) та рендеринг у PNG
у єдиній колірній гамі (монохромний інженерний стиль) з "чоловічками" на Use-Case.
"""

import zlib
import urllib.request
import os

def plantuml_encode(plantuml_text):
    utf8_bytes = plantuml_text.encode('utf-8')
    compressed = zlib.compress(utf8_bytes)[2:-4]
    mapping = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_'
    
    def encode_3bytes(b1, b2, b3):
        c1 = b1 >> 2
        c2 = ((b1 & 0x3) << 4) | (b2 >> 4)
        c3 = ((b2 & 0xF) << 2) | (b3 >> 6)
        c4 = b3 & 0x3F
        return mapping[c1] + mapping[c2] + mapping[c3] + mapping[c4]
    
    res = []
    i = 0
    while i < len(compressed):
        b1 = compressed[i]
        b2 = compressed[i+1] if i+1 < len(compressed) else 0
        b3 = compressed[i+2] if i+2 < len(compressed) else 0
        res.append(encode_3bytes(b1, b2, b3))
        i += 3
    return ''.join(res)

def render_puml_to_png(puml_path, png_path):
    with open(puml_path, 'r', encoding='utf-8') as f:
        text = f.read()
    code_enc = plantuml_encode(text)
    url = f'http://www.plantuml.com/plantuml/png/~1{code_enc}'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    with urllib.request.urlopen(req, timeout=15) as resp:
        data = resp.read()
        with open(png_path, 'wb') as out_f:
            out_f.write(data)
    print(f'Rendered: {png_path} ({len(data)} bytes)')

# 1. Use-Case Client (Решетніков)
PUML_USECASE_CLIENT = """@startuml
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Times New Roman"
skinparam defaultFontSize 12
skinparam packageStyle rectangle
skinparam roundCorner 8

left to right direction

:Користувач смартфона: as User
:Серверний процес ПК: as Server

rectangle "Система RemotePad (Мобільний клієнт)" {
  (UC-1: Пошук ПК та авторизація за PIN) as UC1
  (UC-2: Жестове керування курсором) as UC2
  (UC-3: Екранне введення тексту та гарячі клавіші) as UC3
  (UC-4: Виконання макросів та медіа-дій) as UC4
  (UC-5: Моніторинг RTT та автоперепідключення) as UC5
}

User --> UC1
User --> UC2
User --> UC3
User --> UC4
User --> UC5

UC1 --> Server
UC2 --> Server
UC3 --> Server
UC4 --> Server
UC5 --> Server
@enduml
"""

# 2. Use-Case Server (Фельчин)
PUML_USECASE_SERVER = """@startuml
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Times New Roman"
skinparam defaultFontSize 12
skinparam packageStyle rectangle
skinparam roundCorner 8

left to right direction

:Клієнтський Android-процес: as ClientApp
:Оператор робочої станції ПК: as Admin
:Ядро ОС ПК (AWT Robot / HID): as OSHid

rectangle "Серверний демон RemotePad (Kotlin Ktor)" {
  (UC-S1: Верифікація PIN та генерація токена) as UCS1
  (UC-S2: Фільтрація дельт та згладжування курсора) as UCS2
  (UC-S3: Декодування скан-кодів та введення тексту) as UCS3
  (UC-S4: Диспетчеризація медіа-дій та макросів) as UCS4
  (UC-S5: Моніторинг активних сесій та Heartbeat) as UCS5
}

Admin --> UCS1 : конфігурація PIN
ClientApp --> UCS1
ClientApp --> UCS2
ClientApp --> UCS3
ClientApp --> UCS4
ClientApp --> UCS5

UCS2 --> OSHid
UCS3 --> OSHid
UCS4 --> OSHid
@enduml
"""

# 3. ER Diagram (Shared)
PUML_ER_DIAGRAM = """@startuml
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Times New Roman"
skinparam defaultFontSize 12
skinparam roundCorner 6

entity "HostPCProfile" as Host {
  * id : Long <<PK>>
  --
  * hostName : String
  * ipAddress : String
  * port : Int
  * lastConnected : Long
}

entity "DeviceSession" as Session {
  * sessionToken : String <<PK>>
  --
  * hostId : Long <<FK>>
  * deviceName : String
  * authTime : Long
  * isActive : Boolean
}

entity "UserPreference" as Pref {
  * id : Long <<PK>>
  --
  * sensitivity : Float
  * acceleration : Boolean
  * smoothingFactor : Float
  * reverseScroll : Boolean
}

entity "MacroPreset" as Macro {
  * macroId : String <<PK>>
  --
  * title : String
  * keySequence : String
  * iconType : String
  * category : String
}

Host ||--o{ Session : "ініціює"
Host ||--|| Pref : "визначає"
Host ||--o{ Macro : "містить"
@enduml
"""

os.makedirs('puml', exist_ok=True)
os.makedirs('figures', exist_ok=True)

with open('puml/usecase_client.puml', 'w', encoding='utf-8') as f:
    f.write(PUML_USECASE_CLIENT)

with open('puml/usecase_server.puml', 'w', encoding='utf-8') as f:
    f.write(PUML_USECASE_SERVER)

with open('puml/er_diagram.puml', 'w', encoding='utf-8') as f:
    f.write(PUML_ER_DIAGRAM)

print("PUML files written successfully.")

render_puml_to_png('puml/usecase_client.puml', 'figures/fig1_usecase_client.png')
render_puml_to_png('puml/usecase_server.puml', 'figures/fig1_usecase_server.png')
render_puml_to_png('puml/er_diagram.puml', 'figures/fig4_er_diagram_mono.png')

print("All PlantUML diagrams rendered to PNG successfully.")
