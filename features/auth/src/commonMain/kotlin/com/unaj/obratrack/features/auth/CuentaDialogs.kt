package com.unaj.obratrack.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.features.auth.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Pide el correo y confirma el envío; sin estado propio: lo maneja [CuentaViewModel]. */
@Composable
fun RecuperarContrasenaDialog(
    state: RecuperacionUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recuperar_title)) },
        text = {
            if (state.enviado) {
                Text(stringResource(Res.string.recuperar_sent, state.email))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.recuperar_body))
                    AuthTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = stringResource(Res.string.auth_email_label),
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            focus.clearFocus()
                            if (state.puedeEnviar) onSend()
                        },
                    )
                    state.error?.let { ErrorBlock(it.message()) }
                }
            }
        },
        confirmButton = {
            if (state.enviado) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.recuperar_close)) }
            } else {
                TextButton(onClick = onSend, enabled = state.puedeEnviar) {
                    if (state.enviando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(stringResource(Res.string.recuperar_send))
                }
            }
        },
        dismissButton = if (state.enviado) null else {
            { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.perfil_cancel)) } }
        },
    )
}

@Composable
fun EditarNombreDialog(
    state: EdicionNombreUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.perfil_edit_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthTextField(
                    value = state.nombre,
                    onValueChange = onNameChange,
                    label = stringResource(Res.string.perfil_name),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focus.clearFocus()
                        if (state.puedeGuardar) onSave()
                    },
                )
                state.error?.let { ErrorBlock(it.message()) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = state.puedeGuardar) {
                if (state.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(Res.string.perfil_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.perfil_cancel)) }
        },
    )
}

@Composable
private fun ErrorRecuperacionUi.message(): String = stringResource(
    when (this) {
        ErrorRecuperacionUi.EmailInvalido -> Res.string.recuperar_error_invalid_email
        ErrorRecuperacionUi.SinConexion -> Res.string.auth_error_no_connection
        ErrorRecuperacionUi.DemasiadosIntentos -> Res.string.auth_error_too_many_attempts
        ErrorRecuperacionUi.Inesperado -> Res.string.recuperar_error_unexpected
    },
)

@Composable
private fun ErrorNombreUi.message(): String = stringResource(
    when (this) {
        ErrorNombreUi.Invalido -> Res.string.perfil_name_error_invalid
        ErrorNombreUi.SinSesion -> Res.string.perfil_name_error_session
        ErrorNombreUi.SinConexion -> Res.string.auth_error_no_connection
        ErrorNombreUi.Inesperado -> Res.string.perfil_name_error_unexpected
    },
)
