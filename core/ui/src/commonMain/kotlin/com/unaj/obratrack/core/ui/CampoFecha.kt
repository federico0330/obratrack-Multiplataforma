package com.unaj.obratrack.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.unaj.obratrack.core.ui.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * Selector de fecha de calendario. El DatePicker trabaja en milisegundos de medianoche UTC,
 * que coincide con cómo se guardan estas fechas; se convierte desde y hacia AAAA-MM-DD.
 * Una fecha vacía es "sin fecha". Si se pasa [onClear], el diálogo ofrece quitar la fecha
 * (para los campos opcionales).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    fechaIso: String,
    label: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = fechaLegible(fechaIso),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    painter = painterResource(Iconos.Calendar),
                    contentDescription = stringResource(Res.string.ui_pick_date),
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // El campo de solo lectura no recibe toques de forma confiable: una capa transparente los captura.
        Box(Modifier.matchParentSize().clickable(enabled = enabled) { abierto = true })
    }
    if (abierto && enabled) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = runCatching { LocalDate.parse(fechaIso) }.getOrNull()
                ?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString())
                        }
                        abierto = false
                    },
                ) { Text(stringResource(Res.string.ui_accept)) }
            },
            dismissButton = {
                Row {
                    if (onClear != null && fechaIso.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onClear()
                                abierto = false
                            },
                        ) { Text(stringResource(Res.string.ui_clear_date)) }
                    }
                    TextButton(onClick = { abierto = false }) { Text(stringResource(Res.string.ui_cancel)) }
                }
            },
        ) {
            DatePicker(state = estado)
        }
    }
}
