package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Production-ready implementation of [AuthService] combining Firebase Authentication
 * with AndroidX Jetpack Credential Manager for Google Sign-In.
 */
class FirebaseAuthService(
    private val context: Context,
    private val auth: FirebaseAuth = Firebase.auth
) : AuthService {

    private val credentialManager: CredentialManager = CredentialManager.create(context.applicationContext)

    private val _authStateFlow = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    override val authStateFlow: StateFlow<FirebaseUser?> = _authStateFlow.asStateFlow()

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _authStateFlow.value = firebaseAuth.currentUser
        }
    }

    /**
     * Resolves the server client ID used for Google Sign-In.
     * Uses R.string.default_web_client_id generated from google-services.json
     * or falls back to the configured web client ID.
     */
    private fun getWebClientId(): String {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            // Fallback web client id from vynora-de737 project config
            "689756791649-s9g3jemtngmuf21kmvbv7oskuidpcoio.apps.googleusercontent.com"
        }
    }

    override suspend fun signInWithGoogle(activity: Activity): AuthResult {
        val serverClientId = getWebClientId()
        if (serverClientId.isBlank()) {
            return AuthResult.Error("Google Sign-In configuration (client ID) is missing.")
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                signInWithGoogleIdToken(googleIdToken)
            } else {
                AuthResult.Error("Received unsupported credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In prompt was cancelled by the user")
            AuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credentials available on device", e)
            AuthResult.Error("No Google account found on device. Please add a Google account or sign in with email.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed via Credential Manager", e)
            AuthResult.Error(e.localizedMessage ?: "Google Sign-In failed", e)
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return AuthResult.Error("Firebase returned an empty user profile.")
            val isNewUser = authResult.additionalUserInfo?.isNewUser == true
            AuthResult.Success(user = user, isNewUser = isNewUser, idToken = idToken)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signInWithCredential failed", e)
            AuthResult.Error(e.localizedMessage ?: "Firebase authentication failed", e)
        }
    }

    override suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.isBlank()) {
            return AuthResult.Error("Email and password cannot be empty")
        }
        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
            val user = authResult.user
                ?: return AuthResult.Error("Firebase returned an empty user profile.")
            AuthResult.Success(user = user, isNewUser = false)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmailAndPassword failed", e)
            AuthResult.Error(e.localizedMessage ?: "Sign in failed", e)
        }
    }

    override suspend fun signUpWithEmail(email: String, pass: String): AuthResult {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.length < 6) {
            return AuthResult.Error("A valid email and password (at least 6 characters) are required.")
        }
        return try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
            val user = authResult.user
                ?: return AuthResult.Error("Firebase returned an empty user after registration.")
            try {
                user.sendEmailVerification().await()
            } catch (ignored: Exception) {
                // Ignore failure to send verification email immediately
            }
            AuthResult.Success(user = user, isNewUser = true)
        } catch (e: Exception) {
            Log.e(TAG, "createUserWithEmailAndPassword failed", e)
            AuthResult.Error(e.localizedMessage ?: "Registration failed", e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No user currently signed in"))
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reloadUser(): Result<Boolean> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No user currently signed in"))
        return try {
            user.reload().await()
            Result.success(user.isEmailVerified)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        auth.signOut()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state", e)
            }
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthService"
    }
}
