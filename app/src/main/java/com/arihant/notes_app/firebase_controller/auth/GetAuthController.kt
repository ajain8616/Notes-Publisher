package com.arihant.notes_app.firebase_controller.auth

/**
 * Author: Arihant Jain
 * Date: 16-11-2025
 * Time: 20:29
 * Year: 2025
 * Month: November (Nov)
 * Day: 16 (Sunday)
 * Hour: 20
 * Minute: 29
 * Project: notes_app
 * Package: com.arihant.notes_app.firebase_controller.auth
 */


import android.content.Context
import android.util.Log
import com.arihant.notes_app.model.UserModel
import com.google.firebase.database.*

class GetAuthController(private val context: Context) {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val TAG = "GetAuthController"

    fun getUserProfileByToken(token: String, callback: (Boolean, UserModel?, String?) -> Unit) {
        Log.d(TAG, "Fetching user profile with token: $token")

        val query = database.child("Users").orderByChild("token").equalTo(token)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnap in snapshot.children) {
                        val user = userSnap.getValue(UserModel::class.java)
                        val uid = userSnap.key
                        if (user != null && uid != null) {
                            Log.d(TAG, "User found: ${user.name}, uid=$uid, online=${user.isOnline}")
                            callback(true, user, uid)
                            return
                        }
                    }
                    callback(false, null, null)
                } else {
                    Log.d(TAG, "No user found with token")
                    callback(false, null, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                callback(false, null, null)
            }
        })
    }

    // --- Update profile by token ---
    fun updateUserProfileByToken(
        token: String,
        name: String? = null,
        email: String? = null,
        isOnline: Boolean? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "Updating user profile with token: $token")

        val query = database.child("Users").orderByChild("token").equalTo(token)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnap in snapshot.children) {
                        val uid = userSnap.key
                        if (uid != null) {
                            val updates = mutableMapOf<String, Any>()
                            name?.let { updates["name"] = it }
                            email?.let { updates["email"] = it }
                            isOnline?.let { updates["isOnline"] = it }
                            updates["updatedTime"] = System.currentTimeMillis()

                            database.child("Users").child(uid).updateChildren(updates)
                                .addOnSuccessListener {
                                    Log.d(TAG, "User profile updated successfully")
                                    callback(true, "Profile updated")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Update failed: ${e.message}")
                                    callback(false, "Update failed: ${e.message}")
                                }
                            return
                        }
                    }
                    callback(false, "User not found")
                } else {
                    callback(false, "User not found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                callback(false, "Database error: ${error.message}")
            }
        })
    }

    // Add this to your GetAuthController class
    fun observeUserOnlineStatus(uid: String, onStatusChange: (Boolean) -> Unit) {
        database.child("Users").child(uid).child("isOnline")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                    onStatusChange(isOnline)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to observe online status: ${error.message}")
                }
            })
    }
}
