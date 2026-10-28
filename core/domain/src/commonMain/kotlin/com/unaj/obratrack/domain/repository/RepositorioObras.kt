package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Obra

/** Puerto de salida de la arquitectura hexagonal; su adaptador Firebase vendrá después. */
interface RepositorioObras {
    suspend fun crear(obra: Obra)
}
