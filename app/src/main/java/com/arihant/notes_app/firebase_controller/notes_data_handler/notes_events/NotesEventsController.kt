package com.arihant.notes_app.firebase_controller.notes_data_handler.notes_events

import android.util.Log
import com.arihant.notes_app.model.NotesModel
import com.arihant.notes_app.model.NotesTypeModel
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class NotesEventsController {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "NotesEventsController"

    // ================================
    // ADD CATEGORY (No Duplicates)
    // ================================
    fun addCategory(
        uid: String,
        category: NotesTypeModel,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Check for duplicate category by title first
        db.collection("Notes_Collections").document(uid)
            .collection("categories")
            .whereEqualTo("title", category.title)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val categoryId = UUID.randomUUID().toString()
                    val categoryData = hashMapOf(
                        "title" to category.title,
                        "filesCount" to 0,
                        "icon" to category.icon,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )

                    db.collection("Notes_Collections").document(uid)
                        .collection("categories").document(categoryId)
                        .set(categoryData)
                        .addOnSuccessListener { onSuccess(categoryId) }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(Exception("Category with this title already exists"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ================================
    // GET CATEGORIES (Sorted A-Z)
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
                        filesCount = 0, // will update after count
                        icon = doc.getString("icon") ?: "ic_important"
                    )
                }

                if (categories.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                var pending = categories.size

                categories.forEach { model ->
                    db.collection("Notes_Collections")
                        .document(uid)
                        .collection("categories")
                        .document(model.id)
                        .collection("categoryNotes")
                        .get()
                        .addOnSuccessListener { notesSnapshot ->
                            model.filesCount = notesSnapshot.size()
                            pending--
                            if (pending == 0) {
                                onSuccess(categories.sortedBy { it.title }) // Sort A-Z
                            }
                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0) {
                                onSuccess(categories.sortedBy { it.title })
                            }
                        }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ==========================================================
    // ADD NOTE BASED ON CATEGORY (No Duplicate Titles)
    // ==========================================================
    fun addNoteBasedOnCategory(
        uid: String,
        categoryId: String,
        note: NotesModel,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Check for duplicate note title first
        db.collection("Notes_Collections").document(uid)
            .collection("categories").document(categoryId)
            .collection("categoryNotes")
            .whereEqualTo("title", note.title)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val noteId = note.id ?: System.currentTimeMillis().toInt()
                    val currentTime = System.currentTimeMillis()
                    val noteData = hashMapOf(
                        "id" to noteId,
                        "title" to note.title,
                        "description" to note.description,
                        "category" to note.category,
                        "createdAt" to currentTime,
                        "updatedAt" to currentTime
                    )

                    db.collection("Notes_Collections").document(uid)
                        .collection("categories").document(categoryId)
                        .collection("categoryNotes").document(noteId.toString())
                        .set(noteData)
                        .addOnSuccessListener {
                            Log.d(TAG, "Note added to $categoryId: $noteId")
                            onSuccess()
                        }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(Exception("Note with this title already exists in this category"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ==========================================================
    // GET NOTES BASED ON CATEGORY (Sorted A-Z)
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
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                }
                onSuccess(notes.sortedBy { it.title }) // Sort A-Z
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ==========================================================
    // EDIT NOTE BASED ON CATEGORY
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
    // DELETE NOTE BASED ON CATEGORY
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
