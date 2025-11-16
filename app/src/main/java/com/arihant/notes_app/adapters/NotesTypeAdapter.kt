package com.arihant.notes_app.adapters

/**
 * Author: Arihant Jain
 * Date: 15-11-2025
 * Time: 23:48
 * Year: 2025
 * Month: November (Nov)
 * Day: 15 (Saturday)
 * Hour: 23
 * Minute: 48
 * Project: notes_app
 * Package: com.arihant.notes_app.adapters
 */

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arihant.notes_app.R
import com.arihant.notes_app.model.NotesTypeModel

class NotesTypeAdapter(
    private var list: ArrayList<NotesTypeModel>,
    private val onClick: (NotesTypeModel) -> Unit
) : RecyclerView.Adapter<NotesTypeAdapter.TypeViewHolder>() {

    // List of available icons
    private val icons = listOf("ic_academic", "ic_work", "ic_important", "ic_others")

    inner class TypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        private val txtFiles: TextView = itemView.findViewById(R.id.txtFiles)

        fun bind(model: NotesTypeModel) {
            // Randomly select an icon from the list
            val randomIcon = icons.random()
            val iconResId = itemView.context.resources.getIdentifier(randomIcon, "drawable", itemView.context.packageName)
            imgIcon.setImageResource(iconResId)
            txtTitle.text = model.title
            txtFiles.text = "${model.filesCount} Notes"

            itemView.setOnClickListener { onClick(model) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notes_item_type, parent, false)
        return TypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TypeViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<NotesTypeModel>) {
        list = ArrayList(newList)
        notifyDataSetChanged()
    }
}
