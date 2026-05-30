package com.kaliki.browser.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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

        // Fast splash — logo appears instantly with quick scale-up
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        logo.alpha = 0f

        // Quick animation — 400ms total (Brave-like speed)
        logo.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(1.5f))
            .withEndAction {
                text.animate().alpha(1f).translationY(0f).setDuration(200).start()
                tagline.animate().alpha(1f).setStartDelay(100).setDuration(200).start()
            }
            .start()

        text.translationY = 20f
        text.alpha = 0f
        tagline.alpha = 0f

        // Navigate quickly — 800ms total (fast like Chrome)
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
        }, 800)
    }
}
