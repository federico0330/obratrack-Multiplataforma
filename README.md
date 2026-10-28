# ObraTrack Multiplataforma

Aplicación multiplataforma para gestión de obras, relevamientos de campo y tareas, desarrollada con Kotlin Multiplatform.

La lógica compartida y los límites de arquitectura están organizados en módulos `core:*`. La guía local de configuración y aprendizaje está en `guia.md` (excluida de Git).

## Arquitectura inicial

`core:domain` contiene reglas puras y contratos. `core:data` implementa repositorios y `core:firebase` encapsula Firebase. `features:auth` publica estado y pantallas; `features:home` renderiza la vista principal. `androidApp` conecta las dependencias y la navegación inicial. Las dependencias de negocio apuntan hacia el dominio.

## Estado

Primera entrega: inicio de sesión Firebase por email/contraseña, estado de sesión persistente y vista principal Android. iOS y Firestore se incorporarán después.

## Configuración local

El archivo `androidApp/google-services.json` debe descargarse desde el proyecto Firebase ObraTrack para la app Android `com.unaj.appmoviles.obratrack`. Está ignorado por Git. Habilitá Email/Password en Firebase Authentication y utilizá una cuenta existente.

Requisitos: JDK 17 y Android SDK 36. Ejecutá `./gradlew :androidApp:installDebug` para instalar en un dispositivo conectado y `./gradlew :core:domain:allTests` para correr las pruebas comunes.
