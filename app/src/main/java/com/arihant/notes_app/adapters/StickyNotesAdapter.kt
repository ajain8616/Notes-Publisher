package com.arihant.notes_app.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arihant.notes_app.R
import com.arihant.notes_app.model.StickyNote
import java.text.SimpleDateFormat
import java.util.*

class StickyNotesAdapter(
    private val context: Context,
    private val notesList: ArrayList<StickyNote>
) : RecyclerView.Adapter<StickyNotesAdapter.StickyNoteViewHolder>() {

    private val rotations = listOf(-8f, -5f, -2f, 0f, 2f, 5f, 8f)

    private val cardColors = mutableListOf(
        R.color.noteMint,
        R.color.notePeach,
        R.color.noteLavender,
        R.color.noteYellow,
        R.color.noteSkyBlue,
        R.color.noteOrange,
        R.color.notePink,
        R.color.notePaleGreen,
        R.color.notePaleBlue
    )

    private var colorQueue = cardColors.shuffled().toMutableList()

    private val pinColors = listOf(
        R.color.forest_green,
        R.color.deep_navy,
        R.color.deep_brown,
        R.color.accent_leaf_green,
        R.color.dark_olive,
        R.color.slate_gray
    )

    inner class StickyNoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardStickyNote)
        val noteMessage: TextView = itemView.findViewById(R.id.txtNoteMessage)
        val noteCategory: TextView = itemView.findViewById(R.id.txtNoteCategory)
        val noteCount: TextView = itemView.findViewById(R.id.txtNoteCount)
        val noteDate: TextView = itemView.findViewById(R.id.txtNoteDate)
        val pinIcon: ImageView = itemView.findViewById(R.id.imgPin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickyNoteViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_sticky_note, parent, false)
        return StickyNoteViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onBindViewHolder(holder: StickyNotesAdapter.StickyNoteViewHolder, position: Int) {
        val note = notesList[position]

        holder.noteCategory.text = note.category
        holder.noteMessage.text = note.message.ifEmpty { "No Title Available" }
        holder.noteCount.text = "${note.count} Notes"

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.noteDate.text = sdf.format(Date(note.date))

        holder.cardView.rotation = rotations.random()

        if (colorQueue.isEmpty()) colorQueue = cardColors.shuffled().toMutableList()
        holder.cardView.backgroundTintList =
            ContextCompat.getColorStateList(context, colorQueue.removeFirst())

        holder.pinIcon.setColorFilter(ContextCompat.getColor(context, pinColors.random()))
    }

    override fun getItemCount(): Int = notesList.size
}
