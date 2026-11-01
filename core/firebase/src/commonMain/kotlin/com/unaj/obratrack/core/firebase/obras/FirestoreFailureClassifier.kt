package com.unaj.obratrack.core.firebase.obras

import dev.gitlive.firebase.firestore.FirestoreExceptionCode

internal fun classifyFirestoreFailure(code: FirestoreExceptionCode): FirebaseObraFailure = when (code) {
    FirestoreExceptionCode.PERMISSION_DENIED, FirestoreExceptionCode.UNAUTHENTICATED -> FirebaseObraFailure.PermissionDenied
    FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> FirebaseObraFailure.Network
    else -> FirebaseObraFailure.Unexpected
}
