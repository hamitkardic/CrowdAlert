package com.example.crowdalert.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for sign-in, sign-up, and session observation.
 * Passwords are passed only to Firebase Auth and are never logged or persisted locally.
 */
interface AuthRepository {
    fun currentUser(): Flow<FirebaseUser?>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(email: String, password: String): Result<Unit>

    suspend fun signOut()
}
