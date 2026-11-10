package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class ProgramarVisita(private val repositorio: RepositorioObras) {
    suspend operator fun invoke(
        userId: String,
        obraId: String,
        fecha: LocalDate?,
        hora: LocalTime?,
    ): ResultadoObra<Unit> {
        val fechaUtc = fecha?.atStartOfDayIn(TimeZone.UTC)
        val minutos = hora?.takeIf { fechaUtc != null }?.let { it.hour * 60 + it.minute }
        return repositorio.programarVisita(userId, obraId, fechaUtc, minutos)
    }
}
