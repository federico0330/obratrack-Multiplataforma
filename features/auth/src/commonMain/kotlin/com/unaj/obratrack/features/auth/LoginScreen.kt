package com.unaj.obratrack.features.auth

import com.unaj.obratrack.core.ui.ErrorBlock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import com.unaj.obratrack.features.auth.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Composable sin estado de negocio: recibe datos y emite intenciones. */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignInWithGoogle: () -> Unit,
    onGoToRegister: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    AuthScaffold(subtitle = stringResource(Res.string.auth_login_subtitle)) {
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = stringResource(Res.string.auth_email_label),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
        )
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = stringResource(Res.string.auth_password_label),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = {
                focus.clearFocus()
                if (state.canSignIn) onSignIn()
            },
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        )
        state.error?.let { ErrorBlock(it.message()) }
        AuthSubmitButton(
            text = stringResource(Res.string.auth_sign_in),
            enabled = state.canSignIn,
            loading = state.isLoading,
            onClick = onSignIn,
        )
        Text(
            stringResource(Res.string.auth_or),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        GoogleSignInButton(
            text = stringResource(Res.string.auth_google_sign_in),
            enabled = state.canSignInWithGoogle,
            loading = state.isGoogleLoading,
            onClick = onSignInWithGoogle,
        )
        TextButton(onClick = onGoToRegister, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.auth_create_account))
        }
    }
}
