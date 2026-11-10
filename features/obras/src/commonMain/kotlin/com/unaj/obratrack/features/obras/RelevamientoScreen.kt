package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pantalla de un relevamiento (detalle, alta o edición). Es una pantalla propia y no un tramo de la de la obra: acá no
 * se ve nada de la obra ni de sus tareas, así que cada botón es del relevamiento. Se muestra mientras haya un
 * relevamiento seleccionado o el formulario abierto, y la flecha vuelve a la obra.
 *
 * [nombreObra] da contexto en el detalle; [ubicacionObra] es dónde arranca el mapa al elegir un lugar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RelevamientoScreen(
    state: RelevamientosUiState,
    acciones: RelevamientosAcciones,
    nombreObra: String?,
    ubicacionObra: Ubicacion?,
    modifier: Modifier = Modifier,
) {
    val enFormulario = state.formularioVisible
    val seleccionado = state.seleccionado
    val guardando = state.formulario.guardando
    val ocupado = state.eliminando || guardando

    // Atrás: primero la confirmación de baja, después el formulario y por último el detalle.
    BackHandler(enabled = true) {
        when {
            state.aEliminar != null -> acciones.onCancelDelete()
            enFormulario -> if (!guardando) acciones.onCloseForm()
            else -> if (!state.eliminando) acciones.onBack()
        }
    }

    state.aEliminar?.let {
        AlertDialog(
            onDismissRequest = acciones.onCancelDelete,
            title = { Text(stringResource(Res.string.relevamientos_delete)) },
            text = { Text(stringResource(Res.string.relevamientos_delete_confirmation)) },
            confirmButton = { TextButton(onClick = acciones.onConfirmDelete) { Text(stringResource(Res.string.relevamientos_confirm_delete)) } },
            dismissButton = { TextButton(onClick = acciones.onCancelDelete) { Text(stringResource(Res.string.relevamientos_cancel)) } },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                when {
                                    !enFormulario -> Res.string.relevamientos_screen_title
                                    state.formulario.id == null -> Res.string.relevamientos_create
                                    else -> Res.string.relevamientos_edit
                                },
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = if (enFormulario) acciones.onCloseForm else acciones.onBack,
                            enabled = !ocupado,
                            modifier = Modifier.testTag("relevamientos_back"),
                        ) {
                            Icon(painter = painterResource(Iconos.ArrowBack), contentDescription = stringResource(Res.string.obras_back))
                        }
                    },
                    actions = {
                        if (!enFormulario && seleccionado != null) {
                            IconButton(onClick = acciones.onEdit, enabled = !ocupado, modifier = Modifier.testTag("relevamientos_edit")) {
                                Icon(painter = painterResource(Iconos.Edit), contentDescription = stringResource(Res.string.relevamientos_edit))
                            }
                        }
                    },
                )
                if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        bottomBar = { if (enFormulario) BarraGuardarRelevamiento(state.formulario, acciones) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            when {
                enFormulario -> FormularioRelevamientoContent(state.formulario, state.tareas, acciones, ubicacionObra)
                seleccionado != null -> DetalleRelevamientoContent(
                    item = seleccionado, nombreObra = nombreObra, tareas = state.tareas, direccion = state.direccion,
                    ocupado = ocupado, errorOperacion = state.errorOperacion, acciones = acciones,
                )
            }
        }
    }
}
