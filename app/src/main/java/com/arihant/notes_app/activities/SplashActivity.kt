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
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {

    private lateinit var appLogo: ImageView
    private lateinit var appName: TextView
    private lateinit var appTagline: TextView
    private lateinit var loadingProgress: LinearProgressIndicator
    private lateinit var versionInfo: TextView

    private val splashDelay: Long = 3500

    // ❗ Token variable
    private var userToken: String? = null

    // ❗ SharedPreferences
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        appLogo = findViewById(R.id.logo)
        appName = findViewById(R.id.app_name)
        appTagline = findViewById(R.id.app_tagline)
        loadingProgress = findViewById(R.id.loading_bar)
        versionInfo = findViewById(R.id.version_name)

        val rootView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SharedPreferences
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userToken = prefs.getString("user_token", null)
        FirebaseApp.initializeApp(this)

        initializeViews()
        startSplashAnimation()
    }

    private fun initializeViews() {
        appLogo.visibility = View.VISIBLE
        appName.visibility = View.INVISIBLE
        appTagline.visibility = View.INVISIBLE
        loadingProgress.visibility = View.INVISIBLE
        versionInfo.visibility = View.INVISIBLE

        appName.alpha = 0f
        appTagline.alpha = 0f
        loadingProgress.alpha = 0f
        versionInfo.alpha = 0f
    }

    private fun startSplashAnimation() {
        val flipInAnimator = android.animation.AnimatorInflater.loadAnimator(
            this,
            R.animator.card_flip_in
        )
        flipInAnimator.setTarget(appLogo)
        flipInAnimator.start()

        flipInAnimator.doOnEnd {
            showAppName()
        }
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

        simulateLoadingProgress()
    }

    private fun simulateLoadingProgress() {
        loadingProgress.setProgressCompat(0, false)

        loadingProgress.animate()
            .setDuration(splashDelay - 2000)
            .withEndAction { loadingProgress.setProgressCompat(100, true) }
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            decideNextActivity()
        }, splashDelay)
    }

    // ❗ Navigate to LoginActivity if token is null
    private fun decideNextActivity() {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val isRegistered = prefs.getBoolean("is_registered", false)
        userToken = prefs.getString("user_token", null)

        val nextIntent = if (userToken.isNullOrEmpty()) {
            Intent(this, LoginActivity::class.java)
        } else {
            when {
                isLoggedIn -> Intent(this, MainActivity::class.java)
                isRegistered -> Intent(this, LoginActivity::class.java)
                else -> Intent(this, SignupActivity::class.java)
            }
        }

        nextIntent.putExtra("user_token", userToken)
        startActivity(nextIntent)
        overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
        finish()
    }

    override fun onPause() {
        super.onPause()
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }
}
