package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.features.obras.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetalleRelevamientoContent(
    item: Relevamiento,
    tareas: OpcionesTareasUiState,
    ocupado: Boolean,
    acciones: RelevamientosAcciones,
) {
    val uriHandler = LocalUriHandler.current
    var errorFoto by remember(item.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("relevamientos_detail")) {
        Text(stringResource(Res.string.relevamientos_detail), style = MaterialTheme.typography.titleMedium)
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.fecha.toLocalDateTime(TimeZone.UTC).date.toString(), style = MaterialTheme.typography.titleMedium)
                Text(item.nota)
                val tarea = item.tareaId?.let { id -> tareas.items.find { it.id == id }?.titulo
                    ?: stringResource(Res.string.relevamientos_task_unavailable) }
                Text(tarea?.let { stringResource(Res.string.relevamientos_linked_task, it) }
                    ?: stringResource(Res.string.relevamientos_no_task))
                item.ubicacion?.let { Text(stringResource(Res.string.relevamientos_location, it.latitud.toString(), it.longitud.toString())) }
                item.proximaVisita?.let { fecha ->
                    Text(stringResource(Res.string.relevamientos_next_visit, fecha.toLocalDateTime(TimeZone.UTC).date.toString()))
                    item.horaVisita?.let { minutos ->
                        Text((minutos / 60).toString().padStart(2, '0') + ":" + (minutos % 60).toString().padStart(2, '0'))
                    }
                }
                item.fotoUrl?.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { url ->
                    TextButton(onClick = { errorFoto = runCatching { uriHandler.openUri(url) }.isFailure }) {
                        Text(stringResource(Res.string.relevamientos_view_photo))
                    }
                }
                if (errorFoto) Text(stringResource(Res.string.relevamientos_photo_error), color = MaterialTheme.colorScheme.error)
            }
        }
        Button(onClick = acciones.onEdit, enabled = !ocupado,
            modifier = Modifier.fillMaxWidth().testTag("relevamientos_edit")) {
            IconoBoton(Iconos.Edit)
            Text(stringResource(Res.string.relevamientos_edit))
        }
        OutlinedButton(onClick = acciones.onRequestDelete, enabled = !ocupado,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("relevamientos_delete")) {
            IconoBoton(Iconos.Delete)
            Text(stringResource(Res.string.relevamientos_delete))
        }
        TextButton(onClick = acciones.onBack, enabled = !ocupado,
            modifier = Modifier.fillMaxWidth().testTag("relevamientos_back")) {
            Text(stringResource(Res.string.relevamientos_back))
        }
    }
}
