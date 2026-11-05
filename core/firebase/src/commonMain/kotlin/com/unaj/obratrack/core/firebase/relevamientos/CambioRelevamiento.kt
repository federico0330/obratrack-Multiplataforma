package com.unaj.obratrack.core.firebase.relevamientos

internal interface TransaccionCambioRelevamiento {
    suspend fun leerDuenoObra(): String?
    suspend fun leerDuenoRelevamiento(): String?
    suspend fun leerDuenoTarea(id: String): String?
    suspend fun leerFechas(): Map<String, Long>
    fun actualizar(fields: RelevamientoUpdateFields, ultimaVisitaMillis: Long)
    fun eliminar(ultimaVisitaMillis: Long?)
}
internal class RelevamientoNoEncontradoException : Exception()

private suspend fun verificarPropiedad(userId: String, tx: TransaccionCambioRelevamiento) {
    if (tx.leerDuenoObra() != userId) throw ObraNoAutorizadaException()
    val dueno = tx.leerDuenoRelevamiento() ?: throw RelevamientoNoEncontradoException()
    if (dueno != userId) throw ObraNoAutorizadaException()
}

internal suspend fun prepararEdicion(userId: String, id: String, fields: RelevamientoUpdateFields, tx: TransaccionCambioRelevamiento) {
    verificarPropiedad(userId, tx)
    if (fields.tareaId != null && tx.leerDuenoTarea(fields.tareaId) != userId) throw TareaNoDisponibleException()
    val otrasFechas = tx.leerFechas().filterKeys { it != id }.values
    tx.actualizar(fields, maxOf(fields.fechaMillis, otrasFechas.maxOrNull() ?: fields.fechaMillis))
}

internal suspend fun prepararBaja(userId: String, id: String, tx: TransaccionCambioRelevamiento) {
    verificarPropiedad(userId, tx)
    tx.eliminar(tx.leerFechas().filterKeys { it != id }.values.maxOrNull())
}
