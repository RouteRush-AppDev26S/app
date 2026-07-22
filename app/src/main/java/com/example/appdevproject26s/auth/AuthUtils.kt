package com.example.appdevproject26s.auth

import retrofit2.HttpException

fun parseHttpError(e: Exception, defaultMessage: String = "Operation failed"): Exception {
    return if (e is HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        val errorMessage = if (!errorBody.isNullOrBlank()) {
            errorBody.replace("\"", "")
        } else {
            e.localizedMessage ?: defaultMessage
        }
        Exception(errorMessage)
    } else {
        Exception("Network error: Check your connection")
    }
}