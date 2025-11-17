package com.arihant.notes_app.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arihant.notes_app.R
import com.arihant.notes_app.firebase_controller.auth.GetAuthController
import com.arihant.notes_app.utils.NetworkChecker
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {

    private lateinit var appLogo: ImageView
    private lateinit var appName: TextView
    private lateinit var appTagline: TextView
    private lateinit var loadingProgress: LinearProgressIndicator
    private lateinit var versionInfo: TextView
    private lateinit var authController: GetAuthController
    private lateinit var prefs: SharedPreferences

    private var userToken: String? = null
    private lateinit var networkChecker: NetworkChecker

    private val splashDelay: Long = 3500

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

        // Initialize NetworkChecker
        networkChecker = NetworkChecker(this, authController, userToken)
        networkChecker.startChecking() // start monitoring network status

        startSplashAnimation()

        // Delay to handle next activity after splash animation
        Handler(Looper.getMainLooper()).postDelayed({
            decideNextActivity()
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

    private fun decideNextActivity() {
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
        startActivity(nextIntent)
        overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
        finish()
    }

    override fun onPause() {
        super.onPause()
        networkChecker.stopChecking() // stop network monitoring
    }
}
