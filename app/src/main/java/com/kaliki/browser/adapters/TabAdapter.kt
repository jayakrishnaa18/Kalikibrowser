package com.kaliki.browser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kaliki.browser.R
import com.kaliki.browser.models.BrowserTab

class TabAdapter(
    private val tabs: List<BrowserTab>,
    private val onTabClick: (Int) -> Unit,
    private val onTabClose: (BrowserTab) -> Unit
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tab_title)
        val url: TextView = view.findViewById(R.id.tab_url)
        val closeBtn: ImageButton = view.findViewById(R.id.tab_close_btn)
        val favicon: ImageView = view.findViewById(R.id.tab_favicon)
        val thumbnail: ImageView = view.findViewById(R.id.tab_thumbnail)
        val urlContainer: LinearLayout = view.findViewById(R.id.tab_url_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifEmpty { "New Tab" }
        holder.url.text = tab.url ?: "kaliki://newtab"

        // Show thumbnail if available
        if (tab.thumbnail != null) {
            holder.thumbnail.setImageBitmap(tab.thumbnail)
            holder.thumbnail.visibility = View.VISIBLE
            holder.urlContainer.visibility = View.GONE
        } else {
            holder.thumbnail.visibility = View.GONE
            holder.urlContainer.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onTabClick(position) }
        holder.closeBtn.setOnClickListener { onTabClose(tab) }
    }

    override fun getItemCount() = tabs.size
}
