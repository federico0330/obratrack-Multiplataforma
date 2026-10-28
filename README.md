# ObraTrack Multiplataforma

Estamos reconstruyendo ObraTrack desde cero con Kotlin Multiplatform. La idea es compartir reglas y datos entre Android e iOS, y avanzar por partes para entender y probar cada decisión.

## Lo que ya está hecho

- [x] Repo inicializado y Gradle Wrapper agregado.
- [x] Módulos separados para dominio, datos, Firebase y las features de acceso e inicio.
- [x] Login Android con Firebase, sesión persistida y cierre de sesión.
- [x] Vista inicial **Mis obras** (todavía vacía; aún no consulta Firestore).
- [x] ViewModel para el estado de acceso, componentes Compose sin estado propio y dependencias conectadas desde `AppContainer`.
- [x] Primeros tests unitarios comunes para reglas y casos de uso.

## Para seguir

- [ ] Confirmar que Gradle sincroniza y que la app inicia en un emulador.
- [ ] Probar el login con una cuenta existente de Firebase.
- [ ] Crear la app anfitriona iOS y verificar Firebase allí.
- [ ] Implementar obras y continuar con relevamientos, tareas, agenda, informes y sincronización sin conexión.

## Dónde está cada cosa

- `core/domain`: modelos, reglas, casos de uso y contratos.
- `core/data`: repositorios que conectan el dominio con fuentes de datos.
- `core/firebase`: acceso al SDK de Firebase.
- `features/auth` y `features/home`: estado y pantallas.
- `androidApp`: arranque, dependencias y navegación Android.

Las dependencias apuntan hacia el dominio. Las pantallas no llaman a Firebase. La configuración local `androidApp/google-services.json` no se sube a Git; usá la del proyecto Firebase de ObraTrack.

**Para compilar:** JDK 21 y Android SDK 36. Desde la terminal: `./gradlew :androidApp:assembleDebug`. Para los tests comunes: `./gradlew :core:domain:allTests`.

La guía de arquitectura y el plan detallado de trabajo están en `guia.md` en la copia local del repo.
