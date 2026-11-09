package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.CambioFoto
import com.unaj.obratrack.domain.model.CambioUbicacion
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.FiltroRelevamientos
import com.unaj.obratrack.domain.model.filtrar
import com.unaj.obratrack.domain.usecase.ActualizarRelevamiento
import com.unaj.obratrack.domain.usecase.EliminarRelevamiento
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento
import com.unaj.obratrack.domain.repository.ResultadoTarea
import com.unaj.obratrack.domain.repository.ResultadoUbicacion
import com.unaj.obratrack.domain.usecase.CrearRelevamiento
import com.unaj.obratrack.domain.usecase.ObservarRelevamientos
import com.unaj.obratrack.domain.usecase.ObtenerUbicacionActual
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.ObservarTareas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class RelevamientosViewModel(
    private val observarSesion: ObservarSesion,
    private val observarRelevamientos: ObservarRelevamientos,
    private val crearRelevamiento: CrearRelevamiento,
    private val observarTareas: ObservarTareas,
    private val actualizarRelevamiento: ActualizarRelevamiento,
    private val eliminarRelevamiento: EliminarRelevamiento,
    private val obtenerUbicacionActual: ObtenerUbicacionActual,
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
    private var capturaJob: Job? = null
    private var listaCompleta: List<Relevamiento> = emptyList()

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
        capturaJob = null
        listaCompleta = emptyList()
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
                if (revision == contexto) recibirLista(result)
            }
        }
        reintentarTareas()
    }

    private fun recibirLista(result: ResultadoRelevamiento<List<Relevamiento>>) {
        when (result) {
            is ResultadoRelevamiento.Fallo -> _state.update {
                it.copy(lista = ListaRelevamientosUiState.Error(result.error.toUi()))
            }
            is ResultadoRelevamiento.Exito -> {
                listaCompleta = result.dato
                val anterior = _state.value.seleccionado
                if (anterior != null && listaCompleta.none { it.id == anterior.id }) nuevaVersionFormulario()
                _state.update { actual ->
                    val seleccionado = actual.seleccionado?.let { previo -> listaCompleta.find { it.id == previo.id } }
                    val desaparecio = actual.seleccionado != null && seleccionado == null
                    actual.copy(
                        lista = listaFiltrada(actual.filtro), seleccionado = seleccionado,
                        formularioVisible = if (desaparecio) false else actual.formularioVisible,
                        formulario = if (desaparecio) FormularioRelevamientoUiState() else actual.formulario,
                        aEliminar = if (desaparecio) null else actual.aEliminar,
                        errorOperacion = if (desaparecio && !actual.eliminando) ErrorRelevamientosUi.INEXISTENTE else actual.errorOperacion,
                    )
                }
            }
        }
    }

    private fun listaFiltrada(filtro: FiltroRelevamientos): ListaRelevamientosUiState {
        val items = listaCompleta.filtrar(filtro)
        return if (items.isEmpty()) ListaRelevamientosUiState.Vacia else ListaRelevamientosUiState.Contenido(items)
    }

    fun filtroCambiado(filtro: FiltroRelevamientos) {
        _state.update {
            val lista = when (it.lista) {
                ListaRelevamientosUiState.Cargando, is ListaRelevamientosUiState.Error -> it.lista
                else -> listaFiltrada(filtro)
            }
            it.copy(filtro = filtro, lista = lista)
        }
    }

    fun abrirDetalle(item: Relevamiento) {
        val actual = listaCompleta.find { it.id == item.id } ?: return
        if (_state.value.eliminando || _state.value.formulario.guardando) return
        _state.update { it.copy(seleccionado = actual, errorOperacion = null) }
    }

    fun cerrarDetalle() {
        if (_state.value.eliminando || _state.value.formulario.guardando) return
        nuevaVersionFormulario()
        _state.update { it.copy(seleccionado = null, formularioVisible = false, aEliminar = null, errorOperacion = null) }
    }

    fun editarSeleccionado() {
        val item = _state.value.seleccionado ?: return
        if (_state.value.eliminando) return
        nuevaVersionFormulario()
        val foto = fotoMostrable(item)
        _state.update { it.copy(formularioVisible = true, errorOperacion = null, formulario = FormularioRelevamientoUiState(
            id = item.id, nota = item.nota, fechaIso = item.fecha.toLocalDateTime(TimeZone.UTC).date.toString(), tareaId = item.tareaId,
            foto = foto ?: FotoFormulario.Ninguna, fotoOriginal = foto,
            ubicacion = item.ubicacion, ubicacionOriginal = item.ubicacion,
        )) }
    }

    /** Solo se muestran URLs web; otro valor en `fotoUrl` se conserva sin tocarlo. */
    private fun fotoMostrable(item: Relevamiento): FotoFormulario.Existente? {
        val url = item.fotoWeb() ?: return null
        return FotoFormulario.Existente(url, item.fotoVistaUrl ?: url)
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
        if (_state.value.eliminando || _state.value.formulario.guardando) return
        nuevaVersionFormulario()
        _state.update {
            it.copy(seleccionado = null, errorOperacion = null, formularioVisible = true, formulario = FormularioRelevamientoUiState(
                fechaIso = clock.todayIn(TimeZone.currentSystemDefault()).toString(),
            ))
        }
    }

    fun cerrarFormulario() {
        if (_state.value.formulario.guardando) return
        nuevaVersionFormulario()
        _state.update { it.copy(formularioVisible = false) }
    }

    /** Invalida lo que dependa del formulario anterior: respuestas tardías y la búsqueda de ubicación en curso. */
    private fun nuevaVersionFormulario() {
        formularioVersion++
        cortarCaptura()
    }

    private fun cortarCaptura() {
        capturaJob?.cancel()
        capturaJob = null
        _state.update {
            if (it.formulario.capturandoUbicacion) it.copy(formulario = it.formulario.copy(capturandoUbicacion = false)) else it
        }
    }

    fun notaCambiada(value: String) = editarFormulario { copy(nota = value, error = null) }
    fun fechaCambiada(value: String) = editarFormulario { copy(fechaIso = value, error = null) }
    fun tareaCambiada(value: String?) = editarFormulario { copy(tareaId = value, error = null) }
    fun fotoElegida(bytes: ByteArray) = editarFormulario { copy(foto = FotoFormulario.Nueva(bytes), error = null) }
    fun quitarFoto() = editarFormulario { copy(foto = FotoFormulario.Ninguna, error = null) }
    fun fotoNoLeida() = editarFormulario { copy(error = ErrorRelevamientosUi.FOTO_LECTURA) }
    fun camaraNoDisponible() = editarFormulario { copy(error = ErrorRelevamientosUi.FOTO_CAMARA) }
    fun quitarUbicacion() = editarFormulario { if (capturandoUbicacion) this else copy(ubicacion = null, error = null) }

    /** Busca la ubicación del dispositivo. Una respuesta de otro formulario, obra o sesión se descarta. */
    fun capturarUbicacion() {
        val actual = _state.value
        if (!actual.formularioVisible || actual.formulario.guardando || actual.formulario.capturandoUbicacion) return
        val revision = contexto
        val versionForm = formularioVersion
        editarFormulario { copy(capturandoUbicacion = true, error = null) }
        capturaJob = viewModelScope.launch(contextoJob) {
            val resultado = try {
                obtenerUbicacionActual()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ResultadoUbicacion.NoDisponible
            }
            // El diálogo del permiso no se puede cancelar: si lo cancelaron mientras esperaba, la respuesta ya no cuenta.
            ensureActive()
            if (revision == contexto && versionForm == formularioVersion) editarFormulario { conResultado(resultado) }
        }
    }

    /** El usuario cancela: conserva la ubicación que había y no muestra ningún error. */
    fun cancelarCaptura() = cortarCaptura()

    private fun FormularioRelevamientoUiState.conResultado(resultado: ResultadoUbicacion): FormularioRelevamientoUiState =
        when (resultado) {
            is ResultadoUbicacion.Exito -> copy(ubicacion = resultado.ubicacion, capturandoUbicacion = false, error = null)
            ResultadoUbicacion.SinPermiso -> copy(capturandoUbicacion = false, error = ErrorRelevamientosUi.UBICACION_SIN_PERMISO)
            ResultadoUbicacion.Desactivada -> copy(capturandoUbicacion = false, error = ErrorRelevamientosUi.UBICACION_DESACTIVADA)
            ResultadoUbicacion.NoDisponible -> copy(capturandoUbicacion = false, error = ErrorRelevamientosUi.UBICACION_NO_DISPONIBLE)
        }

    private fun editarFormulario(transform: FormularioRelevamientoUiState.() -> FormularioRelevamientoUiState) {
        // Una foto puede terminar de leerse con el formulario ya cerrado: se ignora.
        _state.update { if (it.formulario.guardando || !it.formularioVisible) it else it.copy(formulario = it.formulario.transform()) }
    }

    private fun FormularioRelevamientoUiState.cambioFoto(): CambioFoto = when (val actual = foto) {
        is FotoFormulario.Nueva -> CambioFoto.Nueva(actual.bytes)
        is FotoFormulario.Existente -> CambioFoto.SinCambio
        FotoFormulario.Ninguna -> if (fotoOriginal != null) CambioFoto.Quitar else CambioFoto.SinCambio
    }

    /** Sin tocar la ubicación se pide `SinCambio`, así la que ya tenía el documento no se pierde. */
    private fun FormularioRelevamientoUiState.cambioUbicacion(): CambioUbicacion = when {
        ubicacion != null && ubicacion != ubicacionOriginal -> CambioUbicacion.Nueva(ubicacion)
        ubicacion == null && ubicacionOriginal != null -> CambioUbicacion.Quitar
        else -> CambioUbicacion.SinCambio
    }

    fun guardar() {
        val form = _state.value.formulario
        // Con una búsqueda en curso la ubicación todavía puede cambiar: se espera o se cancela.
        if (form.guardando || form.capturandoUbicacion || _state.value.eliminando) return
        val owner = userId ?: return errorFormulario(ErrorRelevamientosUi.SESION)
        val obra = obraId ?: return
        val fecha = runCatching { LocalDate.parse(form.fechaIso).atStartOfDayIn(TimeZone.UTC) }.getOrNull()
            ?: return errorFormulario(ErrorRelevamientosUi.FECHA)
        val revision = contexto
        val versionForm = formularioVersion
        _state.update { it.copy(formulario = form.copy(guardando = true, error = null)) }
        viewModelScope.launch(contextoJob) {
            try {
                val draft = RelevamientoDraft(fecha, form.nota, form.tareaId, form.cambioFoto(), form.cambioUbicacion())
                val error = if (form.id == null) {
                    (crearRelevamiento(owner, obra, draft) as? ResultadoRelevamiento.Fallo)?.error
                } else {
                    (actualizarRelevamiento(owner, obra, form.id, draft) as? ResultadoRelevamiento.Fallo)?.error
                }
                if (revision != contexto || versionForm != formularioVersion) return@launch
                if (error == null) {
                    _state.update {
                        val filtro = if (form.id == null) FiltroRelevamientos.Todos else it.filtro
                        it.copy(formularioVisible = false, formulario = FormularioRelevamientoUiState(), filtro = filtro,
                            lista = if (it.lista is ListaRelevamientosUiState.Error) it.lista else listaFiltrada(filtro))
                    }
                } else errorFormulario(error.toUi())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision == contexto && versionForm == formularioVersion) errorFormulario(ErrorRelevamientosUi.DESCONOCIDO)
            }
        }
    }

    fun solicitarEliminacion() {
        val actual = _state.value
        if (actual.eliminando || actual.formulario.guardando) return
        _state.update { it.copy(aEliminar = it.seleccionado, errorOperacion = null) }
    }

    fun cancelarEliminacion() = _state.update { it.copy(aEliminar = null) }

    fun confirmarEliminacion() {
        val owner = userId ?: return
        val obra = obraId ?: return
        val actual = _state.value
        val item = actual.aEliminar ?: return
        if (actual.eliminando) return
        val revision = contexto
        _state.update { it.copy(aEliminar = null, eliminando = true, errorOperacion = null) }
        viewModelScope.launch(contextoJob) {
            try {
                val result = eliminarRelevamiento(owner, obra, item.id)
                actualizar(revision) {
                    when (result) {
                        is ResultadoRelevamiento.Exito -> it.copy(seleccionado = null, formularioVisible = false)
                        is ResultadoRelevamiento.Fallo -> it.copy(errorOperacion = result.error.toUi())
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                actualizar(revision) { it.copy(errorOperacion = ErrorRelevamientosUi.DESCONOCIDO) }
            } finally {
                actualizar(revision) { it.copy(eliminando = false) }
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
        ErrorRelevamiento.NO_ENCONTRADO -> ErrorRelevamientosUi.INEXISTENTE
        ErrorRelevamiento.NOTA_VACIA -> ErrorRelevamientosUi.NOTA
        ErrorRelevamiento.FECHA_INVALIDA -> ErrorRelevamientosUi.FECHA
        ErrorRelevamiento.NO_AUTORIZADA -> ErrorRelevamientosUi.SESION
        ErrorRelevamiento.TAREA_NO_DISPONIBLE -> ErrorRelevamientosUi.TAREA
        ErrorRelevamiento.RED -> ErrorRelevamientosUi.RED
        ErrorRelevamiento.DESCONOCIDO -> ErrorRelevamientosUi.DESCONOCIDO
        ErrorRelevamiento.FOTO_SIN_CONEXION -> ErrorRelevamientosUi.FOTO_SIN_CONEXION
        ErrorRelevamiento.FOTO_MUY_GRANDE -> ErrorRelevamientosUi.FOTO_MUY_GRANDE
        ErrorRelevamiento.FOTO_FALLO -> ErrorRelevamientosUi.FOTO_FALLO
        ErrorRelevamiento.UBICACION_INVALIDA -> ErrorRelevamientosUi.UBICACION_INVALIDA
    }
}
