package com.arihant.notes_app.firebase_controller.auth

/**
 * Author: Arihant Jain
 * Date: 16-11-2025
 * Time: 12:37
 * Year: 2025
 * Month: November (Nov)
 * Day: 16 (Sunday)
 * Hour: 12
 * Minute: 37
 * Project: notes_app
 * Package: com.arihant.notes_app.firebase_controller.auth
 */

import android.content.Context
import android.util.Log
import com.arihant.notes_app.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.SecureRandom

class AuthController(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val TAG = "AuthController"

    // ----------------------------------------------------
    // REGISTER USER
    // ----------------------------------------------------
    fun registerUser(
        name: String,
        email: String,
        password: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val user = auth.currentUser!!
                    val uid = user.uid
                    val token = generateBearerToken(name)

                    val userObj = UserModel(
                        uid = uid,
                        name = name,
                        email = email,
                        token = token,
                        createdTime = System.currentTimeMillis(),
                        updatedTime = System.currentTimeMillis(),
                        isOnline = true // User is online initially
                    )

                    Log.d(TAG, "Sending verification email...")

                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            database.child("Users").child(uid).setValue(userObj)
                                .addOnSuccessListener {
                                    Log.d(TAG, "User saved successfully: $email")
                                    Log.d(TAG, "Bearer token generated: $token")
                                    callback(true, "Verification email sent to $email", token)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Database error: ${e.message}")
                                    callback(false, "Database error: ${e.message}", null)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Verification email failed, deleting user...")
                            user.delete().addOnSuccessListener {
                                Log.d(TAG, "Unverified user removed from Auth.")
                            }
                            callback(false, "Verification failed: ${e.message}", null)
                        }

                } else {
                    Log.e(TAG, "Registration failed: ${task.exception?.message}")
                    callback(false, task.exception?.message, null)
                }
            }
    }

    // ----------------------------------------------------
    // LOGIN USER
    // ----------------------------------------------------
    fun loginUser(
        email: String,
        password: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val user = auth.currentUser

                    when {
                        user == null ->
                            callback(false, "User not found", null)

                        !user.isEmailVerified ->
                            callback(false, "Email not verified. Please check your inbox.", null)

                        else -> {
                            val uid = user.uid
                            val newToken = generateBearerToken(user.displayName ?: "user")

                            val updates = mapOf(
                                "token" to newToken,
                                "updatedTime" to System.currentTimeMillis()
                            )

                            database.child("Users").child(uid).updateChildren(updates)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Bearer token updated on login.")
                                    callback(true, "Login successful", newToken)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to update token: ${e.message}")
                                    callback(false, "Login successful but token update failed", newToken)
                                }
                        }
                    }
                } else {
                    callback(false, task.exception?.message, null)
                }
            }
    }

    // ----------------------------------------------------
    // LOGOUT USER
    // ----------------------------------------------------
    fun logoutUser(token: String, callback: (Boolean, String?) -> Unit) {

        Log.d(TAG, "logoutUser() called")

        val user = auth.currentUser
        if (user != null) {

            val uid = user.uid
            Log.d(TAG, "Attempting logout for uid=$uid")

            database.child("Users").child(uid).get()
                .addOnSuccessListener { snapshot ->

                    val dbToken = snapshot.child("token").value as? String
                    Log.d(TAG, "DB token = $dbToken, Provided token = $token")

                    if (dbToken == token) {

                        Log.d(TAG, "Token matched → clearing token in database...")

                        val updates = mapOf(
                            "token" to null,
                            "updatedTime" to System.currentTimeMillis()
                        )

                        database.child("Users").child(uid).updateChildren(updates)
                            .addOnSuccessListener {

                                Log.d(TAG, "Token cleared (NULL) from database")

                                auth.signOut()
                                Log.d(TAG, "FirebaseAuth signOut() successful")

                                callback(true, "Successfully logged out")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to clear token: ${e.message}")
                                callback(false, "Failed to clear token: ${e.message}")
                            }

                    } else {
                        Log.e(TAG, "Logout denied: Token mismatch")
                        callback(false, "Invalid token. Logout failed.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Database error during logout: ${e.message}")
                    callback(false, "Database error: ${e.message}")
                }

        } else {
            Log.e(TAG, "logoutUser failed: no user logged in")
            callback(false, "No user logged in")
        }
    }


    // ----------------------------------------------------
    // RESEND EMAIL VERIFICATION
    // ----------------------------------------------------
    fun resendVerificationEmail(callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnSuccessListener {
                    callback(true, "Verification email sent again!")
                }
                .addOnFailureListener { e ->
                    callback(false, e.message)
                }
        } else {
            callback(false, "No user logged in")
        }
    }

    // ----------------------------------------------------
    // REAL-TIME TOKEN CHECK — AUTO LOGOUT ON TOKEN CHANGE
    // ----------------------------------------------------
    fun observeTokenChanges(
        uid: String,
        currentToken: String,
        onSessionInvalid: (String) -> Unit
    ) {
        val userRef = database.child("Users").child(uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val dbToken = snapshot.child("token").value as? String
                val logoutByUser = snapshot.child("logoutByUser").value as? Boolean ?: false

                // User clicked logout → token = null → DO NOTHING
                if (logoutByUser && dbToken.isNullOrEmpty()) {
                    Log.d(TAG, "User logged out manually → ignoring token listener")
                    return
                }

                // Session expired → token removed unexpectedly
                if (dbToken.isNullOrEmpty()) {
                    onSessionInvalid("Session expired. Please login again.")
                    return
                }

                // Token replaced → logged in on another device
                if (dbToken != currentToken) {
                    onSessionInvalid("Session expired. Please login again.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Token listener cancelled: ${error.message}")
            }
        })
    }

    // ----------------------------------------------------
    // BEARER TOKEN GENERATOR
    // ----------------------------------------------------
    private fun generateBearerToken(name: String, length: Int = 32): String {
        val random = SecureRandom()
        val digits = ('0'..'9').toList()
        val lower = ('a'..'z').toList()
        val upper = ('A'..'Z').toList()
        val special = listOf('!', '@', '#', '$', '%', '^', '&', '*', '-', '_', '+', '?')

        val allChars = digits + lower + upper + special

        val sb = StringBuilder(name.take(4))
        repeat(length) {
            sb.append(allChars[random.nextInt(allChars.size)])
        }
        return sb.toString()
    }
}
