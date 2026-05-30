package com.kaliki.browser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kaliki.browser.R

class SuggestionsAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.suggestion_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.text.text = items[position]
        holder.itemView.setOnClickListener { onClick(items[position]) }
    }

    override fun getItemCount() = items.size
}
