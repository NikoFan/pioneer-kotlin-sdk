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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.NikoFan"
                artifactId = "pioneer-kotlin-sdk"

                from(components["release"])

                pom {
                    name.set("Pioneer Kotlin SDK")
                    description.set("Kotlin SDK for GEOSCAN Pioneer Mini drone")
                    url.set("https://github.com/NikoFan/pioneer-kotlin-sdk")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("NikoFan")
                            name.set("Oleg Nestruev")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com:NikoFan/pioneer-kotlin-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com:NikoFan/pioneer-kotlin-sdk.git")
                        url.set("https://github.com/NikoFan/pioneer-kotlin-sdk")
                    }
                }
            }
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