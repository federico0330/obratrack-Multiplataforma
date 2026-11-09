package com.unaj.obratrack.composeapp

import com.unaj.obratrack.domain.repository.ProveedorTokenGoogle
import com.unaj.obratrack.domain.repository.ResultadoTokenGoogle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Lo implementa Swift con el SDK de Google Sign-In. Usa callbacks y no `suspend` porque Swift no
 * puede implementar funciones suspend de Kotlin.
 */
interface SelectorCuentaGoogle {
    fun pedirIdToken(respuesta: RespuestaSelectorGoogle)
}

/** Swift responde una sola vez: con el token, con el NSError (dominio y código) o sin token. */
class RespuestaSelectorGoogle internal constructor(
    private val alTerminar: (ResultadoTokenGoogle) -> Unit,
) {
    private var respondida = false

    fun exito(idToken: String) = responder(ResultadoTokenGoogle.Exito(idToken))

    fun error(dominio: String, codigo: Long) = responder(clasificarErrorGoogleIos(dominio, codigo))

    /** El SDK terminó sin error pero no entregó idToken. */
    fun sinToken() = responder(ResultadoTokenGoogle.Fallo)

    private fun responder(resultado: ResultadoTokenGoogle) {
        if (respondida) return
        respondida = true
        alTerminar(resultado)
    }
}

internal class ProveedorTokenGoogleIos(private val selector: SelectorCuentaGoogle) : ProveedorTokenGoogle {
    override suspend fun obtenerIdToken(): ResultadoTokenGoogle = suspendCancellableCoroutine { cont ->
        selector.pedirIdToken(RespuestaSelectorGoogle { if (cont.isActive) cont.resume(it) })
    }
}
