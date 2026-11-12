package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorRecuperarContrasena
import com.unaj.obratrack.domain.repository.RepositorioCuenta
import com.unaj.obratrack.domain.repository.ResultadoRecuperarContrasena
import com.unaj.obratrack.domain.validation.ValidadorCredencialesRegistro

/** Pide el correo de restablecimiento. No requiere sesión. */
class RecuperarContrasena(
    private val repositorio: RepositorioCuenta,
    private val validador: ValidadorCredencialesRegistro = ValidadorCredencialesRegistro(),
) {
    suspend operator fun invoke(email: String): ResultadoRecuperarContrasena {
        val emailNormalizado = email.trim()
        if (!validador.emailValido(emailNormalizado)) {
            return ResultadoRecuperarContrasena.Fallo(ErrorRecuperarContrasena.EmailInvalido)
        }
        return repositorio.recuperarContrasena(emailNormalizado)
    }
}
