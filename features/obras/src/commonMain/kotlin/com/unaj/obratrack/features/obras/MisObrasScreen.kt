package com.unaj.obratrack.features.obras

import com.unaj.obratrack.core.ui.Iconos
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Orquestador de la sección de obras: elige entre lista, detalle y formulario según el estado
 * y arma la estructura común (barra superior, botón flotante y diálogo de baja).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MisObrasScreen(
    user: Usuario,
    state: ObrasUiState,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    onSelectObra: (Obra) -> Unit,
    onBackToList: () -> Unit,
    onEditObra: () -> Unit,
    onChangeStatus: (EstadoObra) -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCloseForm: () -> Unit,
    onNameChange: (String) -> Unit,
    onClientChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    pestanaDetalle: Int = PESTANA_RESUMEN,
    onPestanaDetalleChange: (Int) -> Unit = {},
    cantidadTareas: Int? = null,
    cantidadRelevamientos: Int? = null,
    tareasContent: @Composable () -> Unit = {},
    relevamientosContent: @Composable () -> Unit = {},
    /** Quien llama lo guarda para que, al volver de un relevamiento, la obra siga donde estaba. */
    detalleScroll: ScrollState = rememberScrollState(),
) {
    val obra = state.obraSeleccionada
    val enFormulario = state.formularioVisible
    val enDetalle = !enFormulario && obra != null

    // Atrás sale del formulario o del detalle; en la lista queda el comportamiento del sistema.
    BackHandler(enabled = enFormulario) { if (!state.formulario.guardando) onCloseForm() }
    BackHandler(enabled = enDetalle) { if (!state.eliminando) onBackToList() }

    if (state.confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(Res.string.obras_delete_title)) },
            text = { Text(stringResource(Res.string.obras_delete_confirmation, obra?.nombre.orEmpty())) },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text(stringResource(Res.string.obras_delete)) } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text(stringResource(Res.string.obras_cancel)) } },
        )
    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                when {
                                    enFormulario && state.formulario.obraId == null -> Res.string.obras_create_title
                                    enFormulario -> Res.string.obras_edit_title
                                    enDetalle -> Res.string.obras_detail_title
                                    else -> Res.string.obras_title
                                },
                            ),
                        )
                    },
                    navigationIcon = {
                        if (enFormulario || enDetalle) {
                            IconButton(
                                onClick = if (enFormulario) onCloseForm else onBackToList,
                                enabled = !state.formulario.guardando && !state.eliminando,
                            ) {
                                Icon(
                                    painter = painterResource(Iconos.ArrowBack),
                                    contentDescription = stringResource(Res.string.obras_back),
                                )
                            }
                        }
                    },
                    actions = {
                        if (enDetalle) {
                            IconButton(
                                onClick = onEditObra,
                                enabled = !state.actualizando && !state.eliminando,
                                modifier = Modifier.testTag("obras_edit"),
                            ) {
                                Icon(painter = painterResource(Iconos.Edit), contentDescription = stringResource(Res.string.obras_edit))
                            }
                        }
                    },
                )
                if (state.actualizando || state.eliminando) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        floatingActionButton = {
            if (!enFormulario && !enDetalle) {
                ExtendedFloatingActionButton(
                    onClick = onCreate,
                    icon = { Icon(painterResource(Iconos.Add), contentDescription = null) },
                    text = { Text(stringResource(Res.string.obras_create)) },
                )
            }
        },
    ) { padding ->
        when {
            enFormulario -> FormularioObraContent(
                form = state.formulario,
                onCloseForm = onCloseForm,
                onNameChange = onNameChange,
                onClientChange = onClientChange,
                onAddressChange = onAddressChange,
                onDateChange = onDateChange,
                onPublicChange = onPublicChange,
                onSave = onSave,
                padding = padding,
            )
            obra != null -> DetalleObraContent(
                obra = obra,
                state = state,
                onChangeStatus = onChangeStatus,
                onRequestDelete = onRequestDelete,
                padding = padding,
                scrollState = detalleScroll,
                pestana = pestanaDetalle,
                onPestanaChange = onPestanaDetalleChange,
                cantidadTareas = cantidadTareas,
                cantidadRelevamientos = cantidadRelevamientos,
                tareasContent = tareasContent,
                relevamientosContent = relevamientosContent,
            )
            else -> ListaObrasContent(
                user = user,
                lista = state.lista,
                errorOperacion = state.errorOperacion,
                onRetry = onRetry,
                onSelectObra = onSelectObra,
                padding = padding,
            )
        }
    }
}
