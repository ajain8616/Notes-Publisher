package com.arihant.notes_app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arihant.notes_app.R

class FilterTabsAdapter(
    private val context: Context,
    private val filterList: List<String>,
    private val listener: (String) -> Unit
) : RecyclerView.Adapter<FilterTabsAdapter.FilterViewHolder>() {

    private var selectedPos = 0

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardFilter: CardView = itemView.findViewById(R.id.cardFilterTab)
        val txtFilter: TextView = itemView.findViewById(R.id.txtFilterTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_filter_tab, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.txtFilter.text = filterList[position]

        // Apply selected / unselected UI state
        if (selectedPos == position) {
            holder.cardFilter.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.accent_leaf_green)
            )
            holder.txtFilter.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.cardFilter.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.white)
            )
            holder.txtFilter.setTextColor(ContextCompat.getColor(context, R.color.dark_charcoal))
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPos
            selectedPos = position

            // Update only changed items (better performance)
            notifyItemChanged(previousPosition)
            notifyItemChanged(position)

            listener(filterList[position])
        }
    }

    override fun getItemCount(): Int = filterList.size
}
