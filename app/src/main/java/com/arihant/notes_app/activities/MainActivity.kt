package com.arihant.notes_app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.arihant.notes_app.R
import com.arihant.notes_app.adapters.FragmentAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupViewPager()
        setupBottomNavigation()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.view_pager)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupViewPager() {
        val adapter = FragmentAdapter(this)
        viewPager.adapter = adapter

        // Sync ViewPager swipe with BottomNavigationView
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigation.menu.getItem(position).isChecked = true
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.nav_report -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.nav_settings -> {
                    viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }
    }
}
