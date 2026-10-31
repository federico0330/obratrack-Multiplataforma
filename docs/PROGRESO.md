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

## Próximos pasos (en orden)
1. Definir la capa local para el requisito offline (ver "Pendiente de decidir").
2. Adaptador Firestore de RepositorioObras: crear y listar, sin esperar al servidor al escribir.
3. UI de Crear Obra y listado en "Mis obras" con su ViewModel y caso de uso ListarObras.
4. Navegación: Login, Mis obras, Crear Obra, Detalle.
5. Relevamientos, tareas, visitas con aviso y agenda (según la app Android previa).

## Pendiente de decidir
- Offline: persistencia de Firestore. A futuro análisis de base local propia (Room KMP o SQLDelight)
- ¿La UI se comparte (Compose Multiplatform) o queda nativa en Android?
- Firestore: usar GitLive firebase-firestore 2.7.0 (misma familia que auth; verificado en Maven). Estructura según docs/firestore.rules.referencia (obras en colección raíz con userId).
- Registro: RegistrarUsuario, validación de credenciales y clasificador de fallos listos en domain/data/firebase (sin UI ni cableado en AppContainer).
- Recuperar contraseña, Google y verificación de email: no existen; no se crea usuarios/{uid}.
- Sin cobertura de tests: capa Firestore (aún no existe) y UI.

## Decisiones tomadas
- La próxima visita vive en la obra; un relevamiento solo la propone al crearse.
- Fechas de calendario en UTC, "hoy" con Clock local, comparar como fecha.
- Avisos de visita locales, sin FCM, sin plan Blaze ni tarjeta.
- Fotos a Cloudinary, no a Firebase Storage.
- Los errores se informan; nada de catch que devuelva éxito.
- Errores de auth: tipo ErrorAuth en domain, AuthError en UI; el texto sale de recursos.
