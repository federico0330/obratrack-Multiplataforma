# ObraTrack Multiplataforma

## Avances

- [x] Login, registro y gestión de obras en Android con Firebase.
- [x] Pantallas de acceso y Mis obras compartidas con iOS; navegación login/registro probada en simulador.
- [ ] Probar autenticación y lectura reales de Firebase en iOS (falta registrar la app y agregar su configuración).
- [x] Capas separadas para dominio, datos, Firebase y presentación.
- [x] Pruebas unitarias para reglas, repositorios y ViewModels.
- [x] Flujo ejecutado en Android físico y emulador.

## Estructura

- `core/domain`: modelos, reglas, casos de uso y contratos.
- `core/data` y `core/firebase`: repositorios, mapeos y acceso a Firebase.
- `features`: pantallas y estado de autenticación y obras.
- `composeApp`: interfaz común y composición de dependencias.
- `androidApp` / `iosApp`: entradas nativas de cada plataforma.

La configuración Firebase de cada plataforma se mantiene fuera de Git.
