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

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val text = findViewById<TextView>(R.id.splash_text)

        // Fade in + scale animation
        logo.alpha = 0f
        text.alpha = 0f
        logo.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(800).withEndAction {
            logo.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
        }.start()
        text.animate().alpha(1f).setStartDelay(500).setDuration(600).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1500)
    }
}
