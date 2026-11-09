package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.ProveedorUbicacion
import com.unaj.obratrack.domain.repository.ResultadoUbicacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class ObtenerUbicacionActualTest {
    private val ubicacion = Ubicacion(-34.7746, -58.2672)

    @Test
    fun devuelveLaUbicacionDelProveedor() = runTest {
        val proveedor = ProveedorUbicacionFalso(posicion = { ResultadoUbicacion.Exito(ubicacion) })
        assertEquals(ResultadoUbicacion.Exito(ubicacion), ObtenerUbicacionActual(proveedor)())
        assertEquals(1, proveedor.posiciones)
    }

    @Test
    fun siNoHayAccesoDevuelveElFalloSinBuscarLaPosicion() = runTest {
        for (fallo in listOf(ResultadoUbicacion.SinPermiso, ResultadoUbicacion.Desactivada, ResultadoUbicacion.NoDisponible)) {
            val proveedor = ProveedorUbicacionFalso(acceso = { fallo })
            assertEquals(fallo, ObtenerUbicacionActual(proveedor)())
            assertEquals(0, proveedor.posiciones)
        }
    }

    @Test
    fun siLaBusquedaNoTerminaAlVencerElTiempoMaximoEsNoDisponible() = runTest {
        val proveedor = ProveedorUbicacionFalso(posicion = { awaitCancellation() })
        assertEquals(ResultadoUbicacion.NoDisponible, ObtenerUbicacionActual(proveedor, 15.seconds)())
        assertEquals(15_000L, testScheduler.currentTime)
    }

    @Test
    fun elTiempoQueTardaElUsuarioEnElPermisoNoCuentaContraElTiempoMaximo() = runTest {
        val proveedor = ProveedorUbicacionFalso(
            acceso = { delay(60.seconds); null },
            posicion = { delay(5.seconds); ResultadoUbicacion.Exito(ubicacion) },
        )
        assertEquals(ResultadoUbicacion.Exito(ubicacion), ObtenerUbicacionActual(proveedor, 15.seconds)())
        assertEquals(65_000L, testScheduler.currentTime)
    }

    @Test
    fun unaPosicionConCoordenadasImposiblesNuncaEsExito() = runTest {
        for (invalida in listOf(Ubicacion(Double.NaN, 0.0), Ubicacion(95.0, 0.0), Ubicacion(0.0, Double.POSITIVE_INFINITY))) {
            val proveedor = ProveedorUbicacionFalso(posicion = { ResultadoUbicacion.Exito(invalida) })
            assertEquals(ResultadoUbicacion.NoDisponible, ObtenerUbicacionActual(proveedor)())
        }
    }

    @Test
    fun laCancelacionDelLlamadorSePropagaYNoSeConvierteEnTimeout() = runTest {
        val proveedor = ProveedorUbicacionFalso(posicion = { awaitCancellation() })
        val busqueda = async { ObtenerUbicacionActual(proveedor, 15.seconds)() }
        runCurrent()
        busqueda.cancel()
        assertFailsWith<CancellationException> { busqueda.await() }
    }

    @Test
    fun elProveedorSePuedeUsarConElTiempoMaximoPorDefecto() = runTest {
        val proveedor = ProveedorUbicacionFalso(posicion = { awaitCancellation() })
        assertEquals(ResultadoUbicacion.NoDisponible, ObtenerUbicacionActual(proveedor)())
        assertEquals(20_000L, testScheduler.currentTime)
    }
}

private class ProveedorUbicacionFalso(
    private val acceso: suspend () -> ResultadoUbicacion.Fallo? = { null },
    private val posicion: suspend () -> ResultadoUbicacion = { ResultadoUbicacion.NoDisponible },
) : ProveedorUbicacion {
    var posiciones = 0
    override suspend fun solicitarAcceso() = acceso()
    override suspend fun posicionActual(): ResultadoUbicacion {
        posiciones++
        return posicion()
    }
}
