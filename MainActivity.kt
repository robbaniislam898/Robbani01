package com.example.imoautoreply

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enabled = findViewById<Switch>(R.id.enabledSwitch)
        val url = findViewById<EditText>(R.id.serverUrl)
        val pkg = findViewById<EditText>(R.id.packageFilter)
        val instructions = findViewById<EditText>(R.id.instructions)
        val fallback = findViewById<EditText>(R.id.fallbackReply)
        val status = findViewById<TextView>(R.id.statusText)

        enabled.isChecked = prefs.getBoolean("enabled", false)
        url.setText(prefs.getString("server_url", ""))
        pkg.setText(prefs.getString("package_filter", "imo"))
        instructions.setText(prefs.getString("instructions",
            "বাংলায় ভদ্র, সংক্ষিপ্ত ও স্বাভাবিক উত্তর দেবে।"))
        fallback.setText(prefs.getString("fallback", 
            "আসসালামু আলাইকুম, আপনার মেসেজ পেয়েছি। একটু পরে রিপ্লাই দিচ্ছি।"))

        enabled.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("enabled", checked).apply()
            status.text = if (checked) "Auto Reply চালু।" else "Auto Reply বন্ধ।"
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            prefs.edit()
                .putString("server_url", url.text.toString().trim())
                .putString("package_filter", pkg.text.toString().trim())
                .putString("instructions", instructions.text.toString().trim())
                .putString("fallback", fallback.text.toString().trim())
                .apply()
            Toast.makeText(this, "সেটিংস সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.permissionButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
