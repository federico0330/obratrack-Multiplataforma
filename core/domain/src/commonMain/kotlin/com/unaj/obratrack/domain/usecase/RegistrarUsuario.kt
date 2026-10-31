package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorRegistro
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.validation.ValidadorCredencialesRegistro

class RegistrarUsuario(
    private val repositorio: RepositorioAutenticacion,
    private val validador: ValidadorCredencialesRegistro = ValidadorCredencialesRegistro(),
) {
    suspend operator fun invoke(email: String, contrasena: String): ResultadoRegistro {
        val emailNormalizado = email.trim()
        if (!validador.emailValido(emailNormalizado)) return ResultadoRegistro.Fallo(ErrorRegistro.EmailInvalido)
        if (!validador.contrasenaValida(contrasena)) return ResultadoRegistro.Fallo(ErrorRegistro.ContrasenaDebil)
        return repositorio.registrar(emailNormalizado, contrasena)
    }
}
