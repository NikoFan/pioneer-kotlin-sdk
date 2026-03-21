# 🚁 Pioneer Kotlin SDK

[![JitPack](https://jitpack.io/v/NikoFan/pioneer-kotlin-sdk.svg)](https://jitpack.io/#NikoFan/pioneer-kotlin-sdk)
[![Platform](https://img.shields.io/badge/platform-Android-blue.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

> Kotlin SDK для управления дронами GEOSCAN Pioneer с поддержкой нативного C++ и MAVLink.
>
> ---

## 📋 Особенности

- 🔄 Асинхронный API на корутинах
- 🧱 Native C++ интеграция через NDK (arm64-v8a)
- 📡 Поддержка MAVLink-протокола
- 📦 Готовый пример использования методов в модуле `example/`
- 🛠 Полная совместимость с Jetpack Compose

---

## 🚀 Подключение

### 1. Добавьте JitPack в `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

### 2. Добавьте зависимости в `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.NikoFan:pioneer-kotlin-sdk:1.1.7")
}


### Быстрый старт
val pioneer = Pioneer(
  ip = "192.168.0.1",
  mavlinkPort = 8001,
  logger = true
)

// Пример запуска команды
fun startArmCommand(){
  viewModelScope.launch(Dispatchers.IO) {
    try{
      pioneer.arm() // Запуск команды
    } catch (e: Exception) {
      Log.d("PIONEER-LOGS", pioneer.getLogs().joinToString("\n))
    }
  }
}

---
## Требования
- Android 7.0+ (API 24)
- NDK r27.0.12077973
- CMake 3.22.1+
- Kotlin 1.9+
- Java 11

---
Copyright © 2026 Oleg Nestruev  
Распространяется под лицензией [Apache License 2.0](LICENSE).

