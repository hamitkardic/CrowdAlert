package com.example.crowdalert.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for sign-in, sign-up, and session observation.
 * The implementation will wrap [com.google.firebase.auth.FirebaseAuth] in a later step.
 */
interface AuthRepository {
    fun currentUserId(): Flow<String?>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(email: String, password: String): Result<Unit>

    suspend fun signOut()
}
