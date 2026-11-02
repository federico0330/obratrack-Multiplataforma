package com.unaj.obratrack.domain.validation

import com.unaj.obratrack.domain.model.TareaDraft
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidadorTareaTest {
    private val validador = ValidadorTarea()
    private val dia = LocalDate(2026, 11, 10)

    @Test
    fun aceptaTituloSinFechas() {
        assertEquals(ResultadoValidacionTarea.Valida, validador.validar(TareaDraft("Colar losa")))
    }

    @Test
    fun aceptaFinIgualOPosteriorAlInicio() {
        assertEquals(ResultadoValidacionTarea.Valida, validador.validar(TareaDraft("T", fechaInicio = dia, fechaFin = dia)))
        assertEquals(
            ResultadoValidacionTarea.Valida,
            validador.validar(TareaDraft("T", fechaInicio = dia, fechaFin = LocalDate(2026, 11, 11))),
        )
    }

    @Test
    fun rechazaTituloEnBlanco() {
        assertEquals(ResultadoValidacionTarea.TituloVacio, validador.validar(TareaDraft("   ")))
    }

    @Test
    fun rechazaUnaSolaFecha() {
        assertEquals(ResultadoValidacionTarea.FechasIncompletas, validador.validar(TareaDraft("T", fechaInicio = dia)))
        assertEquals(ResultadoValidacionTarea.FechasIncompletas, validador.validar(TareaDraft("T", fechaFin = dia)))
    }

    @Test
    fun rechazaFinAnteriorAlInicio() {
        assertEquals(
            ResultadoValidacionTarea.FechaFinAnterior,
            validador.validar(TareaDraft("T", fechaInicio = dia, fechaFin = LocalDate(2026, 11, 9))),
        )
    }
}
