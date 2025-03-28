package com.example.puretoneequalizer_250223

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class TestSelectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_select)

        val pureToneTestButton = findViewById<Button>(R.id.button_pure_tone_test)
        val speechAudiometryButton = findViewById<Button>(R.id.button_speech_audiometry)

        // 設定 "Pure-Tone Test" 按鈕的樣式和點擊事件
        pureToneTestButton.apply {
            setBackgroundColor(Color.rgb(70, 147, 211)) // R70 G147 B211
            setTextColor(Color.WHITE)
            setOnClickListener {
                showPureToneAudiogramDialog()
            }
        }

        // 設定 "Speech Audiometry" 按鈕的樣式和點擊事件
        speechAudiometryButton.apply {
            setBackgroundColor(Color.rgb(234, 135, 69)) // R234 G135 B69
            setTextColor(Color.WHITE)
            setOnClickListener {
                // TODO: 導向 Speech Audiometry 頁面 (這部分先略過)
            }
        }
    }

    private fun showPureToneAudiogramDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Do you have a pure-tone audiogram?")

        builder.setPositiveButton("No") { dialog, which ->
            val intent = Intent(this, SelectEarActivity::class.java)
            startActivity(intent)
        }

        builder.setNegativeButton("Yes, and upload") { dialog, which ->
            // TODO: 導向 Pure-tone test result upload 頁面 (這部分先略過)
        }

        builder.show()
    }
}