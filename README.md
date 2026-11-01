# ObraTrack Multiplataforma

## Avances

- [x] Login, registro y sesión con Firebase.
- [x] Mis obras: listar, crear, ver, editar, cambiar estado y eliminar.
- [x] Capas separadas para dominio, datos, Firebase y presentación.
- [x] Pruebas unitarias para reglas, repositorios y ViewModels.
- [x] Flujo ejecutado en Android físico y emulador.
- [ ] Interfaz compartida y navegación login/registro/obras en iOS: código agregado; falta validarlo en simulador.

## Estructura

- `core/domain`: modelos, reglas, casos de uso y contratos.
- `core/data` y `core/firebase`: repositorios, mapeos y acceso a Firebase.
- `features`: pantallas y estado de autenticación y obras.
- `composeApp`: interfaz común y composición de dependencias.
- `androidApp` / `iosApp`: entradas nativas de cada plataforma.

`guia.md` contiene el checklist detallado de migración; la configuración Firebase de cada plataforma se mantiene fuera de Git.
