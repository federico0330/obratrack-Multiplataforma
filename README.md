# ObraTrack Multiplataforma

Aplicación de seguimiento de obras desarrollada por Federico Oviedo y Sabrina Russell con Kotlin Multiplatform y Compose Multiplatform. Comparte las pantallas y la lógica entre Android e iOS, y utiliza el mismo Firebase que ObraTrack nativo.

## Avance del trabajo

- [x] Inicializamos el proyecto por módulos y separamos dominio, datos, Firebase y presentación.
- [x] Incorporamos acceso por correo y contraseña, registro, sesión e inicio con Google.
- [x] Implementamos alta, listado, detalle, edición, estados y eliminación de obras propias.
- [x] Compartimos las pantallas con iOS y agregamos su aplicación anfitriona en SwiftUI.
- [x] Incorporamos el ABM de tareas, categorías, fechas, estados y resumen de atrasadas.
- [x] Incorporamos el ABM de relevamientos, vínculo opcional con tareas, filtros y actualización de la última visita.
- [x] Sumamos fotos desde cámara y galería, compresión, carga a Cloudinary y miniaturas.
- [x] Sumamos ubicación, búsqueda de direcciones y selección de un punto en el mapa.
- [x] Organizamos el detalle de obra en pestañas, con formularios propios para tareas y relevamientos.
- [x] Implementamos Agenda con visitas agrupadas y opciones para programar, reprogramar y cancelar, con hora opcional.
- [x] Agregamos perfil con resumen de obras, tema claro/oscuro del sistema y textos en español, inglés e italiano.
- [x] Incorporamos pruebas de reglas de negocio, repositorios y ViewModels, y recorridos de integración e interfaz en Android.

## Cómo ejecutarlo

Clonar el repositorio y abrir su carpeta raíz. Para probar el acceso y los datos hacen falta los archivos del Firebase compartido, que se entregan por separado y quedan fuera de Git.

| Plataforma | Archivo y ubicación |
| --- | --- |
| Android | `androidApp/google-services.json` |
| iOS | `iosApp/GoogleService-Info.plist` |

Crear una cuenta desde **Crear cuenta** o utilizar una cuenta de prueba del proyecto. El teléfono o simulador necesita conexión para autenticar, guardar cambios, subir fotos y cargar mapas. Google requiere además la configuración OAuth correspondiente a la plataforma. En Android, el SHA de la firma usada para compilar debe estar registrado en Firebase. Para el recorrido de evaluación se puede usar correo y contraseña.

### Android Studio, en emulador o teléfono

1. Abrir la raíz del proyecto con **File → Open** y esperar la sincronización de Gradle.
2. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, seleccionar un **Gradle JDK 21**. En **Tools → SDK Manager**, instalar Android SDK Platform 37 si Android Studio lo solicita.
3. Colocar `google-services.json` en `androidApp/` y sincronizar nuevamente.
4. Elegir el módulo **androidApp** como configuración de ejecución.
5. Elegir dónde probar y pulsar **Run ▶**.

| Destino | Preparación |
| --- | --- |
| Emulador | En **Tools → Device Manager**, crear e iniciar un dispositivo virtual con Android 8.0/API 26 o superior. Para el recorrido recomendamos Android 14 o 15. Seleccionarlo en la barra de ejecución. |
| Teléfono Android | Activar **Opciones de desarrollador → Depuración USB**, conectarlo por USB, aceptar la autorización de la PC y seleccionarlo en la barra de ejecución. |

La ubicación se puede simular desde los controles del emulador. En un teléfono se usan su GPS, cámara y galería. Referencia para conectar un equipo físico en la [documentación de Android Studio](https://developer.android.com/studio/run/device).

### iOS, desde una Mac

Se necesita Xcode con las herramientas de línea de comandos y el runtime de iOS, JDK 21, CocoaPods y XcodeGen. La app requiere iOS 16 o superior. El simulador configurado en Gradle es `iosSimulatorArm64`, para Mac con Apple Silicon. El destino de dispositivo físico es `iosArm64`.

1. Colocar el `GoogleService-Info.plist` real en `iosApp/`.
2. Abrir la terminal en la raíz del repositorio y preparar el proyecto.

```bash
brew install cocoapods xcodegen
java -version
bash ./gradlew :composeApp:podspec :composeApp:generateDummyFramework
cd iosApp
xcodegen generate --spec project.yml
pod install
open ObraTrackIOS.xcworkspace
```

`java -version` debe mostrar JDK 21. El workspace se genera con estos pasos. Abrir `ObraTrackIOS.xcworkspace`, seleccionar el esquema **ObraTrackIOS** y elegir el destino.

| Destino | Preparación |
| --- | --- |
| Simulador iOS | Seleccionar un iPhone virtual disponible en Xcode y pulsar **Run ▶**. |
| iPhone físico | Conectarlo a la Mac, aceptar la confianza y habilitar **Developer Mode** si se solicita. En **Signing & Capabilities**, seleccionar el equipo de desarrollo y habilitar la firma automática. Elegir el iPhone y pulsar **Run ▶**. |

Conservar el bundle ID `com.unaj.appmoviles.obratrack.ios`, que coincide con Firebase. Si la firma requiere otro identificador, hay que registrar esa aplicación en Firebase y utilizar su configuración. Para Google, el plist debe incluir `CLIENT_ID` y `REVERSED_CLIENT_ID`. El proyecto copia este último al URL scheme durante la compilación.

La integración del framework Kotlin se realiza mediante CocoaPods. El archivo `GoogleService-Info.plist.example` se utiliza en la prueba visual de acceso y tiene datos ficticios. Referencias de [integración CocoaPods](https://kotlinlang.org/docs/multiplatform/multiplatform-cocoapods-xcode.html) y [ejecución desde Xcode](https://developer.apple.com/documentation/xcode/running-your-app-on-simulated-or-physical-devices).

## Recorrido para evaluar el avance

La cuenta de demostración tiene tres obras ficticias. **Casa Los Aromos** incluye tareas, relevamientos con foto y ubicación, y una próxima visita. **Local San Martín** permite revisar una tarea atrasada y otro avance con foto. **Ampliación Los Tilos** queda sin próxima visita programada. Los datos y las imágenes son ejemplos para la evaluación. Las credenciales se entregan por privado.

1. **Acceso.** Crear una cuenta o iniciar sesión con correo y contraseña. Comprobar que al volver a abrir la aplicación se conserva la sesión.
2. **Obras.** Crear una obra con nombre, cliente, dirección y fecha. Abrir su detalle, editarla, pausarla y reactivarla. La visibilidad se guarda, aunque la exploración de obras públicas todavía está pendiente.
3. **Tareas.** Entrar en la pestaña **Tareas**, crear una, elegir su categoría y opcionalmente ambas fechas. Editarla y pasarla de pendiente a en curso y completada. Revisar el resumen de la lista.
4. **Relevamientos.** Entrar en **Relevamientos**, cargar fecha y nota, y vincular una tarea si corresponde. Agregar una foto de cámara o galería y una ubicación mediante GPS, dirección o mapa. Guardar, abrir el detalle, editar y probar los filtros por tarea.
5. **Agenda.** Volver al listado principal y abrir **Agenda**. Programar una visita, indicar una hora opcional y guardarla. Verificar su grupo, reprogramarla y cancelar la visita con confirmación.
6. **Perfil.** Revisar la cuenta y el resumen de obras. Cerrar sesión y volver a ingresar para comprobar la persistencia de los datos.
7. **Bajas.** Usar los registros de prueba para comprobar la cancelación y confirmación de cada baja. Al eliminar una tarea se puede conservar sus relevamientos sin vínculo o eliminarlos también. Al eliminar una obra se borran sus registros relacionados.

## Arquitectura y comparación con el nativo

| Módulo | Responsabilidad |
| --- | --- |
| `core/domain` | Modelos, reglas, casos de uso y contratos. |
| `core/data` | Repositorios, mapeos y servicios de fotos y ubicación. |
| `core/firebase` | Adaptadores de autenticación y Firestore. |
| `core/ui` | Componentes visuales reutilizables. |
| `features/auth`, `features/obras` | Pantallas, estado y ViewModels. |
| `composeApp` | Navegación, tema y dependencias construidas en `AppGraph`. |
| `androidApp`, `iosApp` | Entradas y configuración de cada plataforma. |

Se mantuvieron Clean Architecture, contratos e inyección por constructor. Las pantallas reciben estado y acciones, los ViewModels llaman a los casos de uso y los repositorios conectan el dominio con los adaptadores.

Respecto de ObraTrack nativo, el cambio principal está en compartir modelos, reglas, casos de uso, ViewModels y pantallas en `commonMain`. Las integraciones específicas se resuelven en cada plataforma. Se conserva el formato de documentos de Firestore para que ambas aplicaciones puedan utilizar los mismos datos.

## Verificación y pendientes

La revisión del 10/11 incluye 413 pruebas de host, compilación Android y cuatro recorridos aprobados en un teléfono Android 15 con Firebase local. También se cargó y abrió la cuenta de demostración en Firebase real, con las fotografías subidas a Cloudinary. El detalle queda en `docs/PROGRESO.md`.

El host iOS y la navegación inicial entre login y registro se ejecutaron anteriormente en el simulador del workflow macOS, con configuración ficticia. La integración real de Firebase y las funciones posteriores requieren una nueva ejecución en iOS. El workflow está deshabilitado manualmente.

- [ ] Completar la verificación de acceso, datos, cámara, ubicación y mapas en iOS.
- [ ] Incorporar cronograma Gantt, avisos de visitas y pronóstico.
- [ ] Incorporar exploración de obras públicas y seguimiento.
- [ ] Completar gestión de cuenta y preferencias persistentes.
- [ ] Completar operación sin conexión y reintentos de fotos.
- [ ] Incorporar informes PDF y compartir archivos.
