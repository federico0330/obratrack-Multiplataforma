package com.unaj.obratrack.core.firebase.obras

import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreFailureClassifierTest {
    @Test
    fun clasificaPermisosRedYErroresDesconocidos() {
        assertEquals(FirebaseObraFailure.PermissionDenied, classifyFirestoreFailure("PERMISSION_DENIED"))
        assertEquals(FirebaseObraFailure.Network, classifyFirestoreFailure("UNAVAILABLE"))
        assertEquals(FirebaseObraFailure.Unexpected, classifyFirestoreFailure("INTERNAL"))
    }
}
