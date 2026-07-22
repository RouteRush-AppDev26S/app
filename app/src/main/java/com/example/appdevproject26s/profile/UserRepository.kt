package com.example.appdevproject26s.user

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService
) {

    suspend fun getCurrentUser(): Result<UserProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = userApiService.getCurrentUser()
                Result.success(profile)
            } catch (e: retrofit2.HttpException) {
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
            } catch (e: retrofit2.HttpException) {
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