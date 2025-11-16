package com.arihant.notes_app.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.arihant.notes_app.fragments.AddNotesFragment
import com.arihant.notes_app.fragments.HomeFragment
import com.arihant.notes_app.fragments.ReportFragment
import com.arihant.notes_app.fragments.SettingsFragment

/**
 * Author: Arihant Jain
 * Date: 15-11-2025
 * Time: 20:12
 * Year: 2025
 * Month: November (Nov)
 * Day: 15 (Saturday)
 * Hour: 20
 * Minute: 12
 * Project: notes_app
 * Package: com.arihant.notes_app.adapters
 */

class FragmentAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = listOf(HomeFragment(), ReportFragment(), SettingsFragment())

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}