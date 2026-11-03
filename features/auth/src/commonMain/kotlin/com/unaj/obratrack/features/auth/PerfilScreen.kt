package com.unaj.obratrack.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.features.auth.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Datos de la cuenta con sesión iniciada. Solo muestra lo que existe en [Usuario]: el nombre
 * (o el correo si no hay) y el correo. Cerrar sesión pide confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    user: Usuario,
    errorMessage: String? = null,
    onSignOut: () -> Unit,
) {
    var confirmando by rememberSaveable { mutableStateOf(false) }
    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text(stringResource(Res.string.perfil_sign_out_title)) },
            text = { Text(stringResource(Res.string.perfil_sign_out_confirmation, user.email)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmando = false
                        onSignOut()
                    },
                ) { Text(stringResource(Res.string.perfil_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) { Text(stringResource(Res.string.perfil_cancel)) }
            },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.perfil_title)) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DatoPerfil(
                        etiqueta = stringResource(Res.string.perfil_name),
                        valor = user.nombre?.takeIf(String::isNotBlank) ?: user.email,
                    )
                    DatoPerfil(etiqueta = stringResource(Res.string.perfil_email), valor = user.email)
                }
            }
            errorMessage?.let { AuthErrorMessage(it) }
            OutlinedButton(
                onClick = { confirmando = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.perfil_sign_out)) }
        }
    }
}

@Composable
private fun DatoPerfil(etiqueta: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
