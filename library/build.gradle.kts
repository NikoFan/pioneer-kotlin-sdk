plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish") // Публикация в JitPack
}

android {
    namespace = "io.github.NikoFan.pioneer"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Подключение ndk для работы с дроном:
         ndk {
             abiFilters += listOf("arm64-v8a")
         }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

     // Подключение Cmake
     externalNativeBuild {
         cmake {
             path = file("src/main/cpp/CMakeLists.txt")
             version = "3.22.1"
         }
     }
}
dependencies {
    // ✅ ОСТАВЛЯЕМ ТОЛЬКО ЭТО:
    // implementation("io.dronefleet.mavlink:mavlink:1.1.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")



    // Тесты — можно оставить (опционально):
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// === ПУБЛИКАЦИЯ ДЛЯ JITPACK ===
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.NikoFan"       // ← GitHub username
                artifactId = "pioneer-kotlin-sdk"       // ← имя репозитория
                // version = "1.1.5"                   // ← версия (лучше указывать в теге)

                from(components["release"])
            }
        }
    }
}