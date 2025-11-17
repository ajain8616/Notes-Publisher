package com.arihant.notes_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.arihant.notes_app.R
import com.arihant.notes_app.adapters.FilterTabsAdapter
import com.arihant.notes_app.adapters.StickyNotesAdapter
import com.arihant.notes_app.model.StickyNote
import java.util.concurrent.TimeUnit

class ReportFragment : Fragment() {

    private lateinit var rvFilterTabs: RecyclerView
    private lateinit var rvStickyNotes: RecyclerView

    private lateinit var filterAdapter: FilterTabsAdapter
    private lateinit var notesAdapter: StickyNotesAdapter

    private val originalNotesList = ArrayList<StickyNote>()
    private val filteredNotesList = ArrayList<StickyNote>()

    private val filterItems = listOf(
        "Today", "Weekly", "15 Days", "Month", "3 Months", "6 Months", "Year"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFilterTabs = view.findViewById(R.id.rvFilterTabs)
        rvStickyNotes = view.findViewById(R.id.rvStickyNotes)

        setupFilters()
        setupNotes()
    }

    private fun setupFilters() {
        filterAdapter = FilterTabsAdapter(requireContext(), filterItems) { filterName ->
            applyFilter(filterName)
        }

        rvFilterTabs.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvFilterTabs.adapter = filterAdapter
    }

    private fun setupNotes() {
        val now = System.currentTimeMillis()

        // Clear existing lists
        originalNotesList.clear()
        filteredNotesList.clear()

        val categories = listOf("Work", "Health", "Learning", "Personal", "Hobby")

        // Generate 50 notes
        for (i in 1..50) {
            val title = "Note $i"
            val category = categories[i % categories.size] // rotate through categories
            val priority = (1..5).random() // random priority between 1 and 5
            val timestamp = now - TimeUnit.DAYS.toMillis((0..60).random().toLong()) // random date within last 60 days

            originalNotesList.add(StickyNote(i, title, category, priority, timestamp))
        }

        // Copy to filtered list
        filteredNotesList.addAll(originalNotesList)

        // Setup RecyclerView
        notesAdapter = StickyNotesAdapter(requireContext(), filteredNotesList)
        rvStickyNotes.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rvStickyNotes.adapter = notesAdapter
    }

    private fun applyFilter(filter: String) {
        val now = System.currentTimeMillis()
        val dayInMillis = TimeUnit.DAYS.toMillis(1)

        val range = when (filter) {
            "Today" -> dayInMillis * 1       // 1 day
            "Weekly" -> dayInMillis * 7      // 7 days
            "15 Days" -> dayInMillis * 15    // 15 days
            "Month" -> dayInMillis * 30      // 30 days
            "3 Months" -> dayInMillis * 90   // 90 days
            "6 Months" -> dayInMillis * 180  // 180 days
            "Year" -> dayInMillis * 365      // 365 days
            else -> Long.MAX_VALUE           // No filter
        }

        filteredNotesList.clear()
        filteredNotesList.addAll(originalNotesList.filter { note ->
            (now - note.date) <= range
        })

        notesAdapter.notifyDataSetChanged()
    }
}
