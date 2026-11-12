package com.unaj.obratrack.features.auth

import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.features.auth.generated.resources.*
import org.jetbrains.compose.resources.StringResource
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
    tema: TemaApp,
    idioma: IdiomaApp,
    onRetry: () -> Unit,
    onEditName: () -> Unit,
    onThemeChange: (TemaApp) -> Unit,
    onLanguageChange: (IdiomaApp) -> Unit,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DatoPerfil(
                            etiqueta = stringResource(Res.string.perfil_name),
                            valor = user.nombre?.takeIf(String::isNotBlank) ?: user.email,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onEditName) {
                            Icon(
                                painter = painterResource(Iconos.Edit),
                                contentDescription = stringResource(Res.string.perfil_edit_name),
                            )
                        }
                    }
                    DatoPerfil(etiqueta = stringResource(Res.string.perfil_email), valor = user.email)
                }
            }
            PreferenciasSeccion(tema, onThemeChange, idioma, onLanguageChange)
            ResumenObrasSeccion(resumen, onRetry)
            errorMessage?.let { ErrorBlock(it) }
            OutlinedButton(
                onClick = { confirmando = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconoBoton(Iconos.Logout)
                Text(stringResource(Res.string.perfil_sign_out))
            }
        }
    }
}

@Composable
private fun PreferenciasSeccion(
    tema: TemaApp,
    onThemeChange: (TemaApp) -> Unit,
    idioma: IdiomaApp,
    onLanguageChange: (IdiomaApp) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.perfil_preferencias_title), style = MaterialTheme.typography.titleMedium)
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OpcionesPreferencia(stringResource(Res.string.perfil_theme_title)) {
                    TemaApp.entries.forEach { opcion ->
                        FilterChip(
                            selected = opcion == tema,
                            onClick = { onThemeChange(opcion) },
                            label = { Text(stringResource(opcion.etiqueta())) },
                        )
                    }
                }
                OpcionesPreferencia(stringResource(Res.string.perfil_language_title)) {
                    IdiomaApp.entries.forEach { opcion ->
                        FilterChip(
                            selected = opcion == idioma,
                            onClick = { onLanguageChange(opcion) },
                            label = { Text(stringResource(opcion.etiqueta())) },
                        )
                    }
                }
            }
        }
    }
}

/** Título y fila de opciones que pasa a la línea siguiente si no entran. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpcionesPreferencia(titulo: String, opciones: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { opciones() }
    }
}

private fun IdiomaApp.etiqueta(): StringResource = when (this) {
    IdiomaApp.SISTEMA -> Res.string.perfil_language_system
    IdiomaApp.ESPANOL -> Res.string.perfil_language_es
    IdiomaApp.INGLES -> Res.string.perfil_language_en
    IdiomaApp.ITALIANO -> Res.string.perfil_language_it
}

private fun TemaApp.etiqueta(): StringResource = when (this) {
    TemaApp.SISTEMA -> Res.string.perfil_theme_system
    TemaApp.CLARO -> Res.string.perfil_theme_light
    TemaApp.OSCURO -> Res.string.perfil_theme_dark
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
                ErrorBlock(mensajeResumen(resumen.motivo))
                TextButton(onClick = onRetry) {
                    IconoBoton(Iconos.Refresh)
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
private fun DatoPerfil(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
