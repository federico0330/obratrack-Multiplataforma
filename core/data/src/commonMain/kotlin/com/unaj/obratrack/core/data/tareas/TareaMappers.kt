package com.unaj.obratrack.core.data.tareas

import com.unaj.obratrack.core.firebase.tareas.TareaDto
import com.unaj.obratrack.core.firebase.tareas.TareaUpdateFields
import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import kotlinx.datetime.LocalDate

/** Una categoría desconocida es solo una etiqueta: se muestra como OTROS en vez de ocultar la tarea. */
internal fun TareaDto.toDomain(): Tarea = Tarea(
    id = id,
    obraId = obraId,
    userId = userId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = CategoriaTarea.entries.firstOrNull { it.name == categoria } ?: CategoriaTarea.OTROS,
    fechaInicio = fechaInicio?.let(LocalDate::parse),
    fechaFin = fechaFin?.let(LocalDate::parse),
    estado = estado.toEstadoTarea(),
)

internal fun TareaDraft.toDto(userId: String, obraId: String): TareaDto = TareaDto(
    id = "",
    obraId = obraId,
    userId = userId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.name,
    estado = estado.name,
    fechaInicio = fechaInicio?.toString(),
    fechaFin = fechaFin?.toString(),
)

internal fun TareaDraft.toUpdateFields(): TareaUpdateFields = TareaUpdateFields(
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.name,
    estado = estado.name,
    fechaInicio = fechaInicio?.toString(),
    fechaFin = fechaFin?.toString(),
)

/** El estado sí es dato de negocio: uno desconocido es un error, no se adivina. */
private fun String.toEstadoTarea(): EstadoTarea =
    EstadoTarea.entries.firstOrNull { it.name == this } ?: error("Estado de tarea desconocido")
