package com.arihant.notes_app.model

/**
 * Author: Arihant Jain
 * Date: 16-11-2025
 * Time: 19:56
 * Year: 2025
 * Month: November (Nov)
 * Day: 16 (Sunday)
 * Hour: 19
 * Minute: 56
 * Project: notes_app
 * Package: com.arihant.notes_app.model
 */
data class UserModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val token: String = "",
    val createdTime: Long = 0L,
    val updatedTime: Long = 0L,
    val isOnline: Boolean = false
)

