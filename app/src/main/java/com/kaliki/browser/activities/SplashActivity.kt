package com.kaliki.browser.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
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
        val tagline = findViewById<TextView>(R.id.splash_tagline)

        // Start small and invisible
        logo.scaleX = 0.3f
        logo.scaleY = 0.3f
        logo.alpha = 0f

        // Logo: spring bounce from small to full size
        logo.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(2.5f))
            .withEndAction {
                // Text slides up and fades in
                text.translationY = 40f
                text.animate().alpha(1f).translationY(0f).setDuration(400)
                    .withEndAction {
                        // Tagline fades in
                        tagline.animate().alpha(1f).setDuration(300).start()
                    }
                    .start()
            }
            .start()

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
        }, 1800)
    }
}
