package com.unaj.obratrack.core.firebase.relevamientos

internal data class ObraParaRelevamiento(val userId: String, val ultimaVisitaMillis: Long?)
internal interface TransaccionAltaRelevamiento {
    suspend fun leerObra(): ObraParaRelevamiento?
    suspend fun leerDuenoTarea(tareaId: String): String?
    fun guardar(dto: RelevamientoDto, ultimaVisitaMillis: Long)
}
internal class ObraNoAutorizadaException : Exception()
internal class TareaNoDisponibleException : Exception()

internal suspend fun prepararAlta(dto: RelevamientoDto, tx: TransaccionAltaRelevamiento) {
    val obra = tx.leerObra()
    if (obra == null || obra.userId != dto.userId) throw ObraNoAutorizadaException()
    if (dto.tareaId != null && tx.leerDuenoTarea(dto.tareaId) != dto.userId) throw TareaNoDisponibleException()
    tx.guardar(dto, maxOf(dto.fechaMillis, obra.ultimaVisitaMillis ?: dto.fechaMillis))
}
