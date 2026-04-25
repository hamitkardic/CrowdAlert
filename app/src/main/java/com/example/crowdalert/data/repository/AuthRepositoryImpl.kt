package com.example.crowdalert.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-in for Firebase Auth. [AuthRepository] will delegate to
 * [com.google.firebase.auth.FirebaseAuth] in a later step.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    private val session = MutableStateFlow<String?>(null)

    override fun currentUserId(): Flow<String?> = session

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        session.value = "stub-user"
        return Result.success(Unit)
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        session.value = "stub-user"
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        session.value = null
    }
}
