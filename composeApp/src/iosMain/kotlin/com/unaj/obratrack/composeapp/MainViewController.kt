package com.unaj.obratrack.composeapp

import androidx.compose.ui.window.ComposeUIViewController
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

fun MainViewController(uiTesting: Boolean, selectorGoogle: SelectorCuentaGoogle): UIViewController {
    if (!uiTesting) Firebase.initialize()
    val proveedorTokenGoogle = ProveedorTokenGoogleIos(selectorGoogle)
    return ComposeUIViewController {
        ObraTrackApp(
            proveedorTokenGoogle,
            uiTesting = uiTesting,
            alAplicarIdioma = ::aplicarIdiomaIos,
            claveGemini = valorDelInfoPlist("GeminiApiKey"),
            modeloGemini = valorDelInfoPlist("GeminiModel"),
        )
    }
}

/** Valor de texto del Info.plist de la app, o vacío si no está definido. */
private fun valorDelInfoPlist(clave: String): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey(clave) as? String).orEmpty()
