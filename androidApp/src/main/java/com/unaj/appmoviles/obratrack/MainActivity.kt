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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.obratrack.features.auth.AuthUiState
import com.unaj.obratrack.features.auth.AuthViewModel
import com.unaj.obratrack.features.auth.LoginScreen
import com.unaj.obratrack.features.auth.RegistroScreen
import com.unaj.obratrack.features.auth.RegistroUiState
import com.unaj.obratrack.features.auth.RegistroViewModel
import com.unaj.obratrack.features.auth.message
import com.unaj.obratrack.features.obras.MisObrasScreen
import com.unaj.obratrack.features.obras.ObrasViewModel
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        (application as ObraTrackApplication).container.authViewModelFactory
    }

    private val registroViewModel: RegistroViewModel by viewModels {
        (application as ObraTrackApplication).container.registroViewModelFactory
    }

    private val obrasViewModel: ObrasViewModel by viewModels {
        (application as ObraTrackApplication).container.obrasViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by authViewModel.state.collectAsStateWithLifecycle()
            val registroState by registroViewModel.state.collectAsStateWithLifecycle()
            val obrasState by obrasViewModel.state.collectAsStateWithLifecycle()
            ObraTrackRoot(state, authViewModel, registroState, registroViewModel, obrasState, obrasViewModel)
        }
    }
}

/** Navegación inicial pequeña; cada destino delega la presentación a su feature. */
@Composable
private fun ObraTrackRoot(
    state: AuthUiState,
    viewModel: AuthViewModel,
    registroState: RegistroUiState,
    registroViewModel: RegistroViewModel,
    obrasState: com.unaj.obratrack.features.obras.ObrasUiState,
    obrasViewModel: ObrasViewModel,
) {
    val currentUser = state.user
    var enRegistro by rememberSaveable { mutableStateOf(false) }
    // Con sesión iniciada se muestra Home; al cerrarla se vuelve a Login, no a Registro.
    LaunchedEffect(currentUser) { if (currentUser != null) enRegistro = false }
    ObraTrackTheme {
        Surface(Modifier.fillMaxSize()) {
            when {
                !state.sessionChecked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                currentUser != null -> MisObrasScreen(
                    user = currentUser,
                    state = obrasState,
                    authErrorMessage = state.error?.message(),
                    onCreate = obrasViewModel::abrirFormulario,
                    onRetry = obrasViewModel::reintentar,
                    onSignOut = viewModel::signOut,
                    onSelectObra = obrasViewModel::abrirDetalle,
                    onBackToList = obrasViewModel::cerrarDetalle,
                    onEditObra = obrasViewModel::editarSeleccionada,
                    onChangeStatus = obrasViewModel::cambiarEstado,
                    onRequestDelete = obrasViewModel::solicitarEliminacion,
                    onCancelDelete = obrasViewModel::cancelarEliminacion,
                    onConfirmDelete = obrasViewModel::confirmarEliminacion,
                    onCloseForm = obrasViewModel::cerrarFormulario,
                    onNameChange = obrasViewModel::nombreCambiado,
                    onClientChange = obrasViewModel::clienteCambiado,
                    onAddressChange = obrasViewModel::direccionCambiada,
                    onDateChange = obrasViewModel::fechaCambiada,
                    onPublicChange = obrasViewModel::visibilidadCambiada,
                    onSave = obrasViewModel::guardar,
                )
                enRegistro -> RegistroScreen(
                    state = registroState,
                    onEmailChanged = registroViewModel::onEmailChanged,
                    onPasswordChanged = registroViewModel::onPasswordChanged,
                    onRepeatPasswordChanged = registroViewModel::onRepeatPasswordChanged,
                    onSubmit = registroViewModel::submit,
                    onGoToLogin = { enRegistro = false },
                )
                else -> LoginScreen(
                    state = state,
                    onEmailChanged = viewModel::onEmailChanged,
                    onPasswordChanged = viewModel::onPasswordChanged,
                    onSignIn = viewModel::signIn,
                    onGoToRegister = { enRegistro = true },
                )
            }
        }
    }
}
