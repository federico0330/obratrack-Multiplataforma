package com.unaj.obratrack.domain.model

data class ResumenObras(val total: Int, val activas: Int, val pausadas: Int, val finalizadas: Int)

/** Cuenta las obras por estado. Cada obra tiene un solo estado, así que el total es la suma de los tres. */
fun resumenObras(obras: List<Obra>): ResumenObras = ResumenObras(
    total = obras.size,
    activas = obras.count { it.estado == EstadoObra.ACTIVA },
    pausadas = obras.count { it.estado == EstadoObra.PAUSADA },
    finalizadas = obras.count { it.estado == EstadoObra.FINALIZADA },
)
