package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.repository.ResultadoObra
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.SincronizarAvisosVisitas
import com.unaj.obratrack.domain.usecase.agendaVisitas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Mantiene los avisos locales del dispositivo iguales a la agenda de visitas: cada emisión de obras
 * de la sesión recalcula el plan y lo aplica. Es de mejor esfuerzo y sin estado visible: si el
 * dispositivo no puede avisar, la Agenda de visitas en pantalla sigue siendo la referencia. Sigue el
 * mismo esquema de sesión y consulta que [ObrasViewModel].
 */
class AvisosViewModel(
    observarSesion: ObservarSesion,
    private val obrasDeSesion: ObservarObras,
    private val sincronizar: SincronizarAvisosVisitas,
    private val clock: Clock = Clock.System,
    private val zona: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private var obrasJob: Job? = null
    private var userId: String? = null

    init {
        viewModelScope.launch {
            observarSesion()
                .catch { error ->
                    if (error is CancellationException) throw error
                    soltarSesion()
                }
                .collect { sesion ->
                    if (sesion == null) soltarSesion() else seguirObras(sesion.id)
                }
        }
    }

    private fun soltarSesion() {
        userId = null
        obrasJob?.cancel()
        obrasJob = null
    }

    private fun seguirObras(id: String) {
        if (userId == id && obrasJob?.isActive == true) return
        userId = id
        obrasJob?.cancel()
        obrasJob = viewModelScope.launch {
            obrasDeSesion(id)
                .catch { error ->
                    if (error is CancellationException) throw error
                    // Una emisión fallida no es motivo de aviso: la próxima emisión corrige el plan.
                }
                .collect { resultado ->
                    if (resultado is ResultadoObra.Exito) {
                        val ahora = clock.now()
                        try {
                            sincronizar(
                                agendaVisitas(resultado.dato, ahora, zona),
                                ahora,
                                zona,
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            // Los avisos son de mejor esfuerzo: la agenda en pantalla no depende de ellos.
                        }
                    }
                }
        }
    }
}
