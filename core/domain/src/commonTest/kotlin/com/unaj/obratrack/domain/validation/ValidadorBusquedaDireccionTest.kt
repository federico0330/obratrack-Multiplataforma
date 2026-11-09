package com.unaj.obratrack.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidadorBusquedaDireccionTest {
    @Test
    fun recortaYUnificaLosEspacios() {
        assertEquals("Av. Calchaquí 6200", ValidadorBusquedaDireccion.normalizar("  Av.   Calchaquí \t 6200 \n"))
    }

    @Test
    fun rechazaElTextoVacioOEnBlanco() {
        assertNull(ValidadorBusquedaDireccion.normalizar(""))
        assertNull(ValidadorBusquedaDireccion.normalizar("   \t\n"))
    }

    @Test
    fun rechazaElTextoDemasiadoCortoDespuesDeNormalizar() {
        assertNull(ValidadorBusquedaDireccion.normalizar("ab"))
        // Los espacios no cuentan: "a b" son dos caracteres.
        assertNull(ValidadorBusquedaDireccion.normalizar("  a   b  "))
    }

    @Test
    fun aceptaElLargoMinimoSinContarLosEspacios() {
        assertEquals("abc", ValidadorBusquedaDireccion.normalizar(" abc "))
        assertEquals("a b c", ValidadorBusquedaDireccion.normalizar("a   b  c"))
        assertEquals("12 3", ValidadorBusquedaDireccion.normalizar("12 3"))
    }
}
