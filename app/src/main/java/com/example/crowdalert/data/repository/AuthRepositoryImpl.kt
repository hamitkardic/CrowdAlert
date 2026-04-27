package com.example.crowdalert.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed auth implementation for email/password sessions.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override fun currentUser(): Flow<FirebaseUser?> = callbackFlow {
        val listener =
            FirebaseAuth.AuthStateListener { auth ->
                trySend(auth.currentUser)
            }
        trySend(firebaseAuth.currentUser)
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}
