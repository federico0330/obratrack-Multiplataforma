plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        namespace = "com.unaj.obratrack.features.obras"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
        androidResources { enable = true }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:ui"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.ui)
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.12.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("io.github.vinceglb:filekit-dialogs-compose:0.16.0")
            implementation("io.coil-kt.coil3:coil-compose:3.6.3")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.6.3")
            // Mapa de la ubicación. Android e iOS están en Beta y la API puede cambiar entre versiones menores: se fija.
            implementation("org.maplibre.compose:maplibre-compose:0.20.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

compose.resources {
    generateResClass = always
    packageOfResClass = "com.unaj.obratrack.features.obras.generated.resources"
}
