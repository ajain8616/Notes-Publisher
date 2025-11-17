package com.arihant.notes_app.model

/**
 * Author: Arihant Jain
 * Date: 17-11-2025
 * Time: 18:08
 * Year: 2025
 * Month: November (Nov)
 * Day: 17 (Monday)
 * Hour: 18
 * Minute: 08
 * Project: notes_app
 * Package: com.arihant.notes_app.model
 */

data class StickyNote(
    val id: Int,
    val message: String,
    val category: String,
    val count: Int,
    val date: Long
)
