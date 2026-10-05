package com.example.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication service contract encapsulating Firebase Authentication and
 * Google Sign-In via Jetpack Credential Manager.
 */
interface AuthService {
    /** Currently signed-in FirebaseUser, or null if unauthenticated. */
    val currentUser: FirebaseUser?

    /** UID of the currently signed-in user, or null. */
    val currentUserId: String?

    /** Whether an authenticated user session is active. */
    val isUserSignedIn: Boolean

    /** Observable state flow of the currently signed in Firebase user. */
    val authStateFlow: StateFlow<FirebaseUser?>

    /**
     * Initiates Google Sign-In using AndroidX Jetpack Credential Manager.
     * Launches the Google Account Picker dialog and exchanges credentials with Firebase.
     *
     * @param activity The host Activity required by CredentialManager.
     * @return [AuthResult] indicating success, error, or user cancellation.
     */
    suspend fun signInWithGoogle(activity: Activity): AuthResult

    /**
     * Signs into Firebase using an existing Google ID Token.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult

    /**
     * Signs in with email and password.
     */
    suspend fun signInWithEmail(email: String, pass: String): AuthResult

    /**
     * Creates a new Firebase user with email and password.
     */
    suspend fun signUpWithEmail(email: String, pass: String): AuthResult

    /**
     * Sends an email verification link to the current user's email address.
     */
    suspend fun sendEmailVerification(): Result<Unit>

    /**
     * Sends a password reset email to the specified address.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /**
     * Reloads the current user profile from Firebase to refresh email verification status.
     * @return Result containing true if email is verified, false otherwise.
     */
    suspend fun reloadUser(): Result<Boolean>

    /**
     * Signs out the current user from Firebase and clears credentials.
     */
    fun signOut()
}
