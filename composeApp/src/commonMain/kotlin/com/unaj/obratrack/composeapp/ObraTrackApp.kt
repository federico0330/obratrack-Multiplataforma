package com.unaj.obratrack.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme
import com.unaj.obratrack.features.auth.AuthViewModel
import com.unaj.obratrack.features.auth.LoginScreen
import com.unaj.obratrack.features.auth.RegistroScreen
import com.unaj.obratrack.features.auth.RegistroViewModel
import com.unaj.obratrack.features.auth.message
import com.unaj.obratrack.features.obras.MisObrasScreen
import com.unaj.obratrack.features.obras.ObrasViewModel

/** Punto de entrada común para Android y SwiftUI. */
@Composable
fun ObraTrackApp() {
    val graph = remember { AppGraph() }
    val authViewModel: AuthViewModel = viewModel { graph.crearAuthViewModel() }
    val registroViewModel: RegistroViewModel = viewModel { graph.crearRegistroViewModel() }
    val obrasViewModel: ObrasViewModel = viewModel { graph.crearObrasViewModel() }
    val authState by authViewModel.state.collectAsState()
    val registroState by registroViewModel.state.collectAsState()
    val obrasState by obrasViewModel.state.collectAsState()
    var enRegistro by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(authState.user) {
        if (authState.user != null) enRegistro = false
    }

    ObraTrackTheme {
        Surface(Modifier.fillMaxSize()) {
            when {
                !authState.sessionChecked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                authState.user != null -> MisObrasScreen(
                    user = authState.user!!,
                    state = obrasState,
                    authErrorMessage = authState.error?.message(),
                    onCreate = obrasViewModel::abrirFormulario,
                    onRetry = obrasViewModel::reintentar,
                    onSignOut = authViewModel::signOut,
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
                    state = authState,
                    onEmailChanged = authViewModel::onEmailChanged,
                    onPasswordChanged = authViewModel::onPasswordChanged,
                    onSignIn = authViewModel::signIn,
                    onGoToRegister = { enRegistro = true },
                )
            }
        }
    }
}
