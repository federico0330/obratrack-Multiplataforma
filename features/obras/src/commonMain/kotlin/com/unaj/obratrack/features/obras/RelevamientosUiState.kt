package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.FiltroRelevamientos
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.CandidatoDireccion
import com.unaj.obratrack.domain.validation.ValidadorBusquedaDireccion

sealed interface ListaRelevamientosUiState {
    data object Cargando : ListaRelevamientosUiState
    data object Vacia : ListaRelevamientosUiState
    data class Contenido(val items: List<Relevamiento>) : ListaRelevamientosUiState
    data class Error(val motivo: ErrorRelevamientosUi) : ListaRelevamientosUiState
}

enum class ErrorRelevamientosUi {
    NOTA, FECHA, SESION, TAREA, INEXISTENTE, RED, DESCONOCIDO,
    FOTO_SIN_CONEXION, FOTO_MUY_GRANDE, FOTO_FALLO, FOTO_LECTURA, FOTO_CAMARA,
    UBICACION_SIN_PERMISO, UBICACION_DESACTIVADA, UBICACION_NO_DISPONIBLE, UBICACION_INVALIDA,
}

/** Foto que muestra el formulario: ninguna, la que ya estaba guardada o una recién elegida. */
sealed interface FotoFormulario {
    data object Ninguna : FotoFormulario
    data class Existente(val url: String, val vistaUrl: String) : FotoFormulario
    class Nueva(val bytes: ByteArray) : FotoFormulario
}

/** Dirección del relevamiento abierto en el detalle. Se carga aparte, nunca se guarda y si falta se ven solo las coordenadas. */
sealed interface DireccionUiState {
    /** Sin ubicación, o todavía sin consultar. */
    data object Ninguna : DireccionUiState
    data object Cargando : DireccionUiState
    data class Disponible(val texto: String) : DireccionUiState

    /** Falló, no hay dirección para esas coordenadas o el dispositivo no geocodifica: no es un error para el usuario. */
    data object NoDisponible : DireccionUiState
}

sealed interface ResultadosBusquedaUi {
    data object SinBuscar : ResultadosBusquedaUi
    data object Buscando : ResultadosBusquedaUi
    data class Lista(val items: List<CandidatoDireccion>) : ResultadosBusquedaUi
    data object SinResultados : ResultadosBusquedaUi
    data object NoDisponible : ResultadosBusquedaUi
}

/** Búsqueda de una dirección para elegir la ubicación sin estar en la obra. */
data class BusquedaDireccionUiState(
    /** El dispositivo geocodifica; si no, el buscador no se ofrece. */
    val disponible: Boolean = false,
    val abierta: Boolean = false,
    val texto: String = "",
    val resultados: ResultadosBusquedaUi = ResultadosBusquedaUi.SinBuscar,
) {
    val puedeBuscar: Boolean
        get() = ValidadorBusquedaDireccion.normalizar(texto) != null && resultados != ResultadosBusquedaUi.Buscando
}

data class FormularioRelevamientoUiState(
    val id: String? = null,
    val nota: String = "",
    val fechaIso: String = "",
    val tareaId: String? = null,
    val foto: FotoFormulario = FotoFormulario.Ninguna,
    /** Foto guardada al abrir la edición; si se quita, al guardar se elimina. */
    val fotoOriginal: FotoFormulario.Existente? = null,
    val ubicacion: Ubicacion? = null,
    /** Ubicación guardada al abrir la edición; si se quita, al guardar se elimina el campo. */
    val ubicacionOriginal: Ubicacion? = null,
    /** Mientras es true hay una búsqueda en curso y no se puede guardar. */
    val capturandoUbicacion: Boolean = false,
    val busqueda: BusquedaDireccionUiState = BusquedaDireccionUiState(),
    val guardando: Boolean = false,
    val error: ErrorRelevamientosUi? = null,
)

data class OpcionesTareasUiState(
    val items: List<Tarea> = emptyList(),
    val cargando: Boolean = true,
    val error: Boolean = false,
)

data class RelevamientosUiState(
    val lista: ListaRelevamientosUiState = ListaRelevamientosUiState.Cargando,
    val formularioVisible: Boolean = false,
    val formulario: FormularioRelevamientoUiState = FormularioRelevamientoUiState(),
    val tareas: OpcionesTareasUiState = OpcionesTareasUiState(),
    val seleccionado: Relevamiento? = null,
    val direccion: DireccionUiState = DireccionUiState.Ninguna,
    val aEliminar: Relevamiento? = null,
    val eliminando: Boolean = false,
    val errorOperacion: ErrorRelevamientosUi? = null,
    val filtro: FiltroRelevamientos = FiltroRelevamientos.Todos,
)
