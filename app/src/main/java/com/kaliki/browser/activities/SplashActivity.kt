package com.kaliki.browser.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.kaliki.browser.R
import com.kaliki.browser.utils.NewsFeedManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val text = findViewById<TextView>(R.id.splash_text)
        val subtitle = findViewById<TextView>(R.id.splash_subtitle)

        // Logo fades in (alpha 0 to 1) in 300ms — no bouncing, no scaling
        logo.alpha = 0f
        logo.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Text appears instantly — no animation
        text.alpha = 1f
        subtitle.alpha = 1f

        // Navigate after 600ms total (fast like Pixdoc/Chrome)
        Handler(Looper.getMainLooper()).postDelayed({
            val feedManager = NewsFeedManager(this)
            val destination = if (feedManager.isInterestsSelected()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, InterestsActivity::class.java)
            }
            startActivity(destination)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 600)
    }
}
