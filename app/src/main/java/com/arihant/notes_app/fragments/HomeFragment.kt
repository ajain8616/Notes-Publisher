package com.arihant.notes_app.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arihant.notes_app.R
import com.arihant.notes_app.adapters.NotesTypeAdapter
import com.arihant.notes_app.firebase_controller.auth.GetAuthController
import com.arihant.notes_app.firebase_controller.notes_data_handler.notes_events.NotesEventsController
import com.arihant.notes_app.model.NotesTypeModel
import com.arihant.notes_app.model.SharedViewModel
import com.arihant.notes_app.utils.NetworkChecker
import com.google.android.material.button.MaterialButton
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.fragment.app.commit

class HomeFragment : Fragment() {

    private val notesList = ArrayList<NotesTypeModel>()
    private lateinit var notesAdapter: NotesTypeAdapter
    private lateinit var txtWelcome: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var userName: String
    private lateinit var notesController: NotesEventsController
    private lateinit var userId: String
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var networkChecker: NetworkChecker
    private var userToken: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        txtWelcome = view.findViewById(R.id.txtWelcome)
        val rvNotesTypes: RecyclerView = view.findViewById(R.id.rvNotesTypes)
        val imgAddCategory: ImageView = view.findViewById(R.id.btnAddCategory)
        val imgSearch: ImageView = view.findViewById(R.id.imgSearch)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        rvNotesTypes.layoutManager = GridLayoutManager(requireContext(), 2)
        notesAdapter = NotesTypeAdapter(notesList) { category ->
            openAddNotesFragment(category)
        }
        rvNotesTypes.adapter = notesAdapter

        notesController = NotesEventsController()

        swipeRefreshLayout.setOnRefreshListener {
            refreshNotes()
        }

        // Get user token from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("user_prefs", 0)
        userToken = prefs.getString("user_token", null)

        if (userToken != null) {
            val authController = GetAuthController(requireContext())
            authController.getUserProfileByToken(userToken!!) { success, user, uid ->
                if (success && user != null && uid != null) {
                    userName = user.name
                    userId = uid
                    txtWelcome.text = "Welcome, $userName!"
                    fetchCategories(uid)
                } else {
                    txtWelcome.text = "Welcome!"
                    Toast.makeText(requireContext(), "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
            }
        }

        imgAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        imgSearch.setOnClickListener {
            showSearchDialog()
        }

        // Initialize NetworkChecker
        val authController = GetAuthController(requireContext())
        networkChecker = NetworkChecker(requireActivity(), authController, userToken)
        networkChecker.startChecking()
        networkChecker.startChecking()

        return view
    }

    override fun onPause() {
        super.onPause()
        networkChecker.stopChecking() // Stop monitoring when fragment is paused
    }

    // ---------------- FETCH CATEGORIES + NOTES COUNT ------------------
    private fun fetchCategories(uid: String) {
        notesController.getCategories(uid, { categories ->
            notesList.clear()
            notesList.addAll(categories)
            sortNotesList()
            notesAdapter.notifyDataSetChanged()
        }, { exception ->
            Toast.makeText(requireContext(), "Failed to load categories: ${exception.message}", Toast.LENGTH_SHORT).show()
        })
    }

    private fun refreshNotes() {
        if (userToken != null) {
            val authController = GetAuthController(requireContext())
            authController.getUserProfileByToken(userToken!!) { success, user, uid ->
                if (success && uid != null) {
                    fetchCategories(uid)
                }
            }
        }
        swipeRefreshLayout.isRefreshing = false
    }

    // ---------------- ADD CATEGORY DIALOG ---------------------
    @SuppressLint("NotifyDataSetChanged")
    private fun showAddCategoryDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_notes_events_handler)
        dialog.setCancelable(true)

        val btnCloseDialog: ImageView? = dialog.findViewById(R.id.btnCloseDialog)
        btnCloseDialog?.setOnClickListener { dialog.dismiss() }

        val edtCategoryTitle: EditText = dialog.findViewById(R.id.edtCategoryTitle)
        val btnSaveCategory: MaterialButton = dialog.findViewById(R.id.btnSaveCategory)

        btnSaveCategory.setOnClickListener {
            val title = edtCategoryTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newCategory = NotesTypeModel(id = "", title = title, filesCount = 0, icon = "ic_others")
            notesList.add(newCategory)
            sortNotesList()
            notesAdapter.notifyDataSetChanged()

            if (userToken != null) {
                val authController = GetAuthController(requireContext())
                authController.getUserProfileByToken(userToken!!) { success, user, uid ->
                    if (success && uid != null) {
                        notesController.addCategory(uid, newCategory, { generatedId ->
                            newCategory.id = generatedId
                            notesAdapter.notifyDataSetChanged()
                            Toast.makeText(requireContext(), "Category added!", Toast.LENGTH_SHORT).show()
                        }, { exception ->
                            Toast.makeText(requireContext(), "Failed to add category: ${exception.message}", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    // ---------------- SEARCH DIALOG ------------------------
    @SuppressLint("NotifyDataSetChanged")
    private fun showSearchDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_notes_events_handler)
        dialog.setCancelable(true)

        val btnCloseDialog: ImageView = dialog.findViewById(R.id.btnCloseDialog)
        val mainCard: View = dialog.findViewById(R.id.dialog_card)
        val searchCard: View = dialog.findViewById(R.id.search_card)

        mainCard.visibility = View.GONE
        searchCard.visibility = View.VISIBLE

        val edtSearch: EditText = dialog.findViewById(R.id.edtSearch)
        val btnSearch: MaterialButton = dialog.findViewById(R.id.btnSearch)

        btnCloseDialog.setOnClickListener { dialog.dismiss() }

        btnSearch.setOnClickListener {
            val query = edtSearch.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "Enter something to search", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val filtered = notesList.filter { it.title.contains(query, ignoreCase = true) }
            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "No result found", Toast.LENGTH_SHORT).show()
            } else {
                notesAdapter.updateList(filtered)
            }

            Handler().postDelayed({ resetNotesList() }, 5000)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun resetNotesList() {
        notesAdapter.updateList(notesList)
    }

    private fun sortNotesList() {
        notesList.sortBy { it.title.lowercase() }
    }

    // ---------------- OPEN ADD NOTES -----------------------
    private fun openAddNotesFragment(category: NotesTypeModel) {
        sharedViewModel.setCategory(category.title)
        sharedViewModel.setCategoryId(category.id)
        sharedViewModel.setUid(userId)

        parentFragmentManager.commit {
            add(android.R.id.content, AddNotesFragment())
            addToBackStack(null)
        }
    }
}
