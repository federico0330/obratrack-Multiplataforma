package com.unaj.obratrack.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.features.auth.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Datos de la cuenta con sesión iniciada. Solo muestra lo que existe en [Usuario]: el nombre
 * (o el correo si no hay) y el correo, que se ven siempre. Debajo, el resumen de obras con su
 * propio estado de carga y de error. Cerrar sesión pide confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    user: Usuario,
    resumen: ResumenPerfilUiState,
    errorMessage: String? = null,
    onRetry: () -> Unit,
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
            ResumenObrasSeccion(resumen, onRetry)
            errorMessage?.let { AuthErrorMessage(it) }
            OutlinedButton(
                onClick = { confirmando = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_logout),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(Res.string.perfil_sign_out))
            }
        }
    }
}

@Composable
private fun ResumenObrasSeccion(resumen: ResumenPerfilUiState, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.perfil_resumen_title), style = MaterialTheme.typography.titleMedium)
        when (resumen) {
            ResumenPerfilUiState.Cargando -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            }
            is ResumenPerfilUiState.Contenido -> OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val datos = resumen.resumen
                    Text(stringResource(Res.string.perfil_resumen_total, datos.total.toString()))
                    Text(stringResource(Res.string.perfil_resumen_activas, datos.activas.toString()))
                    Text(stringResource(Res.string.perfil_resumen_pausadas, datos.pausadas.toString()))
                    Text(stringResource(Res.string.perfil_resumen_finalizadas, datos.finalizadas.toString()))
                }
            }
            is ResumenPerfilUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthErrorMessage(mensajeResumen(resumen.motivo))
                TextButton(onClick = onRetry) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_refresh),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(Res.string.perfil_resumen_retry))
                }
            }
        }
    }
}

@Composable
private fun mensajeResumen(motivo: ErrorResumenUi): String = stringResource(
    when (motivo) {
        ErrorResumenUi.SESION -> Res.string.perfil_resumen_error_session
        ErrorResumenUi.RED -> Res.string.perfil_resumen_error_network
        ErrorResumenUi.PERMISOS -> Res.string.perfil_resumen_error_permissions
        ErrorResumenUi.DESCONOCIDO -> Res.string.perfil_resumen_error_generic
    },
)

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
