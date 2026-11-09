package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.FiltroRelevamientos
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.core.ui.CampoFecha
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
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RelevamientosSection(state: RelevamientosUiState, acciones: RelevamientosAcciones, modifier: Modifier = Modifier) {
    BackHandler(enabled = state.formularioVisible || state.seleccionado != null || state.aEliminar != null) {
        when {
            state.aEliminar != null -> acciones.onCancelDelete()
            state.formularioVisible -> acciones.onCloseForm()
            else -> acciones.onBack()
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.relevamientos_title), style = MaterialTheme.typography.titleLarge)
        state.aEliminar?.let {
            AlertDialog(
                onDismissRequest = acciones.onCancelDelete,
                title = { Text(stringResource(Res.string.relevamientos_delete)) },
                text = { Text(stringResource(Res.string.relevamientos_delete_confirmation)) },
                confirmButton = { TextButton(onClick = acciones.onConfirmDelete) { Text(stringResource(Res.string.relevamientos_confirm_delete)) } },
                dismissButton = { TextButton(onClick = acciones.onCancelDelete) { Text(stringResource(Res.string.relevamientos_cancel)) } },
            )
        }
        state.errorOperacion?.let { ErrorBlock(mensajeRelevamiento(it)) }
        if (state.eliminando) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.formularioVisible) {
            FormularioRelevamiento(state.formulario, state.tareas, acciones)
        } else if (state.seleccionado != null) {
            DetalleRelevamientoContent(state.seleccionado, state.tareas, state.eliminando, acciones)
        } else {
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
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Button(onClick = acciones.onCreate, enabled = !state.eliminando, modifier = Modifier.fillMaxWidth().testTag("relevamientos_create")) {
                IconoBoton(Iconos.Add)
                Text(stringResource(Res.string.relevamientos_create))
            }
        }
    }
}

@Composable
private fun FormularioRelevamiento(
    form: FormularioRelevamientoUiState,
    tareas: OpcionesTareasUiState,
    acciones: RelevamientosAcciones,
) {
    Text(stringResource(if (form.id == null) Res.string.relevamientos_create else Res.string.relevamientos_edit), style = MaterialTheme.typography.titleMedium)
    CampoFecha(form.fechaIso, stringResource(Res.string.relevamientos_date), acciones.onDateChange, enabled = !form.guardando)
    OutlinedTextField(
        value = form.nota, onValueChange = acciones.onNoteChange,
        label = { Text(stringResource(Res.string.relevamientos_note)) },
        enabled = !form.guardando, minLines = 3, modifier = Modifier.fillMaxWidth().testTag("relevamientos_note"),
    )
    SelectorTarea(form, tareas, acciones)
    SelectorFoto(form, acciones)
    form.error?.let { ErrorBlock(mensajeRelevamiento(it)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = acciones.onSave, enabled = !form.guardando, modifier = Modifier.weight(1f).testTag("relevamientos_save")) {
            if (form.guardando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(Res.string.relevamientos_save))
        }
        OutlinedButton(onClick = acciones.onCloseForm, enabled = !form.guardando, modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.relevamientos_cancel))
        }
    }
}

@Composable
private fun SelectorTarea(form: FormularioRelevamientoUiState, tareas: OpcionesTareasUiState, acciones: RelevamientosAcciones) {
    var abierto by remember { mutableStateOf(false) }
    Text(stringResource(Res.string.relevamientos_task_optional), style = MaterialTheme.typography.labelMedium)
    val sinTarea = stringResource(Res.string.relevamientos_no_task)
    val seleccion = if (form.tareaId == null) sinTarea
        else tareas.items.firstOrNull { it.id == form.tareaId }?.titulo ?: stringResource(Res.string.relevamientos_task_unavailable)
    Box {
        OutlinedButton(onClick = { abierto = true }, enabled = !form.guardando, modifier = Modifier.fillMaxWidth().testTag("relevamientos_task")) {
            Text(seleccion)
        }
        DropdownMenu(expanded = abierto && !form.guardando, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(text = { Text(sinTarea) }, onClick = { abierto = false; acciones.onTaskChange(null) })
            tareas.items.forEach { tarea ->
                DropdownMenuItem(modifier = Modifier.testTag("relevamientos_task_${tarea.id}"), text = { Text(tarea.titulo) }, onClick = { abierto = false; acciones.onTaskChange(tarea.id) })
            }
        }
    }
    when {
        tareas.cargando -> Text(stringResource(Res.string.relevamientos_tasks_loading), style = MaterialTheme.typography.bodySmall)
        tareas.error -> {
            Text(stringResource(Res.string.relevamientos_tasks_error), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = acciones.onRetryTasks, enabled = !form.guardando) { Text(stringResource(Res.string.relevamientos_retry)) }
        }
        tareas.items.isEmpty() -> Text(stringResource(Res.string.relevamientos_tasks_empty), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun mensajeRelevamiento(error: ErrorRelevamientosUi): String = stringResource(when (error) {
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
