package com.kaliki.browser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.kaliki.browser.R
import com.kaliki.browser.models.BrowserTab

class TabAdapter(
    private val tabs: MutableList<BrowserTab>,
    private val onTabClick: (Int) -> Unit,
    private val onTabClose: (BrowserTab) -> Unit,
    private val onTabLongPress: ((BrowserTab, Int) -> Unit)? = null
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tab_title)
        val url: TextView = view.findViewById(R.id.tab_url)
        val closeBtn: ImageButton = view.findViewById(R.id.tab_close_btn)
        val favicon: ImageView = view.findViewById(R.id.tab_favicon)
        val thumbnail: ImageView = view.findViewById(R.id.tab_thumbnail)
        val urlContainer: LinearLayout = view.findViewById(R.id.tab_url_container)
        val groupStripe: View = view.findViewById(R.id.tab_group_stripe)
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
        } else {
            holder.thumbnail.setImageDrawable(null)
            holder.thumbnail.visibility = View.VISIBLE // still show with bg color
        }

        // Show tab group color stripe
        if (tab.groupColor != 0) {
            holder.groupStripe.setBackgroundColor(tab.groupColor)
            holder.groupStripe.visibility = View.VISIBLE
        } else {
            holder.groupStripe.visibility = View.GONE
        }

        // URL container always visible at bottom
        holder.urlContainer.visibility = View.VISIBLE

        holder.itemView.setOnClickListener { onTabClick(position) }
        holder.closeBtn.setOnClickListener { onTabClose(tab) }
        holder.itemView.setOnLongClickListener {
            onTabLongPress?.invoke(tab, position)
            true
        }
    }

    override fun getItemCount() = tabs.size

    /**
     * Attach swipe-to-close functionality to a RecyclerView
     */
    fun attachSwipeToClose(recyclerView: RecyclerView) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position in tabs.indices) {
                    val tab = tabs[position]
                    onTabClose(tab)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }
}
