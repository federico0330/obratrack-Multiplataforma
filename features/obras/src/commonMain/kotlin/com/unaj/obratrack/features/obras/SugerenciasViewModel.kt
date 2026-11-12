package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.model.aDraft
import com.unaj.obratrack.domain.repository.ErrorSugerir
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import com.unaj.obratrack.domain.usecase.CrearTareasSugeridas
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.ResultadoCrearSugeridas
import com.unaj.obratrack.domain.usecase.SugerirTareas
import com.unaj.obratrack.domain.usecase.SugerirTareasConIa
import com.unaj.obratrack.domain.validation.ValidadorDescripcionObra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sugiere tareas para la obra abierta y crea las que el usuario elige. No conoce textos: los títulos
 * ya resueltos en el idioma de la app llegan desde la pantalla al confirmar.
 */
class SugerenciasViewModel(
    private val observarSesion: ObservarSesion,
    private val sugerirTareas: SugerirTareas,
    private val crearTareasSugeridas: CrearTareasSugeridas,
    /** Null cuando no hay un servicio de IA configurado: la pantalla solo ofrece las plantillas. */
    private val sugerirTareasConIa: SugerirTareasConIa? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(estadoInicial())
    val state = _state.asStateFlow()

    private var userId: String? = null
    private var obraId: String? = null
    private var sugerirJob: Job? = null
    private var crearJob: Job? = null

    init {
        viewModelScope.launch {
            observarSesion()
                .catch { error ->
                    if (error is CancellationException) throw error
                    userId = null
                }
                .collect { usuario -> userId = usuario?.id }
        }
    }

    fun abrir(obraId: String) {
        cancelarTrabajos()
        this.obraId = obraId
        _state.value = estadoInicial().copy(visible = true)
    }

    fun cerrar() {
        cancelarTrabajos()
        reiniciar()
    }

    private fun reiniciar() {
        obraId = null
        _state.value = estadoInicial()
    }

    private fun estadoInicial() = SugerenciasUiState(iaDisponible = sugerirTareasConIa != null)

    fun elegirTipo(tipo: TipoObra) {
        if (_state.value.creando) return
        sugerirJob?.cancel()
        _state.update { it.copy(tipo = tipo, cargando = true, sugerencias = emptyList(), seleccionadas = emptySet(), error = null, creadasAntesDelError = 0) }
        sugerirJob = viewModelScope.launch {
            when (val resultado = sugerirTareas(tipo)) {
                is ResultadoSugerir.Exito -> _state.update {
                    it.copy(cargando = false, sugerencias = resultado.sugerencias, seleccionadas = resultado.sugerencias.toSet())
                }
                is ResultadoSugerir.Fallo -> _state.update { it.copy(cargando = false, error = resultado.error.toUi()) }
            }
        }
    }

    fun descripcionCambiada(texto: String) = _state.update {
        if (it.creando) it else it.copy(descripcion = texto.take(ValidadorDescripcionObra.LARGO_MAXIMO), error = null)
    }

    /** Si falla, la lista que ya hubiera en pantalla (p. ej. la de una plantilla) se conserva. */
    fun sugerirConIa() {
        val ia = sugerirTareasConIa ?: return
        val descripcion = _state.value.takeIf { it.puedeSugerirConIa }?.descripcion ?: return
        sugerirJob?.cancel()
        _state.update { it.copy(cargando = true, error = null, creadasAntesDelError = 0) }
        sugerirJob = viewModelScope.launch {
            when (val resultado = ia(descripcion)) {
                is ResultadoSugerir.Exito -> _state.update {
                    it.copy(cargando = false, tipo = null, sugerencias = resultado.sugerencias, seleccionadas = resultado.sugerencias.toSet())
                }
                is ResultadoSugerir.Fallo -> _state.update { it.copy(cargando = false, error = resultado.error.toUi()) }
            }
        }
    }

    fun alternar(sugerencia: SugerenciaTarea) = _state.update {
        if (it.creando) it
        else it.copy(seleccionadas = if (sugerencia in it.seleccionadas) it.seleccionadas - sugerencia else it.seleccionadas + sugerencia)
    }

    fun seleccionarTodas(seleccionar: Boolean) = _state.update {
        if (it.creando) it else it.copy(seleccionadas = if (seleccionar) it.sugerencias.toSet() else emptySet())
    }

    /** [titulos] trae el título de cada sugerencia ya traducido; solo se crean las elegidas, en el orden de la lista. */
    fun crear(titulos: Map<SugerenciaTarea, String>) {
        val actual = _state.value
        if (!actual.puedeCrear) return
        val uid = userId
        val obra = obraId
        if (uid == null || obra == null) {
            _state.update { it.copy(error = ErrorSugerenciasUi.SESION) }
            return
        }
        val elegidas = actual.sugerencias.filter { it in actual.seleccionadas }
        val drafts = elegidas.map { sugerencia -> sugerencia.aDraft(titulos[sugerencia] ?: return) }
        _state.update { it.copy(creando = true, error = null, creadasAntesDelError = 0) }
        crearJob = viewModelScope.launch {
            when (val resultado = crearTareasSugeridas(uid, obra, drafts)) {
                is ResultadoCrearSugeridas.Exito -> reiniciar()
                is ResultadoCrearSugeridas.Fallo -> {
                    val guardadas = elegidas.take(resultado.creadas).toSet()
                    _state.update {
                        it.copy(
                            creando = false,
                            sugerencias = it.sugerencias - guardadas,
                            seleccionadas = it.seleccionadas - guardadas,
                            creadasAntesDelError = resultado.creadas,
                            error = resultado.error.toUi(),
                        )
                    }
                }
            }
        }
    }

    private fun cancelarTrabajos() {
        sugerirJob?.cancel()
        crearJob?.cancel()
    }

    private fun ErrorSugerir.toUi() = when (this) {
        ErrorSugerir.SIN_CONEXION -> ErrorSugerenciasUi.RED
        ErrorSugerir.LIMITE_ALCANZADO -> ErrorSugerenciasUi.LIMITE
        ErrorSugerir.RESPUESTA_INVALIDA -> ErrorSugerenciasUi.RESPUESTA
        ErrorSugerir.DESCRIPCION_INVALIDA -> ErrorSugerenciasUi.DESCRIPCION
        ErrorSugerir.DESCONOCIDO -> ErrorSugerenciasUi.DESCONOCIDO
    }

    private fun ErrorTarea.toUi() = when (this) {
        ErrorTarea.RED -> ErrorSugerenciasUi.RED
        ErrorTarea.NO_AUTORIZADA -> ErrorSugerenciasUi.SESION
        else -> ErrorSugerenciasUi.DESCONOCIDO
    }
}
