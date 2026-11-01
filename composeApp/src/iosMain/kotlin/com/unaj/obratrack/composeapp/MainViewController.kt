package com.unaj.obratrack.composeapp

import androidx.compose.ui.window.ComposeUIViewController
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import platform.UIKit.UIViewController

fun MainViewController(uiTesting: Boolean): UIViewController {
    if (!uiTesting) Firebase.initialize()
    return ComposeUIViewController { ObraTrackApp(uiTesting = uiTesting) }
}
