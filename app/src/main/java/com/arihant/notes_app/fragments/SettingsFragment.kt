package com.arihant.notes_app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.arihant.notes_app.R
import com.arihant.notes_app.activities.LoginActivity
import com.arihant.notes_app.firebase_controller.auth.AuthController
import com.arihant.notes_app.firebase_controller.auth.GetAuthController

class SettingsFragment : Fragment() {

    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView

    // New layout sections
    private lateinit var layoutUserDetails: RelativeLayout
    private lateinit var layoutOnlineOffline: RelativeLayout
    private lateinit var layoutDownloadData: RelativeLayout
    private lateinit var layoutAppPreferences: RelativeLayout
    private lateinit var layoutLogout: RelativeLayout

    private lateinit var getAuthController: GetAuthController
    private lateinit var authController: AuthController

    private var token: String? = null
    private var uid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        getAuthController = GetAuthController(requireContext())
        authController = AuthController(requireContext())

        // TEXT FIELDS
        txtUserName = view.findViewById(R.id.textUserName)
        txtUserEmail = view.findViewById(R.id.textUserEmail)

        // NEW LAYOUT SECTIONS
        layoutUserDetails = view.findViewById(R.id.layoutUserDetails)
        layoutOnlineOffline = view.findViewById(R.id.layoutOnlineOffline)
        layoutDownloadData = view.findViewById(R.id.layoutDownloadData)
        layoutAppPreferences = view.findViewById(R.id.layoutAppPreferences)
        layoutLogout = view.findViewById(R.id.layoutLogout)

        val prefs = requireActivity().getSharedPreferences("user_prefs", 0)
        token = prefs.getString("user_token", null)
        uid = prefs.getString("user_uid", null)

        if (token == null) {
            redirectToLogin()
            return view
        }

        loadUserProfile(token!!)
        if (uid != null) startTokenListener(uid!!, token!!)


        layoutUserDetails.setOnClickListener {
            Toast.makeText(requireContext(), "User details clicked", Toast.LENGTH_SHORT).show()
        }

        layoutOnlineOffline.setOnClickListener {
            Toast.makeText(requireContext(), "Switched Online/Offline", Toast.LENGTH_SHORT).show()
        }

        layoutDownloadData.setOnClickListener {
            Toast.makeText(requireContext(), "Download started...", Toast.LENGTH_SHORT).show()
        }

        layoutAppPreferences.setOnClickListener {
            Toast.makeText(requireContext(), "App Preferences clicked", Toast.LENGTH_SHORT).show()
        }

        layoutLogout.setOnClickListener {
            logoutUser()
        }

        return view
    }

    private fun loadUserProfile(token: String) {
        getAuthController.getUserProfileByToken(token) { success, user, fetchedUid ->
            if (success && user != null && fetchedUid != null) {

                txtUserName.text = user.name
                txtUserEmail.text = user.email
                uid = fetchedUid
                startTokenListener(fetchedUid, token)

            } else {
                Toast.makeText(requireContext(), "User not found!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startTokenListener(uid: String, token: String) {
        authController.observeTokenChanges(uid, token) { reason ->
            Toast.makeText(requireContext(), reason, Toast.LENGTH_LONG).show()
            redirectToLogin()
        }
    }

    private fun logoutUser() {
        val tokenValue = token ?: return

        authController.logoutUser(tokenValue) { success, message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            if (success) {
                val prefs = requireActivity().getSharedPreferences("user_prefs", 0)
                prefs.edit().clear().apply()
                redirectToLogin()
            }
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }
}
