package com.arihant.notes_app.firebase_controller.notes_data_handler.notes_events


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

import android.util.Log
import com.arihant.notes_app.model.NotesModel
import com.arihant.notes_app.model.NotesTypeModel
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class NotesEventsController {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "NotesEventsController"

    // ================================
    // ADD CATEGORY
    // ================================
    fun addCategory(
        uid: String,
        category: NotesTypeModel,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val categoryId = UUID.randomUUID().toString()
        val categoryData = hashMapOf(
            "title" to category.title,
            "filesCount" to category.filesCount,
            "icon" to category.icon
        )

        db.collection("Notes_Collections").document(uid)
            .collection("categories").document(categoryId)
            .set(categoryData)
            .addOnSuccessListener { onSuccess(categoryId) }
            .addOnFailureListener { onFailure(it) }
    }



    // ================================
    // GET CATEGORIES
    // ================================
    fun getCategories(
        uid: String,
        onSuccess: (List<NotesTypeModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("Notes_Collections").document(uid)
            .collection("categories")
            .get()
            .addOnSuccessListener { snapshot ->
                val categories = snapshot.documents.mapNotNull { doc ->
                    NotesTypeModel(
                        id = doc.id,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        filesCount = doc.getLong("filesCount")?.toInt() ?: 0,
                        icon = doc.getString("icon") ?: "ic_important"
                    )
                }
                onSuccess(categories)
            }
            .addOnFailureListener { onFailure(it) }
    }


    // ==========================================================
    // 1️⃣ ADD NOTE BASED ON CATEGORY
    // ==========================================================
    fun addNoteBasedOnCategory(
        uid: String,
        categoryId: String,
        note: NotesModel,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteId = note.id.toString()

        val noteData = hashMapOf(
            "id" to note.id,
            "title" to note.title,
            "description" to note.description,
            "category" to note.category,
            "createdAt" to note.createdAt,
            "updatedAt" to note.updatedAt
        )

        db.collection("Notes_Collections").document(uid)
            .collection("categories").document(categoryId)
            .collection("categoryNotes").document(noteId)
            .set(noteData)
            .addOnSuccessListener {
                Log.d(TAG, "Note added to $categoryId: $noteId")
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ==========================================================
    // 2️⃣ GET NOTES BASED ON CATEGORY
    // ==========================================================
    fun getNotesBasedOnCategory(
        uid: String,
        categoryId: String,
        onSuccess: (List<NotesModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("Notes_Collections").document(uid)
            .collection("categories").document(categoryId)
            .collection("categoryNotes")
            .get()
            .addOnSuccessListener { snapshot ->
                val notes = snapshot.documents.mapNotNull { doc ->
                    NotesModel(
                        id = doc.getLong("id")?.toInt() ?: 0,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                }
                onSuccess(notes)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ==========================================================
    // 3️⃣ EDIT NOTE BASED ON CATEGORY
    // ==========================================================
    fun editNoteBasedOnCategory(
        uid: String,
        categoryId: String,
        note: NotesModel,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteId = note.id.toString()

        val updatedData = mapOf(
            "title" to note.title,
            "description" to note.description,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("Notes_Collections").document(uid)
            .collection("categories").document(categoryId)
            .collection("categoryNotes").document(noteId)
            .update(updatedData)
            .addOnSuccessListener {
                Log.d(TAG, "Note updated: $noteId")
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ==========================================================
    // 4️⃣ DELETE NOTE BASED ON CATEGORY
    // ==========================================================
    fun deleteNoteBasedOnCategory(
        uid: String,
        categoryId: String,
        noteId: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("Notes_Collections").document(uid)
            .collection("categories").document(categoryId)
            .collection("categoryNotes").document(noteId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Note deleted: $noteId")
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }
}
