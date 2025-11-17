package com.arihant.notes_app.adapters

/**
 * Author: Arihant Jain
 * Date: 17-11-2025
 * Time: 03:32
 * Year: 2025
 * Month: November (Nov)
 * Day: 17 (Monday)
 * Hour: 03
 * Minute: 32
 * Project: notes_app
 * Package: com.arihant.notes_app.adapters
 */

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arihant.notes_app.R
import com.arihant.notes_app.model.NotesModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val context: Context,
    private var notesList: MutableList<NotesModel>,
    private val onEditClick: (NotesModel) -> Unit,
    private val onDeleteClick: (NotesModel) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtDescription: TextView = view.findViewById(R.id.txtDescription)
        val txtDate: TextView = view.findViewById(R.id.txtDate)
        val imgEdit: ImageView = view.findViewById(R.id.imgEdit)
        val imgDelete: ImageView = view.findViewById(R.id.imgDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.notes_add_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun getItemCount(): Int = notesList.size

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notesList[position]

        holder.txtTitle.text = note.title
        holder.txtDescription.text = note.description

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.txtDate.text = sdf.format(Date(note.createdAt))

        holder.imgEdit.setOnClickListener { onEditClick(note) }
        holder.imgDelete.setOnClickListener { onDeleteClick(note) }
    }

    fun updateList(newNotes: List<NotesModel>) {
        notesList = newNotes.toMutableList()
        notifyDataSetChanged()
    }
}