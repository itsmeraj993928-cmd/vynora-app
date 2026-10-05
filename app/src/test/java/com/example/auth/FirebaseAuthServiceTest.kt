package com.example.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirebaseAuthServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @Test
    fun testAuthResultHierarchy() {
        val errorResult: AuthResult = AuthResult.Error("Network failure", RuntimeException("Timeout"))
        assertTrue(errorResult is AuthResult.Error)
        assertEquals("Network failure", (errorResult as AuthResult.Error).message)
        assertNotNull(errorResult.exception)

        val cancelledResult: AuthResult = AuthResult.Cancelled
        assertTrue(cancelledResult is AuthResult.Cancelled)
    }

    @Test
    fun testEmailSignInValidation() = runTest {
        val authService = FirebaseAuthService(context)
        
        // Blank email should fail with validation error
        val blankEmailResult = authService.signInWithEmail("", "password123")
        assertTrue(blankEmailResult is AuthResult.Error)
        assertTrue((blankEmailResult as AuthResult.Error).message.contains("cannot be empty"))

        // Blank password should fail
        val blankPassResult = authService.signInWithEmail("user@example.com", "")
        assertTrue(blankPassResult is AuthResult.Error)
    }

    @Test
    fun testEmailSignUpValidation() = runTest {
        val authService = FirebaseAuthService(context)

        // Short password (< 6 chars) should fail with validation error
        val shortPassResult = authService.signUpWithEmail("user@example.com", "123")
        assertTrue(shortPassResult is AuthResult.Error)
        assertTrue((shortPassResult as AuthResult.Error).message.contains("at least 6 characters"))

        // Blank email should fail
        val blankEmailResult = authService.signUpWithEmail("", "password123")
        assertTrue(blankEmailResult is AuthResult.Error)
    }

    @Test
    fun testSignOutClearsSession() {
        val authService = FirebaseAuthService(context)
        authService.signOut()
        assertNull(authService.currentUser)
        assertFalse(authService.isUserSignedIn)
    }
}
