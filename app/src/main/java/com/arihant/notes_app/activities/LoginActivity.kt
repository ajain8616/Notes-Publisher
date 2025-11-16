package com.arihant.notes_app.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arihant.notes_app.R
import com.arihant.notes_app.firebase_controller.auth.AuthController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signUpLink: TextView
    private lateinit var forgotPassword: TextView
    private lateinit var resendVerification: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var authController: AuthController
    private lateinit var prefs: SharedPreferences

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authController = AuthController(this)

        // Initialize views
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signUpLink = findViewById(R.id.sign_up_link)
        forgotPassword = findViewById(R.id.forgot_password)
        resendVerification = findViewById(R.id.resend_verification)
        progressBar = findViewById(R.id.login_progress)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            Log.d(TAG, "Login button clicked. Email: $email")

            if (email.isEmpty() || password.isEmpty()) {
                Log.d(TAG, "Email or password is empty")
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            Log.d(TAG, "Attempting login...")

            authController.loginUser(email, password) { success, message, token ->
                showLoading(false)

                Log.d(TAG, "Login callback → Success: $success, Message: $message, Token: $token")

                if (success) {
                    prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit {
                        putBoolean("is_logged_in", true)
                        if (token != null) {
                            putString("user_token", token)
                            Log.d(TAG, "Token saved in SharedPreferences")
                            Log.d(TAG, "BEARER TOKEN: $token")
                        }
                    }

                    Log.d(TAG, "Login successful → Opening MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()

                } else {
                    Log.d(TAG, "Login failed: $message")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                    if (message?.contains("verify", ignoreCase = true) == true) {
                        Log.d(TAG, "Email not verified → showing resend verification")
                        resendVerification.visibility = TextView.VISIBLE
                    }
                }
            }
        }

        resendVerification.setOnClickListener {
            Log.d(TAG, "Resend verification clicked")

            authController.resendVerificationEmail { success, msg ->
                Log.d(TAG, "Resend verification callback → Success: $success, Msg: $msg")
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        signUpLink.setOnClickListener {
            Log.d(TAG, "Navigating to SignupActivity")
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun showLoading(isLoading: Boolean) {
        Log.d(TAG, "Loading state changed: $isLoading")

        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
        forgotPassword.isEnabled = !isLoading
        signUpLink.isEnabled = !isLoading
        resendVerification.isEnabled = !isLoading
    }
}
