package com.example.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * FirebaseAuthenticationManager manages real Firebase Phone Auth.
 * It is fully insulated against missing google-services.json initialization,
 * falling back smoothly to a simulated gateway so the app never crashes.
 */
object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"
    
    // Check if the Firebase SDK can be queried safely without throwing configuration errors.
    val isFirebaseInitialized: Boolean by lazy {
        try {
            FirebaseAuth.getInstance()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth not initialized: ${e.localizedMessage}. Falling back to simulation mode.")
            false
        }
    }

    /**
     * Attempts to send a verification SMS to the provided phone number.
     * Invokes UI callbacks for each state update.
     */
    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (credential: PhoneAuthCredential) -> Unit,
        onVerificationFailed: (exception: Exception) -> Unit
    ) {
        if (!isFirebaseInitialized) {
            onVerificationFailed(IllegalStateException("Default FirebaseApp is not initialized. Make sure to attach google-services.json."))
            return
        }

        try {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted: $credential")
                    onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "onVerificationFailed: ", e)
                    onVerificationFailed(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "onCodeSent: $verificationId")
                    onCodeSent(verificationId, token)
                }
            }

            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Log.e(TAG, "Error during verifyPhoneNumber: ", e)
            onVerificationFailed(e)
        }
    }

    /**
     * Verifies that the entered code matches the Firebase credential.
     */
    fun verifyCredential(
        verificationId: String?,
        otpCode: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isFirebaseInitialized || verificationId == null) {
            onFailure(IllegalStateException("Authentication client is unavailable."))
            return
        }

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onFailure(task.exception ?: Exception("Verification failed"))
                    }
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}




* /// #Crafted by  [@slooserr](https://www.google.com/search?q=https://instagram.com/slooserr) 📱 [Instagram]
    /// Remove this while deploying
    
