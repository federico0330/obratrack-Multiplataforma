package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Acciones de la sección de tareas; la pantalla no conoce al ViewModel. */
class TareasAcciones(
    val onCreate: () -> Unit,
    val onRetry: () -> Unit,
    val onEdit: (Tarea) -> Unit,
    val onChangeState: (Tarea, EstadoTarea) -> Unit,
    val onRequestDelete: (Tarea) -> Unit,
    val onCancelDelete: () -> Unit,
    val onConfirmDelete: () -> Unit,
    val onLinkedChange: (RelevamientosVinculados) -> Unit,
    val onCloseForm: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategoryChange: (CategoriaTarea) -> Unit,
    val onStartChange: (String) -> Unit,
    val onEndChange: (String) -> Unit,
    val onSave: () -> Unit,
)

@Composable
fun TareasSection(state: TareasUiState, acciones: TareasAcciones, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.tareas_title), style = MaterialTheme.typography.titleLarge)
        state.tareaAEliminar?.let { DialogoEliminarTarea(it, state.vinculados, acciones) }
        if (state.formularioVisible) FormularioTarea(state.formulario, acciones) else ListaTareas(state, acciones)
    }
}

@Composable
private fun ListaTareas(state: TareasUiState, acciones: TareasAcciones) {
    when (val lista = state.lista) {
        ListaTareasUiState.Cargando -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ListaTareasUiState.Vacia -> Text(
            stringResource(Res.string.tareas_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is ListaTareasUiState.Error -> {
            ErrorBlock(mensajeError(lista.motivo))
            TextButton(onClick = acciones.onRetry) { Text(stringResource(Res.string.tareas_retry)) }
        }
        is ListaTareasUiState.Contenido -> {
            Text(
                stringResource(
                    Res.string.tareas_summary,
                    lista.resumen.completadas,
                    lista.resumen.pendientes,
                    lista.resumen.atrasadas,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lista.items.forEach { item -> FilaTarea(item, state, acciones) }
        }
    }
    state.errorOperacion?.let { ErrorBlock(mensajeError(it)) }
    if (state.eliminando) LinearProgressIndicator(Modifier.fillMaxWidth())
    Button(onClick = acciones.onCreate, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.tareas_create))
    }
}

@Composable
private fun FilaTarea(item: TareaItem, state: TareasUiState, acciones: TareasAcciones) {
    val tarea = item.tarea
    val ocupada = state.actualizandoId != null || state.eliminando
    val secundario = MaterialTheme.colorScheme.onSurfaceVariant
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tarea.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                EtiquetaTarea(nombreEstado(tarea.estado))
            }
            Text(nombreCategoria(tarea.categoria), style = MaterialTheme.typography.bodyMedium, color = secundario)
            if (tarea.fechaInicio != null && tarea.fechaFin != null) {
                Text(
                    stringResource(Res.string.tareas_dates_value, tarea.fechaInicio.toString(), tarea.fechaFin.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secundario,
                )
            }
            if (item.atrasada) {
                Text(
                    stringResource(Res.string.tareas_overdue),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (tarea.descripcion.isNotBlank()) Text(tarea.descripcion, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (tarea.estado) {
                    EstadoTarea.PENDIENTE -> {
                        AccionTarea(Res.string.tareas_start, !ocupada, Modifier.weight(1f)) {
                            acciones.onChangeState(tarea, EstadoTarea.EN_CURSO)
                        }
                        AccionTarea(Res.string.tareas_complete, !ocupada, Modifier.weight(1f)) {
                            acciones.onChangeState(tarea, EstadoTarea.COMPLETADA)
                        }
                    }
                    EstadoTarea.EN_CURSO -> AccionTarea(Res.string.tareas_complete, !ocupada, Modifier.weight(1f)) {
                        acciones.onChangeState(tarea, EstadoTarea.COMPLETADA)
                    }
                    EstadoTarea.COMPLETADA -> AccionTarea(Res.string.tareas_reopen, !ocupada, Modifier.weight(1f)) {
                        acciones.onChangeState(tarea, EstadoTarea.PENDIENTE)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { acciones.onEdit(tarea) }, enabled = !ocupada) {
                    Text(stringResource(Res.string.tareas_edit))
                }
                TextButton(
                    onClick = { acciones.onRequestDelete(tarea) },
                    enabled = !ocupada,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.tareas_delete)) }
            }
        }
    }
}

@Composable
private fun AccionTarea(
    texto: org.jetbrains.compose.resources.StringResource,
    habilitado: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, enabled = habilitado, modifier = modifier) { Text(stringResource(texto)) }
}

/** Etiqueta con el estado de la tarea, con el mismo aspecto que la de la obra. */
@Composable
private fun EtiquetaTarea(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun FormularioTarea(form: FormularioTareaUiState, acciones: TareasAcciones) {
    Text(
        stringResource(if (form.tareaId == null) Res.string.tareas_create_title else Res.string.tareas_edit_title),
        style = MaterialTheme.typography.titleMedium,
    )
    OutlinedTextField(
        form.titulo, acciones.onTitleChange,
        label = { Text(stringResource(Res.string.tareas_field_title)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
    )
    OutlinedTextField(
        form.descripcion, acciones.onDescriptionChange,
        label = { Text(stringResource(Res.string.tareas_field_description)) },
        modifier = Modifier.fillMaxWidth(),
    )
    SelectorCategoria(form.categoria, acciones.onCategoryChange)
    OutlinedTextField(
        form.fechaInicio, acciones.onStartChange,
        label = { Text(stringResource(Res.string.tareas_field_start)) },
        supportingText = { Text(stringResource(Res.string.tareas_date_hint)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
    )
    OutlinedTextField(
        form.fechaFin, acciones.onEndChange,
        label = { Text(stringResource(Res.string.tareas_field_end)) },
        supportingText = { Text(stringResource(Res.string.tareas_date_hint)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
    )
    form.error?.let { ErrorBlock(mensajeError(it)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = acciones.onSave, enabled = !form.guardando, modifier = Modifier.weight(1f)) {
            if (form.guardando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(Res.string.tareas_save))
        }
        OutlinedButton(onClick = acciones.onCloseForm, enabled = !form.guardando, modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.tareas_cancel))
        }
    }
}

@Composable
private fun SelectorCategoria(seleccionada: CategoriaTarea, onChange: (CategoriaTarea) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column {
        Text(stringResource(Res.string.tareas_field_category), style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(nombreCategoria(seleccionada))
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            CategoriaTarea.entries.forEach { categoria ->
                DropdownMenuItem(
                    text = { Text(nombreCategoria(categoria)) },
                    onClick = {
                        abierto = false
                        onChange(categoria)
                    },
                )
            }
        }
    }
}

@Composable
private fun DialogoEliminarTarea(tarea: Tarea, vinculados: RelevamientosVinculados, acciones: TareasAcciones) {
    AlertDialog(
        onDismissRequest = acciones.onCancelDelete,
        title = { Text(stringResource(Res.string.tareas_delete_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.tareas_delete_confirmation, tarea.titulo))
                OpcionVinculados(
                    seleccionada = vinculados == RelevamientosVinculados.DESVINCULAR,
                    texto = stringResource(Res.string.tareas_delete_unlink),
                    onClick = { acciones.onLinkedChange(RelevamientosVinculados.DESVINCULAR) },
                )
                OpcionVinculados(
                    seleccionada = vinculados == RelevamientosVinculados.ELIMINAR,
                    texto = stringResource(Res.string.tareas_delete_remove),
                    onClick = { acciones.onLinkedChange(RelevamientosVinculados.ELIMINAR) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = acciones.onConfirmDelete) { Text(stringResource(Res.string.tareas_delete)) }
        },
        dismissButton = {
            TextButton(onClick = acciones.onCancelDelete) { Text(stringResource(Res.string.tareas_cancel)) }
        },
    )
}

@Composable
private fun OpcionVinculados(seleccionada: Boolean, texto: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = seleccionada, onClick = onClick)
        Text(texto)
    }
}

@Composable
private fun nombreEstado(estado: EstadoTarea): String = stringResource(
    when (estado) {
        EstadoTarea.PENDIENTE -> Res.string.tareas_state_pending
        EstadoTarea.EN_CURSO -> Res.string.tareas_state_in_progress
        EstadoTarea.COMPLETADA -> Res.string.tareas_state_completed
    },
)

@Composable
private fun nombreCategoria(categoria: CategoriaTarea): String = stringResource(
    when (categoria) {
        CategoriaTarea.CIMIENTOS_Y_EXCAVACION -> Res.string.tareas_cat_foundations
        CategoriaTarea.ESTRUCTURA -> Res.string.tareas_cat_structure
        CategoriaTarea.ALBANILERIA -> Res.string.tareas_cat_masonry
        CategoriaTarea.TECHOS -> Res.string.tareas_cat_roofs
        CategoriaTarea.ELECTRICIDAD -> Res.string.tareas_cat_electrical
        CategoriaTarea.PLOMERIA -> Res.string.tareas_cat_plumbing
        CategoriaTarea.GAS -> Res.string.tareas_cat_gas
        CategoriaTarea.CLIMATIZACION -> Res.string.tareas_cat_hvac
        CategoriaTarea.CARPINTERIA -> Res.string.tareas_cat_carpentry
        CategoriaTarea.HERRERIA -> Res.string.tareas_cat_ironwork
        CategoriaTarea.PISOS_Y_REVESTIMIENTOS -> Res.string.tareas_cat_flooring
        CategoriaTarea.PINTURA -> Res.string.tareas_cat_painting
        CategoriaTarea.ABERTURAS -> Res.string.tareas_cat_openings
        CategoriaTarea.IMPERMEABILIZACION -> Res.string.tareas_cat_waterproofing
        CategoriaTarea.LIMPIEZA -> Res.string.tareas_cat_cleaning
        CategoriaTarea.OTROS -> Res.string.tareas_cat_other
    },
)

@Composable
private fun mensajeError(error: ErrorTareasUi): String = stringResource(
    when (error) {
        ErrorTareasUi.SESION, ErrorTareasUi.PERMISOS -> Res.string.tareas_error_session
        ErrorTareasUi.TITULO -> Res.string.tareas_error_title
        ErrorTareasUi.FECHAS_INCOMPLETAS -> Res.string.tareas_error_dates_incomplete
        ErrorTareasUi.FECHA_FIN_ANTERIOR -> Res.string.tareas_error_dates_order
        ErrorTareasUi.FECHA_FORMATO -> Res.string.tareas_error_date_format
        ErrorTareasUi.RED -> Res.string.tareas_error_network
        ErrorTareasUi.BAJA_PARCIAL -> Res.string.tareas_error_partial_delete
        ErrorTareasUi.DESCONOCIDO -> Res.string.tareas_error_generic
    },
)
