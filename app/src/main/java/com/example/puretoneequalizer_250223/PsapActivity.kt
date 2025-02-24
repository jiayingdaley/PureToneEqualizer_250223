package com.example.puretoneequalizer_250223

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PsapActivity : AppCompatActivity() {

    private lateinit var switchPsap: Switch
    private lateinit var switchMediaAssist: Switch
    private lateinit var seekBarAmplification: SeekBar
    private lateinit var seekBarBalance: SeekBar
    private lateinit var buttonEqualizerAdjust: Button
    private lateinit var buttonPsapOk: Button //  **  宣告 'OK' 按鈕 **

    // ** [重要]  宣告 MediaPlayer 和 Equalizer 物件 (與 EqualizerActivity 共用或重新建立？) **
    private var mediaPlayer: MediaPlayer? = null
    private var equalizerLeft: Equalizer? = null
    private var equalizerRight: Equalizer? = null

    private var isPsapEnabled = false // 追蹤 PSAP 開關狀態
    private var isMediaAssistEnabled = false // 追蹤 Media Assist 開關狀態
    private var currentAmplificationLevel = 50f // 追蹤 Amplification Slider 值 (預設 50)
    private var currentBalanceLevel = 0f // 追蹤 Balance Slider 值 (預設 0)

    private lateinit var prefs: SharedPreferences //  **  新增 SharedPreferences 變數 **


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_psap)

        // 1. 綁定 UI 元素
        switchPsap = findViewById(R.id.switch_psap)
        switchMediaAssist = findViewById(R.id.switch_media_assist)
        seekBarAmplification = findViewById(R.id.seekBar_amplification)
        seekBarBalance = findViewById(R.id.seekBar_balance)
        buttonEqualizerAdjust = findViewById(R.id.button_equalizer_adjust)
        buttonPsapOk = findViewById(R.id.button_psap_ok) //  **  綁定 'OK' 按鈕 **


        // ** [重要]  取得 MediaPlayer 和 Equalizer 物件 (從 Intent 傳遞過來？ 全局變數？ 重新初始化？) **
        // ... (取得 MediaPlayer 和 Equalizer 物件的程式碼) ...
        // ** [修改] 取得從 Intent 傳遞過來的 AudioSessionId **
        val audioSessionId = intent.getIntExtra("AUDIO_SESSION_ID", 0)

        // ** 初始化 SharedPreferences 物件 **
        prefs = getPreferences(Context.MODE_PRIVATE) //  **  初始化 prefs 物件 **

        // ** [修改] 使用 AudioSessionId 重新初始化 Equalizer 物件 (左右耳) **
        try {
            equalizerLeft = Equalizer(0, audioSessionId) // 使用傳遞過來的 AudioSessionId
            equalizerLeft?.enabled = true
            println("Initialized Left Equalizer in PSAP Activity")
        } catch (e: Exception) {
            println("Error initializing Left Equalizer in PSAP Activity: ${e.message}")
            equalizerLeft = null
        }

        try {
            equalizerRight = Equalizer(0, audioSessionId) // 使用傳遞過來的 AudioSessionId
            equalizerRight?.enabled = true
            println("Initialized Right Equalizer in PSAP Activity")
        } catch (e: Exception) {
            println("Error initializing Right Equalizer in PSAP Activity: ${e.message}")
            equalizerRight = null
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.fkj_drops) //  **  [暫時] 建立一個 MediaPlayer 物件，方便測試，之後可能需要根據實際音訊來源調整  **
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()


        // TODO... (各 UI 元素事件監聽器設定 ) ...
        // ** 恢復 UI 狀態從 SharedPreferences **
        isPsapEnabled = prefs.getBoolean("psap_switch_state", false)
        isMediaAssistEnabled = prefs.getBoolean("media_assist_switch_state", false)
        currentAmplificationLevel = prefs.getInt("amplification_level", 50).toFloat()
        // ** [修正] 直接讀取 SeekBar 的 progress 值，預設為中間值 100 **
        val savedBalanceProgress = prefs.getInt("balance_level", 100)
        seekBarBalance.progress = savedBalanceProgress
        currentBalanceLevel = (savedBalanceProgress - 100).toFloat() // 同步更新 currentBalanceLevel

        seekBarAmplification.progress = currentAmplificationLevel.toInt()

        // 2. 設定 PSAP 開關事件監聽器
        switchPsap.setOnCheckedChangeListener { _, isChecked ->
            isPsapEnabled = isChecked // 更新 PSAP 開關狀態
            equalizerLeft?.enabled = isChecked //  **  啟用/停用 左耳 Equalizer **
            equalizerRight?.enabled = isChecked //  **  啟用/停用 右耳 Equalizer **
            println("PSAP Switch: ${if (isChecked) "ON" else "OFF"}") // Log 提示

            // ** 保存 PSAP 開關狀態到 SharedPreferences **
            val editor = prefs.edit() // 取得 SharedPreferences.Editor 物件
            editor.putBoolean("psap_switch_state", isChecked) // 儲存 boolean 值
            editor.apply() //  **  [重要]  使用 apply() 方法異步保存，或使用 commit() 方法同步保存 **
        }
        switchPsap.isChecked = isPsapEnabled //  **  設定 PSAP 開關初始狀態為從 SharedPreferences 讀取的值 **

        // 3. 設定 Media Assist 開關事件監聽器
        switchMediaAssist.setOnCheckedChangeListener { _, isChecked ->
            isMediaAssistEnabled = isChecked // 更新 Media Assist 開關狀態
            println("Media Assist Switch: ${if (isChecked) "ON" else "OFF"}") // Log 提示

            // ** 保存 Media Assist 開關狀態到 SharedPreferences **
            val editor = prefs.edit() // 取得 SharedPreferences.Editor 物件
            editor.putBoolean("media_assist_switch_state", isChecked) // 儲存 boolean 值
            editor.apply() //  **  [重要]  使用 apply() 方法異步保存 **
            // TODO: 實作 Media Assist 背景持續應用等化器效果
        }
        switchMediaAssist.isChecked = isMediaAssistEnabled //  **  設定 Media Assist 開關初始狀態為從 SharedPreferences 讀取的值 **

        // 4. 設定 Amplification SeekBar 事件監聽器
        seekBarAmplification.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentAmplificationLevel = progress.toFloat()
                val volumeLevel = progress.toFloat() / 100f // 將 SeekBar Progress (0-100) 轉換為相對音量值 (0.0-1.0)
                mediaPlayer?.setVolume(volumeLevel, volumeLevel)
                println("Amplification SeekBar Progress: ${progress}, Volume Level: ${volumeLevel}")

                // ** 保存 Amplification SeekBar 值到 SharedPreferences **
                val editor = prefs.edit() // 取得 SharedPreferences.Editor 物件
                editor.putInt("amplification_level", progress) // 儲存 integer 值
                editor.apply() //  **  [重要]  使用 apply() 方法異步保存 **
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 手指開始觸摸 SeekBar 時 (可選)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 手指離開 SeekBar 時 (可選)
            }
        })
        seekBarAmplification.progress = currentAmplificationLevel.toInt() //  **  設定 Amplification SeekBar 初始狀態為從 SharedPreferences 讀取的值 **


        // 5. 設定 Balance SeekBar 事件監聽器
        seekBarBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBalanceLevel = (progress - 100).toFloat() // 更新 Balance SeekBar 值，SeekBar 的 progress 範圍是 0-200，需要轉換回 -100 ~ +100
                val balanceValue = currentBalanceLevel / 100f // 歸一化到 -1.0f ~ +1.0f
                val rightVolume = if (balanceValue > 0) 1f else 1f + balanceValue
                val leftVolume = if (balanceValue < 0) 1f else 1f - balanceValue
                mediaPlayer?.setVolume(leftVolume, rightVolume)
                println("Balance SeekBar Progress: ${progress}, Balance Value: ${balanceValue}, Left Volume: ${leftVolume}, Right Volume: ${rightVolume}")

                // ** 保存 Balance SeekBar 值到 SharedPreferences **
                val editor = prefs.edit() // 取得 SharedPreferences.Editor 物件
                editor.putInt("balance_level", progress) // 儲存 integer 值
                editor.apply() //  **  [重要]  使用 apply() 方法異步保存 **
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 手指開始觸摸 SeekBar 時 (可選)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 手指離開 SeekBar 時 (可選)
            }
        })

        // 6. 設定 Equalizer Adjust 按鈕事件監聽器
        findViewById<Button>(R.id.button_equalizer_adjust).setOnClickListener {
            val currentTime = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val filename = "${currentTime}_PureTone_" // 產生檔名

            val intent = Intent(this, EqualizerActivity::class.java)
            intent.putExtra("FILENAME", filename) // 將檔名放入 Intent
            startActivity(intent)
        }

        // 7. 設定 'OK' 按鈕事件監聽器  ** [新增] **
        buttonPsapOk.setOnClickListener {
            // 跳轉回 MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP //  **  [建議]  清除 Activity Stack，避免重複建立 MainActivity
            startActivity(intent)
            finish() //  **  [建議]  關閉 PsapActivity，釋放資源
        }


    }
}