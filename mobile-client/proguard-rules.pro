# Протокол серіалізується через kotlinx.serialization, тому залишаю згенеровані серіалізатори.
-keepclassmembers class com.remotepad.protocol.** { *** Companion; }
-keepclasseswithmembers class com.remotepad.protocol.** { kotlinx.serialization.KSerializer serializer(...); }
