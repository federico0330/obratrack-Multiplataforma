# App iOS

El target `ObraTrackIOS` aloja la interfaz Compose compartida. El bundle ID es `com.unaj.appmoviles.obratrack.ios`.

Para compilar la app, colocá `GoogleService-Info.plist` en esta carpeta. El archivo está ignorado por Git. En Firebase, registrá el bundle ID indicado como una app iOS dentro del mismo proyecto usado por Android y descargá su configuración. Para pruebas de interfaz, `GoogleService-Info.plist.example` contiene valores ficticios: permite abrir la app y recorrer login/registro, pero no iniciar sesión ni guardar datos.

Para "Continuar con Google", el `GoogleService-Info.plist` tiene que descargarse después de habilitar Google en Firebase Authentication, así trae `CLIENT_ID` y `REVERSED_CLIENT_ID`. La app lee `CLIENT_ID` en tiempo de ejecución y un script de compilación (definido en `project.yml`) registra `REVERSED_CLIENT_ID` como URL scheme; no hay que cargar nada a mano en Xcode. Después de cambiar el `Podfile`, corré `pod install`.

No hace falta un iPhone: en una Mac, abrí `ObraTrackIOS.xcworkspace` y ejecutá `ObraTrackIOS` con un simulador. Desde Windows, el workflow de GitHub Actions compila y recorre login/registro en un simulador alojado en macOS.
