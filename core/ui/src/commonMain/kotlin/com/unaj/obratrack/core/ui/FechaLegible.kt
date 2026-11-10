package com.unaj.obratrack.core.ui

import androidx.compose.runtime.Composable
import com.unaj.obratrack.core.ui.generated.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/**
 * Una fecha de calendario como la lee una persona ("5 nov 2026", "Nov 5, 2026"), en el idioma de la app. El orden y los
 * meses salen de los recursos: en commonMain no hay java.time ni formateadores del sistema. Guardar y comparar sigue
 * siendo con [LocalDate] (AAAA-MM-DD); esto es solo para mostrar.
 */
@Composable
fun fechaLegible(fecha: LocalDate): String {
    val meses = stringArrayResource(Res.array.ui_months_short)
    return stringResource(Res.string.ui_date_format, fecha.day, meses[fecha.month.ordinal], fecha.year)
}

/** Igual que [fechaLegible] para el texto AAAA-MM-DD de un formulario; si no es una fecha válida lo devuelve tal cual. */
@Composable
fun fechaLegible(fechaIso: String): String {
    val fecha = runCatching { LocalDate.parse(fechaIso) }.getOrNull()
    return if (fecha == null) fechaIso else fechaLegible(fecha)
}
