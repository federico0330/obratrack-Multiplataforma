package com.unaj.appmoviles.obratrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.obratrack.features.auth.AuthUiState
import com.unaj.obratrack.features.auth.AuthViewModel
import com.unaj.obratrack.features.auth.LoginScreen
import com.unaj.obratrack.features.home.HomeScreen
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        (application as ObraTrackApplication).container.authViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by authViewModel.state.collectAsStateWithLifecycle()
            ObraTrackRoot(state, authViewModel)
        }
    }
}

/** Navegación inicial pequeña; cada destino delega la presentación a su feature. */
@Composable
private fun ObraTrackRoot(state: AuthUiState, viewModel: AuthViewModel) {
    ObraTrackTheme {
        Surface(Modifier.fillMaxSize()) {
            when {
                !state.sessionChecked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.user != null -> HomeScreen(state.user, viewModel::signOut)
                else -> LoginScreen(
                    state = state,
                    onEmailChanged = viewModel::onEmailChanged,
                    onPasswordChanged = viewModel::onPasswordChanged,
                    onSignIn = viewModel::signIn,
                )
            }
        }
    }
}
