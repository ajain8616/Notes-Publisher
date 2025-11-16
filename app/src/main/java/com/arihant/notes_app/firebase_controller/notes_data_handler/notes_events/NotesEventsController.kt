package com.arihant.notes_app.firebase_controller.notes_data_handler.notes_events

import android.util.Log
import com.arihant.notes_app.model.NotesTypeModel
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Author: Arihant Jain
 * Date: 16-11-2025
 * Time: 22:40
 * Year: 2025
 * Month: November (Nov)
 * Day: 16 (Sunday)
 * Hour: 22
 * Minute: 40
 * Project: notes_app
 * Package: com.arihant.notes_app.firebase_controller.notes_events
 */

class NotesEventsController {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "NotesEventsController"

    // Method to add a category to Firestore
    fun addCategory(uid: String, category: NotesTypeModel, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val categoryId = UUID.randomUUID().toString()
        val categoryData = hashMapOf(
            "title" to category.title,
            "filesCount" to category.filesCount,
            "icon" to category.icon
        )

        Log.d(TAG, "Adding category for UID: $uid, Category: ${category.title}, ID: $categoryId")

        db.collection("notes").document(uid).collection("categories").document(categoryId)
            .set(categoryData)
            .addOnSuccessListener {
                Log.d(TAG, "Category added successfully: ${category.title}")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Failed to add category: ${exception.message}")
                onFailure(exception)
            }
    }

    // Method to get all categories from Firestore
    fun getCategories(uid: String, onSuccess: (List<NotesTypeModel>) -> Unit, onFailure: (Exception) -> Unit) {
        Log.d(TAG, "Fetching categories for UID: $uid")

        db.collection("notes").document(uid).collection("categories")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val categories = querySnapshot.documents.mapNotNull { document ->
                    val title = document.getString("title") ?: return@mapNotNull null
                    val filesCount = document.getLong("filesCount")?.toInt() ?: 0
                    val icon = document.getString("icon") ?: "ic_important"
                    NotesTypeModel(title, filesCount, icon)
                }
                Log.d(TAG, "Fetched ${categories.size} categories successfully")
                onSuccess(categories)
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Failed to fetch categories: ${exception.message}")
                onFailure(exception)
            }
    }
}