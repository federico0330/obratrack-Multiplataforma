package com.unaj.obratrack.composeapp

import androidx.compose.ui.window.ComposeUIViewController
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import platform.UIKit.UIViewController

fun MainViewController(uiTesting: Boolean, selectorGoogle: SelectorCuentaGoogle): UIViewController {
    if (!uiTesting) Firebase.initialize()
    val proveedorTokenGoogle = ProveedorTokenGoogleIos(selectorGoogle)
    return ComposeUIViewController { ObraTrackApp(proveedorTokenGoogle, uiTesting = uiTesting) }
}
