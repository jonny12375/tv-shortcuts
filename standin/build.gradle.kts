import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreProperties = rootProject.extra["keystoreProperties"] as java.util.Properties
val hasKeystore = keystoreProperties.containsKey("storeFile")
val standinVersion = (project.findProperty("standinVersion") as String).toInt()
if (!hasKeystore) logger.warn("keystore.properties not found: release builds of ${project.name} are signed with the DEBUG key")

// One tiny APK per remote-control button, generated from config/buttons.json.
// Each flavor claims the package name Sony's firmware expects and forwards the
// key press to whatever app is configured in the hub.
@Suppress("UNCHECKED_CAST")
val buttons = JsonSlurper().parse(rootProject.file("config/buttons.json")) as List<Map<String, Any?>>

android {
    namespace = "dev.jonathanclark.tvshortcuts.standin"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
        manifestPlaceholders["standinVersion"] = standinVersion
    }

    // Release builds use keystore.properties; without it they fall back to the debug key so that
    // assembleRelease still works for contributors (those builds are not real releases).
    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(if (hasKeystore) "release" else "debug")
        }
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
        for (b in buttons) {
            create(b["id"] as String) {
                dimension = "button"
                applicationId = b["package"] as String
                buildConfigField("String", "BUTTON_ID", "\"${b["id"]}\"")
                resValue("string", "app_name", "${b["label"]} shortcut")
                // System apps ship with a high versionCode; match it so the install is
                // refused for the real reason (signature) instead of "downgrade".
                (b["versionCode"] as? Number)?.let { versionCode = it.toInt() }
            }
        }
    }
}

// Some buttons launch their app with an implicit intent carrying a specific action
// (e.g. Netflix). Declare those actions on LaunchActivity via a generated manifest.
for (b in buttons) {
    @Suppress("UNCHECKED_CAST")
    val actions = (b["actions"] as? List<String>) ?: continue
    val manifest = layout.buildDirectory.file("generated/standin-manifests/${b["id"]}/AndroidManifest.xml").get().asFile
    manifest.parentFile.mkdirs()
    manifest.writeText(
        buildString {
            appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            appendLine("""<manifest xmlns:android="http://schemas.android.com/apk/res/android">""")
            appendLine("""    <application>""")
            appendLine("""        <activity android:name="dev.jonathanclark.tvshortcuts.standin.LaunchActivity" android:exported="true">""")
            appendLine("""            <intent-filter>""")
            actions.forEach { appendLine("""                <action android:name="$it" />""") }
            appendLine("""                <category android:name="android.intent.category.DEFAULT" />""")
            appendLine("""            </intent-filter>""")
            appendLine("""        </activity>""")
            appendLine("""    </application>""")
            appendLine("""</manifest>""")
        }
    )
    android.sourceSets.getByName(b["id"] as String).manifest.srcFile(manifest)
}
