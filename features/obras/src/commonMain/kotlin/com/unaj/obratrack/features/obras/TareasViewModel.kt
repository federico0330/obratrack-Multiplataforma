package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.model.estaAtrasada
import com.unaj.obratrack.domain.model.resumen
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.ResultadoTarea
import com.unaj.obratrack.domain.usecase.ActualizarTarea
import com.unaj.obratrack.domain.usecase.CambiarEstadoTarea
import com.unaj.obratrack.domain.usecase.CrearTarea
import com.unaj.obratrack.domain.usecase.EliminarTarea
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.ObservarTareas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Tareas de la obra abierta: observa la lista y gestiona formulario, estado y baja. */
class TareasViewModel(
    private val observarSesion: ObservarSesion,
    private val observarTareas: ObservarTareas,
    private val crearTarea: CrearTarea,
    private val actualizarTarea: ActualizarTarea,
    private val cambiarEstadoTarea: CambiarEstadoTarea,
    private val eliminarTarea: EliminarTarea,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val _state = MutableStateFlow(TareasUiState())
    val state = _state.asStateFlow()
    private var listaJob: Job? = null
    private var userId: String? = null
    private var obraId: String? = null

    init {
        viewModelScope.launch {
            observarSesion()
                .catch { error ->
                    if (error is CancellationException) throw error
                    sesionPerdida()
                }
                .collect { usuario ->
                    if (usuario == null) {
                        sesionPerdida()
                    } else if (usuario.id != userId) {
                        userId = usuario.id
                        observar()
                    }
                }
        }
    }

    /** Abre las tareas de una obra; si ya era la actual no reinicia la consulta. */
    fun abrirObra(id: String) {
        if (obraId == id) return
        obraId = id
        observar()
    }

    fun cerrarObra() {
        obraId = null
        listaJob?.cancel()
        _state.value = TareasUiState()
    }

    fun reintentar() = observar()

    private fun sesionPerdida() {
        userId = null
        listaJob?.cancel()
        _state.value = TareasUiState(lista = ListaTareasUiState.Error(ErrorTareasUi.SESION))
    }

    private fun observar() {
        listaJob?.cancel()
        val owner = userId ?: return
        val obra = obraId ?: return
        _state.value = TareasUiState()
        listaJob = viewModelScope.launch {
            observarTareas(owner, obra)
                .catch { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(lista = ListaTareasUiState.Error(ErrorTareasUi.DESCONOCIDO)) }
                }
                .collect { result ->
                    _state.update { it.copy(lista = result.aListaUi()) }
                }
        }
    }

    private fun ResultadoTarea<List<Tarea>>.aListaUi(): ListaTareasUiState = when (this) {
        is ResultadoTarea.Fallo -> ListaTareasUiState.Error(error.toUiError())
        is ResultadoTarea.Exito -> if (dato.isEmpty()) {
            ListaTareasUiState.Vacia
        } else {
            val hoy = clock.todayIn(TimeZone.currentSystemDefault())
            ListaTareasUiState.Contenido(
                items = dato.map { TareaItem(it, it.estaAtrasada(hoy)) },
                resumen = dato.resumen(hoy),
            )
        }
    }

    fun abrirFormulario() = _state.update {
        it.copy(formularioVisible = true, formulario = FormularioTareaUiState(), errorOperacion = null)
    }

    fun editar(tarea: Tarea) = _state.update {
        it.copy(
            formularioVisible = true,
            errorOperacion = null,
            formulario = FormularioTareaUiState(
                tareaId = tarea.id,
                titulo = tarea.titulo,
                descripcion = tarea.descripcion,
                categoria = tarea.categoria,
                fechaInicio = tarea.fechaInicio?.toString().orEmpty(),
                fechaFin = tarea.fechaFin?.toString().orEmpty(),
                estado = tarea.estado,
            ),
        )
    }

    fun cerrarFormulario() = _state.update { it.copy(formularioVisible = false) }
    fun tituloCambiado(value: String) = updateForm { copy(titulo = value, error = null) }
    fun descripcionCambiada(value: String) = updateForm { copy(descripcion = value, error = null) }
    fun categoriaCambiada(value: CategoriaTarea) = updateForm { copy(categoria = value, error = null) }
    fun fechaInicioCambiada(value: String) = updateForm { copy(fechaInicio = value, error = null) }
    fun fechaFinCambiada(value: String) = updateForm { copy(fechaFin = value, error = null) }

    fun guardar() {
        val owner = userId ?: return updateForm { copy(error = ErrorTareasUi.SESION) }
        val obra = obraId ?: return
        val form = _state.value.formulario
        if (form.guardando) return
        val inicio = parsearFecha(form.fechaInicio) ?: return updateForm { copy(error = ErrorTareasUi.FECHA_FORMATO) }
        val fin = parsearFecha(form.fechaFin) ?: return updateForm { copy(error = ErrorTareasUi.FECHA_FORMATO) }
        val draft = TareaDraft(
            titulo = form.titulo,
            descripcion = form.descripcion,
            categoria = form.categoria,
            fechaInicio = inicio.valor,
            fechaFin = fin.valor,
            estado = form.estado,
        )
        updateForm { copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                val tareaId = form.tareaId
                val error = if (tareaId == null) {
                    (crearTarea(owner, obra, draft) as? ResultadoTarea.Fallo)?.error
                } else {
                    (actualizarTarea(owner, obra, tareaId, draft) as? ResultadoTarea.Fallo)?.error
                }
                if (error == null) {
                    _state.update { it.copy(formularioVisible = false, formulario = FormularioTareaUiState()) }
                } else {
                    updateForm { copy(guardando = false, error = error.toUiError()) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateForm { copy(guardando = false, error = ErrorTareasUi.DESCONOCIDO) }
            }
        }
    }

    fun cambiarEstado(tarea: Tarea, estado: EstadoTarea) {
        val owner = userId ?: return _state.update { it.copy(errorOperacion = ErrorTareasUi.SESION) }
        val obra = obraId ?: return
        if (_state.value.actualizandoId != null) return
        _state.update { it.copy(actualizandoId = tarea.id, errorOperacion = null) }
        viewModelScope.launch {
            try {
                val resultado = cambiarEstadoTarea(owner, obra, tarea.id, estado)
                if (resultado is ResultadoTarea.Fallo) {
                    _state.update { it.copy(errorOperacion = resultado.error.toUiError()) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(errorOperacion = ErrorTareasUi.DESCONOCIDO) }
            } finally {
                _state.update { it.copy(actualizandoId = null) }
            }
        }
    }

    fun solicitarEliminacion(tarea: Tarea) = _state.update {
        it.copy(tareaAEliminar = tarea, vinculados = RelevamientosVinculados.DESVINCULAR, errorOperacion = null)
    }

    fun vinculadosCambiados(value: RelevamientosVinculados) = _state.update { it.copy(vinculados = value) }

    fun cancelarEliminacion() = _state.update { it.copy(tareaAEliminar = null) }

    fun confirmarEliminacion() {
        val owner = userId ?: return _state.update { it.copy(errorOperacion = ErrorTareasUi.SESION) }
        val obra = obraId ?: return
        val estado = _state.value
        val tarea = estado.tareaAEliminar ?: return
        if (estado.eliminando) return
        _state.update { it.copy(tareaAEliminar = null, eliminando = true, errorOperacion = null) }
        viewModelScope.launch {
            try {
                val resultado = eliminarTarea(owner, obra, tarea.id, estado.vinculados)
                if (resultado is ResultadoTarea.Fallo) {
                    _state.update { it.copy(errorOperacion = resultado.error.toUiError()) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(errorOperacion = ErrorTareasUi.DESCONOCIDO) }
            } finally {
                _state.update { it.copy(eliminando = false) }
            }
        }
    }

    private fun updateForm(transform: FormularioTareaUiState.() -> FormularioTareaUiState) =
        _state.update { it.copy(formulario = it.formulario.transform()) }

    /** Fecha opcional: vacía es válida y vale null; null en el resultado significa formato inválido. */
    private fun parsearFecha(texto: String): FechaOpcional? {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return FechaOpcional(null)
        return runCatching { LocalDate.parse(limpio) }.getOrNull()?.let(::FechaOpcional)
    }

    private class FechaOpcional(val valor: LocalDate?)

    private fun ErrorTarea.toUiError() = when (this) {
        ErrorTarea.TITULO_VACIO -> ErrorTareasUi.TITULO
        ErrorTarea.FECHAS_INCOMPLETAS -> ErrorTareasUi.FECHAS_INCOMPLETAS
        ErrorTarea.FECHA_FIN_ANTERIOR -> ErrorTareasUi.FECHA_FIN_ANTERIOR
        ErrorTarea.NO_AUTORIZADA -> ErrorTareasUi.PERMISOS
        ErrorTarea.RED -> ErrorTareasUi.RED
        ErrorTarea.FALLO_PARCIAL -> ErrorTareasUi.BAJA_PARCIAL
        ErrorTarea.DESCONOCIDO -> ErrorTareasUi.DESCONOCIDO
    }
}
