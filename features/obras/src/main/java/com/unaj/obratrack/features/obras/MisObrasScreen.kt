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
import androidx.compose.ui.res.stringResource
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
    onCloseForm: () -> Unit,
    onNameChange: (String) -> Unit,
    onClientChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.obras_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.obras_signed_in, user.nombre?.takeIf(String::isNotBlank) ?: user.email))
        authErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = onSignOut) { Text(stringResource(R.string.obras_sign_out)) }
        if (state.formularioVisible) {
            Text(
                stringResource(if (state.formulario.obraId == null) R.string.obras_create_title else R.string.obras_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(state.formulario.nombre, onNameChange, label = { Text(stringResource(R.string.obras_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.formulario.cliente, onClientChange, label = { Text(stringResource(R.string.obras_client)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.formulario.direccion, onAddressChange, label = { Text(stringResource(R.string.obras_address)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.formulario.fechaInicio, onDateChange, label = { Text(stringResource(R.string.obras_start_date)) }, supportingText = { Text(stringResource(R.string.obras_date_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !state.formulario.publica, onClick = { onPublicChange(false) })
                Text(stringResource(R.string.obras_private))
                RadioButton(selected = state.formulario.publica, onClick = { onPublicChange(true) })
                Text(stringResource(R.string.obras_public))
            }
            state.formulario.error?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !state.formulario.guardando) {
                    if (state.formulario.guardando) CircularProgressIndicator()
                    else Text(stringResource(R.string.obras_save))
                }
                OutlinedButton(onClick = onCloseForm, enabled = !state.formulario.guardando) {
                    Text(stringResource(R.string.obras_cancel))
                }
            }
        } else if (state.obraSeleccionada != null) {
            val obra = state.obraSeleccionada
            Text(stringResource(R.string.obras_detail_title), style = MaterialTheme.typography.titleLarge)
            Text(obra.nombre, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.obras_client_value, obra.cliente))
            Text(obra.direccion)
            Text(stringResource(R.string.obras_visibility_value, stringResource(if (obra.publica) R.string.obras_public else R.string.obras_private)))
            Text(stringResource(R.string.obras_status_value, estadoNombre(obra.estado)))
            HorizontalDivider()
            state.errorOperacion?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBackToList) { Text(stringResource(R.string.obras_back)) }
                Button(onClick = onEditObra) { Text(stringResource(R.string.obras_edit)) }
            }
            if (state.actualizando) CircularProgressIndicator()
            when (obra.estado) {
                EstadoObra.ACTIVA -> OutlinedButton(onClick = { onChangeStatus(EstadoObra.PAUSADA) }, enabled = !state.actualizando) {
                    Text(stringResource(R.string.obras_pause))
                }
                EstadoObra.PAUSADA -> OutlinedButton(onClick = { onChangeStatus(EstadoObra.ACTIVA) }, enabled = !state.actualizando) {
                    Text(stringResource(R.string.obras_reactivate))
                }
                EstadoObra.FINALIZADA -> Unit
            }
            if (obra.estado != EstadoObra.FINALIZADA) {
                OutlinedButton(onClick = { onChangeStatus(EstadoObra.FINALIZADA) }, enabled = !state.actualizando) {
                    Text(stringResource(R.string.obras_finish))
                }
            }
        } else {
            when (val list = state.lista) {
                ListaObrasUiState.Cargando -> CircularProgressIndicator()
                ListaObrasUiState.Vacia -> {
                    Text(stringResource(R.string.obras_empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.obras_empty_hint))
                }
                is ListaObrasUiState.Contenido -> list.obras.forEach { ObraRow(it, onSelectObra) }
                is ListaObrasUiState.Error -> {
                    Text(errorMessage(list.motivo), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.obras_retry)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCreate) { Text(stringResource(R.string.obras_create)) }
        }
    }
}

@Composable
private fun ObraRow(obra: Obra, onClick: (Obra) -> Unit) {
    TextButton(onClick = { onClick(obra) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(obra.nombre, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.obras_client_value, obra.cliente))
            Text(obra.direccion)
            Text(stringResource(R.string.obras_status_value, estadoNombre(obra.estado)))
        }
    }
}

@Composable
private fun estadoNombre(estado: EstadoObra): String = stringResource(
    when (estado) {
        EstadoObra.ACTIVA -> R.string.obras_state_active
        EstadoObra.PAUSADA -> R.string.obras_state_paused
        EstadoObra.FINALIZADA -> R.string.obras_state_finished
    },
)

@Composable
private fun errorMessage(error: ErrorObrasUi): String = stringResource(
    when (error) {
        ErrorObrasUi.SESION, ErrorObrasUi.PERMISOS -> R.string.obras_error_session
        ErrorObrasUi.NOMBRE -> R.string.obras_error_name
        ErrorObrasUi.CLIENTE -> R.string.obras_error_client
        ErrorObrasUi.DIRECCION -> R.string.obras_error_address
        ErrorObrasUi.FECHA -> R.string.obras_error_date
        ErrorObrasUi.RED -> R.string.obras_error_network
        ErrorObrasUi.DESCONOCIDO -> R.string.obras_error_generic
    },
)
