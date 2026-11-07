plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// Release signing details come from keystore.properties (gitignored); see README.
// Both modules must use the same key, because the hub recognises a stand-in by its certificate.
extra["keystoreProperties"] = java.util.Properties().apply {
    file("keystore.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}

// One version for the whole project, from appVersion in gradle.properties (semver: MAJOR.MINOR.PATCH).
// Android's versionCode is derived from it so the two can't drift: 1.2.3 -> 10203.
val appVersionName = property("appVersion") as String
val (appMajor, appMinor, appPatch) = appVersionName.split(".").map { it.toInt() }
require(appMinor in 0..99 && appPatch in 0..99) { "appVersion minor and patch must be 0-99 (versionCode = major*10000 + minor*100 + patch)" }
extra["appVersionName"] = appVersionName
extra["appVersionCode"] = appMajor * 10000 + appMinor * 100 + appPatch
