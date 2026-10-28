# ObraTrack Multiplataforma

## Avances

- [x] Repo y Gradle configurados; sincronización y compilación completadas en Android Studio.
- [x] App ejecutada en un teléfono Android físico.
- [x] Login probado con una cuenta existente de Firebase; al ingresar abre **Mis obras**.
- [x] Módulos de dominio, datos, Firebase y features separados.
- [x] Estado de acceso en un ViewModel; pantallas Compose separadas y dependencias conectadas desde `AppContainer`.
- [x] Tests unitarios comunes escritos para reglas y casos de uso (falta ejecutarlos).

La pantalla **Mis obras** todavía no lee Firestore, por eso aparece vacía.

## Estructura

- `core/domain`: modelos, reglas, casos de uso y contratos.
- `core/data` y `core/firebase`: repositorios y acceso a Firebase.
- `features/auth` y `features/home`: estado y pantallas.
- `androidApp`: inicio y conexión de dependencias Android.

La configuración local de Firebase no se versiona. `guia.md` contiene el checklist de arquitectura y la continuidad del proyecto en la copia local del repo.
