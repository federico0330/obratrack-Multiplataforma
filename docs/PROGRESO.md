# Progreso de ObraTrack Multiplataforma

## Revisión del 10/11/2026

- [x] Revisados los recorridos de acceso, obras, tareas, relevamientos, Agenda y perfil en un teléfono Android 15.
- [x] Aprobadas 413 pruebas de host, la compilación Android y cuatro recorridos de dispositivo con Firebase local.
- [x] Corregido Reintentar en Obras y Agenda. Una suscripción que seguía activa podía impedir abrir una consulta nueva después de un error.
- [x] Corregida la última visita ante altas y ediciones simultáneas. Firestore puede conservar la versión de un documento si se escribe el mismo valor. Ahora cada mutación incrementa `revisionRelevamientos`, aunque `ultimaVisita` no cambie.
- [x] La baja de tareas y sus relevamientos utiliza una transacción que lee la obra antes de calcular el plan. Si otra operación cambia sus relevamientos, se vuelve a consultar y calcular la fecha. Se conserva el límite de escrituras y la opción de desvincular.
- [x] La prueba de integración verifica tres rondas de alta/edición simultáneas y cinco rondas de alta/baja de tarea simultáneas, con lecturas del servidor.
- [x] Agregadas descripciones de accesibilidad a creación, edición de obras y acciones de tareas. El selector de fecha expone su etiqueta y valor.
- [x] Corregido el contraste del reloj y los íconos del sistema al cambiar entre tema claro y oscuro en Android.
- [x] Preparada una cuenta de demostración separada, con 3 obras privadas, 8 tareas y 5 relevamientos. Dos fotografías se subieron a Cloudinary mediante el caso de uso de creación de relevamientos. La cuenta se abre desde la app habitual.
- [x] Verificadas la persistencia de la sesión después de detener y abrir la app y las pantallas de la demostración con Firebase real.
- [x] Abiertas y canceladas la galería y la cámara del sistema. Los archivos personales del dispositivo no se usaron para las fotos de la demostración. Se comprobó que el mapa termina de cargar.
- [x] Retiradas del teléfono las instalaciones y la instrumentación de pruebas. Quedó la app ObraTrack con sesión de demostración, en español.
- [x] README actualizado con alcance, checklist, recorrido, preparación Android/iOS y pendientes.

El contador es un campo interno adicional de la obra. Los documentos anteriores pueden incorporarlo al modificarse y el cliente nativo puede seguir leyendo los datos. La comprobación de concurrencia cubre operaciones de este cliente. Las escrituras simultáneas desde el cliente nativo anterior requieren una revisión propia porque aquel no incrementa el contador.

La captura de una foto de 50 MP y el GPS físico con precisión aproximada siguen pendientes. El flujo real de Firebase, cámara y ubicación en iOS continúa pendiente. El workflow de simulador está deshabilitado. Las credenciales y la configuración de la demostración se entregan por privado. Los scripts, imágenes y capturas de esta revisión quedan en `pruebas/revision-10-11/`, fuera de Git.

## Base inicial
- Gradle configurado; app ejecutada en teléfono Android físico.
- Login Firebase probado con una cuenta existente; registro implementado.
- Capas separadas: `core/domain`, `core/data`, `core/firebase`, features Android y `androidApp`.
- Login/registro con ViewModels, pantallas Compose, errores tipados y dependencias desde `AppContainer`.
- Textos de auth/home en es/en/it; el italiano requiere revisión.
- Última verificación registrada antes de esta feature: 121 tests y `assembleDebug` OK.
- Kotlin 2.4.21, AGP 9.4.1, Gradle 9.6.0, JDK 21.

## Crear y listar obras
- Dominio, puerto Firestore, repositorio, feature Android y navegación inicial están implementados en la rama `feat/obras-create-list`.
- La consulta filtra por `userId`; la creación conserva `obras/{id}`, los campos nativos y fechas Firestore Timestamp.
- Pasan los tests de domain, data, firebase y obras, y `:androidApp:assembleDebug`.
- En teléfono: sesión existente, lectura, validación, creación y persistencia tras reiniciar probadas con Firebase.
- Quedó el registro `PRUEBA_QA_NO_USAR_20261101` para borrar desde Firebase Console.

## Detalle, edición y estado de obras
- Se agregó el detalle de una obra propia, la edición de sus campos básicos y el cambio entre activa, pausada y finalizada.
- Las escrituras son parciales: conservan `userId` y campos que el formulario no edita; la fecha continúa guardándose como Timestamp y los estados en minúscula.
- Al cambiar la sesión, se cancela la consulta anterior y se limpia el detalle/formulario antes de cargar la lista del nuevo UID.
- Tests de domain, data, Firebase y `features:obras` pasan; `:androidApp:assembleDebug` pasa.
- En emulador se revisaron el listado, detalle y formulario con los valores existentes. No se guardaron cambios ni se alteraron datos compartidos durante esa revisión.
- El aislamiento está cubierto en pruebas del ViewModel y repositorio; falta comprobarlo manualmente con dos cuentas reales y contra las reglas publicadas.

## Baja de obras
- La eliminación pide confirmación y verifica en servidor que la obra pertenezca a la sesión actual.
- Se borran primero las tareas, relevamientos y suscriptores en lotes; la obra se borra al final. Si falla después de borrar datos relacionados, se informa que el resultado puede ser parcial.
- Se agregaron tests de dominio, repositorio y ViewModel, incluyendo que cancelar no invoque la baja.
- Pasan los tests de domain, data, firebase y `features:obras`, y `:androidApp:assembleDebug`.
- En emulador se revisaron el detalle y el diálogo de confirmación; se canceló la baja y no se modificaron datos de Firebase.
- Corregido (`fix/baja-obra-trabada`): la lista en vivo quitaba la obra seleccionada antes de que la baja confirmara y la barra de progreso quedaba trabada, bloqueando editar y eliminar hasta reiniciar; ahora la baja se cierra por sesión y el error de una baja que falla se muestra en la lista.

## Tareas de obra (rama `feat/tareas`)
- Dominio, puerto Firestore (`obras/{id}/tareas`, fechas ISO, enums por nombre), repositorio y pantalla integrada al detalle de la obra: listado con resumen y atrasadas, alta, edición, cambio de estado y baja eligiendo si los relevamientos vinculados se desvinculan o se eliminan. Textos en es/en/it (el italiano requiere revisión).
- Pasan los tests de domain, data, firebase y `features:obras`; `:androidApp:assembleDebug` pasa. Falta probar con Firebase real en el teléfono y el aislamiento con dos cuentas; iOS solo compila en el CI.
- Las escrituras de obras y tareas esperan al servidor; hay que revisarlas para el requisito offline.

## Próximo
1. Verificar el aislamiento de datos con dos cuentas reales y las reglas publicadas, sin modificarlas.
2. Completar la comprobación real de Firebase en iOS cuando haya una Mac o CI disponible. El workflow figura deshabilitado manualmente.
3. ABM de relevamientos validado en Android. Fotos/cámara, ubicación y PDF conservan sus etapas propias en la guía, según el alcance confirmado.

## Interfaz compartida y host iOS
- Login, registro, sesión y Mis obras se movieron a `commonMain`, junto con sus textos español/inglés/italiano.
- `composeApp` contiene el punto de composición de dependencias y navegación común; Android e iOS llaman a la misma interfaz.
- Se agregó el host SwiftUI de iOS y una prueba de interfaz que recorre login → registro → login sin inicializar Firebase ni escribir datos.
- Pasan `assembleDebug` y las pruebas de `core:domain`, `core:data`, `core:firebase`, `features:auth` y `features:obras` desde una ruta de trabajo sin espacios.
- El APK abre en el emulador Android, recupera la sesión y muestra las obras de Firebase sin hacer escrituras.
- El build y la prueba de navegación pasan en iOS Simulator mediante GitHub Actions con runner macOS; no hace falta un dispositivo físico.
- La app iOS `com.unaj.appmoviles.obratrack.ios` ya está registrada en el proyecto compartido de Firebase y su `GoogleService-Info.plist` está en `iosApp/` en la máquina de quien lo descargó (ignorado por Git, se pasa por canal privado). El plist de ejemplo que usa el CI sirve solo para la prueba visual; no autentica ni escribe datos. El flujo real de Firebase en iOS todavía no se probó.
- Estado de la verificación en iOS: compila y recorre login/registro en el simulador del CI con el plist de ejemplo. El login real con Firebase solo está probado en Android; no hay dispositivo ni Mac propia. Si hace falta, se puede probar en el CI cargando el plist como secreto del repo y sumando una prueba de UI de solo lectura.
- Los `*.podspec` son generados (`.\gradlew :composeApp:podspec`) y están ignorados por Git; el plist real va en `iosApp/` y tampoco se versiona.
- El `compileSdk` de Android pasó a 37, requerido por Compose Multiplatform 1.12.2; el `targetSdk` se mantiene en 36.

## Mejora de interfaces (rama `feat/ui-pantallas`)
- Tema completo (claro y oscuro, con tipografía y formas), componentes comunes de acceso con acciones de teclado, y lista, detalle y formulario de obras rediseñados con barra superior, tarjetas con etiqueta de estado, selector de fecha de Material 3 y botón Atrás. `MisObrasScreen` mantiene sus parámetros, sin cambios para quien conecta Firestore.
- Sin íconos nuevos: `features:obras` no usa `materialIconsExtended`. `BackHandler` común (`ui-backhandler`) está deprecado a favor de `NavigationEventHandler`; migrar cuando haya navegación real con back stack.
- Pasan `assembleDebug`, los tests de `features:auth` y `features:obras` y la compilación de `features:obras` para iOS. Falta la revisión visual en el emulador (claro y oscuro) y el CI de iOS.

## Barra inferior y perfil (rama `feat/barra-inferior`)
- Barra inferior con Obras y Mi perfil en `composeApp` (solo texto: `composeApp` no resuelve los íconos de Material sin `materialIconsExtended`, que no se suma). Se oculta en el detalle y el formulario de obras; Atrás desde Perfil vuelve a Obras. `composeApp` tiene ahora recursos de texto en es/en/it.
- `PerfilScreen` (en `features:auth`) muestra nombre (o el correo si no hay) y correo, sin campos nuevos. Cerrar sesión pasó de Obras a Perfil, con diálogo de confirmación; `MisObrasScreen` ya no recibe `onSignOut`.
- Pendiente para la capa offline: cerrar sesión con escrituras sin sincronizar debe revisarse (qué pasa con los cambios pendientes de ese usuario antes de salir).

- Íconos en la barra inferior: vector drawables en `composeApp/.../composeResources/drawable` (`ic_nav_obras`, `ic_nav_perfil`, trazos de Material) cargados con `painterResource`; sin dependencias nuevas. Patrón a repetir para los próximos íconos. Compila `composeApp` en Android; en iOS la tarea se saltea en Windows y falta la revisión visual y el CI.
- Íconos en acciones: flecha atrás (barra superior), `+` en el botón flotante de obras y en crear tarea, lápiz y tacho en detalle de obra, lápiz y tacho como `IconButton` en cada tarea (con `contentDescription` desde los textos existentes), y salida en cerrar sesión. Cada feature tiene sus drawables (`features/obras` y `features/auth`); los repetidos se duplican hasta tener un módulo de UI compartido, que queda como siguiente paso (requiere OK).
- Pasan `testAndroidHostTest` de `features:obras` y `features:auth` (estos módulos ya no tienen `testDebugUnitTest`) y `:androidApp:assembleDebug`. Falta la revisión visual y el CI de iOS.
- Los íconos de mostrar y ocultar contraseña pasaron a vector drawables (`ic_visibility`, `ic_visibility_off` en `features/auth`) y se quitó `materialIconsExtended` de `features:auth`: ya no queda ningún uso en el repo. Pasan `features:auth:testAndroidHostTest` y `:androidApp:assembleDebug`.
- Íconos de prioridad media: etiquetas de estado de obra y de tarea con ícono (`Etiqueta` común en `ObrasComponents.kt`), aviso de tarea atrasada, calendario en el selector de fecha de la obra, reintentar en lista de obras, tareas y perfil, ícono en `ErrorBlock` y en el estado vacío de obras. Pasan `testAndroidHostTest` de `features:obras` y `features:auth` y `:androidApp:assembleDebug`. Falta la revisión visual.
- Las fechas de inicio y fin de la tarea usan el `DatePicker` de Material 3 mediante `CampoFecha` (extraído a su propio archivo y compartido con la obra). Como son opcionales, el diálogo ofrece "Quitar fecha". El estado del formulario sigue siendo texto ISO (vacío = sin fecha), así que ViewModel y dominio no cambiaron; se quitó el texto de ayuda de formato. Pasan `features:obras:testAndroidHostTest` y `:androidApp:assembleDebug`; falta la revisión visual.
- Corregido: una obra finalizada no ofrecía ninguna acción de estado en el detalle. Ahora muestra "Reactivar obra" (vuelve a activa), igual que una pausada. Era solo la UI: dominio, repositorio y ViewModel ya aceptaban la transición.

## Módulo de UI compartido (rama `feat/core-ui`, sobre `feat/ui-iconos`)
- Nuevo `:core:ui` (`core/ui`, paquete `com.unaj.obratrack.core.ui`): todos los drawables de íconos (ya sin duplicados entre features), expuestos como `Iconos.*` porque su `Res` es interna y chocaría con la de cada feature, y los componentes comunes `IconoBoton`, `Etiqueta`, `ErrorBlock` y `CampoFecha`, con sus textos en es/en/it (`ui_*`). Las features dependen de él; `:core:ui` no depende de ninguna.
- `features:auth` reemplaza `AuthErrorMessage` por `ErrorBlock` (ahora los errores de login, registro y perfil llevan el ícono de error). Quedan fuera del módulo el tema (sigue en `composeApp`) y los íconos de la barra inferior.
- Pasan `testAndroidHostTest` de `features:auth` y `features:obras`, `:androidApp:assembleDebug` y la compilación de iOS Simulator de `core:ui`, `features:auth` y `features:obras`. Falta la revisión visual y el CI de iOS.

## Perfil con resumen de obras (rama `feat/perfil-datos`)
- `resumenObras` (función pura en `core:domain`) cuenta total, activas, pausadas y finalizadas. `PerfilViewModel` y `PerfilScreen` están en `features:auth` y reutilizan `ObservarObras`; no dependen de `features:obras` ni modifican `ObrasViewModel`. Los datos de la cuenta se ven siempre; el resumen tiene su propio estado de carga y error con "Reintentar", y distingue 0 obras de "no se pudo cargar".
- La consulta corre solo mientras la pestaña Perfil está visible (5 s de gracia). Al volver se muestra el último valor mientras actualiza; cerrar sesión no muestra error y descarta el valor, para que otra cuenta nunca vea el resumen de la anterior.
- Perfil y Mis obras son dos suscripciones a la misma consulta de obras; se espera que el SDK de Firestore las agrupe, pero no está verificado. Pasan los tests de `core:domain` y `features:auth`; falta la revisión visual y el CI de iOS.

## Continuar con Google (rama `feat/login-google`, sobre `main` con relevamientos)
- Integración directa con los SDK. Se evitó KMPAuth porque su módulo de Firebase depende de GitLive 3.0.0-alpha01. En el dominio, `IniciarSesionConGoogle` usa los puertos `ProveedorTokenGoogle` (uno por plataforma: Credential Manager en `androidApp`; Google Sign-In en Swift, que responde por callback a `composeApp/iosMain`) y `RepositorioAccesoGoogle`. Este último está separado de `RepositorioAutenticacion` para no romper los fakes de otras ramas. Cancelar no muestra error.
- Pasan los tests de todos los módulos (incluidos los relevamientos) y se probó en un dispositivo Android. Falta: el CI de iOS (`pod install` con GoogleSignIn y el script del URL scheme) y probarlo en iOS con el plist real.

## Fotos de relevamientos, PR 1 (rama `feat/fotos-cloudinary`)
- Galería, compresión, subida a Cloudinary sin firma y vista en el detalle. Misma cuenta, preset y carpeta que la app Android anterior (`iwsier4h`, `obratrack_unsigned`, `relevamientos/{obraId}`), con nombre UUID en cada subida. Puerto `AlmacenFotos` en el dominio; los casos de uso suben primero y escriben `fotoUrl` después, fuera de la transacción. Editar sin tocar la foto no modifica el campo; quitarla lo elimina.
- `core:data/fotos`: Ktor 3.6.0 (OkHttp/Darwin), compresión de FileKit 0.16.0 (lado 1920 px, objetivo 1,5 MB, tope 5 MB) y URL reducida `c_fill,w_800` para mostrar. UI con FileKit (selector) y Coil 3.6.3. Pasan 287 pruebas de host y `assembleDebug`; core:data y composeApp no compilan para iOS desde Windows.
- Quedan imágenes huérfanas en Cloudinary al reemplazar o quitar una foto, al borrar el relevamiento o si Firestore falla después de la subida: sin firma no se pueden borrar. Sin conexión se informa un error y se conserva el formulario; la cola de subidas queda para la etapa offline.
- Probado en un teléfono Android: crear con foto de la galería, verla en el detalle, reemplazarla y quitarla al editar. Falta probar el error sin conexión (modo avión). La rama se actualizó sobre `main` con el login con Google.

## Fotos de relevamientos, PR 2 (rama `feat/fotos-camara`, sobre `feat/fotos-cloudinary`)
- Cámara con `rememberCameraPickerLauncher` de FileKit (Android e iOS), sin dependencias nuevas de UI. Android no declara `CAMERA`, así que usa la cámara del sistema sin pedir permiso. iOS lleva `NSCameraUsageDescription` traducido en `iosApp/Resources/*.lproj`. La foto temporal se borra después de leerla. Miniatura de 64 dp en el listado (recorte `c_fill,w_160,h_160`).
- En Android, compresor propio (`androidx.exifinterface` 1.4.2): lee la foto reducida con `inSampleSize`, la endereza según EXIF y la decodifica una sola vez para todas las calidades; FileKit cargaba la imagen completa (unos 200 MB con 50 MP). iOS sigue con FileKit.
- La cámara se probó en un teléfono Android. Pendiente: confirmar con una foto de alta resolución (50 MP). iOS (cámara, textos del permiso y `core:data`) no se puede compilar ni probar desde Windows. Si Android cierra la app con la cámara abierta, el formulario se pierde (el ViewModel no guarda estado).

## Verificación de fotos en Android

- El PR #16 integró cámara, miniaturas y compresión Android a `main`. El compresor aplica el espejo horizontal antes de girar, como indica EXIF. Esto corrige las fotos que traen las dos transformaciones a la vez.
- Pasaron 304 pruebas de host y las compilaciones de la app y sus pruebas Android. La app arrancó hasta la pantalla de acceso en el emulador. El `google-services.json` local se renovó desde Firebase para que Gradle genere `default_web_client_id` después de incorporar el acceso con Google.
- En el emulador Android 14, las dos orientaciones espejadas fallaron antes del cambio y pasaron después. El compresor procesó un JPEG sintético de 8160 × 6120 y entregó 1920 × 1440 dentro del tope de 5 MB. El material de la prueba se guarda en `pruebas/`, fuera de Git.
- Las pruebas de host simulan la falta de red y verifican que el formulario conserve la nota y la foto sin crear el relevamiento. Queda pendiente repetir el flujo en modo avión, cancelar la cámara y capturar a 50 MP en un teléfono. La prueba del flujo completo en iOS también queda pendiente.

## Ubicación de relevamientos, PR 1 (rama `feat/ubicacion-relevamientos`)
- Captura con el GPS, guardado, quitar y coordenadas con «Abrir en el mapa» (`https://www.google.com/maps/search/?api=1&query=lat,lon` con el manejador de URIs de Compose). Compass 4.0.1 (`geolocation`, `geolocation-mobile`, `permissions`, `permissions-mobile`): en Android usa el `LocationManager` del sistema, sin Play Services, y los permisos entran por merge. El puerto `ProveedorUbicacion` tiene dos pasos (`solicitarAcceso` y `posicionActual`) para que el diálogo del permiso no cuente contra el tiempo máximo de 20 s de `ObtenerUbicacionActual`. Acepta la ubicación aproximada. Se escribe el campo `ubicacion` (`GeoPoint`) sin cambiar esquema ni reglas; editar sin tocarla no lo modifica, así que conserva el de la app anterior.
- iOS: `NSLocationWhenInUseUsageDescription` en `project.yml` y traducido en `iosApp/Resources/*.lproj`. Android declara `android.hardware.location` y `.gps` como no requeridos para que Google Play no filtre dispositivos sin GPS.
- Pendiente: probar en un teléfono (permiso, GPS apagado, sin señal, «Aproximada»); `core:data` y `composeApp` no compilan para iOS desde Windows, así que Compass en iOS no está verificado.
- Dirección escrita y búsqueda de dirección (misma rama): Compass `geocoder` y `geocoder-mobile` 4.0.1, sin permisos ni dependencias nuevas (solo `INTERNET`). El detalle muestra la dirección debajo de las coordenadas, cargada aparte; si falla o el dispositivo no geocodifica se ven solo las coordenadas, sin error. El formulario tiene «Buscar dirección» (botón explícito, mínimo 3 caracteres, hasta 5 candidatos) y elegir uno carga esa ubicación como nueva; si el dispositivo no geocodifica el buscador no se ofrece. La dirección NO se guarda en Firestore: depende del geocodificador del sistema (`Geocoder` en Android, que necesita un servicio del sistema y red; `CLGeocoder` en iOS) y no hay forma de saber si falló por falta de conexión, porque Compass descarta el tipo del error. Sin red el guardado no se bloquea.
- Mapa (misma rama): maplibre-compose 0.20.0 (Android e iOS en Beta; versión fijada) con los mapas gratuitos de OpenFreeMap, sin clave; el crédito lo muestra el propio mapa. Las coordenadas escritas se sacaron de la pantalla: el detalle y el formulario muestran un mapa chico con el pin (sin gestos, para no trabar el desplazamiento) y el detalle suma la dirección y «Abrir en el mapa». «Elegir en el mapa» abre un diálogo a pantalla completa con un pin fijo en el centro: se mueve el mapa y se confirma; arranca en el lugar ya elegido, si no en el de la obra y si no en Buenos Aires. Android declara el motor OpenGL en `androidApp` (`runtimeOnly`). iOS: banderas de enlazado en `OTHER_LDFLAGS` de `iosApp/project.yml` (iOS 15.5 o más).
- Costo: el APK de debug pasó de 22,2 a 62,2 MB porque incluye el motor nativo de tres arquitecturas (arm64 13,8 MB, armv7 9,4 MB, x86_64 14,3 MB); en un teléfono arm64 se instalan unos 14 MB. El mapa necesita red. Pendiente: verlo en un teléfono (renderizado, gestos del diálogo, crédito) y en iOS (`core:data` y `composeApp` no compilan desde Windows; las banderas de enlazado se verifican en CI o con una Mac). Diferidos: el mapa en la lista de relevamientos y la dirección en el formulario.

## Ícono de la app
- Ícono adaptativo de Android armado desde `ic_launcher-playstore.png` (512 px, copiado a `androidApp/src/main/`): fondo azul marino `#09203A` y el logo como capa de primer plano (separado del fondo con «color a alfa» y escalado al 80 % para que no lo corte el recorte circular), más la capa monocromática del ícono temático de Android 13+. Como el `minSdk` es 26 no hacen falta íconos clásicos. El anillo tenue del dibujo original no se incluye en el ícono del launcher; el PNG de Play Store sí lo conserva.
- iOS: catálogo `iosApp/Resources/Assets.xcassets/AppIcon.appiconset` con un ícono único de 1024 px (Xcode 14 o más), guardado sin canal alfa porque App Store Connect lo rechaza, y `ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon` en `project.yml`. iOS le aplica su propio recorte, así que ahí se ve el dibujo completo, con el anillo tenue.
- Pendiente: verlo en el launcher del teléfono Android (la vista previa con recortes circular y redondeado se revisó a ojo, no en el dispositivo) y en iOS (XcodeGen y el ícono no se pueden probar desde Windows; se verifica en CI o con una Mac).

## Orden del detalle y el formulario de relevamientos (rama `feat/ux-relevamiento`)
- Problema: el relevamiento no tenía pantalla propia; se abría al final de la pantalla de la obra, con el título «Detalle de la obra», los datos y botones de la obra arriba, las tareas en el medio y «Eliminar obra» abajo. Había dos «Editar» y dos «Eliminar» juntos, y los datos del relevamiento estaban sin títulos ni jerarquía.
- Ahora `RelevamientoScreen` es una pantalla propia (barra con el título «Relevamiento», «Nuevo relevamiento» o «Editar relevamiento», flecha y lápiz de editar). `RelevamientosSection` quedó solo con filtro, listado y «Nuevo relevamiento» dentro de la obra. Detalle, de lo más a lo menos importante: fecha y obra, foto, nota, tarea, ubicación (mapa, dirección, «Abrir en el mapa»), próxima visita con la hora en la misma línea, y «Eliminar» al final y separado; lo que no existe no se muestra. Formulario en tres bloques (Datos, Foto, Ubicación) con «Guardar» y «Cancelar» fijos abajo, donde también se ve el error. La obra recuerda su posición de desplazamiento al volver. Se conservaron los `testTag` de las pruebas de dispositivo (`relevamientos_back` pasó a la flecha y `relevamientos_edit` al lápiz).
- Mapa chico: el crédito de los datos pasó a una línea debajo del mapa, con los enlaces del estilo, porque la barra de la librería solo se retrae al arrastrar y tapaba una parte del mapa para siempre. El selector a pantalla completa conserva la de la librería.
- Verificación: pasan 406 pruebas de host y `assembleDebug`. Las pantallas se revisaron en el emulador con datos de ejemplo (detalle completo y mínimo, formulario con y sin error, selector de mapa, modo claro y oscuro). Falta revisarlas en el teléfono con datos reales y correr las pruebas de dispositivo, que escriben en Firebase y no se ejecutaron.
- El formulario de tareas sigue el mismo patrón: `TareaScreen` (pantalla propia, bloques Datos y Cronograma, barra fija Guardar/Cancelar). `BarraGuardar` y los bloques comunes viven en `CamposPantalla.kt`.
- Detalle de obra con pestañas Resumen | Tareas | Relevamientos (con contadores). Editar pasó a un ícono de la barra superior; la pestaña activa se recuerda por obra al volver de un formulario. `MisObrasScreen` recibe `tareasContent` y `relevamientosContent` en lugar de `detailExtra`.
- Crear tarea o relevamiento: botón flotante extendido abajo a la derecha, según la pestaña (no aparece en Resumen). Los tests instrumentados de relevamientos abren primero la pestaña (`obras_tab_relevamientos`); no se corrieron (piden Firebase).
- Fechas legibles: `fechaLegible(...)` en `core/ui` ("5 nov 2026" / "Nov 5, 2026" / "5 nov 2026"), con los meses y el orden en recursos. Solo para mostrar: se guarda y compara siempre como `LocalDate` (AAAA-MM-DD). Aplicada en detalle de obra, tareas, relevamientos, agenda y `CampoFecha`.

## Decisiones vigentes
- Login, registro y obras comparten presentación Compose Multiplatform; migrar el resto de features cuando corresponda.
- Clean Architecture: dominio define modelos, validación y puertos; data adapta DTO; Firebase queda encapsulado; UI depende del dominio.
- Compose stateless + UDF; ViewModels no contienen strings ni Context; constructor injection desde `AppGraph`.
- Mantener `obras` raíz con `userId`, `nombre`, `cliente`, `publica`, `direccion`, `fechaInicio` Timestamp y `estado` minúsculo.
- No crear `usuarios/{uid}` ni modificar/desplegar reglas. Visibilidad pública no habilita explorar ni seguir obras en este corte.
- “Hoy” se calcula con la zona local; `fechaInicio` se guarda desde medianoche UTC. La hora de visita son minutos desde medianoche.
- Sin fotos en Firebase Storage; se acordó Cloudinary. Sin FCM para avisos locales.
- Errores tipados; cancellation se relanza; no convertir escrituras fallidas en éxito.


## Revisión de alcance — 04/11/2026
- Base: `1dd21d2`. Tareas ya está integrado en main (PR #7), junto con íconos, calendario de tareas, reactivación de obras finalizadas (PR #8) y el módulo compartido `core:ui` (PR #9).
- Se revisaron dominio, repositorio, adaptador Firebase, ViewModel, conexión de pantallas y componentes compartidos. Se mantiene la inyección por constructor desde `AppGraph`; `core:ui` no depende de las features.
- Hallazgo de lectura: las operaciones suspendidas de `TareasViewModel` actualizan el estado al terminar sin comprobar que siga siendo la misma obra/sesión. Se planificó reproducirlo con operaciones demoradas y proteger el contexto completo.
- Diferencia con el nativo: la baja KMP de tarea puede borrar relevamientos sin recalcular `ultimaVisita`. Además divide en lotes lo que el nativo limita a un batch atómico. Se prioriza resolver consistencia y política de límites antes de ampliar relevamientos.
- Esta intervención actualiza planificación y seguimiento. No se ejecutaron pruebas, compilaciones ni escrituras en Firebase; los resultados de pruebas anteriores siguen siendo los registrados en cada sección.


## Consistencia de tareas
- Al guardar una tarea y cambiar de obra antes de recibir la respuesta, el guardado anterior podía cerrar el formulario nuevo. Una edición fallida podía mostrar su error al reabrir la misma obra; una operación de estado o baja anterior podía apagar la carga de otra operación o mostrar errores en otra sesión.
- `TareasViewModel` cancela los trabajos al cambiar el contexto y comprueba una revisión de la apertura antes de aplicar resultados, errores o limpieza final. El formulario tiene su propia revisión para distinguir cerrar y volver a abrirlo en la misma obra. Cancelar la espera no revierte una escritura ya enviada a Firebase.
- Al eliminar una tarea con relevamientos se recalcula `ultimaVisita` con la fecha máxima de los que quedan; si no queda ninguno con fecha, se quita el campo. Desvincular conserva los relevamientos y no modifica esa fecha.
- La tarea, sus relevamientos y la actualización de la obra se escriben en un único batch, igual que en el nativo. Se eliminó el fraccionamiento de esta baja: si requiere más de 500 escrituras, se rechaza antes de escribir, con un error tipado y un mensaje en es/en/it. El límite permite 498 vinculados al eliminar y actualizar la obra, o 499 al desvincular.
- El adaptador conserva los detalles de Firestore; `BajaTarea.kt` separa la coordinación de lecturas y plan de escritura para probarla con un almacén falso. No se modificaron reglas, esquema ni dependencias de producción.
- Regresión: cuatro casos nuevos fallaron con el código anterior. Con las correcciones pasan 217 pruebas de domain (66), data (30), firebase (37), auth (57) y obras (27), además de `:androidApp:assembleDebug`. Se agregaron siete casos de ViewModel y nueve de baja, más cobertura del mapeo del límite de escritura.
- Integración real: `BajaTareaFirebaseTest` pasó en el teléfono Android T613P con la sesión existente. Creó una obra aislada, comprobó mediante `Source.SERVER` la baja del relevamiento más reciente, la desvinculación sin perder la fecha y la eliminación del último con limpieza de `ultimaVisita`; eliminó sus registros al finalizar. La prueba tarda unos 10 segundos y requiere habilitar explícitamente `firebaseIntegration=true`.
- Para repetir la integración desde Gradle, conectar un único Android con sesión abierta y ejecutar `:androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.firebaseIntegration=true -Pandroid.testInstrumentationRunnerArguments.class=com.unaj.appmoviles.obratrack.BajaTareaFirebaseTest`. Sin el argumento, la prueba se omite para evitar escrituras involuntarias en ejecuciones generales.
- Recorrido manual en el teléfono: alta de `PRUEBA_QA_TAREAS_20261105` en la obra de QA existente, edición de título y fechas con calendario, persistencia tras reiniciar, cambio a completada con actualización del resumen y cancelación/confirmación de baja. Se eliminó únicamente la tarea creada para esta prueba; la obra de QA anterior se conserva. Se revisó visualmente el formulario en modo oscuro.
- Entorno de verificación: copia de trabajo temporal fuera del repo, con ruta sin espacios, JBR de Android Studio y compilador Kotlin en el proceso de Gradle. No se incorporaron scripts de entorno al proyecto.
- iOS no se volvió a ejecutar: el workflow `iOS Simulator` figura `disabled_manually`. Esta verificación es Android; no acredita el flujo real de Firebase en iOS ni aislamiento entre dos cuentas.


## Relevamientos: alta y listado
- Se agregó una sección en el detalle de obra para listar relevamientos y crear uno con fecha, nota y tarea opcional. La fecha usa el calendario compartido; la nota se valida y normaliza en el dominio. La lista ordena por fecha descendente y muestra el vínculo con la tarea. El detalle de obra muestra el último relevamiento.
- `core:domain` define modelo, draft, contrato y casos de uso; `core:data` mapea DTO y errores; `core:firebase` contiene el adaptador GitLive y la transacción. `RelevamientosViewModel` coordina sesión, consultas y formulario; los composables reciben estado y acciones. Las dependencias se construyen en `AppGraph`.
- Se conserva `obras/{id}/relevamientos`, `userId`, `fecha` como Timestamp, `nota` y `tareaId` opcional. La lectura contempla documentos nativos con foto, ubicación y próxima visita; este formulario crea únicamente fecha, nota y vínculo. No se cambiaron reglas ni índices de Firestore.
- La transacción comprueba el dueño de la obra y, si se eligió una tarea, su existencia y dueño dentro de esa misma obra. Guarda el relevamiento y actualiza `ultimaVisita` juntos. Usa el máximo entre la fecha nueva y la existente, para que cargar un relevamiento anterior no haga retroceder el resumen. El ID se genera fuera del bloque que Firestore puede reintentar.
- El ViewModel descarta respuestas de una obra o sesión anteriores, conserva el borrador ante un error y bloquea envíos duplicados. El selector permite guardar sin vínculo si no hay tareas o no se pudieron cargar; una tarea eliminada antes de confirmar se informa como error, sin perder la nota. Los textos están en es/en/it.
- Pasaron 245 pruebas: domain 71, data 35, firebase 44, auth 57 y obras 38. Incluyen 28 casos nuevos sobre validación, orden, mapeos nativos, errores, cancelación, vínculo, última visita, reintento y respuestas demoradas. Compilaron el APK de la app y el de instrumentación.
- `RelevamientosFirebaseTest` pasó en Android físico con Firestore real: alta con y sin tarea, lectura, rechazo de tarea inexistente sin escrituras parciales y conservación/avance de `ultimaVisita`, usando lecturas del servidor. Creó una obra aislada y limpió sus registros al terminar.
- `RelevamientosUiTest` pasó en el mismo teléfono: abrió una obra de QA, creó una nota sin tarea y otra vinculada, comprobó ambos documentos en el servidor y volvió a abrir la app para verificar el listado. Limpió la obra, tarea y relevamientos al terminar. El recorrido tarda unos 30 segundos y requiere `firebaseIntegration=true`, sesión abierta y teléfono desbloqueado. Se agregaron identificadores de controles para sincronizar las acciones; Android los expone a UI Automator sin cambiar los textos visibles.
- Para ejecutar las pruebas de dispositivo: `:androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.firebaseIntegration=true -Pandroid.testInstrumentationRunnerArguments.class=com.unaj.appmoviles.obratrack.RelevamientosFirebaseTest,com.unaj.appmoviles.obratrack.RelevamientosUiTest`. Sin el argumento de integración se omiten las escrituras de prueba.
- Se revisó visualmente el formulario y la fecha del último relevamiento en modo oscuro durante el recorrido. iOS sigue sin una ejecución nueva, por el workflow deshabilitado. Edición, baja, historial de cambios, captura de fotos y ubicación quedan para cortes posteriores; tampoco se acredita aún aislamiento con dos cuentas ni funcionamiento sin conexión.


## Cierre del ABM de relevamientos
- Código implementado: detalle, edición de fecha/nota/vínculo, baja con confirmación y filtros para todos los relevamientos, sin tarea o de una tarea. Atrás y cancelar edición regresan al detalle; la baja conserva la tarea y los demás relevamientos.
- La edición escribe únicamente `fecha`, `nota` y `tareaId` (se elimina este campo al desvincular). Conserva dueño, foto, ubicación, próxima visita, hora y cualquier campo adicional del cliente nativo. El detalle muestra los datos existentes y permite abrir una foto ya guardada mediante el visor del sistema; no incorpora captura ni carga de fotos.
- Dominio: se comparten las validaciones de alta/edición y se agregan casos de uso de actualización y baja, más un filtro puro. Data traduce DTO y errores. Firebase adapta la transacción; los ViewModels siguen recibiendo casos de uso por constructor desde `AppGraph`.
- Edición y baja verifican obra y registro propios antes de escribir y recalculan `ultimaVisita` con el conjunto restante. Al editar el más reciente hacia atrás puede retroceder esa fecha; al borrar el último se elimina el campo. No se modifican la agenda ni las reglas de Firestore.
- La consulta de fechas se hace al servidor dentro de cada intento del callback. Esa consulta no es una lectura transaccional de colección: el control de concurrencia se apoya en la obra leída por la transacción, que ambos clientes actualizan al modificar relevamientos. La prueba real de alta y edición concurrentes pasó en tres rondas sobre una obra aislada y mantuvo el máximo correcto.
- Regresiones cubiertas: listado que elimina el seleccionado antes de que confirme la baja, respuesta de una obra anterior, eliminación externa durante una edición pendiente y filtro cambiado mientras todavía carga. Se libera la operación por contexto y se invalida el formulario anterior para que no afecte uno nuevo.
- Pasan 264 pruebas unitarias: domain 74, data 36, Firebase 52, auth 57 y obras 45. `:androidApp:assembleDebug` y `:androidApp:assembleDebugAndroidTest` pasan. Se conservó la compilación desde la copia temporal sin espacios y el compilador Kotlin dentro del proceso de Gradle.
- Ejecutadas y aprobadas en teléfono Android T613P contra Firebase real: `RelevamientosAbmFirebaseTest` (preservación de campos nativos, movimientos de fecha, desvinculación, baja del último y concurrencia) y `RelevamientosAbmUiTest` (detalle, edición, Atrás, filtros, cancelación y baja hasta lista vacía). Ambas usan una obra aislada y limpian sus propios datos al finalizar; requieren la opción explícita `firebaseIntegration=true`.
- Para repetirlas: `:androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.firebaseIntegration=true -Pandroid.testInstrumentationRunnerArguments.class=com.unaj.appmoviles.obratrack.RelevamientosAbmFirebaseTest,com.unaj.appmoviles.obratrack.RelevamientosAbmUiTest`, con Android desbloqueado y sesión abierta. `RelevamientosUiTest` también volvió a pasar: alta con/sin tarea y persistencia al reabrir con las tarjetas ahora navegables.
- Se resolvió la verificación conectando el teléfono. La integración comprobó preservación de foto/ubicación/agenda/campos extra, recálculo al mover fechas, eliminación del último y concurrencia. La interfaz comprobó detalle, edición, Atrás, desvinculación, filtros, cancelación y baja hasta lista vacía. Cada prueba limpió su obra, tarea y relevamientos de QA al terminar.
- La automatización se ajustó para volver a buscar controles después de recomposiciones (referencias obsoletas) y localizar las tarjetas por su identificador al reabrir. Pasaron las dos pruebas de interfaz juntas en unos 64 segundos; la integración de Firebase pasó por separado. Los fallos iniciales de localización no se contaron como verificaciones aprobadas.
- Estado de entrega: 264 pruebas de host, compilaciones Android y tres pruebas de dispositivo aprobadas antes de integrar el PR #12. El alcance confirmado cierra el ABM; cámara/galería, carga de fotos, selección de ubicación y PDF se implementarán en sus etapas propias. iOS, aislamiento con dos cuentas y offline siguen pendientes.

## Programación de visitas desde Agenda

- Se puede programar una visita, cambiar su fecha u hora y cancelarla con confirmación. El formulario reutiliza el calendario de obras. La agenda se actualiza con la misma suscripción a obras y conserva el orden anterior.
- `ProgramarVisita` convierte el día a medianoche UTC y la hora opcional a minutos desde la medianoche. El adaptador Firestore verifica el dueño en una transacción y modifica solo `proximaVisita` y `horaVisita` en la obra. Al cancelar elimina ambos campos, igual que el nativo.
- Pasaron 412 pruebas de host y la compilación Android. Los casos nuevos cubren conversión, validación de la hora, envío duplicado, errores y cambio de sesión.
- En Android 14 con Firebase local se verificó el alta y edición de un relevamiento desde la nueva pestaña de obra, el aviso de carga de los mapas y el acceso a Agenda. En Agenda se probó programar con hora, reprogramar sin hora y cancelar, con lecturas de servidor para comprobar los campos y la conservación del resto de la obra.

## Cuenta y preferencias (rama `feat/cuenta-preferencias`)

- Recuperar contraseña desde el login (enlace "¿Olvidaste tu contraseña?" + diálogo), editar el nombre desde Perfil y elegir el tema (Sistema, Claro, Oscuro). Puertos nuevos `RepositorioCuenta` y `RepositorioPreferencias`, separados de `RepositorioAutenticacion`; casos de uso `RecuperarContrasena`, `ActualizarNombreUsuario`, `ObservarTema` y `CambiarTema`; `CuentaViewModel` y `PreferenciasViewModel` en `features:auth`. El nombre es el `displayName` de Auth: no se crea nada en Firestore. Pedir el correo para una dirección sin cuenta responde igual que si existiera, para no revelar qué correos están registrados.
- El tema se guarda con `multiplatform-settings-no-arg` 1.3.0 (SharedPreferences / NSUserDefaults). `authStateChanged` no avisa cuando cambia el perfil, así que el adaptador de Firebase emite a mano para que el nombre nuevo llegue a la sesión. En Android las barras del sistema siguen al tema elegido (`alAplicarTema` de `ObraTrackApp`).
- Pasan los tests de domain, data, firebase y `features:auth`, y `:androidApp:assembleDebug`. Falta: probarlo en un teléfono (correo real de recuperación, nombre con y sin red, tema forzado en las dos direcciones), y iOS (`core:data` y `core:firebase` no compilan desde Windows, así que `multiplatform-settings` y la barra de estado con tema forzado no están verificados). Fuera de este corte: idioma, cambiar contraseña y eliminar cuenta.
