package com.unaj.obratrack.core.firebase.obras

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.GeoPoint
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.code
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Adaptador GitLive: conoce Firestore y mantiene la forma exacta de los documentos nativos. */
class GitLiveFirebaseObraDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : FirebaseObraDataSource {
    override fun observarPropias(userId: String): Flow<FirebaseObraResult<List<ObraDto>>> =
        firestore.collection(COLLECTION)
            .where { FIELD_USER_ID equalTo userId }
            .snapshots
            .map { snapshot ->
                val result: FirebaseObraResult<List<ObraDto>> =
                    FirebaseObraResult.Success(snapshot.documents.map { it.toDto() })
                result
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(FirebaseObraResult.Failure(error.toFailure()))
            }

    override suspend fun crear(dto: ObraDto): FirebaseObraResult<ObraDto> = try {
        val ref = firestore.collection(COLLECTION).document
        ref.set(dto.toFirestoreMap())
        FirebaseObraResult.Success(dto.copy(id = ref.id))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FirebaseObraResult.Failure(error.toFailure())
    }

    override suspend fun actualizar(
        userId: String,
        obraId: String,
        fields: ObraUpdateFields,
    ): FirebaseObraResult<Unit> = writeResult {
        ownedDocument(userId, obraId).updateFields {
            FIELD_NOMBRE to fields.nombre
            FIELD_CLIENTE to fields.cliente
            FIELD_DIRECCION to fields.direccion
            FIELD_PUBLICA to fields.publica
            FIELD_FECHA_INICIO to Timestamp.fromMilliseconds(fields.fechaInicioMillis.toDouble())
        }
    }

    override suspend fun cambiarEstado(
        userId: String,
        obraId: String,
        estado: String,
    ): FirebaseObraResult<Unit> = writeResult {
        ownedDocument(userId, obraId).updateFields { FIELD_ESTADO to estado }
    }

    override suspend fun programarVisita(
        userId: String,
        obraId: String,
        fechaMillis: Long?,
        horaMinutos: Int?,
    ): FirebaseObraResult<Unit> = try {
        val obra = ownedDocument(userId, obraId)
        val actualizada = firestore.runTransaction {
            val snapshot = get(obra)
            if (!snapshot.exists || snapshot.get<String>(FIELD_USER_ID) != userId) {
                false
            } else {
                updateFields(obra) {
                    FIELD_PROXIMA_VISITA to (fechaMillis?.let { Timestamp.fromMilliseconds(it.toDouble()) } ?: FieldValue.delete)
                    FIELD_HORA_VISITA to (horaMinutos ?: FieldValue.delete)
                }
                true
            }
        }
        if (actualizada) FirebaseObraResult.Success(Unit)
        else FirebaseObraResult.Failure(FirebaseObraFailure.PermissionDenied)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FirebaseObraResult.Failure(error.toFailure())
    }

    override suspend fun eliminar(userId: String, obraId: String): FirebaseObraResult<Unit> {
        if (userId.isBlank() || obraId.isBlank()) {
            return FirebaseObraResult.Failure(FirebaseObraFailure.PermissionDenied)
        }
        val obra = firestore.collection(COLLECTION).document(obraId)
        val snapshot = try {
            obra.get(Source.SERVER)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return FirebaseObraResult.Failure(error.toFailure())
        }
        if (!snapshot.exists) return FirebaseObraResult.Success(Unit)
        if (snapshot.get<String>(FIELD_USER_ID) != userId) {
            return FirebaseObraResult.Failure(FirebaseObraFailure.PermissionDenied)
        }

        var huboBorrado = false
        return try {
            listOf(SUBCOLLECTION_TAREAS, SUBCOLLECTION_RELEVAMIENTOS, SUBCOLLECTION_SUSCRIPTORES)
                .forEach { subcoleccion ->
                    val documentos = obra.collection(subcoleccion).get(Source.SERVER).documents
                    documentos.chunked(BATCH_SIZE).forEach { grupo ->
                        val batch = firestore.batch()
                        grupo.forEach { batch.delete(it.reference) }
                        batch.commit()
                        huboBorrado = true
                    }
                }
            obra.delete()
            FirebaseObraResult.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            FirebaseObraResult.Failure(
                if (huboBorrado) FirebaseObraFailure.Partial else error.toFailure(),
            )
        }
    }

    private fun ownedDocument(userId: String, obraId: String) =
        firestore.collection(COLLECTION).document(obraId).also {
            require(userId.isNotBlank() && obraId.isNotBlank())
        }

    private suspend inline fun writeResult(crossinline write: suspend () -> Unit): FirebaseObraResult<Unit> = try {
        write()
        FirebaseObraResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FirebaseObraResult.Failure(error.toFailure())
    }

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toDto(): ObraDto {
        val point = if (contains(FIELD_UBICACION)) get<GeoPoint>(FIELD_UBICACION) else null
        return ObraDto(
            id = id,
            userId = get(FIELD_USER_ID),
            nombre = get(FIELD_NOMBRE),
            cliente = get(FIELD_CLIENTE),
            direccion = get(FIELD_DIRECCION),
            publica = get(FIELD_PUBLICA),
            estado = get(FIELD_ESTADO),
            fechaInicioMillis = get<Timestamp>(FIELD_FECHA_INICIO).toMilliseconds().toLong(),
            ultimaVisitaMillis = if (contains(FIELD_ULTIMA_VISITA)) get<Timestamp>(FIELD_ULTIMA_VISITA).toMilliseconds().toLong() else null,
            latitud = point?.latitude,
            longitud = point?.longitude,
            proximaVisitaMillis = if (contains(FIELD_PROXIMA_VISITA)) get<Timestamp>(FIELD_PROXIMA_VISITA).toMilliseconds().toLong() else null,
            horaVisita = if (contains(FIELD_HORA_VISITA)) get(FIELD_HORA_VISITA) else null,
        )
    }

    private fun ObraDto.toFirestoreMap(): Map<String, Any> = buildMap {
        put(FIELD_USER_ID, userId)
        put(FIELD_NOMBRE, nombre)
        put(FIELD_CLIENTE, cliente)
        put(FIELD_DIRECCION, direccion)
        put(FIELD_PUBLICA, publica)
        put(FIELD_ESTADO, estado)
        put(FIELD_FECHA_INICIO, Timestamp.fromMilliseconds(fechaInicioMillis.toDouble()))
        if (latitud != null && longitud != null) put(FIELD_UBICACION, GeoPoint(latitud, longitud))
        proximaVisitaMillis?.let { put(FIELD_PROXIMA_VISITA, Timestamp.fromMilliseconds(it.toDouble())) }
        horaVisita?.let { put(FIELD_HORA_VISITA, it) }
        // ultimaVisita no se escribe en una obra nueva.
    }

    private fun Throwable.toFailure(): FirebaseObraFailure = when (this) {
        is FirebaseNetworkException -> FirebaseObraFailure.Network
        is FirebaseFirestoreException -> classifyFirestoreFailure(code.name)
        else -> FirebaseObraFailure.Unexpected
    }

    private companion object {
        const val COLLECTION = "obras"
        const val FIELD_USER_ID = "userId"
        const val FIELD_NOMBRE = "nombre"
        const val FIELD_CLIENTE = "cliente"
        const val FIELD_DIRECCION = "direccion"
        const val FIELD_PUBLICA = "publica"
        const val FIELD_ESTADO = "estado"
        const val FIELD_FECHA_INICIO = "fechaInicio"
        const val FIELD_ULTIMA_VISITA = "ultimaVisita"
        const val FIELD_UBICACION = "ubicacion"
        const val FIELD_PROXIMA_VISITA = "proximaVisita"
        const val FIELD_HORA_VISITA = "horaVisita"
        const val SUBCOLLECTION_TAREAS = "tareas"
        const val SUBCOLLECTION_RELEVAMIENTOS = "relevamientos"
        const val SUBCOLLECTION_SUSCRIPTORES = "suscriptores"
        const val BATCH_SIZE = 500
    }
}
