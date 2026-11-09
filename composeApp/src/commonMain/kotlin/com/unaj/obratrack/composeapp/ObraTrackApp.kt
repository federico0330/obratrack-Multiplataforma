package com.unaj.obratrack.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme
import com.unaj.obratrack.domain.repository.ProveedorTokenGoogle
import com.unaj.obratrack.features.auth.AuthViewModel
import com.unaj.obratrack.features.auth.LoginScreen
import com.unaj.obratrack.features.auth.PerfilScreen
import com.unaj.obratrack.features.auth.PerfilViewModel
import com.unaj.obratrack.features.auth.RegistroScreen
import com.unaj.obratrack.features.auth.RegistroViewModel
import com.unaj.obratrack.features.auth.message
import com.unaj.obratrack.features.obras.MisObrasScreen
import com.unaj.obratrack.features.obras.ObrasViewModel
import com.unaj.obratrack.features.obras.RelevamientosAcciones
import com.unaj.obratrack.features.obras.RelevamientosSection
import com.unaj.obratrack.features.obras.RelevamientosViewModel
import com.unaj.obratrack.features.obras.TareasAcciones
import com.unaj.obratrack.features.obras.TareasSection
import com.unaj.obratrack.features.obras.TareasViewModel

/** Punto de entrada común para Android y SwiftUI. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ObraTrackApp(proveedorTokenGoogle: ProveedorTokenGoogle, uiTesting: Boolean = false) {
    if (uiTesting) {
        AccessFlowPreview()
        return
    }

    val graph = remember { AppGraph(proveedorTokenGoogle) }
    val authViewModel: AuthViewModel = viewModel { graph.crearAuthViewModel() }
    val registroViewModel: RegistroViewModel = viewModel { graph.crearRegistroViewModel() }
    val obrasViewModel: ObrasViewModel = viewModel { graph.crearObrasViewModel() }
    val tareasViewModel: TareasViewModel = viewModel { graph.crearTareasViewModel() }
    val relevamientosViewModel: RelevamientosViewModel = viewModel { graph.crearRelevamientosViewModel() }
    val perfilViewModel: PerfilViewModel = viewModel { graph.crearPerfilViewModel() }
    val authState by authViewModel.state.collectAsState()
    val registroState by registroViewModel.state.collectAsState()
    val obrasState by obrasViewModel.state.collectAsState()
    val tareasState by tareasViewModel.state.collectAsState()
    val relevamientosState by relevamientosViewModel.state.collectAsState()
    var enRegistro by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(authState.user) {
        if (authState.user != null) enRegistro = false
    }

    // Ambas secciones siguen a la obra abierta en el detalle.
    val obraAbiertaId = obrasState.obraSeleccionada?.id
    LaunchedEffect(obraAbiertaId) {
        if (obraAbiertaId != null) {
            tareasViewModel.abrirObra(obraAbiertaId)
            relevamientosViewModel.abrirObra(obraAbiertaId)
        } else {
            tareasViewModel.cerrarObra()
            relevamientosViewModel.cerrarObra()
        }
    }

    ObraTrackTheme {
        Surface(Modifier.fillMaxSize()) {
            when {
                !authState.sessionChecked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                authState.user != null -> {
                    var pestana by rememberSaveable(stateSaver = PestanaPrincipalSaver) {
                        mutableStateOf(PestanaPrincipal.OBRAS)
                    }
                    // La barra solo aparece en las raíces: lista de obras y perfil, no en detalle ni formulario.
                    val enRaiz = pestana == PestanaPrincipal.PERFIL ||
                        (obrasState.obraSeleccionada == null && !obrasState.formularioVisible)
                    BackHandler(enabled = pestana == PestanaPrincipal.PERFIL) { pestana = PestanaPrincipal.OBRAS }
                    Scaffold(
                        bottomBar = { if (enRaiz) BarraInferior(pestana) { pestana = it } },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { padding ->
                        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                            when (pestana) {
                                PestanaPrincipal.OBRAS -> MisObrasScreen(
                                    user = authState.user!!,
                                    state = obrasState,
                                    onCreate = obrasViewModel::abrirFormulario,
                                    onRetry = obrasViewModel::reintentar,
                                    onSelectObra = obrasViewModel::abrirDetalle,
                                    onBackToList = {
                                        when {
                                            relevamientosState.formularioVisible -> relevamientosViewModel.cerrarFormulario()
                                            relevamientosState.seleccionado != null -> relevamientosViewModel.cerrarDetalle()
                                            else -> obrasViewModel.cerrarDetalle()
                                        }
                                    },
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
                                    detailExtra = {
                                        TareasSection(
                                            state = tareasState,
                                            acciones = TareasAcciones(
                                                onCreate = tareasViewModel::abrirFormulario,
                                                onRetry = tareasViewModel::reintentar,
                                                onEdit = tareasViewModel::editar,
                                                onChangeState = tareasViewModel::cambiarEstado,
                                                onRequestDelete = tareasViewModel::solicitarEliminacion,
                                                onCancelDelete = tareasViewModel::cancelarEliminacion,
                                                onConfirmDelete = tareasViewModel::confirmarEliminacion,
                                                onLinkedChange = tareasViewModel::vinculadosCambiados,
                                                onCloseForm = tareasViewModel::cerrarFormulario,
                                                onTitleChange = tareasViewModel::tituloCambiado,
                                                onDescriptionChange = tareasViewModel::descripcionCambiada,
                                                onCategoryChange = tareasViewModel::categoriaCambiada,
                                                onStartChange = tareasViewModel::fechaInicioCambiada,
                                                onEndChange = tareasViewModel::fechaFinCambiada,
                                                onSave = tareasViewModel::guardar,
                                            ),
                                        )
                                        RelevamientosSection(
                                            state = relevamientosState,
                                            acciones = RelevamientosAcciones(
                                                onCreate = relevamientosViewModel::abrirFormulario,
                                                onRetry = relevamientosViewModel::reintentar,
                                                onRetryTasks = relevamientosViewModel::reintentarTareas,
                                                onCloseForm = relevamientosViewModel::cerrarFormulario,
                                                onNoteChange = relevamientosViewModel::notaCambiada,
                                                onDateChange = relevamientosViewModel::fechaCambiada,
                                                onTaskChange = relevamientosViewModel::tareaCambiada,
                                                onSave = relevamientosViewModel::guardar,
                                                onSelect = relevamientosViewModel::abrirDetalle,
                                                onEdit = relevamientosViewModel::editarSeleccionado,
                                                onBack = relevamientosViewModel::cerrarDetalle,
                                                onRequestDelete = relevamientosViewModel::solicitarEliminacion,
                                                onCancelDelete = relevamientosViewModel::cancelarEliminacion,
                                                onConfirmDelete = relevamientosViewModel::confirmarEliminacion,
                                                onFilterChange = relevamientosViewModel::filtroCambiado,
                                                onPhotoPicked = relevamientosViewModel::fotoElegida,
                                                onRemovePhoto = relevamientosViewModel::quitarFoto,
                                                onPhotoReadError = relevamientosViewModel::fotoNoLeida,
                                                onCameraError = relevamientosViewModel::camaraNoDisponible,
                                                onLocationCapture = relevamientosViewModel::capturarUbicacion,
                                                onLocationCancel = relevamientosViewModel::cancelarCaptura,
                                                onLocationRemove = relevamientosViewModel::quitarUbicacion,
                                                onAddressSearchOpen = relevamientosViewModel::abrirBusquedaDireccion,
                                                onAddressSearchClose = relevamientosViewModel::cerrarBusquedaDireccion,
                                                onAddressTextChange = relevamientosViewModel::textoDireccionCambiado,
                                                onAddressSearch = relevamientosViewModel::buscarDireccion,
                                                onAddressPick = relevamientosViewModel::elegirDireccion,
                                            ),
                                        )
                                    },
                                )
                                PestanaPrincipal.PERFIL -> {
                                    // Se recolecta solo aquí: la consulta de obras corre mientras Perfil está visible.
                                    val resumen by perfilViewModel.state.collectAsState()
                                    PerfilScreen(
                                        user = authState.user!!,
                                        resumen = resumen,
                                        errorMessage = authState.error?.message(),
                                        onRetry = perfilViewModel::reintentar,
                                        onSignOut = authViewModel::signOut,
                                    )
                                }
                            }
                        }
                    }
                }
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
                    onSignInWithGoogle = authViewModel::signInWithGoogle,
                    onGoToRegister = { enRegistro = true },
                )
            }
        }
    }
}
