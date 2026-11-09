package com.unaj.obratrack.composeapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme
import com.unaj.obratrack.features.auth.AuthUiState
import com.unaj.obratrack.features.auth.LoginScreen
import com.unaj.obratrack.features.auth.RegistroScreen
import com.unaj.obratrack.features.auth.RegistroUiState

/** Presentación sin Firebase para pruebas de navegación de la interfaz compartida. */
@Composable
fun AccessFlowPreview() {
    var enRegistro by rememberSaveable { mutableStateOf(false) }

    ObraTrackTheme {
        Surface(Modifier.fillMaxSize()) {
            if (enRegistro) {
                RegistroScreen(
                    state = RegistroUiState(),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onRepeatPasswordChanged = {},
                    onSubmit = {},
                    onGoToLogin = { enRegistro = false },
                )
            } else {
                LoginScreen(
                    state = AuthUiState(sessionChecked = true),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignIn = {},
                    onSignInWithGoogle = {},
                    onGoToRegister = { enRegistro = true },
                )
            }
        }
    }
}
