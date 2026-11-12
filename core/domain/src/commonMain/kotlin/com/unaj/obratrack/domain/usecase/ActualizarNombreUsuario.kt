package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorActualizarNombre
import com.unaj.obratrack.domain.repository.RepositorioCuenta
import com.unaj.obratrack.domain.repository.ResultadoActualizarNombre
import com.unaj.obratrack.domain.validation.ValidadorNombreUsuario

/** Cambia el nombre visible del usuario con sesión; el nombre nuevo llega por [ObservarSesion]. */
class ActualizarNombreUsuario(
    private val repositorio: RepositorioCuenta,
    private val validador: ValidadorNombreUsuario = ValidadorNombreUsuario(),
) {
    suspend operator fun invoke(nombre: String): ResultadoActualizarNombre {
        if (!validador.nombreValido(nombre)) {
            return ResultadoActualizarNombre.Fallo(ErrorActualizarNombre.NombreInvalido)
        }
        return repositorio.actualizarNombre(validador.normalizar(nombre))
    }
}
