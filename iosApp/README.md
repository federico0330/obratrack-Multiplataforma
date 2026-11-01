# App iOS

El target `ObraTrackIOS` aloja la interfaz Compose compartida. El bundle ID es `com.unaj.appmoviles.obratrack.ios`.

Para compilar la app, colocá `GoogleService-Info.plist` en esta carpeta. El archivo está ignorado por Git. En Firebase, registrá el bundle ID indicado como una app iOS dentro del mismo proyecto usado por Android y descargá su configuración. Para pruebas de interfaz, `GoogleService-Info.plist.example` contiene valores ficticios: permite abrir la app y recorrer login/registro, pero no iniciar sesión ni guardar datos.

No hace falta un iPhone: en una Mac, abrí `iosApp/iosApp.xcworkspace` y ejecutá `ObraTrackIOS` con un simulador. Desde Windows, el workflow de GitHub Actions compila y recorre login/registro en un simulador alojado en macOS.
