import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreProperties = rootProject.extra["keystoreProperties"] as java.util.Properties
val hasKeystore = keystoreProperties.containsKey("storeFile")
val standinVersion = (project.findProperty("standinVersion") as String).toInt()
if (!hasKeystore) logger.warn("keystore.properties not found: release builds of ${project.name} are signed with the DEBUG key")

@Suppress("UNCHECKED_CAST")
val buttons = JsonSlurper().parse(rootProject.file("config/buttons.json")) as List<Map<String, Any?>>

android {
    namespace = "dev.jonathanclark.tvshortcuts.hub"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.jonathanclark.tvshortcuts.hub"
        minSdk = 26
        targetSdk = 34
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
        buildConfigField("int", "STANDIN_VERSION", "$standinVersion")
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
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // The hub reads the same button list the stand-in flavors are generated from.
    sourceSets.getByName("main").assets.srcDir(rootProject.file("config"))
}

// The hub also carries the stand-in APKs so it can install them itself. Each build variant
// bundles the stand-ins of the same build type, so a release hub holds release-signed ones.
abstract class BundleStandIns : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apks: ConfigurableFileCollection

    @get:Input
    abstract val buildType: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun bundle() {
        val out = outputDir.get().asFile.resolve("standins").apply { deleteRecursively(); mkdirs() }
        apks.files.forEach { apk ->
            val id = apk.name.removePrefix("standin-").removeSuffix("-${buildType.get()}.apk")
            apk.copyTo(out.resolve("$id.apk"), overwrite = true)
        }
    }
}

androidComponents {
    onVariants { variant ->
        val type = variant.name
        val bundle = tasks.register<BundleStandIns>("bundleStandIns${type.replaceFirstChar { it.uppercase() }}") {
            dependsOn(":standin:assemble${type.replaceFirstChar { it.uppercase() }}")
            apks.from(rootProject.fileTree("standin/build/outputs/apk") { include("*/$type/*.apk") })
            buildType.set(type)
        }
        variant.sources.assets?.addGeneratedSourceDirectory(bundle, BundleStandIns::outputDir)
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
