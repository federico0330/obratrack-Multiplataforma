package com.unaj.obratrack.features.obras

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatoRelevamientoTest {
    @Test
    fun laHoraSeMuestraConDosDigitosEnCadaParte() {
        assertEquals("09:30", formatearHoraVisita(9 * 60 + 30))
        assertEquals("00:00", formatearHoraVisita(0))
        assertEquals("00:05", formatearHoraVisita(5))
        assertEquals("12:00", formatearHoraVisita(12 * 60))
        assertEquals("23:59", formatearHoraVisita(23 * 60 + 59))
    }

    @Test
    fun laVisitaLlevaLaHoraSoloSiLaTiene() {
        assertEquals("2026-11-12", textoVisita("2026-11-12", null))
        assertEquals("2026-11-12 · 09:30", textoVisita("2026-11-12", 570))
        // La hora 00:00 es una hora válida y no se confunde con «sin hora».
        assertEquals("2026-11-12 · 00:00", textoVisita("2026-11-12", 0))
    }
}
