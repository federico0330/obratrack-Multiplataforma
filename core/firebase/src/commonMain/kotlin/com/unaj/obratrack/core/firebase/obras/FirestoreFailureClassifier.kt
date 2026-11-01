package com.unaj.obratrack.core.firebase.obras

internal fun classifyFirestoreFailure(code: String): FirebaseObraFailure = when (code) {
    "PERMISSION_DENIED", "UNAUTHENTICATED" -> FirebaseObraFailure.PermissionDenied
    "UNAVAILABLE", "DEADLINE_EXCEEDED" -> FirebaseObraFailure.Network
    else -> FirebaseObraFailure.Unexpected
}
