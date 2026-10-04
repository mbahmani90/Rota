import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

// ktlint rules live here (the single source for CI); .editorconfig only has general editor settings.
val ktlintRules = mapOf(
    // Android Studio's Kotlin style, close to the official style the project uses
    "ktlint_code_style" to "android_studio",
    "max_line_length" to 120,
    // Keep the project's style: class parameters one per line with trailing commas, even when short
    "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to 1,
    // Composables are PascalCase by convention
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
)

// ./gradlew spotlessCheck fails on violations; ./gradlew spotlessApply fixes what it can.
spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(ktlintRules)
            .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"))
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
}

// Release version and signing for both apps (mobile, automotive), used by the release workflow.
// Version: -Prota.versionName=1.2.3 -Prota.versionCode=10203 (defaults: 1.0 / 1).
// Signing: only from environment variables (CI secrets); without them, release APKs are unsigned.
subprojects {
    pluginManager.withPlugin("com.android.application") {
        // Variant API: runs after the module's own android {} block, so it isn't overwritten there.
        val versionName = findProperty("rota.versionName") as String?
        val versionCode = (findProperty("rota.versionCode") as String?)?.toInt()
        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                variant.outputs.forEach { output ->
                    versionName?.let { output.versionName.set(it) }
                    versionCode?.let { output.versionCode.set(it) }
                }
            }
        }

        extensions.configure<ApplicationExtension> {
            val keystore = System.getenv("ROTA_KEYSTORE_FILE")
            if (!keystore.isNullOrBlank()) {
                val release = signingConfigs.create("release") {
                    storeFile = file(keystore)
                    storePassword = System.getenv("ROTA_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("ROTA_KEY_ALIAS")
                    keyPassword = System.getenv("ROTA_KEY_PASSWORD")
                }
                buildTypes.getByName("release").signingConfig = release
            }
        }
    }
}
