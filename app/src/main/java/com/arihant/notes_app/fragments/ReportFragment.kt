package com.arihant.notes_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arihant.notes_app.R
import com.arihant.notes_app.adapters.StickyNotesAdapter
import com.arihant.notes_app.firebase_controller.auth.GetAuthController
import com.arihant.notes_app.model.StickyNote
import com.arihant.notes_app.utils.NetworkChecker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportFragment : Fragment() {

    private lateinit var rvStickyNotes: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var notesAdapter: StickyNotesAdapter
    private val originalNotesList = ArrayList<StickyNote>()

    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private var userToken: String? = null
    private lateinit var networkChecker: NetworkChecker

    companion object {
        private const val TAG = "ReportFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvStickyNotes = view.findViewById(R.id.rvStickyNotes)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        db = FirebaseFirestore.getInstance()

        // Get user token from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("user_prefs", 0)
        userToken = prefs.getString("user_token", null)

        // Initialize NetworkChecker
        val authController = GetAuthController(requireContext())
        networkChecker = NetworkChecker(requireActivity(), authController, userToken)
        networkChecker.startChecking()

        setupSwipeToRefresh()
        getCurrentUserAndLoadNotes()
    }

    override fun onPause() {
        super.onPause()
        networkChecker.stopChecking() // Stop monitoring when fragment is paused
    }

    private fun setupSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadUserCategories()
        }
    }

    private fun getCurrentUserAndLoadNotes() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }
        userId = currentUser.uid
        loadUserCategories()
    }

    private fun loadUserCategories() {
        if (userId.isNullOrEmpty()) return

        swipeRefreshLayout.isRefreshing = true
        originalNotesList.clear()

        db.collection("Notes_Collections")
            .document(userId!!)
            .collection("categories")
            .get()
            .addOnSuccessListener { categorySnapshot ->

                if (categorySnapshot.isEmpty) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "No categories found!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                var pending = categorySnapshot.size()

                for (categoryDoc in categorySnapshot.documents) {

                    val categoryId = categoryDoc.id
                    val categoryName = categoryDoc.getString("title") ?: "Unknown"

                    db.collection("Notes_Collections")
                        .document(userId!!)
                        .collection("categories")
                        .document(categoryId)
                        .collection("categoryNotes")
                        .orderBy("createdAt")
                        .get()
                        .addOnSuccessListener { notesSnapshot ->

                            val notes = notesSnapshot.documents.map { doc ->
                                val title = doc.getString("title") ?: ""
                                val createdAt = doc.getLong("createdAt") ?: 0L
                                title to createdAt
                            }

                            val latestTitle =
                                if (notes.isNotEmpty()) notes.last().first else "No notes added"
                            val latestDate =
                                if (notes.isNotEmpty()) notes.last().second else 0L

                            originalNotesList.add(
                                StickyNote(
                                    id = categoryName.hashCode(),
                                    message = latestTitle,
                                    category = categoryName,
                                    count = notes.size,
                                    date = latestDate
                                )
                            )

                            pending--
                            if (pending == 0) finalizeNotesLoad()
                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0) finalizeNotesLoad()
                        }
                }
            }
            .addOnFailureListener {
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(requireContext(), "Failed to load categories!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun finalizeNotesLoad() {
        swipeRefreshLayout.isRefreshing = false
        sortNotesList()
        setupNotesRecycler()
    }

    private fun setupNotesRecycler() {
        if (!::notesAdapter.isInitialized) {
            notesAdapter = StickyNotesAdapter(requireContext(), originalNotesList)
            rvStickyNotes.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            rvStickyNotes.adapter = notesAdapter
        } else {
            notesAdapter.notifyDataSetChanged()
        }
    }

    private fun sortNotesList() {
        originalNotesList.sortBy { it.message.lowercase() }
        originalNotesList.reverse()
    }
}
