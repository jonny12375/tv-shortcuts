import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

@Suppress("UNCHECKED_CAST")
val buttons = JsonSlurper().parse(rootProject.file("config/buttons.json")) as List<Map<String, Any?>>

android {
    namespace = "dev.jonathanclark.tvshortcuts.hub"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.jonathanclark.tvshortcuts.hub"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets.getByName("main").assets.apply {
        // The hub reads the same button list the stand-in flavors are generated from...
        srcDir(rootProject.file("config"))
        // ...and carries the stand-in APKs so it can install them itself.
        srcDir(layout.buildDirectory.dir("generated/standins"))
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.activity:activity-compose:1.9.3")
}

// Copy every built stand-in APK to assets/standins/<buttonId>.apk.
val bundleStandIns = tasks.register<Copy>("bundleStandIns") {
    dependsOn(":standin:assembleDebug")
    from(rootProject.layout.projectDirectory.dir("standin/build/outputs/apk")) {
        include("*/debug/*.apk")
        eachFile {
            val id = name.removePrefix("standin-").removeSuffix("-debug.apk")
            path = "standins/$id.apk"
        }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("generated/standins"))
}
tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Assets")) dependsOn(bundleStandIns)
}

// Android 11+ package visibility: the hub must declare every package name it inspects.
val queriesManifest = layout.buildDirectory.file("generated/hub-queries/AndroidManifest.xml").get().asFile
queriesManifest.parentFile.mkdirs()
queriesManifest.writeText(
    buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<manifest xmlns:android="http://schemas.android.com/apk/res/android">""")
        appendLine("""    <queries>""")
        buttons.map { it["package"] as String }.distinct().forEach {
            appendLine("""        <package android:name="$it" />""")
        }
        appendLine("""    </queries>""")
        appendLine("""</manifest>""")
    }
)
for (type in listOf("debug", "release")) {
    android.sourceSets.getByName(type).manifest.srcFile(queriesManifest)
}
