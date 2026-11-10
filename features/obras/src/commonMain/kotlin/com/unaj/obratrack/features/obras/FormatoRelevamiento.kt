package com.unaj.obratrack.features.obras

/** `09:30` a partir de los minutos desde la medianoche, que es como se guarda la hora de la visita. */
internal fun formatearHoraVisita(minutos: Int): String =
    "${(minutos / 60).toString().padStart(2, '0')}:${(minutos % 60).toString().padStart(2, '0')}"

/** La próxima visita en una sola línea: la fecha y, si la tiene, la hora. */
internal fun textoVisita(fecha: String, minutos: Int?): String =
    if (minutos == null) fecha else "$fecha · ${formatearHoraVisita(minutos)}"
