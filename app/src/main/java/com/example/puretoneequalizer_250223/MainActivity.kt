package com.example.puretoneequalizer_250223

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 載入 activity_main.xml 介面

        findViewById<Button>(R.id.button_equalizer).setOnClickListener {
            val intent = Intent(this, EqualizerActivity::class.java)

            // 產生檔名
            val currentTime = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date()) // 使用 SimpleDateFormat 格式化時間
            val filename = "${currentTime}_Customize"

            intent.putExtra("FILENAME", filename) // 將檔名放入 Intent
            startActivity(intent)
        }

        val equalizerButton = findViewById<Button>(R.id.button_equalizer)
        equalizerButton.setOnClickListener {
            // 啟動 EqualizerActivity，並傳遞檔名資訊 (例如預設檔名 "Default_Filename")
            val intent = Intent(this, EqualizerActivity::class.java)
            intent.putExtra("FILENAME", "Default_Filename") // 傳遞預設檔名
            startActivity(intent)
        }

        // ... 其他按鈕的點擊事件處理 ...
    }
}
