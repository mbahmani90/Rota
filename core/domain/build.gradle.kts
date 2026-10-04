import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: the compiler rejects any Android or framework import in the domain layer.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // @Inject only (JSR-330 annotations, no Android)
    implementation(libs.javax.inject)
    // Flow in public interfaces, so consumers get it too; pure Kotlin, no Android
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
