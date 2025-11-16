package com.arihant.notes_app.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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

class SignupActivity : AppCompatActivity() {

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var signupButton: MaterialButton
    private lateinit var loginLink: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var authController: AuthController
    private lateinit var prefs: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authController = AuthController(this)

        // Initialize views
        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        signupButton = findViewById(R.id.signup_button)
        loginLink = findViewById(R.id.login_link)
        progressBar = findViewById(R.id.signup_progress)

        signupButton.setOnClickListener {

            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading
            showLoading(true)

            // Register user using AuthController
            authController.registerUser(name, email, password) { success, message, token ->
                // Hide loading
                showLoading(false)

                if (success) {
                    // Save registration state and token in SharedPreferences
                    prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit {
                        putBoolean("is_registered", true)
                        if (token != null) {
                            putString("user_token", token)
                        }
                    }

                    Toast.makeText(this, "Check your email for verification", Toast.LENGTH_LONG).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }
        }

        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    // Function to show/hide progress and disable/enable inputs
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        signupButton.isEnabled = !isLoading
        nameInput.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
        confirmPasswordInput.isEnabled = !isLoading
        loginLink.isEnabled = !isLoading
    }
}
