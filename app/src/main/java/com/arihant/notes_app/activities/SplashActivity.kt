package com.arihant.notes_app.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arihant.notes_app.R
import com.arihant.notes_app.firebase_controller.auth.GetAuthController
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {

    private lateinit var appLogo: ImageView
    private lateinit var appName: TextView
    private lateinit var appTagline: TextView
    private lateinit var loadingProgress: LinearProgressIndicator
    private lateinit var versionInfo: TextView
    private lateinit var authController: GetAuthController

    private val splashDelay: Long = 3500

    private var userToken: String? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        initializeViews()
        setupWindowInsets()

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userToken = prefs.getString("user_token", null)

        FirebaseApp.initializeApp(this)
        authController = GetAuthController(this)

        startSplashAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            handleUserStatus()
        }, splashDelay)
    }

    private fun initializeViews() {
        appLogo = findViewById(R.id.logo)
        appName = findViewById(R.id.app_name)
        appTagline = findViewById(R.id.app_tagline)
        loadingProgress = findViewById(R.id.loading_bar)
        versionInfo = findViewById(R.id.version_name)

        appName.visibility = View.INVISIBLE
        appTagline.visibility = View.INVISIBLE
        loadingProgress.visibility = View.INVISIBLE
        versionInfo.visibility = View.INVISIBLE

        appName.alpha = 0f
        appTagline.alpha = 0f
        loadingProgress.alpha = 0f
        versionInfo.alpha = 0f
    }

    private fun setupWindowInsets() {
        val rootView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startSplashAnimation() {
        val flipInAnimator = android.animation.AnimatorInflater.loadAnimator(
            this,
            R.animator.card_flip_in
        )
        flipInAnimator.setTarget(appLogo)
        flipInAnimator.start()

        flipInAnimator.doOnEnd { showAppName() }
    }

    private fun showAppName() {
        appName.visibility = View.VISIBLE
        appName.animate()
            .alpha(1f)
            .setDuration(800)
            .withEndAction { showTagline() }
            .start()
    }

    private fun showTagline() {
        appTagline.visibility = View.VISIBLE
        appTagline.animate()
            .alpha(1f)
            .setDuration(800)
            .withEndAction { showLoadingElements() }
            .start()
    }

    private fun showLoadingElements() {
        loadingProgress.visibility = View.VISIBLE
        versionInfo.visibility = View.VISIBLE

        loadingProgress.animate().alpha(1f).setDuration(600).start()
        versionInfo.animate().alpha(1f).setDuration(600).start()
    }

    private fun handleUserStatus() {
        if (!isOnline()) {
            showOfflineMessage()
            decideNextActivity(isOffline = true)
        } else {
            userToken?.let { token ->
                authController.getUserProfileByToken(token) { success, user, uid ->
                    if (success && user != null && uid != null) {
                        authController.updateUserProfileByToken(token, isOnline = true) { updateSuccess, message ->
                            if (updateSuccess) {
                                Toast.makeText(this, "Status Updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            }
                            decideNextActivity(isOffline = false)
                        }
                    } else {
                        // If user not found, proceed
                        decideNextActivity(isOffline = false)
                    }
                }
            } ?: run {
                // No token, go to login/signup
                decideNextActivity(isOffline = false)
            }
        }
    }

    private fun decideNextActivity(isOffline: Boolean) {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val isRegistered = prefs.getBoolean("is_registered", false)
        userToken = prefs.getString("user_token", null)

        val nextIntent = when {
            userToken.isNullOrEmpty() -> Intent(this, LoginActivity::class.java)
            isLoggedIn -> Intent(this, MainActivity::class.java)
            isRegistered -> Intent(this, LoginActivity::class.java)
            else -> Intent(this, SignupActivity::class.java)
        }

        nextIntent.putExtra("user_token", userToken)
        nextIntent.putExtra("is_offline", isOffline)
        startActivity(nextIntent)
        overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
        finish()
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showOfflineMessage() {
        versionInfo.text = "You are offline"
        versionInfo.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }
}
