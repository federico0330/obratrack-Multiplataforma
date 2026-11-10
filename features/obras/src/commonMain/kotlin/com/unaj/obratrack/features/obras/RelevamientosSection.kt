package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.FiltroRelevamientos
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.CandidatoDireccion
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.features.obras.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

data class RelevamientosAcciones(
    val onCreate: () -> Unit,
    val onRetry: () -> Unit,
    val onRetryTasks: () -> Unit,
    val onCloseForm: () -> Unit,
    val onNoteChange: (String) -> Unit,
    val onDateChange: (String) -> Unit,
    val onTaskChange: (String?) -> Unit,
    val onSave: () -> Unit,
    val onSelect: (Relevamiento) -> Unit,
    val onEdit: () -> Unit,
    val onBack: () -> Unit,
    val onRequestDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
    val onConfirmDelete: () -> Unit,
    val onFilterChange: (FiltroRelevamientos) -> Unit,
    val onPhotoPicked: (ByteArray) -> Unit,
    val onRemovePhoto: () -> Unit,
    val onPhotoReadError: () -> Unit,
    val onCameraError: () -> Unit,
    val onLocationCapture: () -> Unit,
    val onLocationCancel: () -> Unit,
    val onLocationRemove: () -> Unit,
    val onAddressSearchOpen: () -> Unit,
    val onAddressSearchClose: () -> Unit,
    val onAddressTextChange: (String) -> Unit,
    val onAddressSearch: () -> Unit,
    val onAddressPick: (CandidatoDireccion) -> Unit,
    val onMapOpen: () -> Unit,
    val onMapClose: () -> Unit,
    val onMapPick: (Ubicacion) -> Unit,
)

/**
 * Los relevamientos dentro de la pantalla de la obra: el filtro, el listado y el botón para crear uno. Abrir uno o
 * crearlo lleva a [RelevamientoScreen], una pantalla aparte.
 */
@Composable
fun RelevamientosSection(state: RelevamientosUiState, acciones: RelevamientosAcciones, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.errorOperacion?.let { ErrorBlock(mensajeRelevamiento(it)) }
        FiltroRelevamientosSelector(state, acciones)
        when (val lista = state.lista) {
            ListaRelevamientosUiState.Cargando -> CircularProgressIndicator()
            ListaRelevamientosUiState.Vacia -> Text(stringResource(if (state.filtro == FiltroRelevamientos.Todos) Res.string.relevamientos_empty else Res.string.relevamientos_filter_empty))
            is ListaRelevamientosUiState.Error -> {
                ErrorBlock(mensajeRelevamiento(lista.motivo))
                TextButton(onClick = acciones.onRetry) { Text(stringResource(Res.string.relevamientos_retry)) }
            }
            is ListaRelevamientosUiState.Contenido -> lista.items.forEach { item ->
                OutlinedCard(onClick = { acciones.onSelect(item) }, enabled = !state.eliminando, modifier = Modifier.fillMaxWidth().testTag("relevamientos_item_${item.id}")) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item.fotoWeb()?.let { url -> MiniaturaRelevamiento(item.fotoMiniaturaUrl ?: url) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.fecha.toLocalDateTime(TimeZone.UTC).date.toString(), style = MaterialTheme.typography.titleMedium)
                            Text(item.nota)
                            item.tareaId?.let { id ->
                                val titulo = state.tareas.items.firstOrNull { it.id == id }?.titulo
                                    ?: stringResource(Res.string.relevamientos_task_unavailable)
                                Text(stringResource(Res.string.relevamientos_linked_task, titulo),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = acciones.onCreate, enabled = !state.eliminando, modifier = Modifier.fillMaxWidth().testTag("relevamientos_create")) {
            IconoBoton(Iconos.Add)
            Text(stringResource(Res.string.relevamientos_create))
        }
    }
}

@Composable
internal fun mensajeRelevamiento(error: ErrorRelevamientosUi): String = stringResource(when (error) {
    ErrorRelevamientosUi.INEXISTENTE -> Res.string.relevamientos_error_not_found
    ErrorRelevamientosUi.NOTA -> Res.string.relevamientos_error_note
    ErrorRelevamientosUi.FECHA -> Res.string.relevamientos_error_date
    ErrorRelevamientosUi.SESION -> Res.string.relevamientos_error_session
    ErrorRelevamientosUi.TAREA -> Res.string.relevamientos_error_task
    ErrorRelevamientosUi.RED -> Res.string.relevamientos_error_network
    ErrorRelevamientosUi.DESCONOCIDO -> Res.string.relevamientos_error_generic
    ErrorRelevamientosUi.FOTO_SIN_CONEXION -> Res.string.relevamientos_error_photo_network
    ErrorRelevamientosUi.FOTO_MUY_GRANDE -> Res.string.relevamientos_error_photo_too_large
    ErrorRelevamientosUi.FOTO_FALLO -> Res.string.relevamientos_error_photo_upload
    ErrorRelevamientosUi.FOTO_LECTURA -> Res.string.relevamientos_error_photo_read
    ErrorRelevamientosUi.FOTO_CAMARA -> Res.string.relevamientos_error_photo_camera
    ErrorRelevamientosUi.UBICACION_SIN_PERMISO -> Res.string.relevamientos_error_location_permission
    ErrorRelevamientosUi.UBICACION_DESACTIVADA -> Res.string.relevamientos_error_location_disabled
    ErrorRelevamientosUi.UBICACION_NO_DISPONIBLE -> Res.string.relevamientos_error_location_unavailable
    ErrorRelevamientosUi.UBICACION_INVALIDA -> Res.string.relevamientos_error_location_invalid
})

@Composable
private fun FiltroRelevamientosSelector(state: RelevamientosUiState, acciones: RelevamientosAcciones) {
    var abierto by remember { mutableStateOf(false) }
    val todos = stringResource(Res.string.relevamientos_filter_all)
    val sinTarea = stringResource(Res.string.relevamientos_no_task)
    val texto = when (val filtro = state.filtro) {
        FiltroRelevamientos.Todos -> todos
        FiltroRelevamientos.SinTarea -> sinTarea
        is FiltroRelevamientos.DeTarea -> state.tareas.items.find { it.id == filtro.tareaId }?.titulo
            ?: stringResource(Res.string.relevamientos_task_unavailable)
    }
    Box {
        OutlinedButton(onClick = { abierto = true }, enabled = !state.eliminando,
            modifier = Modifier.fillMaxWidth().testTag("relevamientos_filter")) {
            Text(stringResource(Res.string.relevamientos_filter, texto))
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(text = { Text(todos) }, onClick = { abierto = false; acciones.onFilterChange(FiltroRelevamientos.Todos) })
            DropdownMenuItem(text = { Text(sinTarea) }, onClick = { abierto = false; acciones.onFilterChange(FiltroRelevamientos.SinTarea) })
            state.tareas.items.forEach { tarea ->
                DropdownMenuItem(text = { Text(tarea.titulo) }, onClick = {
                    abierto = false
                    acciones.onFilterChange(FiltroRelevamientos.DeTarea(tarea.id))
                })
            }
        }
    }
}
