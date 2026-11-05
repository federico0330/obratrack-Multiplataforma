package com.unaj.obratrack.domain.model

sealed interface FiltroRelevamientos {
    data object Todos : FiltroRelevamientos
    data object SinTarea : FiltroRelevamientos
    data class DeTarea(val tareaId: String) : FiltroRelevamientos
}

fun List<Relevamiento>.filtrar(filtro: FiltroRelevamientos): List<Relevamiento> = when (filtro) {
    FiltroRelevamientos.Todos -> this
    FiltroRelevamientos.SinTarea -> filter { it.tareaId == null }
    is FiltroRelevamientos.DeTarea -> filter { it.tareaId == filtro.tareaId }
}
