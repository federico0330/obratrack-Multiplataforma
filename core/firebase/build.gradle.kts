plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    jvmToolchain(21)
    android {
        namespace = "com.unaj.obratrack.core.firebase"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()
    cocoapods {
        summary = "Acceso multiplataforma a Firebase para ObraTrack"
        homepage = "https://github.com/federico0330/obratrack-Multiplataforma"
        ios.deploymentTarget = "16.0"
        pod("FirebaseAuth")
        pod("FirebaseFirestore")
        framework {
            baseName = "ObraTrackFirebase"
            isStatic = true
        }
    }
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        commonMain.dependencies {
            implementation("dev.gitlive:firebase-auth:2.7.0")
            implementation("dev.gitlive:firebase-firestore:2.7.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}
