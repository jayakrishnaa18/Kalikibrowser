package com.kaliki.browser.adapters

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
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
        val favicon: ImageView = view.findViewById(R.id.item_favicon)
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

        // Show first letter of domain as icon fallback
        val host = try { Uri.parse(item.subtitle).host?.removePrefix("www.") ?: "" } catch (_: Exception) { "" }
        holder.icon.text = host.take(1).uppercase().ifEmpty { "?" }

        // Load real favicon
        if (host.isNotEmpty()) {
            val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=32"
            Thread {
                try {
                    val conn = java.net.URL(faviconUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 2000; conn.readTimeout = 2000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.connect()
                    if (conn.responseCode == 200) {
                        val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                        if (bitmap != null) holder.favicon.post {
                            holder.favicon.setImageBitmap(bitmap)
                            holder.favicon.visibility = View.VISIBLE
                            holder.icon.visibility = View.GONE
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
            }.start()
        }

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
