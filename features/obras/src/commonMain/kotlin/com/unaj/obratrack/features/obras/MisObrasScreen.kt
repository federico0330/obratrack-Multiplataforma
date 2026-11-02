package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.unaj.obratrack.features.obras.generated.resources.Res
import com.unaj.obratrack.features.obras.generated.resources.*
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Usuario

@Composable
fun MisObrasScreen(
    user: Usuario,
    state: ObrasUiState,
    authErrorMessage: String? = null,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
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
    detailExtra: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.obras_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(Res.string.obras_signed_in, user.nombre?.takeIf(String::isNotBlank) ?: user.email))
        authErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = onSignOut) { Text(stringResource(Res.string.obras_sign_out)) }
        if (state.confirmarEliminacion) {
            AlertDialog(
                onDismissRequest = onCancelDelete,
                title = { Text(stringResource(Res.string.obras_delete_title)) },
                text = { Text(stringResource(Res.string.obras_delete_confirmation, state.obraSeleccionada?.nombre.orEmpty())) },
                confirmButton = {
                    TextButton(onClick = onConfirmDelete) { Text(stringResource(Res.string.obras_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = onCancelDelete) { Text(stringResource(Res.string.obras_cancel)) }
                },
            )
        }
        if (state.formularioVisible) {
            Text(
                stringResource(if (state.formulario.obraId == null) Res.string.obras_create_title else Res.string.obras_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(state.formulario.nombre, onNameChange, label = { Text(stringResource(Res.string.obras_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.formulario.cliente, onClientChange, label = { Text(stringResource(Res.string.obras_client)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.formulario.direccion, onAddressChange, label = { Text(stringResource(Res.string.obras_address)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.formulario.fechaInicio, onDateChange, label = { Text(stringResource(Res.string.obras_start_date)) }, supportingText = { Text(stringResource(Res.string.obras_date_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !state.formulario.publica, onClick = { onPublicChange(false) })
                Text(stringResource(Res.string.obras_private))
                RadioButton(selected = state.formulario.publica, onClick = { onPublicChange(true) })
                Text(stringResource(Res.string.obras_public))
            }
            state.formulario.error?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !state.formulario.guardando) {
                    if (state.formulario.guardando) CircularProgressIndicator()
                    else Text(stringResource(Res.string.obras_save))
                }
                OutlinedButton(onClick = onCloseForm, enabled = !state.formulario.guardando) {
                    Text(stringResource(Res.string.obras_cancel))
                }
            }
        } else if (state.obraSeleccionada != null) {
            val obra = state.obraSeleccionada
            Text(stringResource(Res.string.obras_detail_title), style = MaterialTheme.typography.titleLarge)
            Text(obra.nombre, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(Res.string.obras_client_value, obra.cliente))
            Text(obra.direccion)
            Text(stringResource(Res.string.obras_visibility_value, stringResource(if (obra.publica) Res.string.obras_public else Res.string.obras_private)))
            Text(stringResource(Res.string.obras_status_value, estadoNombre(obra.estado)))
            HorizontalDivider()
            state.errorOperacion?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBackToList, enabled = !state.eliminando) { Text(stringResource(Res.string.obras_back)) }
                Button(onClick = onEditObra, enabled = !state.eliminando) { Text(stringResource(Res.string.obras_edit)) }
            }
            OutlinedButton(onClick = onRequestDelete, enabled = !state.eliminando && !state.actualizando) {
                Text(stringResource(Res.string.obras_delete))
            }
            if (state.eliminando) CircularProgressIndicator()
            if (state.actualizando) CircularProgressIndicator()
            when (obra.estado) {
                EstadoObra.ACTIVA -> OutlinedButton(onClick = { onChangeStatus(EstadoObra.PAUSADA) }, enabled = !state.actualizando) {
                    Text(stringResource(Res.string.obras_pause))
                }
                EstadoObra.PAUSADA -> OutlinedButton(onClick = { onChangeStatus(EstadoObra.ACTIVA) }, enabled = !state.actualizando) {
                    Text(stringResource(Res.string.obras_reactivate))
                }
                EstadoObra.FINALIZADA -> Unit
            }
            if (obra.estado != EstadoObra.FINALIZADA) {
                OutlinedButton(onClick = { onChangeStatus(EstadoObra.FINALIZADA) }, enabled = !state.actualizando) {
                    Text(stringResource(Res.string.obras_finish))
                }
            }
            detailExtra()
        } else {
            when (val list = state.lista) {
                ListaObrasUiState.Cargando -> CircularProgressIndicator()
                ListaObrasUiState.Vacia -> {
                    Text(stringResource(Res.string.obras_empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.obras_empty_hint))
                }
                is ListaObrasUiState.Contenido -> list.obras.forEach { ObraRow(it, onSelectObra) }
                is ListaObrasUiState.Error -> {
                    Text(errorMessage(list.motivo), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.obras_retry)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCreate) { Text(stringResource(Res.string.obras_create)) }
        }
    }
}

@Composable
private fun ObraRow(obra: Obra, onClick: (Obra) -> Unit) {
    TextButton(onClick = { onClick(obra) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(obra.nombre, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.obras_client_value, obra.cliente))
            Text(obra.direccion)
            Text(stringResource(Res.string.obras_status_value, estadoNombre(obra.estado)))
        }
    }
}

@Composable
private fun estadoNombre(estado: EstadoObra): String = stringResource(
    when (estado) {
        EstadoObra.ACTIVA -> Res.string.obras_state_active
        EstadoObra.PAUSADA -> Res.string.obras_state_paused
        EstadoObra.FINALIZADA -> Res.string.obras_state_finished
    },
)

@Composable
private fun errorMessage(error: ErrorObrasUi): String = stringResource(
    when (error) {
        ErrorObrasUi.SESION, ErrorObrasUi.PERMISOS -> Res.string.obras_error_session
        ErrorObrasUi.NOMBRE -> Res.string.obras_error_name
        ErrorObrasUi.CLIENTE -> Res.string.obras_error_client
        ErrorObrasUi.DIRECCION -> Res.string.obras_error_address
        ErrorObrasUi.FECHA -> Res.string.obras_error_date
        ErrorObrasUi.RED -> Res.string.obras_error_network
        ErrorObrasUi.BAJA_PARCIAL -> Res.string.obras_error_partial_delete
        ErrorObrasUi.DESCONOCIDO -> Res.string.obras_error_generic
    },
)
