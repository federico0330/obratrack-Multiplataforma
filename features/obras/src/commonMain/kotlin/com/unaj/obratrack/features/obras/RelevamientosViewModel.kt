package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento
import com.unaj.obratrack.domain.repository.ResultadoTarea
import com.unaj.obratrack.domain.usecase.CrearRelevamiento
import com.unaj.obratrack.domain.usecase.ObservarRelevamientos
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
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class RelevamientosViewModel(
    private val observarSesion: ObservarSesion,
    private val observarRelevamientos: ObservarRelevamientos,
    private val crearRelevamiento: CrearRelevamiento,
    private val observarTareas: ObservarTareas,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val _state = MutableStateFlow(RelevamientosUiState())
    val state = _state.asStateFlow()
    private var userId: String? = null
    private var obraId: String? = null
    private var contexto = 0L
    private var formularioVersion = 0L
    private var contextoJob = Job(viewModelScope.coroutineContext[Job])
    private var tareasJob: Job? = null

    init {
        viewModelScope.launch {
            observarSesion().catch { error ->
                if (error is CancellationException) throw error
                cambiarSesion(null)
            }.collect { cambiarSesion(it?.id) }
        }
    }

    private fun cambiarSesion(id: String?) {
        if (id == userId && id != null) return
        userId = id
        cargar()
    }

    fun abrirObra(id: String) {
        if (obraId == id) return
        obraId = id
        cargar()
    }

    fun cerrarObra() {
        obraId = null
        renovarContexto()
        _state.value = RelevamientosUiState()
    }

    fun reintentar() = cargar()

    private fun renovarContexto() {
        contexto++
        contextoJob.cancel()
        contextoJob = Job(viewModelScope.coroutineContext[Job])
    }

    private fun cargar() {
        renovarContexto()
        val owner = userId
        if (owner == null) {
            _state.value = RelevamientosUiState(lista = ListaRelevamientosUiState.Error(ErrorRelevamientosUi.SESION))
            return
        }
        _state.value = RelevamientosUiState()
        val obra = obraId ?: return
        val revision = contexto
        viewModelScope.launch(contextoJob) {
            observarRelevamientos(owner, obra).catch { error ->
                if (error is CancellationException) throw error
                actualizar(revision) { it.copy(lista = ListaRelevamientosUiState.Error(ErrorRelevamientosUi.DESCONOCIDO)) }
            }.collect { result ->
                val lista = when (result) {
                    is ResultadoRelevamiento.Fallo -> ListaRelevamientosUiState.Error(result.error.toUi())
                    is ResultadoRelevamiento.Exito -> if (result.dato.isEmpty()) ListaRelevamientosUiState.Vacia
                        else ListaRelevamientosUiState.Contenido(result.dato)
                }
                actualizar(revision) { it.copy(lista = lista) }
            }
        }
        reintentarTareas()
    }

    fun reintentarTareas() {
        tareasJob?.cancel()
        val owner = userId ?: return
        val obra = obraId ?: return
        val revision = contexto
        _state.update { it.copy(tareas = OpcionesTareasUiState()) }
        tareasJob = viewModelScope.launch(contextoJob) {
            observarTareas(owner, obra).catch { error ->
                if (error is CancellationException) throw error
                actualizar(revision) { it.copy(tareas = OpcionesTareasUiState(cargando = false, error = true)) }
            }.collect { result ->
                val opciones = when (result) {
                    is ResultadoTarea.Exito -> OpcionesTareasUiState(result.dato, cargando = false)
                    is ResultadoTarea.Fallo -> OpcionesTareasUiState(cargando = false, error = true)
                }
                actualizar(revision) { it.copy(tareas = opciones) }
            }
        }
    }

    fun abrirFormulario() {
        formularioVersion++
        _state.update {
            it.copy(formularioVisible = true, formulario = FormularioRelevamientoUiState(
                fechaIso = clock.todayIn(TimeZone.currentSystemDefault()).toString(),
            ))
        }
    }

    fun cerrarFormulario() {
        if (_state.value.formulario.guardando) return
        formularioVersion++
        _state.update { it.copy(formularioVisible = false) }
    }

    fun notaCambiada(value: String) = editarFormulario { copy(nota = value, error = null) }
    fun fechaCambiada(value: String) = editarFormulario { copy(fechaIso = value, error = null) }
    fun tareaCambiada(value: String?) = editarFormulario { copy(tareaId = value, error = null) }

    private fun editarFormulario(transform: FormularioRelevamientoUiState.() -> FormularioRelevamientoUiState) {
        _state.update { if (it.formulario.guardando) it else it.copy(formulario = it.formulario.transform()) }
    }

    fun guardar() {
        val form = _state.value.formulario
        if (form.guardando) return
        val owner = userId ?: return errorFormulario(ErrorRelevamientosUi.SESION)
        val obra = obraId ?: return
        val fecha = runCatching { LocalDate.parse(form.fechaIso).atStartOfDayIn(TimeZone.UTC) }.getOrNull()
            ?: return errorFormulario(ErrorRelevamientosUi.FECHA)
        val revision = contexto
        val versionForm = formularioVersion
        _state.update { it.copy(formulario = form.copy(guardando = true, error = null)) }
        viewModelScope.launch(contextoJob) {
            try {
                val result = crearRelevamiento(owner, obra, RelevamientoDraft(fecha, form.nota, form.tareaId))
                if (revision != contexto || versionForm != formularioVersion) return@launch
                when (result) {
                    is ResultadoRelevamiento.Exito -> _state.update {
                        it.copy(formularioVisible = false, formulario = FormularioRelevamientoUiState())
                    }
                    is ResultadoRelevamiento.Fallo -> errorFormulario(result.error.toUi())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision == contexto && versionForm == formularioVersion) errorFormulario(ErrorRelevamientosUi.DESCONOCIDO)
            }
        }
    }

    private fun errorFormulario(error: ErrorRelevamientosUi) {
        _state.update { it.copy(formulario = it.formulario.copy(guardando = false, error = error)) }
    }

    private inline fun actualizar(revision: Long, transform: (RelevamientosUiState) -> RelevamientosUiState) {
        if (revision == contexto) _state.update(transform)
    }

    private fun ErrorRelevamiento.toUi() = when (this) {
        ErrorRelevamiento.NOTA_VACIA -> ErrorRelevamientosUi.NOTA
        ErrorRelevamiento.FECHA_INVALIDA -> ErrorRelevamientosUi.FECHA
        ErrorRelevamiento.NO_AUTORIZADA -> ErrorRelevamientosUi.SESION
        ErrorRelevamiento.TAREA_NO_DISPONIBLE -> ErrorRelevamientosUi.TAREA
        ErrorRelevamiento.RED -> ErrorRelevamientosUi.RED
        ErrorRelevamiento.DESCONOCIDO -> ErrorRelevamientosUi.DESCONOCIDO
    }
}
