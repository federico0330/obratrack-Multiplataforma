# ObraTrack Multiplataforma

## Avances

- [x] Login, registro y gestión de obras en Android con Firebase.
- [x] Alta, edición, estados y baja de tareas.
- [x] Alta y listado de relevamientos, con vínculo opcional a una tarea.
- [x] Pantallas de acceso y Mis obras compartidas con iOS; navegación login/registro probada en simulador.
- [x] Capas separadas para dominio, datos, Firebase y presentación.
- [x] Pruebas unitarias para reglas, repositorios y ViewModels.
- [x] Flujo ejecutado en Android físico y emulador.

## Estructura

- `core/domain`: modelos, reglas, casos de uso y contratos.
- `core/data` y `core/firebase`: repositorios, mapeos y acceso a Firebase.
- `core/ui`: componentes visuales compartidos.
- `features`: pantallas y estado de autenticación, obras, tareas y relevamientos.
- `composeApp`: interfaz común y composición de dependencias.
- `androidApp` / `iosApp`: entradas nativas de cada plataforma.

La configuración Firebase de cada plataforma se mantiene fuera de Git.
