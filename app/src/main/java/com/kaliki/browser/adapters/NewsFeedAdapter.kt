package com.kaliki.browser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.kaliki.browser.R
import com.kaliki.browser.models.NewsItem

class NewsFeedAdapter(
    private val items: MutableList<Any>,  // Mix of NewsItem and NativeAd
    private val onNewsClick: (NewsItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_NEWS = 0
        const val TYPE_AD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is NativeAd -> TYPE_AD
            else -> TYPE_NEWS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_native_ad, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
            NewsViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NewsViewHolder -> {
                val item = items[position]
                if (item is NewsItem) holder.bind(item, onNewsClick)
            }
            is AdViewHolder -> {
                val ad = items[position]
                if (ad is NativeAd) holder.bind(ad)
            }
        }
    }

    override fun getItemCount() = items.size

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.news_title)
        private val source: TextView = view.findViewById(R.id.news_source)
        private val image: ImageView = view.findViewById(R.id.news_image)
        private val category: TextView = view.findViewById(R.id.news_category)

        fun bind(item: NewsItem, onClick: (NewsItem) -> Unit) {
            title.text = item.title
            source.text = item.source
            category.text = item.category
            // Use colored background as placeholder for news image
            image.setBackgroundColor(item.color)
            itemView.setOnClickListener { onClick(item) }
        }
    }

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val adView: NativeAdView = view.findViewById(R.id.native_ad_view)

        fun bind(nativeAd: NativeAd) {
            // Headline
            val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
            headlineView.text = nativeAd.headline
            adView.headlineView = headlineView

            // Body
            val bodyView = adView.findViewById<TextView>(R.id.ad_body)
            bodyView.text = nativeAd.body ?: ""
            bodyView.visibility = if (nativeAd.body != null) View.VISIBLE else View.GONE
            adView.bodyView = bodyView

            // Call to action
            val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
            ctaView.text = nativeAd.callToAction ?: "Learn More"
            ctaView.visibility = if (nativeAd.callToAction != null) View.VISIBLE else View.GONE
            adView.callToActionView = ctaView

            // Icon
            val iconView = adView.findViewById<ImageView>(R.id.ad_icon)
            if (nativeAd.icon != null) {
                iconView.setImageDrawable(nativeAd.icon!!.drawable)
                iconView.visibility = View.VISIBLE
            } else {
                iconView.visibility = View.GONE
            }
            adView.iconView = iconView

            // Advertiser
            val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
            advertiserView.text = nativeAd.advertiser ?: "Sponsored"
            adView.advertiserView = advertiserView

            adView.setNativeAd(nativeAd)
        }
    }
}
