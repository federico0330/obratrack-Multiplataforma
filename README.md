# ObraTrack Multiplataforma

## Avances

- [x] Login y registro con Firebase; probado en Android.
- [x] Mis obras: listar, crear, ver detalle, editar y cambiar estado.
- [x] Dominio, datos y Firebase separados; pantallas Compose con ViewModels y dependencias desde `AppContainer`.
- [x] Tests unitarios de dominio, repositorios y ViewModels.
- [x] App compilada y ejecutada en un teléfono Android; detalle y edición revisados en emulador.

## Estructura

- `core/domain`: modelos, reglas, casos de uso y contratos.
- `core/data` y `core/firebase`: repositorios, mapeos y acceso a Firebase.
- `features`: presentación Android.
- `androidApp`: inicio, navegación y composición de dependencias.

La configuración local de Firebase no se versiona. `guia.md` contiene el plan de migración en la copia local del repo.
