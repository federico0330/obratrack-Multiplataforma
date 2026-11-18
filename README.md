# ObraTrack Multiplataforma

Aplicación de seguimiento de obras desarrollada por Federico Oviedo y Sabrina Russell con Kotlin Multiplatform y Compose Multiplatform. Comparte las pantallas y la lógica entre Android e iOS, y utiliza el mismo Firebase que ObraTrack nativo.

## Lo que incluye la app

- [x] Acceso por correo y contraseña, registro, recuperación de contraseña e inicio con Google, con sesión persistente.
- [x] Obras propias: alta, listado, detalle en pestañas, edición, cambio de estado (activa, pausada, finalizada) y baja con sus datos relacionados. Cliente y dirección opcionales.
- [x] Tareas: alta, edición, cambio de estado y baja eligiendo si los relevamientos vinculados se desvinculan o se eliminan; fechas, categorías y resumen de atrasadas.
- [x] Sugerencias de tareas por tipo de obra desde plantillas y, con clave de Gemini, desde una descripción; se ofrecen al crear la obra y desde su pestaña de tareas.
- [x] Relevamientos: alta, edición, baja y filtros, con foto de cámara o galería subida a Cloudinary y ubicación por GPS, búsqueda de direcciones o punto en el mapa.
- [x] Agenda de visitas: agrupadas en vencidas, hoy, esta semana, próximas y sin programar; programar, reprogramar y cancelar, con hora opcional.
- [x] Avisos locales de visitas: el dispositivo muestra un recordatorio a la fecha y hora programadas aunque la app esté cerrada; el permiso de notificaciones se pide al programar una visita y denegarlo no afecta el resto de la app.
- [x] Perfil con resumen de obras y cuenta, tema claro/oscuro e idioma en español, inglés o italiano, preferencias que se conservan entre sesiones.
- [x] 519 pruebas de host (dominio, datos, Firebase, autenticación y pantallas), recorridos de integración e interfaz en un teléfono Android con Firebase real, y el recorrido de acceso en iOS verificado en el workflow «iOS Simulator» de GitHub Actions.

## Cómo ejecutarlo

Clonar el repositorio y abrir su carpeta raíz. Para probar el acceso y los datos hacen falta los archivos del Firebase compartido, que se entregan por separado y quedan fuera de Git.

| Plataforma | Archivo y ubicación |
| --- | --- |
| Android | `androidApp/google-services.json` |
| iOS | `iosApp/GoogleService-Info.plist` |

Crear una cuenta desde **Crear cuenta** o utilizar la cuenta de demostración (credenciales entregadas por privado). El teléfono o simulador necesita conexión para autenticar, guardar cambios, subir fotos, cargar mapas y recibir los avisos. En Android, el SHA de la firma usada para compilar debe estar registrado en Firebase; para el recorrido de evaluación alcanza con correo y contraseña.

### Android Studio, en emulador o teléfono

1. Abrir la raíz del proyecto con **File → Open** y esperar la sincronización de Gradle.
2. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, seleccionar un **Gradle JDK 21**. En **Tools → SDK Manager**, instalar Android SDK Platform 37 si Android Studio lo solicita.
3. Colocar `google-services.json` en `androidApp/` y sincronizar nuevamente.
4. Elegir el módulo **androidApp** como configuración de ejecución.
5. Elegir dónde probar y pulsar **Run ▶**.

| Destino | Preparación |
| --- | --- |
| Emulador | En **Tools → Device Manager**, crear e iniciar un dispositivo virtual con Android 8.0/API 26 o superior. Para el recorrido recomendamos Android 14 o 15, que permite probar también el permiso de notificaciones. Seleccionarlo en la barra de ejecución. |
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

Conservar el bundle ID `com.unaj.appmoviles.obratrack.ios`, que coincide con Firebase. Si la firma requiere otro identificador, hay que registrar esa aplicación en Firebase y utilizar su configuración. Para Google, el plist debe incluir `CLIENT_ID` y `REVERSED_CLIENT_ID`. El proyecto copia este último al URL scheme durante la compilación. La integración del framework Kotlin se realiza mediante CocoaPods, y el archivo `GoogleService-Info.plist.example` se usa para la prueba visual de acceso con datos ficticios. Referencias de [integración CocoaPods](https://kotlinlang.org/docs/multiplatform/multiplatform-cocoapods-xcode.html) y [ejecución desde Xcode](https://developer.apple.com/documentation/xcode/running-your-app-on-simulated-or-physical-devices).

El recorrido completo con Firebase real está verificado en Android. En iOS, el workflow «iOS Simulator» de GitHub Actions compila la app en cada cambio y verifica el recorrido de acceso (login → registro → login) en el simulador; el resto del flujo con datos reales se puede probar con el plist real siguiendo estos pasos.

## Recorrido sugerido para evaluar

La cuenta de demostración tiene tres obras ficticias. **Casa Los Aromos** incluye tareas, relevamientos con foto y ubicación, y una próxima visita. **Local San Martín** permite revisar una tarea atrasada y otro avance con foto. **Ampliación Los Tilos** queda sin próxima visita programada, ideal para probar el aviso. Los datos y las imágenes son ejemplos para la evaluación. Las credenciales se entregan por privado.

1. **Acceso.** Crear una cuenta o iniciar sesión con correo y contraseña. Comprobar que al volver a abrir la aplicación se conserva la sesión.
2. **Obras.** Crear una obra con nombre, cliente y dirección opcionales, y fecha. Al guardar se ofrece **Sugerir tareas**: elegir el tipo de obra, marcar las sugeridas (plantillas o, con clave configurada, IA) y crearlas, u **Omitir**.
3. **Tareas.** Entrar en la pestaña **Tareas** de una obra, crear una tarea con categoría y fechas opcionales, editarla, pasarla de pendiente a en curso y completada, y revisar el resumen de atrasadas. Desde ahí también se pueden sugerir tareas para una obra ya creada.
4. **Relevamientos.** Entrar en **Relevamientos**, cargar fecha y nota, y vincular una tarea si corresponde. Agregar una foto de cámara o galería y una ubicación mediante GPS, búsqueda de dirección o punto en el mapa. Guardar, abrir el detalle, editar y probar los filtros por tarea.
5. **Agenda y avisos.** Volver al listado principal y abrir **Agenda**. Tocar **Programar visita** en una obra sin fecha, elegir hoy y una hora a un par de minutos, y guardar: Android pide permitir las notificaciones, aceptar. Cerrar la app y esperar la hora programada: llega el recordatorio con el nombre de la obra, la fecha y la hora; al tocarlo se abre la app. Reprogramar la visita a otra hora (el aviso anterior se reemplaza) y luego cancelar la visita con confirmación.
6. **Perfil.** Revisar la cuenta y el resumen de obras, cambiar el tema y el idioma, y comprobar que las preferencias se conservan. Cerrar sesión y volver a ingresar.
7. **Bajas.** Usar los registros de prueba para comprobar la cancelación y confirmación de cada baja. Al eliminar una tarea se puede conservar sus relevamientos sin vínculo o eliminarlos también. Al eliminar una obra se borran sus registros relacionados y queda sin aviso pendiente.

## Estructura del proyecto

| Módulo | Responsabilidad |
| --- | --- |
| `core/domain` | Modelos, reglas, casos de uso y contratos. |
| `core/data` | Repositorios, mapeos y servicios de fotos, avisos y ubicación. |
| `core/firebase` | Adaptadores de autenticación y Firestore. |
| `core/ui` | Componentes visuales reutilizables. |
| `features/auth`, `features/obras` | Pantallas, estado y ViewModels. |
| `composeApp` | Navegación, tema y dependencias construidas en `AppGraph`. |
| `androidApp`, `iosApp` | Entradas y configuración de cada plataforma. |

Las pantallas y la lógica viven en `commonMain` compartido; las integraciones específicas (tokens de Google, avisos locales, fotos y ubicación) se resuelven en cada plataforma. El detalle del trabajo paso a paso queda en `docs/PROGRESO.md`.
