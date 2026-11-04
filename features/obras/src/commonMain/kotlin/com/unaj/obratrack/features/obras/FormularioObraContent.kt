package com.unaj.obratrack.features.obras

import com.unaj.obratrack.core.ui.CampoFecha
import com.unaj.obratrack.core.ui.ErrorBlock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FormularioObraContent(
    form: FormularioObraUiState,
    onCloseForm: () -> Unit,
    onNameChange: (String) -> Unit,
    onClientChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    padding: PaddingValues,
) {
    val focus = LocalFocusManager.current
    val siguiente: () -> Unit = { focus.moveFocus(FocusDirection.Down) }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CampoTexto(form.nombre, onNameChange, stringResource(Res.string.obras_name), siguiente)
        CampoTexto(form.cliente, onClientChange, stringResource(Res.string.obras_client), siguiente)
        CampoTexto(form.direccion, onAddressChange, stringResource(Res.string.obras_address), siguiente)
        CampoFecha(form.fechaInicio, stringResource(Res.string.obras_start_date), onDateChange)
        Text(stringResource(Res.string.obras_visibility), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !form.publica,
                onClick = { onPublicChange(false) },
                label = { Text(stringResource(Res.string.obras_private)) },
            )
            FilterChip(
                selected = form.publica,
                onClick = { onPublicChange(true) },
                label = { Text(stringResource(Res.string.obras_public)) },
            )
        }
        form.error?.let { ErrorBlock(errorMessage(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSave, enabled = !form.guardando, modifier = Modifier.weight(1f)) {
                if (form.guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(stringResource(Res.string.obras_save))
                }
            }
            OutlinedButton(onClick = onCloseForm, enabled = !form.guardando, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.obras_cancel))
            }
        }
    }
}

@Composable
private fun CampoTexto(value: String, onValueChange: (String) -> Unit, label: String, onNext: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
