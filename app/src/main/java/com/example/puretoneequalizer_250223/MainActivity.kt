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
            val filename = "${currentTime}_PureTone_"

            intent.putExtra("FILENAME", filename) // 將檔名放入 Intent
            startActivity(intent)
        }

        // 'PSAP' 按鈕並設定點擊事件監聽器
        findViewById<Button>(R.id.button_psap).setOnClickListener { //  <--  找到 button_psap
            val intent = Intent(this, PsapActivity::class.java) //  <--  建立啟動 PsapActivity 的 Intent
            startActivity(intent) //  <--  啟動 PsapActivity
        }

        // 修改 "Get start" 按鈕的點擊事件
        findViewById<Button>(R.id.button_get_start).setOnClickListener {
            val intent = Intent(this, TestSelectActivity::class.java)
            startActivity(intent)
        }
    // ... 其他按鈕的點擊事件處理 ...
    }
}
