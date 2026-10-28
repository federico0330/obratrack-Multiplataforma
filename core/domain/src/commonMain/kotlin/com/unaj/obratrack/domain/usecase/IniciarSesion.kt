package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data object DatosInvalidos : ResultadoLogin
    data object CredencialesInvalidas : ResultadoLogin
    data object ErrorDeRed : ResultadoLogin
    data object ErrorInesperado : ResultadoLogin
}

class IniciarSesion(private val repositorio: RepositorioAutenticacion) {
    suspend operator fun invoke(email: String, clave: String): ResultadoLogin {
        val emailNormalizado = email.trim()
        if (!emailNormalizado.contains('@') || clave.isBlank()) return ResultadoLogin.DatosInvalidos
        return when (val result = repositorio.iniciarSesion(emailNormalizado, clave)) {
            is ResultadoAcceso.Exito -> ResultadoLogin.Exito(result.usuario)
            ResultadoAcceso.CredencialesInvalidas -> ResultadoLogin.CredencialesInvalidas
            ResultadoAcceso.SinConexion -> ResultadoLogin.ErrorDeRed
            ResultadoAcceso.ErrorInesperado -> ResultadoLogin.ErrorInesperado
        }
    }
}
