# Progreso de ObraTrack Multiplataforma

## Hecho y verificado
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
1. Verificar manualmente el aislamiento con dos cuentas y las reglas publicadas, sin modificarlas.
2. Probar login y lectura de obras reales en el simulador de iOS (la app ya está registrada en Firebase; falta una Mac o el CI con el plist como secreto).
3. Después seguir con relevamientos y tareas; dejar offline para el final.

## Interfaz compartida y host iOS (rama `codex/ios-access-obras`)
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

## Decisiones vigentes
- Login, registro y obras comparten presentación Compose Multiplatform; migrar el resto de features cuando corresponda.
- Clean Architecture: dominio define modelos, validación y puertos; data adapta DTO; Firebase queda encapsulado; UI depende del dominio.
- Compose stateless + UDF; ViewModels no contienen strings ni Context; constructor injection desde `AppContainer`.
- Mantener `obras` raíz con `userId`, `nombre`, `cliente`, `publica`, `direccion`, `fechaInicio` Timestamp y `estado` minúsculo.
- No crear `usuarios/{uid}` ni modificar/desplegar reglas. Visibilidad pública no habilita explorar ni seguir obras en este corte.
- “Hoy” se calcula con la zona local; `fechaInicio` se guarda desde medianoche UTC. La hora de visita son minutos desde medianoche.
- Sin fotos en Firebase Storage; se acordó Cloudinary. Sin FCM para avisos locales.
- Errores tipados; cancellation se relanza; no convertir escrituras fallidas en éxito.
