package com.arihant.notes_app.utils

/**
 * Author: Arihant Jain
 * Date: 18-11-2025
 * Time: 02:36
 * Year: 2025
 * Month: November (Nov)
 * Day: 18 (Tuesday)
 * Hour: 02
 * Minute: 36
 * Project: notes_app
 * Package: com.arihant.notes_app.utils
 */

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import com.arihant.notes_app.R
import com.arihant.notes_app.firebase_controller.auth.GetAuthController

class NetworkChecker(
    private val activity: Activity,
    private val authController: GetAuthController, // Firebase auth controller
    private val userToken: String?                 // user token from SharedPreferences
) {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 3000 // check every 3 seconds

    private val networkCheckRunnable = object : Runnable {
        override fun run() {
            if (isOnline()) {
                Log.d("NetworkChecker", "Device is online")
                // Update Firebase with online status
                userToken?.let { token ->
                    authController.updateUserProfileByToken(token, isOnline = true) { success, message ->
                        Log.d("NetworkChecker", "Firebase update online: $success, message: $message")
                    }
                }
            } else {
                Log.d("NetworkChecker", "Device is offline")
                // Show no internet dialog
                showNoInternetDialog()
                // Update Firebase with offline status
                userToken?.let { token ->
                    authController.updateUserProfileByToken(token, isOnline = false) { success, message ->
                        Log.d("NetworkChecker", "Firebase update offline: $success, message: $message")
                    }
                }
            }
            handler.postDelayed(this, checkInterval)
        }
    }

    fun startChecking() {
        Log.d("NetworkChecker", "Starting network check")
        handler.post(networkCheckRunnable)
    }

    fun stopChecking() {
        Log.d("NetworkChecker", "Stopping network check")
        handler.removeCallbacks(networkCheckRunnable)
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        val online = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        Log.d("NetworkChecker", "Network capabilities checked: online = $online")
        return online
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showNoInternetDialog() {
        Log.d("NetworkChecker", "Showing no internet dialog")
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_notes_events_handler)
        dialog.setCancelable(true)

        val btnCloseDialog: ImageView = dialog.findViewById(R.id.btnCloseDialog)
        val btnRetry: View = dialog.findViewById(R.id.retry_button)
        val noInternetCard: CardView = dialog.findViewById(R.id.no_internet_card)

        // Show only the no-internet card
        noInternetCard.visibility = View.VISIBLE
        dialog.findViewById<View>(R.id.dialog_card).visibility = View.GONE
        dialog.findViewById<View>(R.id.search_card).visibility = View.GONE
        dialog.findViewById<View>(R.id.permission_card).visibility = View.GONE
        dialog.findViewById<View>(R.id.profile_update_card).visibility = View.GONE
        dialog.findViewById<View>(R.id.theme_select_card).visibility = View.GONE

        btnCloseDialog.setOnClickListener {
            Log.d("NetworkChecker", "No internet dialog dismissed")
            dialog.dismiss()
        }
        btnRetry.setOnClickListener {
            Log.d("NetworkChecker", "Retry clicked → Opening settings")
            openNetworkSettings()
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun openNetworkSettings() {
        try {
            activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } catch (e: Exception) {
            activity.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

}
