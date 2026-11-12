package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorSugerir
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import com.unaj.obratrack.domain.repository.SugeridorConIA
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class SugeridorConIaFalso(private val resultado: ResultadoSugerir = ResultadoSugerir.Exito(emptyList())) : SugeridorConIA {
    var llamadas = 0
    var ultimaDescripcion: String? = null

    override suspend fun sugerir(descripcion: String): ResultadoSugerir {
        llamadas++
        ultimaDescripcion = descripcion
        return resultado
    }
}

class SugerirTareasConIaTest {
    @Test
    fun enviaLaDescripcionRecortada() = runTest {
        val sugeridor = SugeridorConIaFalso()

        val resultado = SugerirTareasConIa(sugeridor)("  Casa de dos plantas  ")

        assertEquals(ResultadoSugerir.Exito(emptyList()), resultado)
        assertEquals("Casa de dos plantas", sugeridor.ultimaDescripcion)
    }

    @Test
    fun descripcionEnBlancoNoSeEnvia() = runTest {
        val sugeridor = SugeridorConIaFalso()

        assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.DESCRIPCION_INVALIDA), SugerirTareasConIa(sugeridor)("   "))
        assertEquals(0, sugeridor.llamadas)
    }

    @Test
    fun descripcionDemasiadoLargaNoSeEnvia() = runTest {
        val sugeridor = SugeridorConIaFalso()

        assertEquals(
            ResultadoSugerir.Fallo(ErrorSugerir.DESCRIPCION_INVALIDA),
            SugerirTareasConIa(sugeridor)("a".repeat(501)),
        )
        assertEquals(0, sugeridor.llamadas)
    }

    @Test
    fun aceptaElLargoMaximo() = runTest {
        val sugeridor = SugeridorConIaFalso()

        SugerirTareasConIa(sugeridor)("a".repeat(500))

        assertEquals(1, sugeridor.llamadas)
    }

    @Test
    fun elFalloDelServicioSePropagaSinLanzar() = runTest {
        val sugeridor = SugeridorConIaFalso(ResultadoSugerir.Fallo(ErrorSugerir.LIMITE_ALCANZADO))

        assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.LIMITE_ALCANZADO), SugerirTareasConIa(sugeridor)("Un baño"))
    }
}
