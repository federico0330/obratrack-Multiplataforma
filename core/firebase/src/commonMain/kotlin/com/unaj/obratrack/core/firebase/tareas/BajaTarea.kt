package com.unaj.obratrack.core.firebase.tareas

internal data class PlanBajaTarea(
    val relevamientos: List<String>,
    val eliminarRelevamientos: Boolean,
    val actualizarUltimaVisita: Boolean,
    val ultimaVisitaMillis: Long?,
)

/** El adaptador aplica el plan completo en una única escritura atómica. */
internal interface AlmacenBajaTarea {
    suspend fun leerVinculados(): List<String>
    suspend fun leerFechas(): Map<String, Long>
    suspend fun aplicar(plan: PlanBajaTarea)
}

internal class DemasiadosVinculadosException : Exception()

internal suspend fun ejecutarBajaTarea(almacen: AlmacenBajaTarea, eliminarRelevamientos: Boolean) {
    val vinculados = almacen.leerVinculados()
    val actualizarVisita = eliminarRelevamientos && vinculados.isNotEmpty()
    val escrituras = vinculados.size + 1 + if (actualizarVisita) 1 else 0
    // Igual que el cliente nativo: no partir una baja que debe ser atómica.
    if (escrituras > 500) throw DemasiadosVinculadosException()
    val ultimaVisita = if (actualizarVisita) {
        val eliminados = vinculados.toSet()
        almacen.leerFechas().filterKeys { it !in eliminados }.values.maxOrNull()
    } else {
        null
    }
    almacen.aplicar(PlanBajaTarea(vinculados, eliminarRelevamientos, actualizarVisita, ultimaVisita))
}
