package com.unaj.obratrack.domain.model

import kotlinx.datetime.LocalDate

enum class EstadoTarea { PENDIENTE, EN_CURSO, COMPLETADA }

enum class CategoriaTarea {
    CIMIENTOS_Y_EXCAVACION,
    ESTRUCTURA,
    ALBANILERIA,
    TECHOS,
    ELECTRICIDAD,
    PLOMERIA,
    GAS,
    CLIMATIZACION,
    CARPINTERIA,
    HERRERIA,
    PISOS_Y_REVESTIMIENTOS,
    PINTURA,
    ABERTURAS,
    IMPERMEABILIZACION,
    LIMPIEZA,
    OTROS,
}

/** Qué pasa con los relevamientos vinculados al borrar una tarea; nunca se borran sin confirmación. */
enum class RelevamientosVinculados {
    /** Se conservan, sin tarea vinculada. */
    DESVINCULAR,

    /** Se eliminan junto con la tarea. */
    ELIMINAR,
}

data class Tarea(
    val id: String,
    val obraId: String,
    val userId: String,
    val titulo: String,
    val descripcion: String = "",
    val categoria: CategoriaTarea = CategoriaTarea.OTROS,
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null,
    val estado: EstadoTarea = EstadoTarea.PENDIENTE,
)

data class TareaDraft(
    val titulo: String,
    val descripcion: String = "",
    val categoria: CategoriaTarea = CategoriaTarea.OTROS,
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null,
    val estado: EstadoTarea = EstadoTarea.PENDIENTE,
)

data class ResumenTareas(val completadas: Int, val pendientes: Int, val atrasadas: Int)
