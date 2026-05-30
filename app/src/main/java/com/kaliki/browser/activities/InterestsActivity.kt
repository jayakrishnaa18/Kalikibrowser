package com.kaliki.browser.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.kaliki.browser.R
import com.kaliki.browser.utils.NewsFeedManager

class InterestsActivity : AppCompatActivity() {

    private val selectedInterests = mutableSetOf<String>()
    private lateinit var btnContinue: MaterialButton
    private lateinit var selectionCount: TextView
    private lateinit var adapter: InterestsAdapter
    private var selectedLanguageCode = "en"

    data class Language(val name: String, val code: String, val country: String, val ceid: String)

    private val languages = listOf(
        Language("English", "en", "US", "US:en"),
        Language("Hindi", "hi", "IN", "IN:hi"),
        Language("Telugu", "te", "IN", "IN:te"),
        Language("Tamil", "ta", "IN", "IN:ta"),
        Language("Kannada", "kn", "IN", "IN:kn"),
        Language("Malayalam", "ml", "IN", "IN:ml"),
        Language("Marathi", "mr", "IN", "IN:mr"),
        Language("Bengali", "bn", "IN", "IN:bn"),
        Language("Gujarati", "gu", "IN", "IN:gu"),
        Language("Spanish", "es", "US", "US:es"),
        Language("French", "fr", "FR", "FR:fr"),
        Language("Japanese", "ja", "JP", "JP:ja"),
        Language("German", "de", "DE", "DE:de"),
        Language("Portuguese", "pt", "BR", "BR:pt")
    )

    data class InterestItem(val name: String, val icon: String)

    private val interests = listOf(
        InterestItem("Technology", "💻"),
        InterestItem("Sports", "⚽"),
        InterestItem("Business", "💼"),
        InterestItem("Entertainment", "🎬"),
        InterestItem("Science", "🔬"),
        InterestItem("Health", "🏥"),
        InterestItem("Gaming", "🎮"),
        InterestItem("World News", "🌍"),
        InterestItem("Crypto", "₿"),
        InterestItem("AI", "🤖"),
        InterestItem("Space", "🚀"),
        InterestItem("Auto", "🚗")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interests)

        btnContinue = findViewById(R.id.btn_continue)
        selectionCount = findViewById(R.id.selection_count)

        // Language selection
        setupLanguageSpinner()

        val recyclerView = findViewById<RecyclerView>(R.id.interests_grid)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = InterestsAdapter()
        recyclerView.adapter = adapter

        btnContinue.setOnClickListener {
            val feedManager = NewsFeedManager(this)
            feedManager.saveInterests(selectedInterests)
            feedManager.saveLanguage(selectedLanguageCode)
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        updateUI()
    }

    private fun toggleInterest(name: String) {
        if (selectedInterests.contains(name)) {
            selectedInterests.remove(name)
        } else {
            selectedInterests.add(name)
        }
        updateUI()
        adapter.notifyDataSetChanged()
    }

    private fun setupLanguageSpinner() {
        val spinner = findViewById<Spinner>(R.id.language_spinner)
        val names = languages.map { it.name }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedLanguageCode = languages[pos].code
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateUI() {
        val count = selectedInterests.size
        btnContinue.isEnabled = count >= 3
        selectionCount.text = if (count < 3) {
            "$count selected — pick at least 3"
        } else {
            "$count selected ✔"
        }
    }

    inner class InterestsAdapter : RecyclerView.Adapter<InterestsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.interest_card)
            val icon: TextView = view.findViewById(R.id.interest_icon)
            val name: TextView = view.findViewById(R.id.interest_name)
            val check: ImageView = view.findViewById(R.id.interest_check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_interest, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = interests[position]
            holder.icon.text = item.icon
            holder.name.text = item.name

            val isSelected = selectedInterests.contains(item.name)
            if (isSelected) {
                holder.card.strokeColor = getColor(R.color.accent)
                holder.card.setCardBackgroundColor(getColor(R.color.dark_bg_tertiary))
                holder.check.visibility = View.VISIBLE
            } else {
                holder.card.strokeColor = getColor(R.color.dark_border)
                holder.card.setCardBackgroundColor(getColor(R.color.dark_bg_secondary))
                holder.check.visibility = View.GONE
            }

            holder.card.setOnClickListener {
                toggleInterest(item.name)
            }
        }

        override fun getItemCount(): Int = interests.size
    }
}
