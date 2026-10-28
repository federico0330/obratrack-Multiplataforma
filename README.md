# ObraTrack Multiplataforma

Aplicación multiplataforma para gestión de obras, relevamientos de campo y tareas, desarrollada con Kotlin Multiplatform.

La lógica compartida vive en `shared/`. La guía local de configuración y aprendizaje está en `guia.md` (excluida de Git).

## Arquitectura inicial

`shared/src/commonMain` contiene dominio puro y un adaptador de Firebase multiplataforma. Las reglas no conocen Firebase ni la interfaz. Los casos de uso dependen de contratos y el adaptador implementa esos contratos; `commonTest` verifica el dominio con repositorios falsos. `androidApp` contiene la aplicación Compose Android.

## Estado

Primera entrega: inicio de sesión Firebase por email/contraseña, estado de sesión persistente y vista principal Android. iOS y Firestore se incorporarán después.

## Configuración local

El archivo `androidApp/google-services.json` debe descargarse desde el proyecto Firebase ObraTrack para la app Android `com.unaj.appmoviles.obratrack`. Está ignorado por Git. Habilitá Email/Password en Firebase Authentication y utilizá una cuenta existente.

Requisitos: JDK 17 y Android SDK 36. Ejecutá `./gradlew :androidApp:installDebug` para instalar en un dispositivo conectado y `./gradlew :shared:allTests` para correr las pruebas comunes.
