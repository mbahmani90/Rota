import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// MAPS_API_KEY from local.properties (gitignored) or a CI environment variable; blank means
// Google Places is off and the app uses Overpass.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY")
    ?: System.getenv("MAPS_API_KEY")
    ?: ""

android {
    namespace = "com.majidbahmani.rota.core.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true // BuildConfig.DEBUG and BuildConfig.MAPS_API_KEY
    }
}

dependencies {
    implementation(project(":core:domain"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Settings
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
