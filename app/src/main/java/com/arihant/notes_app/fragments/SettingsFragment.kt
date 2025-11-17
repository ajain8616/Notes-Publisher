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
        val sharedPreferences =
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
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

        showLoading("Preparing your professional report...")

        val pdf = PdfDocument()

        val colors = mapOf(
            "primary" to ContextCompat.getColor(requireContext(), R.color.colorPrimary), // forest green
            "secondary" to ContextCompat.getColor(requireContext(), R.color.colorSecondary), // leaf green
            "accent" to ContextCompat.getColor(requireContext(), R.color.accent_earth_brown), // accent brown
            "background" to ContextCompat.getColor(requireContext(), R.color.colorBackground), // soft beige
            "textPrimary" to ContextCompat.getColor(requireContext(), R.color.dark_charcoal), // dark charcoal
            "textSecondary" to ContextCompat.getColor(requireContext(), R.color.slate_gray), // slate gray
            "textTertiary" to ContextCompat.getColor(requireContext(), R.color.soft_gray), // soft gray
            "white" to ContextCompat.getColor(requireContext(), R.color.colorSurface), // white
            "shadow" to ContextCompat.getColor(requireContext(), R.color.mist_blue), // mist blue for subtle shadows
            "error" to ContextCompat.getColor(requireContext(), R.color.colorError) // red error
        )

        val paints = createPaints(colors)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        var currentY = margin + 100f
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader() {
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paints["headerBackground"]!!)
            canvas.drawText(getString(R.string.app_name), (pageWidth / 2).toFloat(), 50f, paints["title"]!!)
            canvas.drawText("Professional Notes Report", (pageWidth / 2).toFloat(), 70f, paints["subtitle"]!!)
            canvas.drawText("Generated on $currentDate", (pageWidth / 2).toFloat(), 90f, paints["metadata"]!!)
            canvas.drawRect(150f, 100f, 445f, 102f, paints["accent"]!!)
            canvas.drawText("Page $pageNumber", (pageWidth / 2).toFloat(), pageHeight - 30f, paints["metadataCenter"]!!)
        }

        fun addNewPage() {
            pdf.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin + 100f
            drawHeader()
        }

        fun checkPageSpace(requiredHeight: Float) {
            if (currentY + requiredHeight > pageHeight - 50f) {
                addNewPage()
            }
        }

        fun drawCategoryHeader(categoryName: String, noteCount: Int) {
            checkPageSpace(60f)

            canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 45f, 8f, 8f, paints["categoryHeader"]!!)
            canvas.drawCircle(margin + 25f, currentY + 22f, 10f, paints["accent"]!!)
            canvas.drawText("📁", margin + 20f, currentY + 26f, paints["categoryIcon"]!!)
            canvas.drawText(categoryName, margin + 45f, currentY + 25f, paints["categoryTitle"]!!)
            canvas.drawText("$noteCount ${if (noteCount == 1) "note" else "notes"}", margin + 45f, currentY + 40f, paints["categorySubtitle"]!!)
            currentY += 55f
        }

        fun drawNoteCard(noteData: Map<String, Any?>, index: Int, totalNotes: Int) {
            val cardHeight = 120f
            checkPageSpace(cardHeight + 20f)

            val id = noteData["id"] as? Long ?: 0L
            val title = noteData["title"] as? String ?: "Untitled Note"
            val description = noteData["description"] as? String ?: "No description provided"
            val createdAt = noteData["createdAt"] as? Long ?: System.currentTimeMillis()
            val updatedAt = noteData["updatedAt"] as? Long ?: createdAt

            canvas.drawRoundRect(margin + 2, currentY + 2, pageWidth - margin - 2, currentY + cardHeight + 2, 8f, 8f, paints["cardShadow"]!!)
            canvas.drawRoundRect(margin, currentY, pageWidth - margin - 2, currentY + cardHeight, 8f, 8f, paints["cardBackground"]!!)
            canvas.drawRect(margin, currentY, margin + 8f, currentY + cardHeight, paints["accent"]!!)

            var contentY = currentY + 25f

            canvas.drawRoundRect(margin + 15f, contentY - 12f, margin + 45f, contentY + 2f, 4f, 4f, paints["badge"]!!)
            canvas.drawText("ID: $id", margin + 18f, contentY - 2f, paints["badgeText"]!!)
            contentY += 15f

            canvas.drawText(truncateText(title, paints["contentTitle"]!!, pageWidth - 2 * margin - 60f), margin + 15f, contentY, paints["contentTitle"]!!)
            contentY += 20f

            canvas.drawText("Description:", margin + 15f, contentY, paints["contentLabel"]!!)
            contentY += 15f

            val descLines = wrapText(description, paints["content"]!!, pageWidth - 2 * margin - 30f)
            for (line in descLines.take(2)) {
                canvas.drawText(line, margin + 15f, contentY, paints["content"]!!)
                contentY += 14f
            }

            if (descLines.size > 2) {
                canvas.drawText("...", margin + 15f, contentY, paints["content"]!!)
                contentY += 14f
            }

            contentY += 5f
            canvas.drawText("Created: ${dateFormat.format(Date(createdAt))}", margin + 15f, contentY, paints["metadata"]!!)
            canvas.drawText("Updated: ${dateFormat.format(Date(updatedAt))}", pageWidth / 2f, contentY, paints["metadata"]!!)
            currentY += cardHeight + 10f

            if (index < totalNotes - 1) {
                canvas.drawLine(margin + 20f, currentY, pageWidth - margin - 20f, currentY, paints["divider"]!!)
                currentY += 15f
            }
        }

        drawHeader()
        currentY = 140f

        db.collection("Notes_Collections")
            .document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { categorySnapshot ->
                hideLoading()

                if (categorySnapshot.isEmpty) {
                    showMessage("No notes found to generate report")
                    return@addOnSuccessListener
                }

                val categories = categorySnapshot.documents
                val totalCategories = categories.size
                var processedCategories = 0

                checkPageSpace(80f)
                canvas.drawText("REPORT SUMMARY", margin, currentY, paints["sectionTitle"]!!)
                currentY += 25f
                canvas.drawText("Total Categories: $totalCategories", margin, currentY, paints["content"]!!)
                currentY += 15f
                canvas.drawText("Report Date: $currentDate", margin, currentY, paints["content"]!!)
                currentY += 15f
                canvas.drawText("Generated by: ${getString(R.string.app_name)}", margin, currentY, paints["content"]!!)
                currentY += 30f

                for (catDoc in categories) {
                    val categoryName = catDoc.getString("title") ?: "Uncategorized"

                    db.collection("Notes_Collections")
                        .document(userId)
                        .collection("categories")
                        .document(catDoc.id)
                        .collection("categoryNotes")
                        .get()
                        .addOnSuccessListener { notesSnapshot ->
                            val notes = notesSnapshot.documents
                            val noteCount = notes.size

                            drawCategoryHeader(categoryName, noteCount)

                            if (notes.isEmpty()) {
                                checkPageSpace(40f)
                                canvas.drawText("No notes available in this category", margin + 20f, currentY, paints["content"]!!)
                                currentY += 40f
                            } else {
                                for ((index, noteDoc) in notes.withIndex()) {
                                    val noteData = mapOf(
                                        "id" to noteDoc.getLong("id"),
                                        "title" to noteDoc.getString("title"),
                                        "description" to noteDoc.getString("description"),
                                        "createdAt" to noteDoc.getLong("createdAt"),
                                        "updatedAt" to noteDoc.getLong("updatedAt")
                                    )
                                    drawNoteCard(noteData, index, notes.size)
                                }
                            }

                            processedCategories++

                            if (processedCategories == totalCategories) {
                                pdf.finishPage(page)
                                saveAndSharePdf(pdf, fileTimestamp, currentDate)
                            }
                        }
                        .addOnFailureListener { e ->
                            processedCategories++
                            checkPageSpace(40f)
                            canvas.drawText("Error loading notes for: $categoryName", margin, currentY, paints["errorText"]!!)
                            currentY += 40f

                            if (processedCategories == totalCategories) {
                                pdf.finishPage(page)
                                saveAndSharePdf(pdf, fileTimestamp, currentDate)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                showMessage("Failed to load data: ${e.localizedMessage}")
            }
    }

    private fun createPaints(colors: Map<String, Int>): Map<String, Paint> {
        return mapOf(
            "headerBackground" to Paint().apply {
                color = colors["primary"]!!
                style = Paint.Style.FILL
                isAntiAlias = true
            },
            "title" to Paint().apply {
                textSize = 22f
                color = colors["white"]!!
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            },
            "subtitle" to Paint().apply {
                textSize = 14f
                color = colors["white"]!!
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                alpha = 200
                isAntiAlias = true
            },
            "categoryHeader" to Paint().apply {
                color = colors["secondary"]!!
                style = Paint.Style.FILL
                isAntiAlias = true
            },
            "categoryTitle" to Paint().apply {
                textSize = 16f
                color = colors["white"]!!
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                isAntiAlias = true
            },
            "categorySubtitle" to Paint().apply {
                textSize = 12f
                color = colors["white"]!!
                typeface = Typeface.DEFAULT
                alpha = 180
                isAntiAlias = true
            },
            "categoryIcon" to Paint().apply {
                textSize = 10f
                color = colors["white"]!!
                isAntiAlias = true
            },
            "cardBackground" to Paint().apply {
                color = colors["background"]!!
                style = Paint.Style.FILL
                isAntiAlias = true
            },
            "cardShadow" to Paint().apply {
                color = colors["shadow"]!!
                style = Paint.Style.FILL
                isAntiAlias = true
            },
            "contentTitle" to Paint().apply {
                textSize = 16f
                color = colors["textPrimary"]!!
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                isAntiAlias = true
            },
            "contentLabel" to Paint().apply {
                textSize = 12f
                color = colors["textSecondary"]!!
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            },
            "content" to Paint().apply {
                textSize = 12f
                color = colors["textPrimary"]!!
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            },
            "metadata" to Paint().apply {
                textSize = 10f
                color = colors["textTertiary"]!!
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            },
            "metadataCenter" to Paint().apply {
                textSize = 10f
                color = colors["textTertiary"]!!
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            },
            "sectionTitle" to Paint().apply {
                textSize = 18f
                color = colors["primary"]!!
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                isAntiAlias = true
            },
            "accent" to Paint().apply {
                color = colors["accent"]!!
                style = Paint.Style.FILL
                isAntiAlias = true
            },
            "badge" to Paint().apply {
                color = colors["accent"]!!
                style = Paint.Style.FILL
                isAntiAlias = true
            },
            "badgeText" to Paint().apply {
                textSize = 9f
                color = colors["white"]!!
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            },
            "divider" to Paint().apply {
                color = colors["textTertiary"]!!
                strokeWidth = 0.5f
                alpha = 80
                isAntiAlias = true
            },
            "errorText" to Paint().apply {
                textSize = 12f
                color = colors["error"]!!
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
        )
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        return if (paint.measureText(text) <= maxWidth) {
            text
        } else {
            var truncated = text
            while (paint.measureText("$truncated...") > maxWidth && truncated.isNotEmpty()) {
                truncated = truncated.dropLast(1)
            }
            "$truncated..."
        }
    }

    private fun saveAndSharePdf(pdf: PdfDocument, timestamp: String, currentDate: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "Professional_Notes_Report_$timestamp.pdf"
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdf.writeTo(outputStream)
            }
            pdf.close()

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            showMessage("Professional report generated successfully!")

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Professional Notes Report - $currentDate")
                putExtra(Intent.EXTRA_TEXT, "Attached is my professional notes report generated on $currentDate.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Professional Report"))

        } catch (e: Exception) {
            showMessage("Failed to save PDF: ${e.localizedMessage}")
        }
    }

    private fun showLoading(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideLoading() {
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

}
