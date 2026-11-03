package com.unaj.obratrack.features.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.unaj.obratrack.features.auth.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Composable sin estado de negocio: recibe datos y emite intenciones. */
@Composable
fun RegistroScreen(
    state: RegistroUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRepeatPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val siguiente: () -> Unit = { focus.moveFocus(FocusDirection.Down) }
    AuthScaffold(subtitle = stringResource(Res.string.auth_register_subtitle)) {
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = stringResource(Res.string.auth_email_label),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = siguiente,
        )
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = stringResource(Res.string.auth_password_label),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            onImeAction = siguiente,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        )
        AuthTextField(
            value = state.repeatPassword,
            onValueChange = onRepeatPasswordChanged,
            label = stringResource(Res.string.auth_repeat_password_label),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = {
                focus.clearFocus()
                if (state.canSubmit) onSubmit()
            },
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        )
        state.error?.let { AuthErrorMessage(it.message()) }
        AuthSubmitButton(
            text = stringResource(Res.string.auth_create_account),
            enabled = state.canSubmit,
            loading = state.isLoading,
            onClick = onSubmit,
        )
        TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.auth_have_account))
        }
    }
}
