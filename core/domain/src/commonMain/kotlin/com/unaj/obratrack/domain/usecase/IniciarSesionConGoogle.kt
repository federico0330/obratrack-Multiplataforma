package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ProveedorTokenGoogle
import com.unaj.obratrack.domain.repository.RepositorioAccesoGoogle
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoTokenGoogle

sealed interface ResultadoLoginGoogle {
    data class Exito(val usuario: Usuario) : ResultadoLoginGoogle
    /** El usuario cerró el selector de cuentas: no es un error para mostrar. */
    data object Cancelado : ResultadoLoginGoogle
    data class Fallo(val error: ErrorAuth) : ResultadoLoginGoogle
}

class IniciarSesionConGoogle(
    private val proveedorToken: ProveedorTokenGoogle,
    private val repositorio: RepositorioAccesoGoogle,
) {
    suspend operator fun invoke(): ResultadoLoginGoogle =
        when (val token = proveedorToken.obtenerIdToken()) {
            ResultadoTokenGoogle.Cancelado -> ResultadoLoginGoogle.Cancelado
            ResultadoTokenGoogle.SinConexion -> ResultadoLoginGoogle.Fallo(ErrorAuth.SinConexion)
            ResultadoTokenGoogle.Fallo -> ResultadoLoginGoogle.Fallo(ErrorAuth.Inesperado)
            is ResultadoTokenGoogle.Exito -> when (val result = repositorio.iniciarSesionConGoogle(token.idToken)) {
                is ResultadoAcceso.Exito -> ResultadoLoginGoogle.Exito(result.usuario)
                // Un token de Google rechazado no es "correo o contraseña incorrectos".
                is ResultadoAcceso.Fallo -> ResultadoLoginGoogle.Fallo(
                    if (result.error == ErrorAuth.CredencialesInvalidas) ErrorAuth.Inesperado else result.error,
                )
            }
        }
}
