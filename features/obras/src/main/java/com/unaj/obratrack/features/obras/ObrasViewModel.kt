package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.ResultadoObra
import com.unaj.obratrack.domain.usecase.CrearObra
import com.unaj.obratrack.domain.usecase.ActualizarObra
import com.unaj.obratrack.domain.usecase.CambiarEstadoObra
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ObrasViewModel(
    private val observarSesion: ObservarSesion,
    private val observarObras: ObservarObras,
    private val crearObra: CrearObra,
    private val actualizarObra: ActualizarObra,
    private val cambiarEstadoObra: CambiarEstadoObra,
) : ViewModel() {
    private val _state = MutableStateFlow(ObrasUiState())
    val state = _state.asStateFlow()
    private var listaJob: Job? = null
    private var userId: String? = null

    init {
        viewModelScope.launch {
            observarSesion()
                .catch { error ->
                    if (error is CancellationException) throw error
                    userId = null
                    listaJob?.cancel()
                    _state.update {
                        it.copy(
                            lista = ListaObrasUiState.Error(ErrorObrasUi.SESION),
                            obraSeleccionada = null,
                            formularioVisible = false,
                            formulario = FormularioObraUiState(),
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        null -> {
                            userId = null
                            listaJob?.cancel()
                            _state.update {
                                it.copy(
                                    lista = ListaObrasUiState.Error(ErrorObrasUi.SESION),
                                    obraSeleccionada = null,
                                    formularioVisible = false,
                                    formulario = FormularioObraUiState(),
                                )
                            }
                        }
                        else -> observeWorks(result.id)
                    }
                }
        }
    }

    private fun observeWorks(id: String) {
        if (userId == id && listaJob?.isActive == true) return
        userId = id
        listaJob?.cancel()
        _state.update {
            it.copy(
                lista = ListaObrasUiState.Cargando,
                obraSeleccionada = null,
                formularioVisible = false,
                formulario = FormularioObraUiState(),
                errorOperacion = null,
            )
        }
        listaJob = viewModelScope.launch {
            observarObras(id)
                .catch { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(lista = ListaObrasUiState.Error(ErrorObrasUi.DESCONOCIDO)) }
                }
                .collect { result ->
                    _state.update {
                        val listState = when (result) {
                            is ResultadoObra.Exito -> result.dato.takeIf { it.isNotEmpty() }
                                ?.let(ListaObrasUiState::Contenido) ?: ListaObrasUiState.Vacia
                            is ResultadoObra.Fallo -> ListaObrasUiState.Error(result.error.toUiError())
                        }
                        val selected = when (listState) {
                            is ListaObrasUiState.Contenido -> listState.obras.firstOrNull { obra -> obra.id == it.obraSeleccionada?.id }
                            else -> null
                        }
                        it.copy(lista = listState, obraSeleccionada = selected)
                    }
                }
        }
    }

    fun reintentar() {
        userId?.let(::observeWorks)
    }

    fun abrirFormulario() {
        val hoyLocal = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        _state.update { it.copy(formularioVisible = true, formulario = FormularioObraUiState(fechaInicio = hoyLocal)) }
    }

    fun abrirDetalle(obra: Obra) = _state.update { it.copy(obraSeleccionada = obra, errorOperacion = null) }

    fun cerrarDetalle() = _state.update { it.copy(obraSeleccionada = null, errorOperacion = null) }

    fun editarSeleccionada() {
        val obra = _state.value.obraSeleccionada ?: return
        val fecha = obra.fechaInicio.toLocalDateTime(TimeZone.UTC).date.toString()
        _state.update {
            it.copy(
                formularioVisible = true,
                formulario = FormularioObraUiState(
                    obraId = obra.id,
                    nombre = obra.nombre,
                    cliente = obra.cliente,
                    direccion = obra.direccion,
                    fechaInicio = fecha,
                    publica = obra.publica,
                ),
            )
        }
    }

    fun cerrarFormulario() = _state.update { it.copy(formularioVisible = false) }
    fun nombreCambiado(value: String) = updateForm { copy(nombre = value, error = null) }
    fun clienteCambiado(value: String) = updateForm { copy(cliente = value, error = null) }
    fun direccionCambiada(value: String) = updateForm { copy(direccion = value, error = null) }
    fun fechaCambiada(value: String) = updateForm { copy(fechaInicio = value, error = null) }
    fun visibilidadCambiada(value: Boolean) = updateForm { copy(publica = value, error = null) }

    fun guardar() {
        val owner = userId ?: return updateForm { copy(error = ErrorObrasUi.SESION) }
        val form = _state.value.formulario
        if (form.guardando) return
        val fecha = runCatching { kotlinx.datetime.LocalDate.parse(form.fechaInicio) }
            .getOrElse { return updateForm { copy(error = ErrorObrasUi.FECHA) } }
        val draft = ObraDraft(
            nombre = form.nombre,
            cliente = form.cliente,
            direccion = form.direccion,
            publica = form.publica,
            fechaInicio = fecha.atStartOfDayIn(TimeZone.UTC),
        )
        updateForm { copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                val obraId = form.obraId
                val result = if (obraId == null) crearObra(owner, draft) else actualizarObra(owner, obraId, draft)
                when (result) {
                    is ResultadoObra.Exito -> _state.update {
                        it.copy(
                            formularioVisible = false,
                            formulario = FormularioObraUiState(),
                            obraSeleccionada = if (obraId == null) null else it.obraSeleccionada,
                        )
                    }
                    is ResultadoObra.Fallo -> updateForm { copy(guardando = false, error = result.error.toUiError()) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateForm { copy(guardando = false, error = ErrorObrasUi.DESCONOCIDO) }
            }
        }
    }

    fun cambiarEstado(estado: EstadoObra) {
        val owner = userId ?: return _state.update { it.copy(errorOperacion = ErrorObrasUi.SESION) }
        val obra = _state.value.obraSeleccionada ?: return
        if (_state.value.actualizando) return
        _state.update { it.copy(actualizando = true, errorOperacion = null) }
        viewModelScope.launch {
            try {
                when (val result = cambiarEstadoObra(owner, obra.id, estado)) {
                    is ResultadoObra.Exito -> Unit // La suscripción en tiempo real actualiza el detalle.
                    is ResultadoObra.Fallo -> _state.update { it.copy(errorOperacion = result.error.toUiError()) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(errorOperacion = ErrorObrasUi.DESCONOCIDO) }
            } finally {
                _state.update { it.copy(actualizando = false) }
            }
        }
    }

    private fun updateForm(transform: FormularioObraUiState.() -> FormularioObraUiState) =
        _state.update { it.copy(formulario = it.formulario.transform()) }

    private fun ErrorObra.toUiError() = when (this) {
        ErrorObra.NOMBRE_VACIO -> ErrorObrasUi.NOMBRE
        ErrorObra.CLIENTE_VACIO -> ErrorObrasUi.CLIENTE
        ErrorObra.DIRECCION_VACIA -> ErrorObrasUi.DIRECCION
        ErrorObra.NO_AUTORIZADA -> ErrorObrasUi.PERMISOS
        ErrorObra.RED -> ErrorObrasUi.RED
        ErrorObra.DESCONOCIDO -> ErrorObrasUi.DESCONOCIDO
    }
}
