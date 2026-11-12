package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TipoObra

/** Fallos conocidos al sugerir; nunca expone texto ni códigos de un servicio externo. */
enum class ErrorSugerir {
    SIN_CONEXION,
    LIMITE_ALCANZADO,
    RESPUESTA_INVALIDA,
    DESCONOCIDO,
}

sealed interface ResultadoSugerir {
    data class Exito(val sugerencias: List<SugerenciaTarea>) : ResultadoSugerir
    data class Fallo(val error: ErrorSugerir) : ResultadoSugerir
}

/** Puerto de salida del dominio: quién propone las tareas (plantillas locales, un modelo de lenguaje, etc.). */
interface SugeridorDeTareas {
    suspend fun sugerir(tipo: TipoObra): ResultadoSugerir
}
