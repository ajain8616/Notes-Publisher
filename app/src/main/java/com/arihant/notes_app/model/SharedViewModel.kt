package com.arihant.notes_app.model

/**
 * Author: Arihant Jain
 * Date: 17-11-2025
 * Time: 03:42
 * Year: 2025
 * Month: November (Nov)
 * Day: 17 (Monday)
 * Hour: 03
 * Minute: 42
 * Project: notes_app
 * Package: com.arihant.notes_app.model
 */


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _selectedCategory = MutableLiveData<String>()
    val selectedCategory: LiveData<String> get() = _selectedCategory

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    private val _selectedCategoryId = MutableLiveData<String>()
    val selectedCategoryId: LiveData<String> get() = _selectedCategoryId

    fun setCategoryId(id: String) {
        _selectedCategoryId.value = id
    }

    // User ID
    private val _uid = MutableLiveData<String>()
    val uid: LiveData<String> get() = _uid

    fun setUid(userId: String) {
        _uid.value = userId
    }
}
