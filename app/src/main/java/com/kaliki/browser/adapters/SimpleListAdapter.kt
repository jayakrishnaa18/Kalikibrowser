package com.kaliki.browser.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kaliki.browser.R
import com.kaliki.browser.models.ListItem

class SimpleListAdapter(
    private val items: List<ListItem>,
    private val onClick: (ListItem) -> Unit,
    private val onDelete: ((ListItem) -> Unit)? = null
) : RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.item_icon)
        val title: TextView = view.findViewById(R.id.item_title)
        val subtitle: TextView = view.findViewById(R.id.item_subtitle)
        val deleteBtn: ImageButton = view.findViewById(R.id.item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle

        // Show first letter of domain as icon
        val domain = try { Uri.parse(item.subtitle).host?.removePrefix("www.")?.take(1)?.uppercase() ?: "?" } catch (_: Exception) { "?" }
        holder.icon.text = domain

        holder.itemView.setOnClickListener { onClick(item) }
        if (onDelete != null) {
            holder.deleteBtn.visibility = View.VISIBLE
            holder.deleteBtn.setOnClickListener { onDelete.invoke(item) }
        } else {
            holder.deleteBtn.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}
