package com.example.crowdalert.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Presents sign-in and sign-up actions; exposes whether a user session exists.
 * Passwords are never logged. Firebase performs verification in the wired implementation.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> =
        authRepository
            .currentUserId()
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    fun signIn(email: String, password: String, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            authRepository
                .signIn(email, password)
                .onSuccess { onResult(null) }
                .onFailure { onResult(it) }
        }
    }

    fun signUp(email: String, password: String, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            authRepository
                .signUp(email, password)
                .onSuccess { onResult(null) }
                .onFailure { onResult(it) }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
