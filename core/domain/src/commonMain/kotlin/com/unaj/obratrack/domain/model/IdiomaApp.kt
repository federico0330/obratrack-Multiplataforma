package com.unaj.obratrack.domain.model

/** Idioma elegido por el usuario; [SISTEMA] sigue el del dispositivo y no tiene código. */
enum class IdiomaApp(val codigo: String?) {
    SISTEMA(null),
    ESPANOL("es"),
    INGLES("en"),
    ITALIANO("it"),
}
