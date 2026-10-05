package com.example.auth

import com.google.firebase.auth.FirebaseUser

/**
 * Sealed class representing authentication outcomes from [AuthService].
 */
sealed class AuthResult {
    /**
     * Authentication succeeded.
     *
     * @param user The authenticated [FirebaseUser].
     * @param isNewUser Whether this user just registered for the first time.
     * @param idToken The Google OAuth ID Token if signed in with Google, or null.
     */
    data class Success(
        val user: FirebaseUser,
        val isNewUser: Boolean = false,
        val idToken: String? = null
    ) : AuthResult()

    /**
     * Authentication failed with an error message and optional underlying exception.
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : AuthResult()

    /**
     * The user explicitly dismissed or cancelled the authentication prompt.
     */
    object Cancelled : AuthResult()
}
