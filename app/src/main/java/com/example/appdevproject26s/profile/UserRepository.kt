package com.example.appdevproject26s.profile

import com.example.appdevproject26s.auth.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val authRepository: AuthRepository
) {
    private val _currentUser = MutableStateFlow<UserProfileResponse?>(null)
    val currentUser: StateFlow<UserProfileResponse?> = _currentUser.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            authRepository.isLoggedInFlow.collect { isLoggedIn ->
                if (isLoggedIn) {
                    fetchCurrentUser()
                } else {
                    _currentUser.value = null
                }
            }
        }
    }

    suspend fun fetchCurrentUser() {
        getCurrentUser().fold(
            onSuccess = { user -> _currentUser.value = user },
            onFailure = { _currentUser.value = null }
        )
    }

    suspend fun getCurrentUser(): Result<UserProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = userApiService.getCurrentUser()
                Result.success(profile)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = if (!errorBody.isNullOrBlank()) {
                    errorBody.replace("\"", "")
                } else {
                    e.localizedMessage ?: "Failed to fetch user profile"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: Check your connection"))
            }
        }
    }

    suspend fun updateProfile(username: String?, email: String?): Result<UserProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateProfileRequest(username = username, email = email)
                val updatedProfile = userApiService.updateProfile(request)
                Result.success(updatedProfile)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = if (!errorBody.isNullOrBlank()) {
                    errorBody.replace("\"", "")
                } else {
                    e.localizedMessage ?: "Failed to update profile"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: Check your connection"))
            }
        }
    }
}