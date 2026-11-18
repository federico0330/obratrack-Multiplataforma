import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

// Claves locales (no versionadas): local.properties está en .gitignore.
val propiedadesLocales = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun propiedadLocal(nombre: String) = propiedadesLocales.getProperty(nombre, "").trim().replace("\"", "")

android {
    namespace = "com.unaj.appmoviles.obratrack"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.unaj.appmoviles.obratrack"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Vacías si no están en local.properties: entonces la app funciona solo con plantillas.
        buildConfigField("String", "GEMINI_API_KEY", "\"${propiedadLocal("GEMINI_API_KEY")}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"${propiedadLocal("GEMINI_MODEL")}\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    androidTestImplementation(project(":core:firebase"))
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("dev.gitlive:firebase-auth:2.7.0")
    androidTestImplementation("dev.gitlive:firebase-firestore:2.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation(project(":composeApp"))
    implementation(project(":core:domain"))
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.core:core-ktx:1.17.0")
    // Motor gráfico del mapa (misma versión que la librería). OpenGL y no Vulkan, por compatibilidad.
    runtimeOnly("org.maplibre.compose:maplibre-compose-runtime-opengl-android:0.20.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
