import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val mapsApiKey = localProperties.getProperty("MAPS_API_KEY", "")
val backendUrl = localProperties.getProperty(
    "RYDEX_BACKEND_URL",
    "http://10.0.2.2:8787/",
)

android {
    namespace = "com.rydex.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rydex.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.4.0"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField(
            "String",
            "MAPS_API_KEY",
            "\"$mapsApiKey\"",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes.all {
        buildConfigField(
            "String",
            "BACKEND_URL",
            "\"$backendUrl\"",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.json)
    implementation(libs.okhttp)
    implementation(libs.kotlinxSerializationJson)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
