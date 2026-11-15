package com.unaj.obratrack.features.obras

import com.unaj.obratrack.core.ui.fechaLegible
import com.unaj.obratrack.core.ui.Etiqueta
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Acciones de la sección de tareas; la pantalla no conoce al ViewModel. */
class TareasAcciones(
    val onCreate: () -> Unit,
    val onSuggest: () -> Unit,
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
        state.tareaAEliminar?.let { DialogoEliminarTarea(it, state.vinculados, acciones) }
        if (state.lista is ListaTareasUiState.Vacia || state.lista is ListaTareasUiState.Contenido) {
            OutlinedButton(onClick = acciones.onSuggest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.sugerencias_open))
            }
        }
        ListaTareas(state, acciones)
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
            TextButton(onClick = acciones.onRetry) {
                IconoBoton(Iconos.Refresh)
                Text(stringResource(Res.string.tareas_retry))
            }
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
                EtiquetaTarea(tarea.estado)
            }
            Text(nombreCategoria(tarea.categoria), style = MaterialTheme.typography.bodyMedium, color = secundario)
            val inicio = tarea.fechaInicio
            val fin = tarea.fechaFin
            if (inicio != null && fin != null) {
                Text(
                    stringResource(Res.string.tareas_dates_value, fechaLegible(inicio), fechaLegible(fin)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secundario,
                )
            }
            if (item.atrasada) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        painter = painterResource(Iconos.Warning),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        stringResource(Res.string.tareas_overdue),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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
                IconButton(onClick = { acciones.onEdit(tarea) }, enabled = !ocupada) {
                    Icon(
                        painter = painterResource(Iconos.Edit),
                        contentDescription = stringResource(Res.string.tareas_edit_title),
                    )
                }
                IconButton(
                    onClick = { acciones.onRequestDelete(tarea) },
                    enabled = !ocupada,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(
                        painter = painterResource(Iconos.Delete),
                        contentDescription = stringResource(Res.string.tareas_delete_title),
                    )
                }
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
private fun EtiquetaTarea(estado: EstadoTarea) {
    val icono = when (estado) {
        EstadoTarea.PENDIENTE -> Iconos.CircleOutline
        EstadoTarea.EN_CURSO -> Iconos.PlayCircle
        EstadoTarea.COMPLETADA -> Iconos.CheckCircle
    }
    Etiqueta(
        texto = nombreEstado(estado),
        icono = icono,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
    )
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
internal fun nombreCategoria(categoria: CategoriaTarea): String = stringResource(
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
internal fun mensajeError(error: ErrorTareasUi): String = stringResource(
    when (error) {
        ErrorTareasUi.SESION, ErrorTareasUi.PERMISOS -> Res.string.tareas_error_session
        ErrorTareasUi.TITULO -> Res.string.tareas_error_title
        ErrorTareasUi.FECHAS_INCOMPLETAS -> Res.string.tareas_error_dates_incomplete
        ErrorTareasUi.FECHA_FIN_ANTERIOR -> Res.string.tareas_error_dates_order
        ErrorTareasUi.FECHA_FORMATO -> Res.string.tareas_error_date_format
        ErrorTareasUi.RED -> Res.string.tareas_error_network
        ErrorTareasUi.DEMASIADOS_VINCULADOS -> Res.string.tareas_error_delete_limit
        ErrorTareasUi.BAJA_PARCIAL -> Res.string.tareas_error_partial_delete
        ErrorTareasUi.DESCONOCIDO -> Res.string.tareas_error_generic
    },
)
