package com.arihant.notes_app.model

/**
 * Author: Arihant Jain
 * Date: 17-11-2025
 * Time: 03:26
 * Year: 2025
 * Month: November (Nov)
 * Day: 17 (Monday)
 * Hour: 03
 * Minute: 26
 * Project: notes_app
 * Package: com.arihant.notes_app.model
 */
data class NotesModel(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)
