plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// One tiny APK per remote-control button. Each flavor pretends to be the app
// Sony's firmware expects to find, and forwards the key press to whatever app
// is configured in the hub.
android {
    namespace = "dev.jonathanclark.tvshortcuts.standin"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    flavorDimensions += "button"
    productFlavors {
        // scancode 491
        create("disney") {
            dimension = "button"
            applicationId = "com.disney.disneyplus"
            buildConfigField("String", "BUTTON_ID", "\"disney\"")
            resValue("string", "app_name", "Disney+ shortcut")
        }
        // scancode 507
        create("crunchyroll") {
            dimension = "button"
            applicationId = "com.crunchyroll.crunchyroid"
            buildConfigField("String", "BUTTON_ID", "\"crunchyroll\"")
            resValue("string", "app_name", "Crunchyroll shortcut")
        }
        // scancode 490
        create("prime") {
            dimension = "button"
            applicationId = "com.amazon.amazonvideo.livingroom"
            buildConfigField("String", "BUTTON_ID", "\"prime\"")
            // Must exceed the preinstalled system app (606024041) or Android rejects it as a downgrade.
            versionCode = 2000000000
            resValue("string", "app_name", "Prime Video shortcut")
        }
    }
}
