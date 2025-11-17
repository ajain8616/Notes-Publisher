package com.arihant.notes_app.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.arihant.notes_app.R
import com.arihant.notes_app.activities.LoginActivity
import com.arihant.notes_app.firebase_controller.auth.AuthController
import com.arihant.notes_app.firebase_controller.auth.GetAuthController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class SettingsFragment : Fragment() {

    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView
    private var isOnline: Boolean = true
    private var currentTheme: String = "default"
    private lateinit var layoutUserDetails: RelativeLayout
    private lateinit var layoutOnlineOffline: RelativeLayout
    private lateinit var layoutDownloadData: RelativeLayout
    private lateinit var layoutAppPreferences: RelativeLayout
    private lateinit var layoutLogout: RelativeLayout

    private lateinit var getAuthController: GetAuthController
    private lateinit var authController: AuthController

    private var token: String? = null
    private var uid: String? = null

    private val REQUEST_STORAGE_PERMISSION = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        getAuthController = GetAuthController(requireContext())
        authController = AuthController(requireContext())

        txtUserName = view.findViewById(R.id.textUserName)
        txtUserEmail = view.findViewById(R.id.textUserEmail)

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
            showProfileUpdateDialog()
        }

        layoutOnlineOffline.setOnClickListener {
            showOnlineOfflineDialog()
        }

        layoutDownloadData.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
            } else {
                showDownloadDataConfirmationDialog()
            }
        }

        layoutAppPreferences.setOnClickListener {
            showThemeSelectDialog()
        }

        layoutLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return view
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showDownloadDataConfirmationDialog()
            } else {
                Toast.makeText(requireContext(), "Storage permission required to download data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfile(token: String) {
        getAuthController.getUserProfileByToken(token) { success, user, fetchedUid ->
            if (success && user != null && fetchedUid != null) {
                txtUserName.text = user.name
                txtUserEmail.text = user.email
                isOnline = user.isOnline
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

    private fun showProfileUpdateDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_events_handler, null)
        val profileCard = dialogView.findViewById<View>(R.id.profile_update_card)
        profileCard.visibility = View.VISIBLE

        val edtProfileName = dialogView.findViewById<TextInputEditText>(R.id.edtProfileName)
        val edtProfileEmail = dialogView.findViewById<TextInputEditText>(R.id.edtProfileEmail)
        val btnProfileUpdate = dialogView.findViewById<MaterialButton>(R.id.btnProfileUpdate)
        val btnCloseDialog = dialogView.findViewById<View>(R.id.btnCloseDialog)

        edtProfileName.setText(txtUserName.text)
        edtProfileEmail.setText(txtUserEmail.text)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnProfileUpdate.setOnClickListener {
            val newName = edtProfileName.text.toString().trim()
            val newEmail = edtProfileEmail.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Name and Email cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getAuthController.updateUserProfileByToken(token!!, newName, newEmail) { success, message ->
                if (success) {
                    txtUserName.text = newName
                    txtUserEmail.text = newEmail
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), message ?: "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showOnlineOfflineDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_events_handler, null)

        val modeCard = dialogView.findViewById<View>(R.id.mode_card)
        modeCard.visibility = View.VISIBLE
        val switchMode = dialogView.findViewById<Switch>(R.id.switchMode)
        val btnApplyMode = dialogView.findViewById<MaterialButton>(R.id.btnApplyMode)
        val btnCloseDialog = dialogView.findViewById<View>(R.id.btnCloseDialog)
        switchMode.isChecked = !isOnline

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }
        btnApplyMode.setOnClickListener {
            val newIsOnline = !switchMode.isChecked

            getAuthController.updateUserProfileByToken(token!!, isOnline = newIsOnline) { success, message ->
                if (success) {
                    isOnline = newIsOnline
                    Toast.makeText(requireContext(), "Mode updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), message ?: "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showDownloadDataConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_events_handler, null)
        val permissionCard = dialogView.findViewById<View>(R.id.permission_card)
        permissionCard.visibility = View.VISIBLE
        val permissionTitle = dialogView.findViewById<TextView>(R.id.permissionTitle)
        val permissionDesc = dialogView.findViewById<TextView>(R.id.permissionDesc)
        val btnPermissionCancel = dialogView.findViewById<MaterialButton>(R.id.btnPermissionCancel)
        val btnPermissionConfirm = dialogView.findViewById<MaterialButton>(R.id.btnPermissionConfirm)
        val btnCloseDialog = dialogView.findViewById<View>(R.id.btnCloseDialog)

        permissionTitle.text = "Confirm Download"
        permissionDesc.text = "Are you sure you want to download all your notes data as PDF?"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnPermissionCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnPermissionConfirm.setOnClickListener {
            dialog.dismiss()
            downloadUserData()
        }

        dialog.show()
    }

    private fun downloadUserData() {
        val db = FirebaseFirestore.getInstance()
        val userId = uid ?: return

        db.collection("Notes_Collections")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.color = Color.BLACK
                paint.textSize = 12f

                var yPosition = 50f
                canvas.drawText("User Notes Data", 50f, yPosition, paint)
                yPosition += 30f

                for (document in querySnapshot.documents) {
                    val data = document.data
                    if (data != null) {
                        canvas.drawText("Note ID: ${document.id}", 50f, yPosition, paint)
                        yPosition += 20f
                        for ((key, value) in data) {
                            canvas.drawText("$key: $value", 50f, yPosition, paint)
                            yPosition += 20f
                        }
                        yPosition += 20f
                    }
                }

                pdfDocument.finishPage(page)

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "notes_data_${System.currentTimeMillis()}.pdf"
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()

                Toast.makeText(requireContext(), "PDF saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "application/pdf"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(intent, "Share Notes PDF"))
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_events_handler, null)
        val permissionCard = dialogView.findViewById<View>(R.id.permission_card)
        permissionCard.visibility = View.VISIBLE
        val permissionTitle = dialogView.findViewById<TextView>(R.id.permissionTitle)
        val permissionDesc = dialogView.findViewById<TextView>(R.id.permissionDesc)
        val btnPermissionCancel = dialogView.findViewById<MaterialButton>(R.id.btnPermissionCancel)
        val btnPermissionConfirm = dialogView.findViewById<MaterialButton>(R.id.btnPermissionConfirm)
        val btnCloseDialog = dialogView.findViewById<View>(R.id.btnCloseDialog)

        permissionTitle.text = "Confirm Logout"
        permissionDesc.text = "Are you sure you want to log out? This will end your current session."

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnPermissionCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnPermissionConfirm.setOnClickListener {
            dialog.dismiss()
            logoutUser()
        }

        dialog.show()
    }

    private fun showThemeSelectDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_events_handler, null)

        val themeCard = dialogView.findViewById<View>(R.id.theme_select_card)
        themeCard.visibility = View.VISIBLE

        val chkDefaultTheme = dialogView.findViewById<CheckBox>(R.id.chkDefaultTheme)
        val chkDarkTheme = dialogView.findViewById<CheckBox>(R.id.chkDarkTheme)
        val chkLightTheme = dialogView.findViewById<CheckBox>(R.id.chkLightTheme)
        val btnApplyTheme = dialogView.findViewById<MaterialButton>(R.id.btnApplyTheme)
        val btnCloseDialog = dialogView.findViewById<View>(R.id.btnCloseDialog)

        when (currentTheme) {
            "default" -> chkDefaultTheme.isChecked = true
            "dark" -> chkDarkTheme.isChecked = true
            "light" -> chkLightTheme.isChecked = true
        }

        chkDefaultTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chkDarkTheme.isChecked = false
                chkLightTheme.isChecked = false
            }
        }
        chkDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chkDefaultTheme.isChecked = false
                chkLightTheme.isChecked = false
            }
        }
        chkLightTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chkDefaultTheme.isChecked = false
                chkDarkTheme.isChecked = false
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnApplyTheme.setOnClickListener {
            val selectedTheme = when {
                chkDefaultTheme.isChecked -> "default"
                chkDarkTheme.isChecked -> "dark"
                chkLightTheme.isChecked -> "light"
                else -> "default"
            }
            applyTheme(selectedTheme)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyTheme(theme: String) {
        currentTheme = theme

        requireActivity().recreate()

        Toast.makeText(requireContext(), "Theme applied: $theme", Toast.LENGTH_SHORT).show()
    }
}
