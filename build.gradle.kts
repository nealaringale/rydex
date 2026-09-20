buildscript {
    dependencies {
        // AGP 9's built-in Kotlin defaults to KGP 2.2.10.
        // RYDEX depends on libraries built with Kotlin 2.4.x,
        // so use the newer KGP explicitly.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
