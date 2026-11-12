package com.unaj.obratrack.domain.model

/** Tipo de obra sobre el que se sugieren tareas. */
enum class TipoObra { CASA_NUEVA, AMPLIACION, REFORMA_INTEGRAL, BANO, COCINA, PINTURA }

/**
 * Catálogo de tareas típicas de una obra. Es una clave, no un texto: el título se resuelve en la
 * interfaz con el idioma de la app, para que el dominio no guarde textos de pantalla.
 */
enum class TareaPlantilla(val categoria: CategoriaTarea) {
    PLANOS_Y_PERMISOS(CategoriaTarea.OTROS),
    LIMPIEZA_DEL_TERRENO(CategoriaTarea.LIMPIEZA),
    REPLANTEO(CategoriaTarea.CIMIENTOS_Y_EXCAVACION),
    EXCAVACION(CategoriaTarea.CIMIENTOS_Y_EXCAVACION),
    FUNDACIONES(CategoriaTarea.CIMIENTOS_Y_EXCAVACION),
    ESTRUCTURA_RESISTENTE(CategoriaTarea.ESTRUCTURA),
    MUROS(CategoriaTarea.ALBANILERIA),
    DEMOLICION(CategoriaTarea.ALBANILERIA),
    CUBIERTA(CategoriaTarea.TECHOS),
    INSTALACION_ELECTRICA(CategoriaTarea.ELECTRICIDAD),
    INSTALACION_SANITARIA(CategoriaTarea.PLOMERIA),
    INSTALACION_GAS(CategoriaTarea.GAS),
    IMPERMEABILIZACION(CategoriaTarea.IMPERMEABILIZACION),
    REVOQUES(CategoriaTarea.ALBANILERIA),
    CONTRAPISOS(CategoriaTarea.ALBANILERIA),
    PISOS(CategoriaTarea.PISOS_Y_REVESTIMIENTOS),
    REVESTIMIENTOS(CategoriaTarea.PISOS_Y_REVESTIMIENTOS),
    ABERTURAS(CategoriaTarea.ABERTURAS),
    CARPINTERIA_INTERIOR(CategoriaTarea.CARPINTERIA),
    MUEBLES_A_MEDIDA(CategoriaTarea.CARPINTERIA),
    HERRERIA(CategoriaTarea.HERRERIA),
    CLIMATIZACION(CategoriaTarea.CLIMATIZACION),
    PINTURA_INTERIOR(CategoriaTarea.PINTURA),
    PINTURA_EXTERIOR(CategoriaTarea.PINTURA),
    ARTEFACTOS_Y_GRIFERIA(CategoriaTarea.PLOMERIA),
    ARTEFACTOS_DE_ILUMINACION(CategoriaTarea.ELECTRICIDAD),
    RETIRO_DE_ESCOMBROS(CategoriaTarea.LIMPIEZA),
    LIMPIEZA_FINAL(CategoriaTarea.LIMPIEZA),
    RECEPCION_Y_ENTREGA(CategoriaTarea.OTROS),
}

/** Cómo se nombra una tarea sugerida: una del catálogo (se traduce) o un texto ya redactado. */
sealed interface TituloSugerido {
    data class Plantilla(val tarea: TareaPlantilla) : TituloSugerido
    data class Libre(val texto: String) : TituloSugerido
}

data class SugerenciaTarea(val titulo: TituloSugerido, val categoria: CategoriaTarea)

/** La tarea a crear a partir de la sugerencia, con el título ya resuelto en el idioma de la app. */
fun SugerenciaTarea.aDraft(tituloResuelto: String) = TareaDraft(titulo = tituloResuelto, categoria = categoria)
