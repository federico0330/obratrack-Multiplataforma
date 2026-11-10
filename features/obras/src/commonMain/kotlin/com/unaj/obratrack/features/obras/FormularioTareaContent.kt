package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.CampoFecha
import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Formulario de alta y edición de una tarea, en dos bloques: qué es y cuándo. Guardar está en [BarraGuardarTarea]. */
@Composable
internal fun FormularioTareaContent(form: FormularioTareaUiState, acciones: TareasAcciones) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        SeccionFormulario(stringResource(Res.string.tareas_section_data)) {
            OutlinedTextField(
                form.titulo, acciones.onTitleChange,
                label = { Text(stringResource(Res.string.tareas_field_title)) },
                enabled = !form.guardando, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(
                form.descripcion, acciones.onDescriptionChange,
                label = { Text(stringResource(Res.string.tareas_field_description)) },
                enabled = !form.guardando, minLines = 3, modifier = Modifier.fillMaxWidth(),
            )
            SelectorCategoria(form.categoria, !form.guardando, acciones.onCategoryChange)
        }
        SeccionFormulario(stringResource(Res.string.tareas_section_schedule)) {
            CampoFecha(
                fechaIso = form.fechaInicio,
                label = stringResource(Res.string.tareas_field_start),
                onDateChange = acciones.onStartChange,
                onClear = { acciones.onStartChange("") },
                enabled = !form.guardando,
            )
            CampoFecha(
                fechaIso = form.fechaFin,
                label = stringResource(Res.string.tareas_field_end),
                onDateChange = acciones.onEndChange,
                onClear = { acciones.onEndChange("") },
                enabled = !form.guardando,
            )
        }
    }
}

@Composable
internal fun BarraGuardarTarea(form: FormularioTareaUiState, acciones: TareasAcciones) {
    BarraGuardar(
        error = form.error?.let { mensajeError(it) }, guardando = form.guardando, puedeGuardar = true,
        textoGuardar = stringResource(Res.string.tareas_save), textoCancelar = stringResource(Res.string.tareas_cancel),
        onGuardar = acciones.onSave, onCancelar = acciones.onCloseForm,
    )
}

@Composable
private fun SelectorCategoria(seleccionada: CategoriaTarea, habilitado: Boolean, onChange: (CategoriaTarea) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EtiquetaCampo(stringResource(Res.string.tareas_field_category))
        Box {
            OutlinedButton(onClick = { abierto = true }, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
                Text(nombreCategoria(seleccionada))
            }
            DropdownMenu(expanded = abierto && habilitado, onDismissRequest = { abierto = false }) {
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
}
