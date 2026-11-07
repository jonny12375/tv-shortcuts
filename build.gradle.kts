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
