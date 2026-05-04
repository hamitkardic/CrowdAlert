package com.example.crowdalert.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.crowdalert.R
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presents sign-in and sign-up actions; exposes whether a user session exists.
 * Passwords are never logged. Firebase performs verification in the wired implementation.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isSignedIn: StateFlow<Boolean> =
        authRepository
            .currentUser()
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    fun signIn(email: String, password: String) {
        val validationError = validateCredentials(email, password)
        if (validationError != null) {
            _uiState.value = AuthUiState(errorMessageRes = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository
                .signIn(email.trim(), password)
                .fold(
                    onSuccess = { _uiState.value = AuthUiState() },
                    onFailure = { error ->
                        _uiState.value = AuthUiState(errorMessageRes = error.toUserMessageRes())
                    },
                )
        }
    }

    fun signUp(email: String, password: String) {
        val validationError = validateCredentials(email, password)
        if (validationError != null) {
            _uiState.value = AuthUiState(errorMessageRes = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository
                .signUp(email.trim(), password)
                .fold(
                    onSuccess = { _uiState.value = AuthUiState() },
                    onFailure = { error ->
                        _uiState.value = AuthUiState(errorMessageRes = error.toUserMessageRes())
                    },
                )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState()
            onSignedOut()
        }
    }

    @StringRes
    private fun validateCredentials(email: String, password: String): Int? {
        val trimmedEmail = email.trim()
        return when {
            trimmedEmail.isBlank() -> R.string.auth_error_email_required
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> R.string.auth_error_email_invalid
            password.isBlank() -> R.string.auth_error_password_required
            password.length < MIN_PASSWORD_LENGTH -> R.string.auth_error_password_too_short
            else -> null
        }
    }

    @StringRes
    private fun Throwable.toUserMessageRes(): Int = R.string.auth_error_failed

    data class AuthUiState(
        val isLoading: Boolean = false,
        @StringRes val errorMessageRes: Int? = null,
    )

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
