package com.unaj.obratrack.composeapp

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme
import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.ProveedorTokenGoogle
import com.unaj.obratrack.features.auth.AuthViewModel
import com.unaj.obratrack.features.auth.CuentaViewModel
import com.unaj.obratrack.features.auth.EditarNombreDialog
import com.unaj.obratrack.features.auth.LoginScreen
import com.unaj.obratrack.features.auth.PerfilScreen
import com.unaj.obratrack.features.auth.PerfilViewModel
import com.unaj.obratrack.features.auth.PreferenciasViewModel
import com.unaj.obratrack.features.auth.RecuperarContrasenaDialog
import com.unaj.obratrack.features.auth.RegistroScreen
import com.unaj.obratrack.features.auth.RegistroViewModel
import com.unaj.obratrack.features.auth.message
import com.unaj.obratrack.features.obras.AgendaVisitasScreen
import com.unaj.obratrack.features.obras.ListaRelevamientosUiState
import com.unaj.obratrack.features.obras.ListaTareasUiState
import com.unaj.obratrack.features.obras.MisObrasScreen
import com.unaj.obratrack.features.obras.ObrasViewModel
import com.unaj.obratrack.features.obras.RelevamientoScreen
import com.unaj.obratrack.features.obras.RelevamientosAcciones
import com.unaj.obratrack.features.obras.RelevamientosSection
import com.unaj.obratrack.features.obras.RelevamientosViewModel
import com.unaj.obratrack.features.obras.SugerenciasAcciones
import com.unaj.obratrack.features.obras.SugerenciasTareasScreen
import com.unaj.obratrack.features.obras.SugerenciasViewModel
import com.unaj.obratrack.features.obras.TareaScreen
import com.unaj.obratrack.features.obras.TareasAcciones
import com.unaj.obratrack.features.obras.TareasSection
import com.unaj.obratrack.features.obras.TareasViewModel

/** Punto de entrada común para Android y SwiftUI. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ObraTrackApp(
    proveedorTokenGoogle: ProveedorTokenGoogle,
    uiTesting: Boolean = false,
    // La plataforma ajusta el color de los íconos de las barras del sistema al tema que se aplica.
    alAplicarTema: (oscuro: Boolean) -> Unit = {},
    // La plataforma cambia el idioma de la aplicación al elegido (Android reinicia la pantalla).
    alAplicarIdioma: (IdiomaApp) -> Unit = {},
    // Clave y modelo de Gemini; sin clave no hay sugerencias con IA. Nunca se versionan.
    claveGemini: String = "",
    modeloGemini: String = "",
) {
    if (uiTesting) {
        AccessFlowPreview()
        return
    }

    val graph = remember { AppGraph(proveedorTokenGoogle, claveGemini, modeloGemini) }
    val authViewModel: AuthViewModel = viewModel { graph.crearAuthViewModel() }
    val registroViewModel: RegistroViewModel = viewModel { graph.crearRegistroViewModel() }
    val obrasViewModel: ObrasViewModel = viewModel { graph.crearObrasViewModel() }
    val tareasViewModel: TareasViewModel = viewModel { graph.crearTareasViewModel() }
    val sugerenciasViewModel: SugerenciasViewModel = viewModel { graph.crearSugerenciasViewModel() }
    val relevamientosViewModel: RelevamientosViewModel = viewModel { graph.crearRelevamientosViewModel() }
    val perfilViewModel: PerfilViewModel = viewModel { graph.crearPerfilViewModel() }
    val cuentaViewModel: CuentaViewModel = viewModel { graph.crearCuentaViewModel() }
    val preferenciasViewModel: PreferenciasViewModel = viewModel { graph.crearPreferenciasViewModel() }
    val authState by authViewModel.state.collectAsState()
    val registroState by registroViewModel.state.collectAsState()
    val obrasState by obrasViewModel.state.collectAsState()
    val tareasState by tareasViewModel.state.collectAsState()
    val sugerenciasState by sugerenciasViewModel.state.collectAsState()
    val relevamientosState by relevamientosViewModel.state.collectAsState()
    val cuentaState by cuentaViewModel.state.collectAsState()
    val tema by preferenciasViewModel.tema.collectAsState()
    val idioma by preferenciasViewModel.idioma.collectAsState()
    val sistemaOscuro = isSystemInDarkTheme()
    val oscuro = when (tema) {
        TemaApp.SISTEMA -> sistemaOscuro
        TemaApp.CLARO -> false
        TemaApp.OSCURO -> true
    }
    LaunchedEffect(oscuro) { alAplicarTema(oscuro) }
    LaunchedEffect(idioma) { alAplicarIdioma(idioma) }
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
            sugerenciasViewModel.cerrar()
        }
    }

    val accionesRelevamientos = remember(relevamientosViewModel) {
        RelevamientosAcciones(
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
            onMapOpen = relevamientosViewModel::abrirMapa,
            onMapClose = relevamientosViewModel::cerrarMapa,
            onMapPick = relevamientosViewModel::elegirEnMapa,
        )
    }
    val accionesSugerencias = remember(sugerenciasViewModel) {
        SugerenciasAcciones(
            onTipo = sugerenciasViewModel::elegirTipo,
            onDescripcion = sugerenciasViewModel::descripcionCambiada,
            onSugerirIa = sugerenciasViewModel::sugerirConIa,
            onAlternar = sugerenciasViewModel::alternar,
            onSeleccionarTodas = sugerenciasViewModel::seleccionarTodas,
            onCrear = sugerenciasViewModel::crear,
            onClose = sugerenciasViewModel::cerrar,
        )
    }
    val accionesTareas = remember(tareasViewModel, sugerenciasViewModel, obraAbiertaId) {
        TareasAcciones(
            onCreate = tareasViewModel::abrirFormulario,
            onSuggest = { obraAbiertaId?.let(sugerenciasViewModel::abrir) },
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
        )
    }
    val relevamientoAbierto = relevamientosState.formularioVisible || relevamientosState.seleccionado != null
    // La obra recuerda hasta dónde estaba desplazada al volver de un relevamiento; al abrir otra obra empieza de arriba.
    val pestanaDetalle = rememberSaveable(obraAbiertaId) { mutableIntStateOf(0) }
    val scrollDetalleObra = rememberSaveable(obraAbiertaId, saver = ScrollState.Saver) { ScrollState(0) }

    ObraTrackTheme(oscuro = oscuro) {
        Surface(Modifier.fillMaxSize()) {
            when {
                !authState.sessionChecked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                authState.user != null -> {
                    var pestana by rememberSaveable(stateSaver = PestanaPrincipalSaver) {
                        mutableStateOf(PestanaPrincipal.OBRAS)
                    }
                    val enRaiz = pestana != PestanaPrincipal.OBRAS ||
                        (obrasState.obraSeleccionada == null && !obrasState.formularioVisible)
                    BackHandler(enabled = pestana != PestanaPrincipal.OBRAS) { pestana = PestanaPrincipal.OBRAS }
                    Scaffold(
                        bottomBar = { if (enRaiz) BarraInferior(pestana) { pestana = it } },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { padding ->
                        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                            when (pestana) {
                                // Un relevamiento abierto (detalle o formulario) es una pantalla propia, no un tramo de la obra.
                                PestanaPrincipal.OBRAS -> if (tareasState.formularioVisible) TareaScreen(
                                    state = tareasState,
                                    acciones = accionesTareas,
                                ) else if (sugerenciasState.visible) SugerenciasTareasScreen(
                                    state = sugerenciasState,
                                    acciones = accionesSugerencias,
                                ) else if (relevamientoAbierto) RelevamientoScreen(
                                    state = relevamientosState,
                                    acciones = accionesRelevamientos,
                                    nombreObra = obrasState.obraSeleccionada?.nombre,
                                    ubicacionObra = obrasState.obraSeleccionada?.ubicacion,
                                ) else MisObrasScreen(
                                    user = authState.user!!,
                                    state = obrasState,
                                    onCreate = obrasViewModel::abrirFormulario,
                                    onRetry = obrasViewModel::reintentar,
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
                                    pestanaDetalle = pestanaDetalle.intValue,
                                    onPestanaDetalleChange = { pestanaDetalle.intValue = it },
                                    onCreateTarea = accionesTareas.onCreate,
                                    onCreateRelevamiento = accionesRelevamientos.onCreate,
                                    puedeCrearRelevamiento = !relevamientosState.eliminando,
                                    cantidadTareas = (tareasState.lista as? ListaTareasUiState.Contenido)?.items?.size,
                                    cantidadRelevamientos = (relevamientosState.lista as? ListaRelevamientosUiState.Contenido)?.items?.size,
                                    tareasContent = { TareasSection(state = tareasState, acciones = accionesTareas) },
                                    relevamientosContent = { RelevamientosSection(state = relevamientosState, acciones = accionesRelevamientos) },
                                    detalleScroll = scrollDetalleObra,
                                )
                                PestanaPrincipal.AGENDA -> AgendaVisitasScreen(
                                    obrasState.lista,
                                    obrasState.edicionVisita,
                                    obrasViewModel::reintentar,
                                    obrasViewModel::editarVisita,
                                    obrasViewModel::cerrarEdicionVisita,
                                    obrasViewModel::guardarVisita,
                                )
                                PestanaPrincipal.PERFIL -> {
                                    // Se recolecta solo aquí: la consulta de obras corre mientras Perfil está visible.
                                    val resumen by perfilViewModel.state.collectAsState()
                                    PerfilScreen(
                                        user = authState.user!!,
                                        resumen = resumen,
                                        errorMessage = authState.error?.message(),
                                        tema = tema,
                                        idioma = idioma,
                                        onRetry = perfilViewModel::reintentar,
                                        onEditName = {
                                            cuentaViewModel.abrirEdicionNombre(authState.user?.nombre.orEmpty())
                                        },
                                        onThemeChange = preferenciasViewModel::elegirTema,
                                        onLanguageChange = preferenciasViewModel::elegirIdioma,
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
                    onForgotPassword = { cuentaViewModel.abrirRecuperacion(authState.email) },
                )
            }
        }
        cuentaState.recuperacion?.let {
            RecuperarContrasenaDialog(
                state = it,
                onEmailChange = cuentaViewModel::emailRecuperacionCambiado,
                onSend = cuentaViewModel::enviarRecuperacion,
                onDismiss = cuentaViewModel::cerrarRecuperacion,
            )
        }
        cuentaState.edicionNombre?.let {
            EditarNombreDialog(
                state = it,
                onNameChange = cuentaViewModel::nombreCambiado,
                onSave = cuentaViewModel::guardarNombre,
                onDismiss = cuentaViewModel::cerrarEdicionNombre,
            )
        }
    }
}
