package com.example.crowdalert.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentUser(): Flow<FirebaseUser?>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(fullName: String, email: String, password: String): Result<Unit>

    suspend fun signOut()
}
