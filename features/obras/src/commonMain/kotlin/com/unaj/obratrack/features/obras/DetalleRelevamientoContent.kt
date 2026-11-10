package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.features.obras.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * Detalle de un relevamiento, de lo más a lo menos importante: qué se encontró (foto y nota), a qué tarea
 * corresponde, dónde fue y cuándo se vuelve. Editar está en la barra superior; eliminar, al final y aparte.
 * Los datos que el relevamiento no tiene no se muestran.
 */
@Composable
internal fun DetalleRelevamientoContent(
    item: Relevamiento,
    nombreObra: String?,
    tareas: OpcionesTareasUiState,
    direccion: DireccionUiState,
    ocupado: Boolean,
    errorOperacion: ErrorRelevamientosUi?,
    acciones: RelevamientosAcciones,
) {
    Column(Modifier.fillMaxWidth().testTag("relevamientos_detail"), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        errorOperacion?.let { ErrorBlock(mensajeRelevamiento(it)) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.fecha.toLocalDateTime(TimeZone.UTC).date.toString(), style = MaterialTheme.typography.headlineSmall)
            nombreObra?.let {
                Text(stringResource(Res.string.relevamientos_work_name, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item.fotoWeb()?.let { url -> FotoRelevamiento(item.fotoVistaUrl ?: url) }
        CampoDetalle(stringResource(Res.string.relevamientos_note)) {
            Text(item.nota, style = MaterialTheme.typography.bodyLarge)
        }
        item.tareaId?.let { id ->
            CampoDetalle(stringResource(Res.string.relevamientos_task_label)) {
                Text(
                    tareas.items.find { it.id == id }?.titulo ?: stringResource(Res.string.relevamientos_task_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item.ubicacion?.let { UbicacionDetalle(it, direccion) }
        item.proximaVisita?.let { fecha ->
            CampoDetalle(stringResource(Res.string.relevamientos_next_visit_label)) {
                Text(textoVisita(fecha.toLocalDateTime(TimeZone.UTC).date.toString(), item.horaVisita), style = MaterialTheme.typography.bodyLarge)
            }
        }
        HorizontalDivider()
        OutlinedButton(
            onClick = acciones.onRequestDelete, enabled = !ocupado,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("relevamientos_delete"),
        ) {
            IconoBoton(Iconos.Delete)
            Text(stringResource(Res.string.relevamientos_delete))
        }
    }
}
