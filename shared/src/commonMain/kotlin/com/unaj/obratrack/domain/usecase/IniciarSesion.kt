package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion

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
        return try {
            ResultadoLogin.Exito(repositorio.iniciarSesion(emailNormalizado, clave))
        } catch (error: Throwable) {
            // La cancelación pertenece a la UI/ciclo de vida, nunca debe convertirse en error de login.
            if (error is kotlinx.coroutines.CancellationException) throw error
            when {
                error.message?.contains("network", ignoreCase = true) == true -> ResultadoLogin.ErrorDeRed
                error.message?.contains("credential", ignoreCase = true) == true ||
                    error.message?.contains("password", ignoreCase = true) == true -> ResultadoLogin.CredencialesInvalidas
                else -> ResultadoLogin.ErrorInesperado
            }
        }
    }
}
