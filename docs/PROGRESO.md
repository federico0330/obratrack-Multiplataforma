# Progreso de ObraTrack Multiplataforma

Mantener corto (unas 40 líneas). Al agregar algo, condensar lo viejo.

## Hecho
- Repo y Gradle configurados; sincroniza y compila en Android Studio.
- App ejecutada en teléfono Android físico.
- Login con Firebase probado con una cuenta existente; al ingresar abre "Mis obras".
- Módulos separados: core/domain, core/data, core/firebase, features/auth, features/home, androidApp.
- Estado de acceso en un ViewModel; pantallas Compose separadas; dependencias desde AppContainer.
- Login: errores tipados (ErrorAuth) por tipo/código de GitLive, sin texto; CerrarSesion y ObservarSesion en domain; AuthViewModel solo usa casos de uso y reanuda la observación de sesión tras una falla.
- Textos de auth y home en recursos es/en/it (italiano a revisar); botón ver/ocultar contraseña.
- Tests: 64 pasan (domain 16, firebase 11, data 9, auth 28). assembleDebug OK. Comandos en CLAUDE.md.
- CrearObra y ValidadorNombreObra en domain (sin UI). RepositorioObras es solo una interfaz, sin adaptador.
- Registro: RegistrarUsuario, validación y clasificador de fallos en domain/data/firebase; pantalla y RegistroViewModel en features/auth, cableados en AppContainer/MainActivity (Login <-> Registro, tras registrarse pasa a Home por la sesión). Textos es/en/it. Falta probarlo contra Firebase real en el teléfono.

## Próximos pasos (en orden)
1. Adaptador Firestore de RepositorioObras: crear y listar (colección raíz `obras`).
2. UI de Crear Obra y listado en "Mis obras" con su ViewModel y caso de uso ListarObras.
3. Navegación: Login, Mis obras, Crear Obra, Detalle.
4. Relevamientos, tareas, visitas con aviso y agenda (según la app Android previa).
5. Última etapa: offline con la persistencia propia de Firestore (escrituras sin esperar al servidor, estado "pendiente de sincronizar").

## Pendiente de decidir
- ¿La UI se comparte (Compose Multiplatform) o queda nativa en Android?
- Recuperar contraseña, Google y verificación de email: no existen; no se crea usuarios/{uid}.
- Sin cobertura de tests: capa Firestore (aún no existe) y UI.

## Decisiones tomadas
- La próxima visita vive en la obra; un relevamiento solo la propone al crearse.
- Fechas de calendario en UTC, "hoy" con Clock local, comparar como fecha.
- Avisos de visita locales, sin FCM, sin plan Blaze ni tarjeta.
- Fotos a Cloudinary, no a Firebase Storage.
- Los errores se informan; nada de catch que devuelva éxito.
- Errores de auth: tipo ErrorAuth en domain, AuthError en UI; el texto sale de recursos.
- Login y registro cerrados. Sin perfil usuarios/{uid} ni AsegurarPerfil (usuarios solo en Authentication).
- Offline: última etapa, con la persistencia propia de Firestore; no se diseña ahora.
- Obras en colección raíz `obras` (userId, publica); reglas publicadas, no se tocan.
- Firestore: GitLive firebase-firestore 2.7.0 (misma familia que auth; verificado en Maven). Estructura según docs/firestore.rules.referencia.
