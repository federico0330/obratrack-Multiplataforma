plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        namespace = "com.unaj.obratrack.core.ui"
        compileSdk = 37
        minSdk = 26
        androidResources { enable = true }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
        }
    }
}

compose.resources {
    generateResClass = always
    packageOfResClass = "com.unaj.obratrack.core.ui.generated.resources"
}
