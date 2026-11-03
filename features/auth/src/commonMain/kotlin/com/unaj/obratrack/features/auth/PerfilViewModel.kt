package com.unaj.obratrack.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.resumenObras
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.ResultadoObra
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Resumen de obras del usuario con sesión. Reutiliza el caso de uso que ya lista las obras y
 * solo escucha mientras la pantalla de perfil está visible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModel(
    observarSesion: ObservarSesion,
    observarObras: ObservarObras,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)

    // Último valor emitido y de quién era. Permite saber, al reiniciar la consulta, si hay un
    // valor conocido del mismo usuario que se pueda seguir mostrando mientras se actualiza.
    private var ultimoValor: ResumenPerfilUiState = ResumenPerfilUiState.Cargando
    private var ultimoUsuarioId: String? = null

    val state: StateFlow<ResumenPerfilUiState> = reintentos
        .flatMapLatest {
            observarSesion()
                .distinctUntilChanged { anterior, nuevo -> anterior?.id == nuevo?.id }
                .flatMapLatest { usuario ->
                    if (usuario == null) {
                        // Sin sesión (p. ej. al cerrarla) no es un error: se descarta lo guardado para
                        // que otra cuenta nunca vea el resumen de la anterior.
                        ultimoValor = ResumenPerfilUiState.Cargando
                        ultimoUsuarioId = null
                        flowOf<ResumenPerfilUiState>(ResumenPerfilUiState.Cargando)
                    } else {
                        // El catch va dentro: si falla la lectura, "Reintentar" todavía puede reiniciar la consulta.
                        observarObras(usuario.id)
                            .map { it.aUi() }
                            .onStart {
                                val hayValorConocido = ultimoUsuarioId == usuario.id &&
                                    ultimoValor is ResumenPerfilUiState.Contenido
                                if (!hayValorConocido) emit(ResumenPerfilUiState.Cargando)
                            }
                            .catch { error ->
                                if (error is CancellationException) throw error
                                emit(ResumenPerfilUiState.Error(ErrorResumenUi.DESCONOCIDO))
                            }
                            .onEach {
                                ultimoValor = it
                                ultimoUsuarioId = usuario.id
                            }
                    }
                }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(ResumenPerfilUiState.Error(ErrorResumenUi.SESION))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ResumenPerfilUiState.Cargando)

    fun reintentar() = reintentos.update { it + 1 }

    private fun ResultadoObra<List<Obra>>.aUi(): ResumenPerfilUiState = when (this) {
        is ResultadoObra.Exito -> ResumenPerfilUiState.Contenido(resumenObras(dato))
        is ResultadoObra.Fallo -> ResumenPerfilUiState.Error(error.toUiError())
    }

    private fun ErrorObra.toUiError() = when (this) {
        ErrorObra.NO_AUTORIZADA -> ErrorResumenUi.PERMISOS
        ErrorObra.RED -> ErrorResumenUi.RED
        else -> ErrorResumenUi.DESCONOCIDO
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
