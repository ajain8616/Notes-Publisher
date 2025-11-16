package com.arihant.notes_app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arihant.notes_app.R
import com.arihant.notes_app.adapters.NotesAdapter
import com.arihant.notes_app.model.NotesModel
import com.arihant.notes_app.model.SharedViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.cardview.widget.CardView

class AddNotesFragment : Fragment() {

    private lateinit var addBtn: ImageView
    private lateinit var notesRecycler: RecyclerView
    private lateinit var addNotesCard: CardView
    private lateinit var noteTypeTitle: TextView
    private lateinit var edtNoteTitle: TextInputEditText
    private lateinit var edtNoteDesc: TextInputEditText
    private lateinit var btnSaveNote: MaterialButton
    private lateinit var titleName: TextView
    private lateinit var backBtn : ImageView

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var selectedCategory: String? = null

    private val notesList = mutableListOf<NotesModel>()
    private lateinit var notesAdapter: NotesAdapter
    private val TAG = "AddNotesFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_notes, container, false)

        // Initialize Views
        addBtn = view.findViewById(R.id.addBtn)
        notesRecycler = view.findViewById(R.id.notesRecycler)
        addNotesCard = view.findViewById(R.id.add_notes_card)
        noteTypeTitle = view.findViewById(R.id.noteTitle)
        edtNoteTitle = view.findViewById(R.id.edtNoteTitle)
        edtNoteDesc = view.findViewById(R.id.edtNoteDesc)
        btnSaveNote = view.findViewById(R.id.btnSaveNote)
        titleName = view.findViewById(R.id.titleName)
        backBtn = view.findViewById(R.id.backBtn)



        // Make noteTypeTitle non-editable and non-clickable
        noteTypeTitle.isClickable = false
        noteTypeTitle.isFocusable = false


        // Observe selected category only once when fragment is created
        selectedCategory = sharedViewModel.selectedCategory.value ?: "Note Category"
        Log.d("AddNotesFragment", "Initial selectedCategory: $selectedCategory")
        titleName.text = selectedCategory
        noteTypeTitle.text = selectedCategory




        // RecyclerView setup
        notesAdapter = NotesAdapter(
            requireContext(),
            notesList,
            onEditClick = { note ->
                Toast.makeText(requireContext(), "Edit: ${note.title}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { note ->
                notesList.remove(note)
                notesAdapter.notifyDataSetChanged()
            }
        )
        notesRecycler.layoutManager = LinearLayoutManager(requireContext())
        notesRecycler.adapter = notesAdapter

        // Back button click listener
        backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }


        // Show Add Note Card
        addBtn.setOnClickListener {
            notesRecycler.visibility = View.GONE
            addNotesCard.visibility = View.VISIBLE
        }

        // Save note
        btnSaveNote.setOnClickListener {
            val title = edtNoteTitle.text.toString().trim()
            val desc = edtNoteDesc.text.toString().trim()
            val category = selectedCategory ?: "Note Category"

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter title and description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timestamp = System.currentTimeMillis()
            val newNote = NotesModel(
                id = notesList.size + 1,
                title = title,
                description = desc,
                category = category,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            Log.d(TAG, "Adding new note: $newNote")

            notesList.add(newNote)
            notesAdapter.notifyDataSetChanged()

            Toast.makeText(requireContext(), "Note added!", Toast.LENGTH_SHORT).show()

            // Clear input fields
            edtNoteTitle.setText("")
            edtNoteDesc.setText("")

            // Hide add note card and show RecyclerView
            addNotesCard.visibility = View.GONE
            notesRecycler.visibility = View.VISIBLE
        }

        return view
    }
}
