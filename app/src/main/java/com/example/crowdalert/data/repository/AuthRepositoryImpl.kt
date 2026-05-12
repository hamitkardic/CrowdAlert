package com.example.crowdalert.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
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
    private val firestore: FirebaseFirestore,
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

    override suspend fun signUp(fullName: String, email: String, password: String): Result<Unit> =
        runCatching {
            val trimmedEmail = email.trim()
            val trimmedFullName = fullName.trim()
            val result = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            result.user?.let { user ->
                runCatching {
                    user
                        .updateProfile(
                            UserProfileChangeRequest
                                .Builder()
                                .setDisplayName(trimmedFullName)
                                .build(),
                        ).await()
                }
                runCatching {
                    firestore
                        .collection(COLLECTION_USERS)
                        .document(user.uid)
                        .set(
                            mapOf(
                                "email" to trimmedEmail,
                                "name" to trimmedFullName,
                            ),
                        )
                        .await()
                }
            }
            Unit
        }

    override suspend fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    private companion object {
        const val COLLECTION_USERS = "users"
    }
}
