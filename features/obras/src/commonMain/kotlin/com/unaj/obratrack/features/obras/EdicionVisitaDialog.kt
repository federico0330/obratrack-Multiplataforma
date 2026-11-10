package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.CampoFecha
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.features.obras.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
internal fun EdicionVisitaDialog(
    edicion: EdicionVisitaUiState,
    onClose: () -> Unit,
    onSave: (LocalDate?, LocalTime?) -> Unit,
) {
    val obra = edicion.obra
    val programada = obra.proximaVisita != null
    var fecha by rememberSaveable(obra.id) {
        val inicial = obra.proximaVisita?.toLocalDateTime(TimeZone.UTC)?.date
            ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
        mutableStateOf(inicial.toString())
    }
    var hora by rememberSaveable(obra.id) {
        mutableStateOf(obra.horaVisita?.takeIf { programada && it in 0 until 1440 }?.let(::horaVisitaTexto).orEmpty())
    }
    var confirmarBaja by rememberSaveable(obra.id) { mutableStateOf(false) }
    val horaValida = horaVisitaDesdeTexto(hora)
    val errorHora = hora.isNotBlank() && horaValida == null
    val habilitado = !edicion.guardando
    val etiquetaFecha = stringResource(Res.string.agenda_visit_date)

    if (confirmarBaja) {
        AlertDialog(
            onDismissRequest = { if (habilitado) confirmarBaja = false },
            title = { Text(stringResource(Res.string.agenda_remove_visit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.agenda_remove_confirmation))
                    edicion.error?.let { ErrorBlock(errorMessage(it)) }
                    if (edicion.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBaja = false }, enabled = habilitado) {
                    Text(stringResource(Res.string.obras_back))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onSave(null, null) },
                    enabled = habilitado,
                ) {
                    Text(stringResource(Res.string.agenda_confirm_remove))
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onClose,
            title = {
                val titulo = if (programada) Res.string.agenda_reschedule else Res.string.agenda_schedule
                Text(stringResource(titulo))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(obra.nombre)
                    CampoFecha(
                        fechaIso = fecha,
                        label = etiquetaFecha,
                        onDateChange = { fecha = it },
                        modifier = Modifier.semantics { contentDescription = etiquetaFecha },
                        enabled = habilitado,
                    )
                    OutlinedTextField(
                        value = hora,
                        onValueChange = { hora = it },
                        label = { Text(stringResource(Res.string.agenda_visit_time)) },
                        placeholder = { Text("HH:mm") },
                        supportingText = if (errorHora) {
                            { Text(stringResource(Res.string.agenda_time_format)) }
                        } else null,
                        isError = errorHora,
                        enabled = habilitado,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (programada) {
                        TextButton(
                            onClick = { confirmarBaja = true },
                            enabled = habilitado,
                        ) {
                            Text(stringResource(Res.string.agenda_remove_visit))
                        }
                    }
                    edicion.error?.let { ErrorBlock(errorMessage(it)) }
                    if (edicion.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            dismissButton = {
                TextButton(onClick = onClose, enabled = habilitado) {
                    Text(stringResource(Res.string.obras_back))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onSave(LocalDate.parse(fecha), horaValida) },
                    enabled = habilitado && !errorHora,
                ) {
                    Text(stringResource(Res.string.obras_save))
                }
            },
        )
    }
}

private val formatoHoraVisita = Regex("[0-9]{2}:[0-9]{2}")

internal fun horaVisitaDesdeTexto(texto: String): LocalTime? {
    val hora = texto.trim()
    if (!formatoHoraVisita.matches(hora)) return null
    return runCatching { LocalTime.parse(hora) }.getOrNull()
}

internal fun horaVisitaTexto(minutos: Int): String =
    "${(minutos / 60).toString().padStart(2, '0')}:${(minutos % 60).toString().padStart(2, '0')}"
