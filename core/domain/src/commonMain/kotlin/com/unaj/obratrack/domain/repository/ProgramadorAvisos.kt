package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.AvisoVisita
import kotlin.time.Instant

/**
 * Programa avisos locales del dispositivo para las visitas de la agenda. La implementación es por
 * plataforma (Android usa WorkManager; iOS todavía no avisa y aporta el puerto nulo): igual que con
 * [ProveedorTokenGoogle], cada plataforma entrega su implementación al grafo compartido.
 */
interface ProgramadorAvisos {
    /** Los avisos todavía por disparar, por obra: el instante en que cada uno se va a disparar. */
    suspend fun programados(): Map<String, Instant>

    /** Programa o reprograma el aviso de una obra; reprogramar reemplaza el aviso anterior. */
    suspend fun programar(aviso: AvisoVisita)

    /** Cancela el aviso pendiente de una obra, si lo hay. */
    suspend fun cancelar(obraId: String)
}

/** Puerto sin efecto: la usan las pruebas y las plataformas que todavía no avisan. */
object ProgramadorAvisosNulo : ProgramadorAvisos {
    override suspend fun programados(): Map<String, Instant> = emptyMap()
    override suspend fun programar(aviso: AvisoVisita) = Unit
    override suspend fun cancelar(obraId: String) = Unit
}
