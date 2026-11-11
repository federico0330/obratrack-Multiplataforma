package com.unaj.obratrack.core.firebase.tareas

import com.unaj.obratrack.core.firebase.obras.FirebaseObraFailure
import com.unaj.obratrack.core.firebase.obras.classifyFirestoreFailure
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.code
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Adaptador GitLive para obras/{obraId}/tareas: conserva la forma de los documentos existentes. */
class GitLiveFirebaseTareaDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : FirebaseTareaDataSource {
    override fun observarDeObra(userId: String, obraId: String): Flow<FirebaseTareaResult<List<TareaDto>>> =
        tareas(obraId)
            .where { FIELD_USER_ID equalTo userId }
            .snapshots
            .map { snapshot ->
                val result: FirebaseTareaResult<List<TareaDto>> =
                    FirebaseTareaResult.Success(snapshot.documents.map { it.toDto(obraId) })
                result
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(FirebaseTareaResult.Failure(error.toFailure()))
            }

    override suspend fun crear(dto: TareaDto): FirebaseTareaResult<TareaDto> = try {
        val ref = tareas(dto.obraId).document
        ref.set(dto.toFirestoreMap())
        FirebaseTareaResult.Success(dto.copy(id = ref.id))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FirebaseTareaResult.Failure(error.toFailure())
    }

    override suspend fun actualizar(
        userId: String,
        obraId: String,
        tareaId: String,
        fields: TareaUpdateFields,
    ): FirebaseTareaResult<Unit> = writeResult {
        // Una fecha ausente borra el campo guardado; omitirla conservaría el valor anterior.
        tareas(obraId).document(tareaId).update(
            FIELD_TITULO to fields.titulo,
            FIELD_DESCRIPCION to fields.descripcion,
            FIELD_CATEGORIA to fields.categoria,
            FIELD_ESTADO to fields.estado,
            FIELD_FECHA_INICIO to (fields.fechaInicio ?: FieldValue.delete),
            FIELD_FECHA_FIN to (fields.fechaFin ?: FieldValue.delete),
        )
    }

    override suspend fun cambiarEstado(
        userId: String,
        obraId: String,
        tareaId: String,
        estado: String,
    ): FirebaseTareaResult<Unit> = writeResult {
        tareas(obraId).document(tareaId).update(FIELD_ESTADO to estado)
    }

    override suspend fun eliminar(
        userId: String,
        obraId: String,
        tareaId: String,
        eliminarRelevamientos: Boolean,
    ): FirebaseTareaResult<Unit> = writeResult {
        val obra = firestore.collection(COLLECTION_OBRAS).document(obraId)
        val relevamientos = obra.collection(SUBCOLLECTION_RELEVAMIENTOS)
        firestore.runTransaction {
            val transaction = this
            transaction.get(obra)
            ejecutarBajaTarea(object : AlmacenBajaTarea {
                override suspend fun leerVinculados(): List<String> = relevamientos
                    .where { FIELD_TAREA_ID equalTo tareaId }
                    .get(Source.SERVER).documents.map { it.id }

                override suspend fun leerFechas(): Map<String, Long> = relevamientos
                    .get(Source.SERVER).documents.mapNotNull { doc ->
                        if (!doc.contains(FIELD_FECHA)) return@mapNotNull null
                        doc.get<Timestamp?>(FIELD_FECHA)?.let { doc.id to it.toMilliseconds().toLong() }
                    }.toMap()

                override suspend fun aplicar(plan: PlanBajaTarea) {
                    plan.relevamientos.forEach { id ->
                        val ref = relevamientos.document(id)
                        if (plan.eliminarRelevamientos) transaction.delete(ref)
                        else transaction.update(ref, FIELD_TAREA_ID to FieldValue.delete)
                    }
                    transaction.delete(tareas(obraId).document(tareaId))
                    if (plan.actualizarUltimaVisita) {
                        transaction.update(
                            obra,
                            FIELD_ULTIMA_VISITA to (plan.ultimaVisitaMillis?.let {
                                Timestamp.fromMilliseconds(it.toDouble())
                            } ?: FieldValue.delete),
                            "revisionRelevamientos" to FieldValue.increment(1),
                        )
                    }
                }
            }, eliminarRelevamientos)
        }
    }

    private fun tareas(obraId: String) =
        firestore.collection(COLLECTION_OBRAS).document(obraId).collection(SUBCOLLECTION_TAREAS)

    private suspend inline fun writeResult(crossinline write: suspend () -> Unit): FirebaseTareaResult<Unit> = try {
        write()
        FirebaseTareaResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FirebaseTareaResult.Failure(error.toFailure())
    }

    private fun DocumentSnapshot.toDto(obraId: String): TareaDto = TareaDto(
        id = id,
        obraId = obraId,
        userId = get(FIELD_USER_ID),
        titulo = get(FIELD_TITULO),
        descripcion = if (contains(FIELD_DESCRIPCION)) get<String?>(FIELD_DESCRIPCION).orEmpty() else "",
        categoria = if (contains(FIELD_CATEGORIA)) get<String?>(FIELD_CATEGORIA).orEmpty() else "",
        estado = get(FIELD_ESTADO),
        fechaInicio = if (contains(FIELD_FECHA_INICIO)) get<String?>(FIELD_FECHA_INICIO) else null,
        fechaFin = if (contains(FIELD_FECHA_FIN)) get<String?>(FIELD_FECHA_FIN) else null,
    )

    private fun TareaDto.toFirestoreMap(): Map<String, Any> = buildMap {
        put(FIELD_USER_ID, userId)
        put(FIELD_TITULO, titulo)
        put(FIELD_DESCRIPCION, descripcion)
        put(FIELD_CATEGORIA, categoria)
        put(FIELD_ESTADO, estado)
        fechaInicio?.let { put(FIELD_FECHA_INICIO, it) }
        fechaFin?.let { put(FIELD_FECHA_FIN, it) }
    }

    private fun Throwable.toFailure(): FirebaseTareaFailure = when (this) {
        is DemasiadosVinculadosException -> FirebaseTareaFailure.TooManyLinked
        is FirebaseNetworkException -> FirebaseTareaFailure.Network
        is FirebaseFirestoreException -> classifyFirestoreFailure(code.name).toTarea()
        else -> FirebaseTareaFailure.Unexpected
    }

    private fun FirebaseObraFailure.toTarea(): FirebaseTareaFailure = when (this) {
        FirebaseObraFailure.PermissionDenied -> FirebaseTareaFailure.PermissionDenied
        FirebaseObraFailure.Network -> FirebaseTareaFailure.Network
        FirebaseObraFailure.Partial -> FirebaseTareaFailure.Partial
        FirebaseObraFailure.Unexpected -> FirebaseTareaFailure.Unexpected
    }

    private companion object {
        const val COLLECTION_OBRAS = "obras"
        const val SUBCOLLECTION_TAREAS = "tareas"
        const val SUBCOLLECTION_RELEVAMIENTOS = "relevamientos"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TITULO = "titulo"
        const val FIELD_DESCRIPCION = "descripcion"
        const val FIELD_CATEGORIA = "categoria"
        const val FIELD_ESTADO = "estado"
        const val FIELD_FECHA_INICIO = "fechaInicio"
        const val FIELD_FECHA_FIN = "fechaFin"
        const val FIELD_TAREA_ID = "tareaId"
        const val FIELD_FECHA = "fecha"
        const val FIELD_ULTIMA_VISITA = "ultimaVisita"
    }
}
