plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.native.cocoapods")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        namespace = "com.unaj.obratrack.composeapp"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
        androidResources { enable = true }
    }

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        name = "ComposeApp"
        version = "1.0.0"
        summary = "Interfaz compartida de ObraTrack"
        homepage = "https://github.com/federico0330/obratrack-Multiplataforma"
        ios.deploymentTarget = "16.0"
        pod("FirebaseAuth")
        pod("FirebaseFirestore")
        framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:data"))
            implementation(project(":core:firebase"))
            implementation(project(":core:ui"))
            implementation(project(":features:auth"))
            implementation(project(":features:obras"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.12.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

compose.resources {
    generateResClass = always
    packageOfResClass = "com.unaj.obratrack.composeapp.generated.resources"
}
