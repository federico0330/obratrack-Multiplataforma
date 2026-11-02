package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.ResumenTareas
import com.unaj.obratrack.domain.model.Tarea

/** Una tarea de la lista con su condición de atraso ya calculada contra "hoy". */
data class TareaItem(val tarea: Tarea, val atrasada: Boolean)

sealed interface ListaTareasUiState {
    data object Cargando : ListaTareasUiState
    data object Vacia : ListaTareasUiState
    data class Contenido(val items: List<TareaItem>, val resumen: ResumenTareas) : ListaTareasUiState
    data class Error(val motivo: ErrorTareasUi) : ListaTareasUiState
}

enum class ErrorTareasUi {
    SESION,
    TITULO,
    FECHAS_INCOMPLETAS,
    FECHA_FIN_ANTERIOR,
    FECHA_FORMATO,
    RED,
    PERMISOS,
    BAJA_PARCIAL,
    DESCONOCIDO,
}

data class FormularioTareaUiState(
    val tareaId: String? = null,
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: CategoriaTarea = CategoriaTarea.OTROS,
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val estado: EstadoTarea = EstadoTarea.PENDIENTE,
    val guardando: Boolean = false,
    val error: ErrorTareasUi? = null,
)

data class TareasUiState(
    val lista: ListaTareasUiState = ListaTareasUiState.Cargando,
    val formularioVisible: Boolean = false,
    val formulario: FormularioTareaUiState = FormularioTareaUiState(),
    val actualizandoId: String? = null,
    val errorOperacion: ErrorTareasUi? = null,
    val tareaAEliminar: Tarea? = null,
    val vinculados: RelevamientosVinculados = RelevamientosVinculados.DESVINCULAR,
    val eliminando: Boolean = false,
)
