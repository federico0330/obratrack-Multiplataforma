package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.unaj.obratrack.core.ui.CampoFecha
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Formulario de alta y edición, en tres bloques: los datos, la foto y la ubicación. Guardar está en [BarraGuardarRelevamiento]. */
@Composable
internal fun FormularioRelevamientoContent(
    form: FormularioRelevamientoUiState,
    tareas: OpcionesTareasUiState,
    acciones: RelevamientosAcciones,
    ubicacionObra: Ubicacion?,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        SeccionFormulario(stringResource(Res.string.relevamientos_section_data)) {
            CampoFecha(form.fechaIso, stringResource(Res.string.relevamientos_date), acciones.onDateChange, enabled = !form.guardando)
            OutlinedTextField(
                value = form.nota, onValueChange = acciones.onNoteChange,
                label = { Text(stringResource(Res.string.relevamientos_note)) },
                enabled = !form.guardando, minLines = 3, modifier = Modifier.fillMaxWidth().testTag("relevamientos_note"),
            )
            SelectorTarea(form, tareas, acciones)
        }
        SeccionFormulario(stringResource(Res.string.relevamientos_photo)) { SelectorFoto(form, acciones) }
        SeccionFormulario(stringResource(Res.string.relevamientos_location_optional)) { SelectorUbicacion(form, acciones) }
    }
    if (form.mapaAbierto) SelectorEnMapa(centroInicialDelSelector(form.ubicacion, ubicacionObra), acciones.onMapPick, acciones.onMapClose)
}

/**
 * Barra fija abajo: el error (para que se vea sin desplazar la pantalla) y los botones. Sube con el teclado, así
 * que no queda tapada ni al final de un formulario largo.
 */
@Composable
internal fun BarraGuardarRelevamiento(form: FormularioRelevamientoUiState, acciones: RelevamientosAcciones) {
    Surface(tonalElevation = 3.dp, shadowElevation = 3.dp, modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            form.error?.let { ErrorBlock(mensajeRelevamiento(it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = acciones.onSave, enabled = !form.guardando && !form.capturandoUbicacion,
                    modifier = Modifier.weight(1f).testTag("relevamientos_save"),
                ) {
                    if (form.guardando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(Res.string.relevamientos_save))
                }
                OutlinedButton(onClick = acciones.onCloseForm, enabled = !form.guardando, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.relevamientos_cancel))
                }
            }
        }
    }
}

@Composable
private fun SelectorTarea(form: FormularioRelevamientoUiState, tareas: OpcionesTareasUiState, acciones: RelevamientosAcciones) {
    var abierto by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EtiquetaCampo(stringResource(Res.string.relevamientos_task_optional))
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
}
