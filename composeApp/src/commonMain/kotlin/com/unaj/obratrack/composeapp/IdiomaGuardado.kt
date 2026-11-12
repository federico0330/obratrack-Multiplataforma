package com.unaj.obratrack.composeapp

import com.unaj.obratrack.core.data.preferencias.leerIdiomaGuardado
import com.unaj.obratrack.domain.model.IdiomaApp

/** Idioma elegido por el usuario, para que la plataforma lo aplique antes de crear la interfaz. */
fun idiomaGuardado(): IdiomaApp = leerIdiomaGuardado()
