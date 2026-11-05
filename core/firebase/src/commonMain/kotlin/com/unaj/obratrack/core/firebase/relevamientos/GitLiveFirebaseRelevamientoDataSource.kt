package com.unaj.obratrack.core.firebase.relevamientos

import com.unaj.obratrack.core.firebase.obras.FirebaseObraFailure
import com.unaj.obratrack.core.firebase.obras.classifyFirestoreFailure
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.GeoPoint
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.code
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GitLiveFirebaseRelevamientoDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : FirebaseRelevamientoDataSource {
    override fun observarDeObra(userId: String, obraId: String): Flow<FirebaseRelevamientoResult<List<RelevamientoDto>>> =
        firestore.collection("obras").document(obraId).collection("relevamientos")
            .where { "userId" equalTo userId }.snapshots
            .map { snapshot ->
                val result: FirebaseRelevamientoResult<List<RelevamientoDto>> =
                    FirebaseRelevamientoResult.Success(snapshot.documents.map { it.toDto(obraId) })
                result
            }.catch { error ->
                if (error is CancellationException) throw error
                emit(FirebaseRelevamientoResult.Failure(error.toFailure()))
            }

    override suspend fun crear(dto: RelevamientoDto): FirebaseRelevamientoResult<RelevamientoDto> = try {
        val obra = firestore.collection("obras").document(dto.obraId)
        // La referencia se genera una sola vez, incluso si Firestore reintenta la transacción.
        val ref = obra.collection("relevamientos").document
        val stored = dto.copy(id = ref.id)
        firestore.runTransaction {
            val transaction = this
            prepararAlta(stored, object : TransaccionAltaRelevamiento {
                override suspend fun leerObra(): ObraParaRelevamiento? {
                    val snapshot = transaction.get(obra)
                    if (!snapshot.exists) return null
                    return ObraParaRelevamiento(snapshot.get("userId"), snapshot.timestampMillis("ultimaVisita"))
                }
                override suspend fun leerDuenoTarea(tareaId: String): String? {
                    val snapshot = transaction.get(obra.collection("tareas").document(tareaId))
                    return if (snapshot.exists) snapshot.get<String>("userId") else null
                }
                override fun guardar(dto: RelevamientoDto, ultimaVisitaMillis: Long) {
                    transaction.set(ref, dto.camposAlta())
                    transaction.update(obra, "ultimaVisita" to Timestamp.fromMilliseconds(ultimaVisitaMillis.toDouble()))
                }
            })
        }
        FirebaseRelevamientoResult.Success(stored)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FirebaseRelevamientoResult.Failure(error.toFailure())
    }

    private fun DocumentSnapshot.timestampMillis(field: String): Long? =
        if (contains(field)) get<Timestamp?>(field)?.toMilliseconds()?.toLong() else null

    private fun DocumentSnapshot.toDto(obraId: String): RelevamientoDto {
        val point = if (contains("ubicacion")) get<GeoPoint?>("ubicacion") else null
        return RelevamientoDto(
            id = id, obraId = obraId, userId = get("userId"),
            fechaMillis = get<Timestamp>("fecha").toMilliseconds().toLong(),
            nota = if (contains("nota")) get<String?>("nota").orEmpty() else "",
            tareaId = if (contains("tareaId")) get("tareaId") else null,
            fotoUrl = if (contains("fotoUrl")) get("fotoUrl") else null,
            latitud = point?.latitude, longitud = point?.longitude,
            proximaVisitaMillis = timestampMillis("proximaVisita"),
            horaVisita = if (contains("horaVisita")) get("horaVisita") else null,
        )
    }

    private fun Throwable.toFailure(): FirebaseRelevamientoFailure = when (this) {
        is ObraNoAutorizadaException -> FirebaseRelevamientoFailure.PermissionDenied
        is TareaNoDisponibleException -> FirebaseRelevamientoFailure.TaskUnavailable
        is FirebaseNetworkException -> FirebaseRelevamientoFailure.Network
        is FirebaseFirestoreException -> when (classifyFirestoreFailure(code.name)) {
            FirebaseObraFailure.PermissionDenied -> FirebaseRelevamientoFailure.PermissionDenied
            FirebaseObraFailure.Network -> FirebaseRelevamientoFailure.Network
            else -> FirebaseRelevamientoFailure.Unexpected
        }
        else -> FirebaseRelevamientoFailure.Unexpected
    }
}

internal fun RelevamientoDto.camposAlta(): Map<String, Any> = buildMap {
    put("userId", userId)
    put("fecha", Timestamp.fromMilliseconds(fechaMillis.toDouble()))
    put("nota", nota)
    tareaId?.let { put("tareaId", it) }
}
