package com.unaj.obratrack.core.firebase.obras

import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreFailureClassifierTest {
    @Test
    fun clasificaPermisosRedYErroresDesconocidos() {
        assertEquals(FirebaseObraFailure.PermissionDenied, classifyFirestoreFailure(FirestoreExceptionCode.PERMISSION_DENIED))
        assertEquals(FirebaseObraFailure.Network, classifyFirestoreFailure(FirestoreExceptionCode.UNAVAILABLE))
        assertEquals(FirebaseObraFailure.Unexpected, classifyFirestoreFailure(FirestoreExceptionCode.INTERNAL))
    }
}
