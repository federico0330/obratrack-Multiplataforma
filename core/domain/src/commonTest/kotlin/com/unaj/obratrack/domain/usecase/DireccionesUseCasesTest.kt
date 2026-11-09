package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.CandidatoDireccion
import com.unaj.obratrack.domain.repository.ProveedorDirecciones
import com.unaj.obratrack.domain.repository.ResultadoDireccion
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DireccionesUseCasesTest {
    private val ubicacion = Ubicacion(-34.7746, -58.2672)

    private fun candidato(texto: String, lat: Double = -34.7746, lon: Double = -58.2672) = CandidatoDireccion(texto, Ubicacion(lat, lon))

    @Test
    fun obtenerDireccionDevuelveElTextoRecortado() = runTest {
        val proveedor = DireccionesFalso(direccion = ResultadoDireccion.Exito("  Av. Calchaquí 6200  "))
        assertEquals(ResultadoDireccion.Exito("Av. Calchaquí 6200"), ObtenerDireccion(proveedor)(ubicacion))
        assertEquals(listOf(ubicacion), proveedor.direccionesPedidas)
    }

    @Test
    fun obtenerDireccionConservaCadaResultadoDelProveedor() = runTest {
        for (resultado in listOf(ResultadoDireccion.SinResultados, ResultadoDireccion.NoDisponible, ResultadoDireccion.NoSoportado)) {
            assertEquals(resultado, ObtenerDireccion(DireccionesFalso(direccion = resultado))(ubicacion))
        }
    }

    @Test
    fun obtenerDireccionConTextoEnBlancoEsSinResultados() = runTest {
        val proveedor = DireccionesFalso(direccion = ResultadoDireccion.Exito("   "))
        assertEquals(ResultadoDireccion.SinResultados, ObtenerDireccion(proveedor)(ubicacion))
    }

    @Test
    fun obtenerDireccionNoConsultaCoordenadasImposibles() = runTest {
        val proveedor = DireccionesFalso()
        for (invalida in listOf(Ubicacion(91.0, 0.0), Ubicacion(0.0, 181.0), Ubicacion(Double.NaN, 0.0))) {
            assertEquals(ResultadoDireccion.SinResultados, ObtenerDireccion(proveedor)(invalida))
        }
        assertTrue(proveedor.direccionesPedidas.isEmpty())
    }

    @Test
    fun buscarNormalizaElTextoAntesDeConsultar() = runTest {
        val proveedor = DireccionesFalso(busqueda = ResultadoDireccion.Exito(listOf(candidato("Av. Calchaquí 6200"))))
        BuscarDirecciones(proveedor)("  Av.   Calchaquí   6200 ")
        assertEquals(listOf("Av. Calchaquí 6200"), proveedor.busquedasPedidas)
    }

    @Test
    fun buscarConTextoVacioOCortoNoConsultaNada() = runTest {
        val proveedor = DireccionesFalso()
        for (texto in listOf("", "   ", "ab", " a ")) {
            assertEquals(ResultadoDireccion.SinResultados, BuscarDirecciones(proveedor)(texto))
        }
        assertTrue(proveedor.busquedasPedidas.isEmpty())
    }

    @Test
    fun buscarDevuelveHastaCincoCandidatosEnElOrdenDelProveedor() = runTest {
        val ocho = (1..8).map { candidato("Calle $it", lat = -34.0 - it / 100.0) }
        val resultado = BuscarDirecciones(DireccionesFalso(busqueda = ResultadoDireccion.Exito(ocho)))("Calle")
        assertEquals(ResultadoDireccion.Exito(ocho.take(5)), resultado)
    }

    @Test
    fun buscarDescartaCandidatosSinTextoConCoordenadasImposiblesORepetidos() = runTest {
        val bueno = candidato("Av. Calchaquí 6200")
        val devueltos = listOf(
            candidato("   "),
            candidato("Fuera de rango", lat = 95.0),
            candidato("Con NaN", lon = Double.NaN),
            bueno,
            candidato("  Av. Calchaquí 6200  "),
        )
        val resultado = BuscarDirecciones(DireccionesFalso(busqueda = ResultadoDireccion.Exito(devueltos)))("Calchaquí")
        assertEquals(ResultadoDireccion.Exito(listOf(bueno)), resultado)
    }

    @Test
    fun buscarSinCandidatosValidosEsSinResultados() = runTest {
        val devueltos = listOf(candidato("Fuera de rango", lat = 95.0), candidato(" "))
        val resultado = BuscarDirecciones(DireccionesFalso(busqueda = ResultadoDireccion.Exito(devueltos)))("Calchaquí")
        assertEquals(ResultadoDireccion.SinResultados, resultado)
    }

    @Test
    fun buscarConservaCadaFalloDelProveedor() = runTest {
        for (resultado in listOf(ResultadoDireccion.SinResultados, ResultadoDireccion.NoDisponible, ResultadoDireccion.NoSoportado)) {
            assertEquals(resultado, BuscarDirecciones(DireccionesFalso(busqueda = resultado))("Calchaquí"))
        }
    }

    @Test
    fun elSoporteLoDecideElProveedor() {
        assertTrue(BuscarDirecciones(DireccionesFalso(soportado = true)).estaSoportado())
        assertFalse(BuscarDirecciones(DireccionesFalso(soportado = false)).estaSoportado())
    }
}

private class DireccionesFalso(
    private val soportado: Boolean = true,
    private val direccion: ResultadoDireccion<String> = ResultadoDireccion.NoDisponible,
    private val busqueda: ResultadoDireccion<List<CandidatoDireccion>> = ResultadoDireccion.NoDisponible,
) : ProveedorDirecciones {
    val direccionesPedidas = mutableListOf<Ubicacion>()
    val busquedasPedidas = mutableListOf<String>()
    override fun estaSoportado() = soportado
    override suspend fun direccionDe(ubicacion: Ubicacion): ResultadoDireccion<String> {
        direccionesPedidas += ubicacion
        return direccion
    }
    override suspend fun buscar(texto: String): ResultadoDireccion<List<CandidatoDireccion>> {
        busquedasPedidas += texto
        return busqueda
    }
}
