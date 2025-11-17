package com.arihant.notes_app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arihant.notes_app.R
import com.arihant.notes_app.adapters.NotesAdapter
import com.arihant.notes_app.firebase_controller.notes_data_handler.notes_events.NotesEventsController
import com.arihant.notes_app.model.NotesModel
import com.arihant.notes_app.model.SharedViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddNotesFragment : Fragment() {

    private lateinit var addBtn: ImageView
    private lateinit var searchBtn: ImageView
    private lateinit var notesRecycler: RecyclerView
    private lateinit var addNotesCard: CardView
    private lateinit var edtNoteTitle: TextInputEditText
    private lateinit var edtNoteDesc: TextInputEditText
    private lateinit var btnSaveNote: MaterialButton
    private lateinit var titleName: TextView
    private lateinit var backBtn: ImageView
    private lateinit var editNotesCard: CardView
    private lateinit var edtEditNoteTitle: TextInputEditText
    private lateinit var edtEditNoteDesc: TextInputEditText
    private lateinit var btnUpdateNote: MaterialButton


    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val notesEventsController = NotesEventsController()

    private var categoryId: String = ""
    private var uid: String = ""
    private var selectedCategory: String? = null

    private val notesList = mutableListOf<NotesModel>()
    private lateinit var notesAdapter: NotesAdapter

    private val TAG = "AddNotesFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_add_notes, container, false)

        Log.d(TAG, "Initializing views...")
        initViews(view)

        Log.d(TAG, "Reading values from SharedViewModel...")
        selectedCategory = sharedViewModel.selectedCategory.value ?: "Note Category"
        categoryId = sharedViewModel.selectedCategoryId.value ?: ""
        uid = sharedViewModel.uid.value ?: ""

        Log.d(TAG, "Selected Category: $selectedCategory")
        Log.d(TAG, "Category ID: $categoryId")
        Log.d(TAG, "User UID: $uid")

        if (categoryId.isEmpty() || uid.isEmpty()) {
            Log.e(TAG, "Invalid categoryId or uid! Cannot load notes.")
            Toast.makeText(requireContext(), "Invalid category or user!", Toast.LENGTH_LONG).show()
            return view
        }

        titleName.text = selectedCategory

        Log.d(TAG, "Setting up RecyclerView...")
        setupRecycler()

        Log.d(TAG, "Loading notes from Firestore...")
        loadNotes()

        addBtn.setOnClickListener {
            Log.d(TAG, "Add button clicked. Showing add-note card.")
            addNotesCard.visibility = View.VISIBLE
            notesRecycler.visibility = View.GONE
        }

        btnSaveNote.setOnClickListener { saveNote() }

        backBtn.setOnClickListener {
            Log.d(TAG, "Back button clicked. Navigating back.")
            requireActivity().onBackPressed()
        }
        searchBtn.setOnClickListener {
            showSearchDialog()
        }

        return view
    }

    private fun initViews(view: View) {
        addBtn = view.findViewById(R.id.addBtn)
        searchBtn = view.findViewById(R.id.searchBtn)
        notesRecycler = view.findViewById(R.id.notesRecycler)
        addNotesCard = view.findViewById(R.id.add_notes_card)
        edtNoteTitle = view.findViewById(R.id.edtNoteTitle)
        edtNoteDesc = view.findViewById(R.id.edtNoteDesc)
        btnSaveNote = view.findViewById(R.id.btnSaveNote)
        titleName = view.findViewById(R.id.titleName)
        backBtn = view.findViewById(R.id.backBtn)
        editNotesCard = view.findViewById(R.id.edit_notes_card)
        edtEditNoteTitle = view.findViewById(R.id.edtEditNoteTitle)
        edtEditNoteDesc = view.findViewById(R.id.edtEditNoteDesc)
        btnUpdateNote = view.findViewById(R.id.btnUpdateNote)

    }

    private fun setupRecycler() {
        notesAdapter = NotesAdapter(
            requireContext(),
            notesList,
            onEditClick = { note -> editNote(note) },
            onDeleteClick = { note -> deleteNote(note) }
        )
        notesRecycler.layoutManager = LinearLayoutManager(requireContext())
        notesRecycler.adapter = notesAdapter
    }

    private fun loadNotes() {
        if (categoryId.isEmpty() || uid.isEmpty()) {
            Log.e(TAG, "Cannot load notes. CategoryId or UID is empty.")
            return
        }

        Log.d(TAG, "Fetching notes for uid=$uid, categoryId=$categoryId...")
        notesEventsController.getNotesBasedOnCategory(
            uid,
            categoryId,
            onSuccess = { notes ->
                Log.d(TAG, "Notes fetched successfully. Count: ${notes.size}")
                notesList.clear()
                notesList.addAll(notes)
                sortNotesList()
                requireActivity().runOnUiThread {
                    notesAdapter.updateList(notesList)
                }
            },
            onFailure = {
                Log.e(TAG, "Failed to load notes!")
                Toast.makeText(requireContext(), "Failed to load notes!", Toast.LENGTH_SHORT).show()
            }
        )

    }

    private fun saveNote() {
        val title = edtNoteTitle.text.toString().trim()
        val desc = edtNoteDesc.text.toString().trim()

        Log.d(TAG, "Save note clicked. Title: $title, Description: $desc")

        if (title.isEmpty() || desc.isEmpty()) {
            Log.w(TAG, "Title or description is empty.")
            Toast.makeText(requireContext(), "Enter title and description", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = System.currentTimeMillis()
        val nextId = notesList.size + 1

        val newNote = NotesModel(
            id = nextId,
            title = title,
            description = desc,
            category = selectedCategory ?: "",
            createdAt = timestamp,
            updatedAt = timestamp
        )

        Log.d(TAG, "Adding new note to Firestore: $newNote")

        notesEventsController.addNoteBasedOnCategory(
            uid,
            categoryId,
            newNote,
            onSuccess = {
                Log.d(TAG, "Note added successfully.")
                sortNotesList()
                Toast.makeText(requireContext(), "Note added!", Toast.LENGTH_SHORT).show()
                edtNoteTitle.setText("")
                edtNoteDesc.setText("")
                addNotesCard.visibility = View.GONE
                notesRecycler.visibility = View.VISIBLE
                loadNotes()
            },
            onFailure = {
                Log.e(TAG, "Failed to add note.")
                Toast.makeText(requireContext(), "Failed to add!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun deleteNote(note: NotesModel) {
        Log.d(TAG, "Deleting note: ${note.id} - ${note.title}")

        notesEventsController.deleteNoteBasedOnCategory(
            uid,
            categoryId,
            note.id,
            onSuccess = {
                Log.d(TAG, "Note deleted successfully.")
                Toast.makeText(requireContext(), "Deleted!", Toast.LENGTH_SHORT).show()
                loadNotes()
            },
            onFailure = {
                Log.e(TAG, "Delete failed for note: ${note.id}")
                Toast.makeText(requireContext(), "Delete failed!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun editNote(note: NotesModel) {
        Log.d(TAG, "Editing note: ${note.id} - ${note.title}")

        edtEditNoteTitle.setText(note.title)
        edtEditNoteDesc.setText(note.description)


        editNotesCard.visibility = View.VISIBLE
        addNotesCard.visibility = View.GONE
        notesRecycler.visibility = View.GONE


        btnUpdateNote.setOnClickListener {
            val newTitle = edtEditNoteTitle.text.toString().trim()
            val newDesc = edtEditNoteDesc.text.toString().trim()

            if (newTitle.isEmpty() || newDesc.isEmpty()) {
                Toast.makeText(requireContext(), "Enter title and description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedNote = note.copy(
                title = newTitle,
                description = newDesc,
                updatedAt = System.currentTimeMillis()
            )

            notesEventsController.editNoteBasedOnCategory(
                uid,
                categoryId,
                updatedNote,
                onSuccess = {
                    Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show()
                    editNotesCard.visibility = View.GONE
                    notesRecycler.visibility = View.VISIBLE
                    loadNotes()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Update failed!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun resetNotesList() {
        notesAdapter.updateList(notesList)
    }

    private fun showSearchDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_add_search_notes)
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

            val filtered = notesList.filter { it.title.trim().contains(query, ignoreCase = true) }

            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "No result found", Toast.LENGTH_SHORT).show()
            } else {
                notesAdapter.updateList(filtered)
            }

            android.os.Handler().postDelayed({
                resetNotesList()
            }, 5000)

            dialog.dismiss()
        }

        dialog.show()
    }


    private fun sortNotesList() {
        notesList.sortBy { it.title.lowercase() }
    }
}
