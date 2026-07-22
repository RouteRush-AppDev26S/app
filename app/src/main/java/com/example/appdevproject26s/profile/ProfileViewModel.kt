package com.example.appdevproject26s.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.auth.AuthState
import com.example.appdevproject26s.user.UserProfileResponse
import com.example.appdevproject26s.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Observe global login status from the repository
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val authToken: StateFlow<String?> = authRepository.tokenFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile: StateFlow<UserProfileResponse?> = _userProfile.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _emailInput = MutableStateFlow("")
    val emailInput = _emailInput.asStateFlow()

    private val _usernameInput = MutableStateFlow("")
    val usernameInput = _usernameInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput = _passwordInput.asStateFlow()

    // Tracks whether the wizard is currently in Register mode (true) or Login mode (false)
    private val _isRegisterMode = MutableStateFlow(false)
    val isRegisterMode = _isRegisterMode.asStateFlow()

    fun fetchUserProfile() {
        viewModelScope.launch {
            userRepository.getCurrentUser().fold(
                onSuccess = { profile ->
                    _userProfile.value = profile
                },
                onFailure = { error ->
                    _userProfile.value = null

                    if (error.localizedMessage?.contains("401") == true ||
                        error.localizedMessage?.contains("Unauthorized") == true) {
                        logout() // Automatically clear the expired token and send them back to login
                    }
                }
            )
        }
    }

    fun updateUsername(username: String) {
        _usernameInput.value = username
    }

    fun updateEmail(email: String) {
        _emailInput.value = email
    }

    fun updatePassword(password: String) {
        _passwordInput.value = password
    }

    fun toggleAuthMode() {
        _isRegisterMode.value = !_isRegisterMode.value
        resetState()
    }

    fun submitAuth() {
        val email = _emailInput.value
        val password = _passwordInput.value
        val username = _usernameInput.value

        if (!_isRegisterMode.value) {
            if (username.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("username and password cannot be empty")
                return
            }
        }

        if (_isRegisterMode.value) {
            if (username.isBlank() || password.isBlank() || email.isBlank()) {
                _authState.value = AuthState.Error("email, username and password cannot be empty")
                return
            }
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = if (_isRegisterMode.value) {
                authRepository.register(email, username, password)
            } else {
                authRepository.login(username, password)
            }

            _authState.value = result.fold(
                onSuccess = { token -> AuthState.Success(token) },
                onFailure = { error ->
                    AuthState.Error(
                        error.localizedMessage ?: "Operation failed"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
            _userProfile.value = null
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}