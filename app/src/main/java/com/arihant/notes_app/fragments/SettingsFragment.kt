package com.arihant.notes_app.fragments

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView

    private var isOnline: Boolean = true
    private var currentTheme: String = "default"
    private var token: String? = null
    private var uid: String? = null

    private val REQUEST_STORAGE_PERMISSION = 100

    private lateinit var getAuthController: GetAuthController
    private lateinit var authController: AuthController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        getAuthController = GetAuthController(requireContext())
        authController = AuthController(requireContext())

        txtUserName = view.findViewById(R.id.textUserName)
        txtUserEmail = view.findViewById(R.id.textUserEmail)

        val prefs = requireActivity().getSharedPreferences("user_prefs", 0)
        token = prefs.getString("user_token", null)
        uid = prefs.getString("user_uid", null)

        if (token == null) {
            redirectToLogin()
            return view
        }

        loadUserProfile(token!!)
        if (uid != null) startTokenListener(uid!!, token!!)

        view.findViewById<RelativeLayout>(R.id.layoutUserDetails).setOnClickListener {
            showProfileUpdateDialog()
        }
        view.findViewById<RelativeLayout>(R.id.layoutOnlineOffline).setOnClickListener {
            showOnlineOfflineDialog()
        }
        view.findViewById<RelativeLayout>(R.id.layoutDownloadData).setOnClickListener {
            requestDownloadPermission()
        }
        view.findViewById<RelativeLayout>(R.id.layoutAppPreferences).setOnClickListener {
            showThemeSelectDialog()
        }
        view.findViewById<RelativeLayout>(R.id.layoutLogout).setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return view
    }

    // -----------------------------------------------------
    // PERMISSION HANDLER
    // -----------------------------------------------------

    private fun requestDownloadPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            showDownloadDataConfirmationDialog()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_STORAGE_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            showDownloadDataConfirmationDialog()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // -----------------------------------------------------
    // AUTH
    // -----------------------------------------------------

    private fun loadUserProfile(token: String) {
        getAuthController.getUserProfileByToken(token) { success, user, fetchedUid ->
            if (success && user != null) {

                txtUserName.text = user.name
                txtUserEmail.text = user.email
                uid = fetchedUid
                isOnline = user.isOnline

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

    // -----------------------------------------------------
    // FULL SCREEN DIALOGS (LIKE SEARCH DIALOG)
    // -----------------------------------------------------

    private fun getFullScreenDialog(): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(R.layout.dialog_notes_events_handler)
            setCancelable(true)
        }
    }

    // -------------------------
    // PROFILE UPDATE DIALOG
    // -------------------------

    private fun showProfileUpdateDialog() {
        val dialog = getFullScreenDialog()

        val mainCard = dialog.findViewById<View>(R.id.dialog_card)
        val card = dialog.findViewById<View>(R.id.profile_update_card)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        mainCard.visibility = View.GONE
        card.visibility = View.VISIBLE

        val edtName = dialog.findViewById<TextInputEditText>(R.id.edtProfileName)
        val edtEmail = dialog.findViewById<TextInputEditText>(R.id.edtProfileEmail)
        val btnUpdate = dialog.findViewById<MaterialButton>(R.id.btnProfileUpdate)

        edtName.setText(txtUserName.text)
        edtEmail.setText(txtUserEmail.text)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnUpdate.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "All fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            getAuthController.updateUserProfileByToken(token!!, name, email) { success, message ->
                if (success) {
                    txtUserName.text = name
                    txtUserEmail.text = email
                    Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // -------------------------
    // ONLINE/OFFLINE DIALOG
    // -------------------------

    private fun showOnlineOfflineDialog() {
        val dialog = getFullScreenDialog()

        dialog.findViewById<View>(R.id.dialog_card)?.visibility = View.GONE
        dialog.findViewById<View>(R.id.mode_card)?.visibility = View.VISIBLE

        val switchMode = dialog.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchMode)
        val txtMode = dialog.findViewById<TextView>(R.id.txtMode)
        val btnApply = dialog.findViewById<MaterialButton>(R.id.btnApplyMode)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        // Set switch checked state: on for online, off for offline
        switchMode.isChecked = isOnline
        txtMode.text = if (switchMode.isChecked) "Online Mode" else "Offline Mode"

        val thumbColors = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                requireContext().getColor(R.color.accent_leaf_green),
                requireContext().getColor(R.color.gray_400)
            )
        )
        val trackColors = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                requireContext().getColor(R.color.mist_blue),
                requireContext().getColor(R.color.gray_200)
            )
        )
        switchMode.thumbTintList = thumbColors
        switchMode.trackTintList = trackColors

        switchMode.setOnCheckedChangeListener { _, isChecked ->
            txtMode.text = if (isChecked) "Online Mode" else "Offline Mode"
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnApply.setOnClickListener {
            val newStatus = switchMode.isChecked

            getAuthController.updateUserProfileByToken(token!!, isOnline = newStatus) { success, message ->
                if (success) {
                    isOnline = newStatus
                    Toast.makeText(requireContext(), "Status Updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // -------------------------
    // DOWNLOAD CONFIRMATION DIALOG
    // -------------------------

    private fun showDownloadDataConfirmationDialog() {
        val dialog = getFullScreenDialog()

        dialog.findViewById<View>(R.id.dialog_card).visibility = View.GONE

        val permissionCard = dialog.findViewById<View>(R.id.permission_card)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnPermissionCancel)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btnPermissionConfirm)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        permissionCard.visibility = View.VISIBLE

        dialog.findViewById<TextView>(R.id.permissionTitle).text = "Confirm Download"
        dialog.findViewById<TextView>(R.id.permissionDesc).text = "Download all your notes as PDF?"

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            downloadUserData()
        }

        dialog.show()
    }

    // -------------------------
    // LOGOUT DIALOG
    // -------------------------

    private fun showLogoutConfirmationDialog() {
        val dialog = getFullScreenDialog()

        dialog.findViewById<View>(R.id.dialog_card).visibility = View.GONE

        val permissionCard = dialog.findViewById<View>(R.id.permission_card)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnPermissionCancel)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btnPermissionConfirm)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        permissionCard.visibility = View.VISIBLE

        dialog.findViewById<TextView>(R.id.permissionTitle).text = "Confirm Logout"
        dialog.findViewById<TextView>(R.id.permissionDesc).text = "Do you want to logout?"

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            logoutUser()
        }

        dialog.show()
    }

    // -------------------------
    // THEME SELECT DIALOG
    // -------------------------

    private fun showThemeSelectDialog() {
        val dialog = getFullScreenDialog()

        dialog.findViewById<View>(R.id.dialog_card).visibility = View.GONE

        val themeCard = dialog.findViewById<View>(R.id.theme_select_card)
        val chkDefault = dialog.findViewById<CheckBox>(R.id.chkDefaultTheme)
        val chkDark = dialog.findViewById<CheckBox>(R.id.chkDarkTheme)
        val chkLight = dialog.findViewById<CheckBox>(R.id.chkLightTheme)
        val btnApply = dialog.findViewById<MaterialButton>(R.id.btnApplyTheme)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        themeCard.visibility = View.VISIBLE

        // Load the current theme from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentTheme = sharedPreferences.getString("selected_theme", "default") ?: "default"

        // Set the checkboxes based on the loaded currentTheme
        when (currentTheme) {
            "default" -> chkDefault.isChecked = true
            "dark" -> chkDark.isChecked = true
            "light" -> chkLight.isChecked = true
        }

        chkDefault.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                chkDark.isChecked = false
                chkLight.isChecked = false
            }
        }
        chkDark.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                chkDefault.isChecked = false
                chkLight.isChecked = false
            }
        }
        chkLight.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                chkDefault.isChecked = false
                chkDark.isChecked = false
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnApply.setOnClickListener {
            currentTheme =
                when {
                    chkDefault.isChecked -> "default"
                    chkDark.isChecked -> "dark"
                    chkLight.isChecked -> "light"
                    else -> "default"
                }

            // Save the selected theme to SharedPreferences
            with(sharedPreferences.edit()) {
                putString("selected_theme", currentTheme)
                apply()
            }

            requireActivity().recreate()
            Toast.makeText(requireContext(), "Theme Applied", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // -----------------------------------------------------
    // DOWNLOAD PDF
    // -----------------------------------------------------

    private fun downloadUserData() {
        val db = FirebaseFirestore.getInstance()
        val userId = uid ?: return

        val pdf = PdfDocument()

        // ---- Load Theme Colors ----
        val colorBg = ContextCompat.getColor(requireContext(), R.color.colorBackground)
        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val colorAccent = ContextCompat.getColor(requireContext(), R.color.accent_leaf_green)
        val colorDivider = ContextCompat.getColor(requireContext(), R.color.divider_gray)
        val textMain = ContextCompat.getColor(requireContext(), R.color.dark_charcoal)
        val textSub = ContextCompat.getColor(requireContext(), R.color.gray_700)
        val white = ContextCompat.getColor(requireContext(), R.color.white)

        // ---- Title Text ----
        val titlePaint = Paint().apply {
            textSize = 24f
            color = white
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // ---- Header Box Paint ----
        val titleBarPaint = Paint().apply {
            color = colorPrimary
            style = Paint.Style.FILL
        }

        // ---- Category Header Label ----
        val categoryPaint = Paint().apply {
            color = colorAccent
            style = Paint.Style.FILL
        }

        // ---- Normal Text ----
        val contentPaint = Paint().apply {
            textSize = 14f
            color = textMain
        }

        // ---- Sub Content / Description ----
        val descPaint = Paint().apply {
            textSize = 13f
            color = textSub
        }

        // ---- Divider Paint ----
        val dividerPaint = Paint().apply {
            color = colorDivider
            strokeWidth = 1.5f
        }

        // Date Formatter
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas
        var y = 40f

        fun addPage() {
            pdf.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = 40f

            // Repeat title bar on every page
            canvas.drawRect(0f, 0f, pageInfo.pageWidth.toFloat(), 60f, titleBarPaint)
            canvas.drawText("User Notes Report", (pageInfo.pageWidth / 2).toFloat(), 38f, titlePaint)
            y = 90f
        }

        // ---- Draw Title Bar ----
        canvas.drawRect(0f, 0f, pageInfo.pageWidth.toFloat(), 60f, titleBarPaint)
        canvas.drawText("User Notes Report", (pageInfo.pageWidth / 2).toFloat(), 38f, titlePaint)
        y += 60

        // Background fill (optional for aesthetic)
        canvas.drawColor(colorBg)

        db.collection("Notes_Collections")
            .document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { categorySnapshot ->

                if (categorySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No notes found!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val categories = categorySnapshot.documents
                var pending = categories.size

                for (catDoc in categories) {

                    val categoryName = catDoc.getString("title") ?: "Untitled Category"

                    if (y > 720) addPage()

                    // ---- Category Header Box ----
                    canvas.drawRect(30f, y, 565f, y + 40f, categoryPaint)
                    canvas.drawText("📂  $categoryName", 40f, y + 27f, contentPaint.apply { color = textMain })

                    y += 50

                    db.collection("Notes_Collections")
                        .document(userId)
                        .collection("categories")
                        .document(catDoc.id)
                        .collection("categoryNotes")
                        .get()
                        .addOnSuccessListener { notesSnapshot ->

                            if (notesSnapshot.isEmpty) {
                                canvas.drawText("No notes available", 40f, y, descPaint)
                                y += 25
                            }

                            for (noteDoc in notesSnapshot) {

                                val id = noteDoc.getLong("id") ?: 0
                                val title = noteDoc.getString("title") ?: "No Title"
                                val desc = noteDoc.getString("description") ?: "No Description"
                                val created = noteDoc.getLong("createdAt") ?: 0L
                                val updated = noteDoc.getLong("updatedAt") ?: 0L

                                if (y > 720) addPage()

                                canvas.drawText("• ID: $id", 40f, y, contentPaint)
                                y += 20

                                canvas.drawText("Title: $title", 40f, y, contentPaint)
                                y += 20

                                canvas.drawText("Description:", 40f, y, contentPaint)
                                y += 18

                                // wrap description
                                desc.chunked(55).forEach {
                                    canvas.drawText("   → $it", 55f, y, descPaint)
                                    y += 18
                                }

                                canvas.drawText("Created: ${dateFormat.format(Date(created))}", 40f, y, descPaint)
                                y += 18

                                canvas.drawText("Updated: ${dateFormat.format(Date(updated))}", 40f, y, descPaint)
                                y += 25

                                // divider
                                canvas.drawLine(30f, y, 565f, y, dividerPaint)
                                y += 15
                            }

                            pending--

                            if (pending == 0) {
                                pdf.finishPage(page)

                                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val file = File(downloads, "UserNotes_${System.currentTimeMillis()}.pdf")
                                FileOutputStream(file).use { pdf.writeTo(it) }
                                pdf.close()

                                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

                                Toast.makeText(requireContext(), "PDF Saved Successfully!", Toast.LENGTH_LONG).show()

                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                startActivity(Intent.createChooser(intent, "Share PDF"))
                            }
                        }
                }

            }
    }

}
