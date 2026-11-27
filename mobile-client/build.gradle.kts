plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    // знімки екранів Compose без емулятора, ними ж ілюстровано звіт
    alias(libs.plugins.paparazzi)
}

// Ключ Firebase (google-services.json) у репозиторій не потрапляє. Якщо файл поклали локально
// або CI відновив його з секрету, вмикаю плагін; без файлу збирається версія без push-сповіщень.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.remotepad.client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.remotepad.client"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            // звіт JaCoCo для юніт-тестів: createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // Robolectric і MigrationTestHelper читають ресурси й схеми бази з assets
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets {
        // MigrationTestHelper читає схеми з assets. Robolectric бачить лише assets варіанта debug,
        // тому схеми (кілька кілобайтів JSON) потрапляють тільки в налагоджувальну збірку
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    // схему БД тримаю в репозиторії: з неї видно зміни між версіями, і на ній перевіряються міграції.
    // Плагін Room, а не аргумент KSP: інакше debug і release одночасно пишуть той самий файл схеми
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":shared-protocol"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    // справжній сервер для наскрізного тесту на loopback
    testImplementation(project(":pc-server"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
}
