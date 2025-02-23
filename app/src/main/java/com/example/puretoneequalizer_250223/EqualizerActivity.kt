package com.example.puretoneequalizer_250223

import android.app.Activity
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import kotlin.text.toDouble


class EqualizerActivity : AppCompatActivity(), Slider.OnSliderTouchListener {

    private lateinit var textViewFilename: TextView
    private lateinit var imageButtonFilenameEdit: ImageButton
    private lateinit var linearLayoutFrequencySeekbarContainer: LinearLayout
    private lateinit var textViewLeftEar: TextView
    private lateinit var textViewRightEar: TextView

    private val frequencyGainMap = HashMap<Double, Float>()

    val frequencies = listOf(31.25, 63, 125, 250, 500, 750, 1000, 1500, 2000, 3000, 4000, 6000, 8000, 10000, 12000, 16000) // 使用完整頻率列表
    private val displayFrequencies = frequencies // 顯示所有頻率


    private var audioTrack: AudioTrack? = null  // AudioTrack 物件
    private var currentFrequency: Double = 0.0 // 目前純音頻率
    private var currentAmplitude: Float = 0f    // 目前純音音量 (線性振幅)

    private var mediaPlayer: MediaPlayer? = null // MediaPlayer 物件
    private var equalizer: Equalizer? = null     // Equalizer 物件
    private var eqBandLevels: ShortArray? = null  // 儲存 Equalizer 各頻段的 Level 值
    private var numberOfEqBands = 0               // Equalizer 頻段數量

    private lateinit var fileLoadActivityResultLauncher: ActivityResultLauncher<Intent> //  <--  新增 ActivityResultLauncher


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)

        // 1. 綁定 View 元件
        textViewFilename = findViewById(R.id.textView_filename)
        imageButtonFilenameEdit = findViewById(R.id.imageButton_filename_edit)
        linearLayoutFrequencySeekbarContainer = findViewById(R.id.linearLayout_frequency_seekbar_container)
        textViewLeftEar = findViewById(R.id.textView_left_ear)
        textViewRightEar = findViewById(R.id.textView_right_ear)


        // ** 初始化 MediaPlayer 物件 **
        mediaPlayer = MediaPlayer()

        // ** 初始化 Equalizer 物件 **
//        try {
//            equalizer = Equalizer(0, mediaPlayer?.audioSessionId ?: 0) // 優先使用 MediaPlayer 的 AudioSessionId
//            equalizer?.enabled = true // 啟用 Equalizer 效果
//
//            numberOfEqBands = equalizer?.numberOfBands?.toInt() ?: 0 // 取得 Equalizer 頻段數量
//            eqBandLevels = ShortArray(numberOfEqBands) // 初始化 eqBandLevels 陣列
//            val eqBandLevelRange = equalizer?.bandLevelRange // 取得 Equalizer 頻段 Level 值的範圍 (通常是 < -15dB to > +15dB)
//            println("Equalizer Band Level Range: ${eqBandLevelRange?.get(0)} dB to ${eqBandLevelRange?.get(1)} dB") // 輸出 Level 範圍
//
//            // 輸出 Equalizer 支援的頻率範圍
//            for (i in 0 until numberOfEqBands) {
//                val centerFreq = equalizer?.getCenterFrequency(i.toShort())?.toDouble() ?: 0.0
//                val lowerCutoffFreq = equalizer?.getBandFreqRange(i.toShort())?.get(0)?.toDouble() ?: 0.0
//                val upperCutoffFreq = equalizer?.getBandFreqRange(i.toShort())?.get(1)?.toDouble() ?: 0.0
//                println("Equalizer Band ${i+1}: Center Frequency = ${centerFreq/1000} kHz, Range = ${lowerCutoffFreq/1000} kHz - ${upperCutoffFreq/1000} kHz")
//            }
//
//
//        } catch (e: Exception) {
//            println("Error initializing Equalizer: ${e.message}")
//            //  **  [錯誤處理]  如果 Equalizer 初始化失敗，可以禁用等化器功能，並提示使用者  **
//            equalizer = null // 將 equalizer 設為 null，表示等化器不可用
//        }
        try {
            equalizer = Equalizer(0, mediaPlayer?.audioSessionId ?: 0) // 優先使用 MediaPlayer 的 AudioSessionId
            equalizer?.enabled = true // 啟用 Equalizer 效果

            numberOfEqBands = equalizer?.numberOfBands?.toInt() ?: 0 // 取得 Equalizer 頻段數量
            eqBandLevels = ShortArray(numberOfEqBands) // 初始化 eqBandLevels 陣列
            val eqBandLevelRange = equalizer?.bandLevelRange // 取得 Equalizer 頻段 Level 值的範圍 (通常是 < -15dB to > +15dB)
            println("Equalizer Band Level Range: ${eqBandLevelRange?.get(0)} dB to ${eqBandLevelRange?.get(1)} dB") // 輸出 Level 範圍

            // 輸出 Equalizer 支援的頻率範圍
            for (i in 0 until numberOfEqBands) {
                val centerFreq = equalizer?.getCenterFrequency(i.toShort())?.toDouble() ?: 0.0
                val lowerCutoffFreq = equalizer?.getBandFreqRange(i.toShort())?.get(0)?.toDouble() ?: 0.0
                val upperCutoffFreq = equalizer?.getBandFreqRange(i.toShort())?.get(1)?.toDouble() ?: 0.0
                println("Equalizer Band ${i+1}: Center Frequency = ${centerFreq/1000} kHz, Range = ${lowerCutoffFreq/1000} kHz - ${upperCutoffFreq/1000} kHz")
            }

        } catch (e: Exception) {
            println("Error initializing Equalizer: ${e.message}")
            //  **  [錯誤處理]  如果 Equalizer 初始化失敗，可以禁用等化器功能，並提示使用者  **
            equalizer = null // 將 equalizer 設為 null，表示等化器不可用
        }

        // ** 初始化 ActivityResultLauncher，用於檔案選擇 **
        fileLoadActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val audioUri = data?.data // 取得選取檔案的 Uri
                if (audioUri != null) {
                    loadAudioFile(audioUri) //  <--  調用 loadAudioFile 函數 (稍後建立)
                } else {
                    println("No audio file selected.") //  Log 提示沒有選擇檔案
                }
            } else {
                println("File selection cancelled.") // Log 提示檔案選擇取消
            }
        }


        // 2. 動態建立頻率 SeekBar (Material Slider)

        displayFrequencies.forEach { frequency ->
            // 建立垂直 LinearLayout 作為頻率標籤和 Slider 的容器
            val frequencyColumnLayout = LinearLayout(this)
            frequencyColumnLayout.orientation = LinearLayout.VERTICAL
            frequencyColumnLayout.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            frequencyColumnLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL

            // 建立頻率標籤 TextView
            val frequencyTextView = TextView(this)
            frequencyTextView.text = frequency.toString()
            frequencyTextView.textSize = 14f
            frequencyTextView.gravity = android.view.Gravity.CENTER_HORIZONTAL
            frequencyColumnLayout.addView(frequencyTextView)

            // 建立 Material Slider
            val slider = Slider(this)
            val sliderLayoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            sliderLayoutParams.height = resources.getDimensionPixelSize(R.dimen.seekbar_height)
            sliderLayoutParams.width = resources.getDimensionPixelSize(R.dimen.seekbar_width) // 使用 dimens.xml 中的寬度 (粗細)
            sliderLayoutParams.setMargins(10, 8, 10, 0)
            slider.layoutParams = sliderLayoutParams
            slider.rotation = -90f // 旋轉 -90 度
            slider.valueFrom = -15f // SeekBar 最小值 (對應 -15dB)
            slider.valueTo = 15f // SeekBar 最大值 (對應 +15dB)
            slider.stepSize = 5f // SeekBar 步進值 (5dB per step), creates 7 ticks
            slider.value = 0f // SeekBar 初始值 (對應 0dB)
            slider.isTickVisible = true // 顯示刻度

            slider.tag = frequency.toString() // 設定 tag

            slider.addOnChangeListener { slider, value, fromUser ->
                // 取得頻率 (從 Slider 的 tag 取得)
                val currentFrequency = slider.tag.toString().toDouble()
                // value 是 Float 類型，代表 Slider 的值 (dB 值，範圍 -15f to +15f, 中心點 0f)
                // 將 頻率 和 Value 值 存儲到 frequencyGainMap 中
                frequencyGainMap[currentFrequency] = value
                //  **  [重要]  在這裡可以調用音訊處理函數，將 frequencyGainMap 傳遞過去，應用等化器效果  **
                applyEqualizerGains(frequencyGainMap) //  <--  調用音訊處理函數
            }

            //  **  設定 OnSliderTouchListener  **
            slider.addOnSliderTouchListener(this@EqualizerActivity) //  <--  設定 OnSliderTouchListener 為 EqualizerActivity 自身

            frequencyColumnLayout.addView(slider)
            linearLayoutFrequencySeekbarContainer.addView(frequencyColumnLayout)
        }

        // 3. 設定左右耳點擊事件
        textViewLeftEar.setOnClickListener {
            textViewLeftEar.isSelected = true
            textViewRightEar.isSelected = false
            // TODO: 切換到左耳等化器設定
        }

        textViewRightEar.setOnClickListener {
            textViewRightEar.isSelected = true
            textViewLeftEar.isSelected = false
            // TODO: 切換到右耳等化器設定
        }
        // 預設選中左耳
        textViewLeftEar.isSelected = true


        // 4. 設定檔案名稱顯示 (從 Intent 中取得檔名)
        val filenameFromIntent = intent.getStringExtra("FILENAME") // 從 Intent 中取得檔名
        val filename = filenameFromIntent ?: "Default_Filename" // 如果 Intent 中沒有檔名，使用預設檔名
        textViewFilename.text = filename

        // 5. 設定按鈕點擊事件
        findViewById<Button>(R.id.button_reset).setOnClickListener {
            showResetConfirmationDialog()
        }

        findViewById<Button>(R.id.button_save_apply).setOnClickListener {
            // TODO: 儲存並應用等化器設定，跳轉到 PSAP 畫面
        }

        // 6. 設定編輯檔名按鈕事件 (修改為檔案載入按鈕事件)
        imageButtonFilenameEdit.setOnClickListener {
            //  **  啟動檔案選擇 Intent  **
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*" //  設定檔案類型為音訊檔案
            }
            fileLoadActivityResultLauncher.launch(intent) //  啟動檔案選擇 Activity，並等待返回結果
        }
    }

    //  **  實作 OnSliderTouchListener 的兩個方法  **
    override fun onStartTrackingTouch(slider: Slider) {
        // 手指開始觸摸 Slider 時，停止播放純音
        stopPureTone()
    }

    override fun onStopTrackingTouch(slider: Slider) {
        // 手指離開 Slider 時，播放對應頻率和音量的純音
        val frequency = slider.tag.toString().toDouble()
        //  **  [重要]  將 Slider 的 Value 值 (dB) 轉換為 線性振幅  **
        val amplitude = dBToAmplitude(slider.value) //  <--  調用 dBToAmplitude 函數
        playPureTone(frequency, amplitude)
    }

    private fun findNearestFrequency(targetFrequency: Double, frequencyList: List<Double>): Double {
        //  在 frequencyList 中查找最接近 targetFrequency 的頻率值
        var nearestFrequency = frequencyList[0]
        var minDifference = Math.abs(targetFrequency - nearestFrequency)

        for (frequency in frequencyList) {
            val difference = Math.abs(targetFrequency - frequency)
            if (difference < minDifference) {
                minDifference = difference
                nearestFrequency = frequency
            }
        }
        return nearestFrequency
    }


    private fun loadAudioFile(audioUri: Uri) {
        try {
            mediaPlayer?.reset() //  **  [重要]  重置 MediaPlayer，釋放之前的資源 **
            mediaPlayer?.setDataSource(this, audioUri) // 設定音訊數據來源為選取的 Uri
            mediaPlayer?.prepare() //  **  [重要]  準備 MediaPlayer，進行同步或異步準備 **
            mediaPlayer?.start() // 開始播放音訊

            textViewFilename.text = getFileNameFromUri(audioUri) ?: "Unknown Filename" // 更新檔名 TextView

            println("Audio file loaded and started playing: $audioUri") // Log 提示載入成功

        } catch (e: Exception) {
            println("Error loading audio file: ${e.message}") // Log 輸出錯誤訊息
            //  **  [錯誤處理]  載入失敗，可以提示使用者，例如使用 Toast 或 Snackbar  **
            Toast.makeText(this, "Error loading audio file", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getFileNameFromUri(uri: Uri): String? {
        //  輔助函數，從 Uri 中取得檔名 (簡化版，可能需要根據實際需求完善)
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }
        return fileName
    }

    private fun dBToAmplitude(dB: Float): Float {
//        // 將 dB 值轉換為線性振幅 (範圍 0.0 ~ 1.0) 此可能有Audio Clipping
//        return Math.pow(10.0, (dB / 20).toDouble()).toFloat()
        // 振幅限制 (Amplitude Clamping): 將 dB 值轉換為線性振幅 (範圍 0.0 ~ 1.0)，並限制振幅值在 0.0 到 1.0 之間
        return Math.pow(10.0, (dB / 20).toDouble()).toFloat().coerceIn(0f, 1f) //  <--  新增 .coerceIn(0f, 1f)

    }

    private fun playPureTone(frequency: Double, amplitude: Float) {
        // stopPureTone() // 停止任何正在播放的純音  **  [移除]  不再每次都停止和釋放 AudioTrack **

        currentFrequency = frequency
        currentAmplitude = amplitude

        val sampleRate = 44100 // 常用取樣率
        val duration = 0.2 // 純音持續時間 (例如 0.1 秒)
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        val buffer = ShortArray(numSamples)

        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)


        // 產生正弦波樣本
        for (i in 0 until numSamples) {
            samples[i] = amplitude * Math.sin(2.0 * Math.PI * frequency * i / sampleRate)
            buffer[i] = (samples[i] * Short.MAX_VALUE).toInt().toShort() // 轉換為 Short
        }

        // 建立 AudioTrack 物件
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize, // 使用最小緩衝區大小
            AudioTrack.MODE_STREAM // 改為 STREAM 模式，更適合頻繁更新數據
        )

        //  **  重複使用已建立的 audioTrack 物件，只更新音訊數據 **
        audioTrack?.stop() //  **  先停止播放 (如果正在播放)  **
        audioTrack?.flush() //  **  清空暫存區 **
        audioTrack?.write(buffer, 0, buffer.size) //  **  寫入新的音訊數據 **

        audioTrack?.play() //  **  重新播放 **
    }

    private fun stopPureTone() {
        audioTrack?.stop()
        //audioTrack?.release() // 釋放 AudioTrack 資源
        //audioTrack = null
    }


    private fun applyEqualizerGains(frequencyGainMap: HashMap<Double, Float>) {
        //  **  [TODO]  在這裡實作音訊處理邏輯，根據 frequencyGainMap 設定各頻率的增益  **
        if (equalizer == null) {
            println("Equalizer not initialized, skipping gain application.") // 如果 Equalizer 未初始化，輸出 Log 並直接返回
            return
        }

        println("Applying Equalizer Gains:")

        for (i in 0 until numberOfEqBands) {
            val bandIndex = i.toShort() //  <--  Explicitly cast i to Short *before* method call
            val centerFrequency = equalizer?.getCenterFrequency(bandIndex)?.toDouble() ?: 0.0 // Use bandIndex
            val lowerCutoffFreq = equalizer?.getBandFreqRange(bandIndex)?.get(0)?.toDouble() ?: 0.0 // Use bandIndex
            val upperCutoffFreq = equalizer?.getBandFreqRange(bandIndex)?.get(1)?.toDouble() ?: 0.0 // Use bandIndex
            println("Equalizer Band ${i + 1}: Center Frequency = ${centerFrequency / 1000} kHz, Range = ${lowerCutoffFreq / 1000} kHz - ${upperCutoffFreq / 1000} kHz")


            // ** 查找最接近的 Slider 頻率 **
            val nearestSliderFrequency = findNearestFrequency(centerFrequency, displayFrequencies) //  <--  調用 findNearestFrequency 函數

            val gainFromSlider = frequencyGainMap[nearestSliderFrequency] ?: 0f // 從 frequencyGainMap 中取得對應 Slider 的增益值，如果找不到，則使用 0dB

            // **  [重要]  將 Slider 的增益值 (dB) 轉換為 Equalizer 可接受的 Level 值 (Short)  **
            //  **  Equalizer 的 Level 值通常是 -1500mB 到 +1500mB (毫貝), 需要將 dB 值 * 100  **
            val eqLevel = (gainFromSlider * 100).toInt().toShort() // 將 Slider 的 dB 值 * 100 轉換為 Short

            eqBandLevels?.set(i, eqLevel) //  儲存 Level 值到 eqBandLevels 陣列
            equalizer?.setBandLevel(bandIndex, eqLevel) //  ** 設定 Equalizer 頻段的 Level 值 ** // Use bandIndex

            println(
                "  Band ${i + 1} (Center Frequency: ${centerFrequency / 1000} kHz), Slider Frequency: ${nearestSliderFrequency} Hz, Slider Gain: ${gainFromSlider} dB, Equalizer Level: ${eqLevel / 100.0} dB"
            ) // 輸出 Log
        }
        println("--------------------")
        }

    override fun onDestroy() {
        super.onDestroy()
        // ** 在 Activity 銷毀時，釋放 AudioTrack 資源 **
        audioTrack?.release()
        audioTrack = null // 釋放後設為 null，避免 dangling reference
    }

    private fun showResetConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Confirm reset?")
            .setPositiveButton("Yes") { dialog, id ->
                // TODO: 執行重置操作 (回到暫存前的狀態)
                dialog.dismiss() // 關閉對話框
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss() // 關閉對話框
            }
        val dialog = builder.create()
        dialog.show()
        // 修改 Yes 按鈕顏色
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.button_reset_color))
        // 修改 No 按鈕顏色
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.button_equalizer_color))
    }
}