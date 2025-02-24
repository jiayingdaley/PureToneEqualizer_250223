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
import androidx.core.view.children
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// ** 定義 EarType Enum 類別 **
enum class EarType {
    Left,
    Right
}

class EqualizerActivity : AppCompatActivity(), Slider.OnSliderTouchListener {

    private lateinit var textViewFilename: TextView
    private lateinit var imageButtonFilenameEdit: ImageButton
    private lateinit var imageButtonFileLoad: ImageButton
    private lateinit var linearLayoutFrequencySliderContainer: LinearLayout
    private lateinit var textViewLeftEar: TextView
    private lateinit var textViewRightEar: TextView

    val frequencies: List<Double> = listOf(
        31.25,
        63.0,
        125.0,
        250.0,
        500.0,
        750.0,
        1000.0,
        1500.0,
        2000.0,
        3000.0,
        4000.0,
        6000.0,
        8000.0,
        10000.0,
        12000.0,
        16000.0
    ) // 使用完整頻率列表
    private val displayFrequencies: List<Double> = frequencies // 顯示所有頻率


    private var audioTrack: AudioTrack? = null  // AudioTrack 物件
    private var currentFrequency: Double = 0.0 // 目前純音頻率
    private var currentAmplitude: Float = 0f    // 目前純音音量 (線性振幅)

    private var mediaPlayer: MediaPlayer? = null // MediaPlayer 物件

    private var equalizerLeft: Equalizer? = null  //  **  新增 左耳 Equalizer 物件 **
    private var equalizerRight: Equalizer? = null //  **  新增 右耳 Equalizer 物件 **
    private var eqBandLevels: ShortArray? = null
    private var numberOfEqBands = 0               // Equalizer 頻段數量

    private val frequencyGainMapLeft = HashMap<Double, Float>() //  **  新增 左耳 frequencyGainMap **
    private val frequencyGainMapRight = HashMap<Double, Float>() //  **  新增 右耳 frequencyGainMap **

    private val sliderValueMapLeft = HashMap<Double, Float>() //  **  新增 左耳 Slider Value Map **
    private val sliderValueMapRight = HashMap<Double, Float>() //  **  新增 右耳 Slider Value Map **

    private var currentSelectedEar: EarType = EarType.Left //  **  新增 目前選中的耳朵變數，預設為左耳 **

    private lateinit var fileLoadActivityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var originalFilenamePrefix: String // 保存原始檔名前綴

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)

        // 1. 綁定 View 元件
        textViewFilename = findViewById(R.id.textView_filename)
        imageButtonFilenameEdit = findViewById(R.id.imageButton_filename_edit)
        imageButtonFileLoad = findViewById(R.id.imageButton_file_load)
        linearLayoutFrequencySliderContainer = findViewById(R.id.linearLayout_frequency_slider_container)
        textViewLeftEar = findViewById(R.id.textView_left_ear)
        textViewRightEar = findViewById(R.id.textView_right_ear)

        // ** 初始化 MediaPlayer 物件 **
        mediaPlayer = MediaPlayer()

        // ** 初始化 Equalizer 物件 (左耳) **
        try {
            equalizerLeft = Equalizer(0, mediaPlayer?.audioSessionId ?: 0)// 優先使用 MediaPlayer 的 AudioSessionId
            equalizerLeft?.enabled = true  // 啟用 Equalizer 效果
            // ... (左耳 Equalizer 初始化程式碼 - 與之前 equalizer 初始化程式碼相同) ...
            println("Initialized Left Equalizer") // Log 提示
        } catch (e: Exception) {
            println("Error initializing Left Equalizer: ${e.message}")
            equalizerLeft = null
        }

        // ** 初始化 Equalizer 物件 (右耳) **
        try {
            equalizerRight = Equalizer(0, mediaPlayer?.audioSessionId ?: 0)
            equalizerRight?.enabled = true
            // ... (右耳 Equalizer 初始化程式碼 - 與左耳相同，可以複製左耳的初始化程式碼) ...
            println("Initialized Right Equalizer") // Log 提示
        } catch (e: Exception) {
            println("Error initializing Right Equalizer: ${e.message}")
            equalizerRight = null
        }

        // ** 初始化 ActivityResultLauncher，用於檔案選擇 **
        fileLoadActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val audioUri = data?.data // 取得選取檔案的 Uri
                    if (audioUri != null) {
                        loadAudioFile(audioUri) //  <--  調用 loadAudioFile 函數
                    } else {
                        println("No audio file selected.") //  Log 提示沒有選擇檔案
                    }
                } else {
                    println("File selection cancelled.") // Log 提示檔案選擇取消
                }
            }


        // 2. 動態建立頻率 Slider (Material Slider)
        displayFrequencies.forEach { frequency ->
            // 建立垂直 LinearLayout 作為頻率標籤和 Slider 的容器
            val frequencyColumnLayout = LinearLayout(this)
            frequencyColumnLayout.orientation = LinearLayout.VERTICAL
            frequencyColumnLayout.layoutParams = LinearLayout.LayoutParams(
                //ViewGroup.LayoutParams.WRAP_CONTENT
                200,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            frequencyColumnLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL

            // 建立頻率標籤 TextView
            val frequencyTextView = TextView(this)
            frequencyTextView.text = frequency.toString()
            frequencyTextView.textSize = 16f
            frequencyTextView.gravity = android.view.Gravity.CENTER_HORIZONTAL
            frequencyColumnLayout.addView(frequencyTextView)

            // 建立 Material Slider
            val slider = Slider(this)
            val sliderLayoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            sliderLayoutParams.height = resources.getDimensionPixelSize(R.dimen.slider_height)
            sliderLayoutParams.width =  resources.getDimensionPixelSize(R.dimen.slider_width) // 使用 dimens.xml 中的寬度 (粗細)

            val marginLeft = resources.getDimensionPixelSize(R.dimen.slider_margin_left)
            val marginTop = resources.getDimensionPixelSize(R.dimen.slider_margin_top)
            val marginRight = resources.getDimensionPixelSize(R.dimen.slider_margin_right)
            val marginBottom = resources.getDimensionPixelSize(R.dimen.slider_margin_bottom)
            sliderLayoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom)

            slider.layoutParams = sliderLayoutParams
            slider.rotation = -90f // 旋轉 -90 度
            slider.valueFrom = -15f // Slider 最小值 (對應 -15dB)
            slider.valueTo = 15f // Slider 最大值 (對應 +15dB)
            slider.stepSize = 1f // Slider 步進值 (1dB per step), creates 7 ticks
            slider.value = 0f // Slider 初始值 (對應 0dB)
            slider.isTickVisible = true // 顯示刻度

            slider.tag = frequency.toString() // 設定 tag

            // ** 初始化 sliderValueMapLeft 和 sliderValueMapRight 的預設值 **
            sliderValueMapLeft[frequency] = 0f //  **  初始化 左耳 Slider Value 為 0f **
            sliderValueMapRight[frequency] = 0f //  **  初始化 右耳 Slider Value 為 0f **

            slider.addOnChangeListener { slider, value, fromUser ->
                // 取得頻率 (從 Slider 的 tag 取得)
                val currentFrequency = slider.tag.toString().toDouble()
                // value 是 Float 類型，代表 Slider 的值 (dB 值，範圍 -15f to +15f, 中心點 0f)
                // 將 頻率 和 Value 值 儲存到 **對應的** frequencyGainMap 和 sliderValueMap 中
                when (currentSelectedEar) {
                    EarType.Left -> {
                        frequencyGainMapLeft[currentFrequency] = value
                        sliderValueMapLeft[currentFrequency] =
                            value //  **  [新增]  更新 sliderValueMapLeft **
                    }

                    EarType.Right -> {
                        frequencyGainMapRight[currentFrequency] = value
                        sliderValueMapRight[currentFrequency] =
                            value //  **  [新增]  更新 sliderValueMapRight **
                    }
                }
                applyEqualizerGains(HashMap())
            }

            //  **  設定 OnSliderTouchListener  **
            slider.addOnSliderTouchListener(this@EqualizerActivity) //  <--  設定 OnSliderTouchListener 為 EqualizerActivity 自身

            frequencyColumnLayout.addView(slider)
            linearLayoutFrequencySliderContainer.addView(frequencyColumnLayout)
        }

        // 3. 設定左右耳點擊事件
        textViewLeftEar.setOnClickListener {
            textViewLeftEar.isSelected = true
            textViewRightEar.isSelected = false
            currentSelectedEar = EarType.Left //  **  設定 currentSelectedEar 為 Left  **
            applyEqualizerGains(HashMap()) //  **  [修改]  調用 applyEqualizerGains() 函數 **

            // TODO: 切換到左耳等化器設定
            // ** 更新 Slider UI 的 Value 值 (左耳) **
            linearLayoutFrequencySliderContainer.children.forEach { columnLayout -> // 迭代 frequencyColumnLayout (垂直 LinearLayout)
                if (columnLayout is LinearLayout) {
                    val slider =
                        columnLayout.getChildAt(1) as? Slider // 取得 columnLayout 中的第二個 child View (Slider)
                    val frequency = slider?.tag.toString().toDouble() // 從 Slider 的 tag 取得頻率
                    if (slider != null && frequency != null) {
                        slider.value = sliderValueMapLeft[frequency]
                            ?: 0f //  **  設定 Slider 的 Value 值為 sliderValueMapLeft 中儲存的值 **
                    }
                }
            }
        }

        textViewRightEar.setOnClickListener {
            textViewRightEar.isSelected = true
            textViewLeftEar.isSelected = false
            currentSelectedEar = EarType.Right //  **  設定 currentSelectedEar 為 Right  **
            applyEqualizerGains(HashMap()) //  **  [修改]  調用 applyEqualizerGains() 函數 **

            // TODO: 切換到右耳等化器設定
            // ** 更新 Slider UI 的 Value 值 (右耳) **
            linearLayoutFrequencySliderContainer.children.forEach { columnLayout -> // 迭代 frequencyColumnLayout (垂直 LinearLayout)
                if (columnLayout is LinearLayout) {
                    val slider =
                        columnLayout.getChildAt(1) as? Slider // 取得 columnLayout 中的第二個 child View (Slider)
                    val frequency = slider?.tag.toString().toDouble() // 從 Slider 的 tag 取得頻率
                    if (slider != null && frequency != null) {
                        slider.value = sliderValueMapRight[frequency]
                            ?: 0f //  **  設定 Slider 的 Value 值為 sliderValueMapRight 中儲存的值 **
                    }
                }
            }
        }
        // 預設選中左耳
        currentSelectedEar = EarType.Left //  **  預設選中左耳 **
        textViewLeftEar.isSelected = true


        // 4. 設定檔案名稱顯示 (從 Intent 中取得檔名)
        val filenameFromIntent = intent.getStringExtra("FILENAME") // 從 Intent 中取得檔名
        val filename = filenameFromIntent // 如果 Intent 中沒有檔名，filename 將為 null
        textViewFilename.text = filename ?: "" // 如果 filename 為 null，則顯示空字串

        // ** 保存原始檔名前綴 **
        val lastUnderscoreIndex = filename?.lastIndexOf('_') ?: -1
        originalFilenamePrefix = if (lastUnderscoreIndex != -1) {
            filename?.substring(0, lastUnderscoreIndex + 1) ?: "" // 包括底線
        } else {
            filename ?: "" // 如果沒有底線，則前綴為整個檔名或空字串
        }

        // 5. 設定按鈕點擊事件
        findViewById<Button>(R.id.button_reset).setOnClickListener {
            showResetConfirmationDialog()
        }

        findViewById<Button>(R.id.button_save_apply).setOnClickListener {
            val currentFilename = textViewFilename.text.toString()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Save and Apply")
            builder.setMessage("Are you sure you want to save and apply the current equalizer settings to the file: \n$currentFilename?")

            builder.setPositiveButton("OK") { dialog, which ->
                saveEqualizerSettings(currentFilename) // 調用儲存函數
                navigateToPsapActivity() // 跳轉到 PSAP 畫面
            }

            builder.setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }

            builder.show()
        }

        // 6. 設定編輯檔名按鈕事件
        imageButtonFilenameEdit.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Edit Filename")

            val input = android.widget.EditText(this)
            // ** 只顯示可編輯的部分 **
            val currentFilename = textViewFilename.text.toString()
            if (currentFilename.startsWith(originalFilenamePrefix)) {
                input.setText(currentFilename.substring(originalFilenamePrefix.length))
            } else {
                input.setText(currentFilename) // 如果格式不符，則顯示完整檔名 (作為備份)
            }
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, which ->
                val newSuffix = input.text.toString()
                textViewFilename.text = originalFilenamePrefix + newSuffix // 組合原始前綴和新的後綴
            }

            builder.setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            builder.show()
        }

        // 7. 設定檔案載入按鈕事件 (原先的 6)
        imageButtonFileLoad.setOnClickListener {
            //  ** 啟動檔案選擇 Intent  **
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain" //  設定檔案類型為所有檔案，後續需要根據您的等化器設定檔案類型調整
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


    private fun saveEqualizerSettings(filename: String) {
        val file = File(filesDir, "$filename.csv") // 使用 .csv 作為等化器設定檔的副檔名
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            val stringBuilder = StringBuilder()

            // 儲存左耳設定
            frequencyGainMapLeft.forEach { (frequency, gain) ->
                stringBuilder.append("$frequency,$gain,${frequencyGainMapRight[frequency] ?: 0f}\n")
            }

            fileOutputStream.write(stringBuilder.toString().toByteArray())
            Toast.makeText(this, "Equalizer settings have been saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            println("Equalizer settings saved to: ${file.absolutePath}")

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save equalizer settings.", Toast.LENGTH_SHORT).show()
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
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
            val fileName = getFileNameFromUri(audioUri) ?: "Unknown Filename"

            // ** 檢查檔名是否包含 "PureTone" 或 "SpeechAudiometry" **
            if (!fileName.contains("PureTone") && !fileName.contains("SpeechAudiometry")) {
                Toast.makeText(this, "The filename must include “PureTone” or “SpeechAudiometry”", Toast.LENGTH_LONG).show()
                println("Error: Filename does not contain PureTone or SpeechAudiometry: $fileName")
                return // 終止載入
            }

            val file = File(filesDir, "$fileName.csv")
            if (file.exists()) {
                val inputStream = file.inputStream()
                inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(",")
                        if (parts.size == 3) {
                            try {
                                val frequency = parts[0].toDouble()
                                val leftGain = parts[1].toFloat()
                                val rightGain = parts[2].toFloat()

                                frequencyGainMapLeft[frequency] = leftGain
                                frequencyGainMapRight[frequency] = rightGain
                                sliderValueMapLeft[frequency] = leftGain
                                sliderValueMapRight[frequency] = rightGain

                                println("Loaded setting from file: Frequency=$frequency, LeftGain=$leftGain, RightGain=$rightGain")
                            } catch (e: NumberFormatException) {
                                println("Skipping invalid line: $line")
                            }
                        } else {
                            println("Skipping malformed line: $line")
                        }
                    }
                }
                inputStream.close()
                textViewFilename.text = fileName
                updateSlidersFromMap()
                applyEqualizerGains(HashMap())
                Toast.makeText(this, "Equalizer Settings Loaded Successfully", Toast.LENGTH_SHORT).show()
                println("Equalizer settings loaded from: ${file.absolutePath}")
            } else {
                Toast.makeText(this, "Cannot find the corresponding equalizer settings file", Toast.LENGTH_SHORT).show()
                println("Equalizer settings file not found: ${file.absolutePath}")
            }

        } catch (e: Exception) {
            println("Error loading equalizer settings file: ${e.message}")
            Toast.makeText(this, "Failed to Load Equalizer Settings File", Toast.LENGTH_SHORT).show()
        } finally {
            stopPureTone()
            mediaPlayer?.reset()
        }

//            // ** [修改] 載入等化器設定檔案 **
//            val inputStream = contentResolver.openInputStream(audioUri)
//            inputStream?.bufferedReader()?.useLines { lines ->
//                lines.forEach { line ->
//                    val parts = line.split(",")
//                    if (parts.size == 3) {
//                        try {
//                            val frequency = parts[0].toDouble()
//                            val leftGain = parts[1].toFloat()
//                            val rightGain = parts[2].toFloat()
//
//                            // 更新對應的 Map
//                            frequencyGainMapLeft[frequency] = leftGain
//                            frequencyGainMapRight[frequency] = rightGain
//                            sliderValueMapLeft[frequency] = leftGain
//                            sliderValueMapRight[frequency] = rightGain
//
//                            println("Loaded setting: Frequency=$frequency, LeftGain=$leftGain, RightGain=$rightGain")
//                        } catch (e: NumberFormatException) {
//                            println("Skipping invalid line: $line")
//                        }
//                    } else {
//                        println("Skipping malformed line: $line")
//                    }
//                }
//            }
//            inputStream?.close()
//
//            textViewFilename.text = fileName // 更新檔名 TextView
//
//            // ** [修改] 載入設定後，更新 UI **
//            updateSlidersFromMap()
//            applyEqualizerGains(HashMap()) // 重新應用等化器增益
//
//            Toast.makeText(this, "Equalizer settings loaded successfully", Toast.LENGTH_SHORT).show()
//            println("Equalizer settings loaded from: $audioUri") // Log 提示載入成功
//
//        } catch (e: Exception) {
//            println("Error loading equalizer settings file: ${e.message}") // Log 輸出錯誤訊息
//            Toast.makeText(this, "Failed to load equalizer settings file", Toast.LENGTH_SHORT).show()
//        } finally {
//            // ** [移除] 停止和重置 MediaPlayer，因為我們現在載入的是設定檔案 **
//            stopPureTone()
//            mediaPlayer?.reset()
//        }
    }

    private fun updateSlidersFromMap() {
        linearLayoutFrequencySliderContainer.children.forEach { columnLayout ->
            if (columnLayout is LinearLayout) {
                val slider = columnLayout.getChildAt(1) as? Slider
                val frequency = slider?.tag.toString().toDouble()
                if (slider != null && frequency != null) {
                    slider.value = when (currentSelectedEar) {
                        EarType.Left -> sliderValueMapLeft[frequency] ?: 0f
                        EarType.Right -> sliderValueMapRight[frequency] ?: 0f
                    }
                }
            }
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
        return Math.pow(10.0, (dB / 20).toDouble()).toFloat()
            .coerceIn(0f, 1f) //  <--  新增 .coerceIn(0f, 1f)

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

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )


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


        println("Pure Tone Amplitude: $amplitude")
        println("Buffer Value Example: ${buffer.firstOrNull()}")
    }

    private fun stopPureTone() {
        audioTrack?.stop()
        //audioTrack?.release() // 釋放 AudioTrack 資源
        //audioTrack = null
    }


    private fun applyEqualizerGains(frequencyGainMap: HashMap<Double, Float>) { //  **  [注意]  函數參數仍然保留 frequencyGainMap，但實際上我們不再使用它，稍後可以移除  **
        val currentEQ =
            when (currentSelectedEar) { //  **  根據 currentSelectedEar 選擇要使用的 Equalizer 物件 **
                EarType.Left -> equalizerLeft
                EarType.Right -> equalizerRight
            }
        val currentGainMap =
            when (currentSelectedEar) { //  **  根據 currentSelectedEar 選擇要使用的 frequencyGainMap **
                EarType.Left -> frequencyGainMapLeft
                EarType.Right -> frequencyGainMapRight
            }

        if (currentEQ == null) { //  **  檢查選中的 Equalizer 物件是否為 null  **
            println("${currentSelectedEar.name} Equalizer not initialized, skipping gain application.")
            return
        }

        println("Applying Equalizer Gains for ${currentSelectedEar.name} Ear:")

        for (i in 0 until numberOfEqBands) {
            val bandIndex = i.toShort()
            val centerFrequency =
                currentEQ?.getCenterFreq(bandIndex)?.toDouble() ?: 0.0 //  **  使用 currentEQ **
            val lowerCutoffFreq = currentEQ?.getBandFreqRange(bandIndex)?.get(0)?.toDouble()
                ?: 0.0 //  **  使用 currentEQ **
            val upperCutoffFreq = currentEQ?.getBandFreqRange(bandIndex)?.get(1)?.toDouble()
                ?: 0.0 //  **  使用 currentEQ **
            println("Equalizer Band ${i + 1}: Center Frequency = ${centerFrequency / 1000} kHz, Range = ${lowerCutoffFreq / 1000} kHz - ${upperCutoffFreq / 1000} kHz")

            // ** 查找最接近的 Slider 頻率 **
            val nearestSliderFrequency = findNearestFrequency(centerFrequency, displayFrequencies)

            val gainFromSlider =
                currentGainMap[nearestSliderFrequency] ?: 0f //  **  使用 currentGainMap **

            val eqLevel = (gainFromSlider * 100).toInt().toShort()

            eqBandLevels?.set(
                i,
                eqLevel
            ) //  **  [注意]  eqBandLevels 仍然是共用的，目前沒有左右耳獨立儲存 Level 值，可以考慮後續優化 **
            currentEQ?.setBandLevel(bandIndex, eqLevel) //  **  使用 currentEQ **

            println(
                "  Band ${i + 1} (Center Frequency: ${centerFrequency / 1000} kHz), Slider Frequency: ${nearestSliderFrequency} Hz, Slider Gain: ${gainFromSlider} dB, Equalizer Level: ${eqLevel / 100.0} dB"
            )
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
                resetEqualizerSettings() //  <--  調用 resetEqualizerSettings() 函數 (稍後建立)
                dialog.dismiss() // 關閉對話框
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss() // 關閉對話框
            }
        val dialog = builder.create()
        dialog.show()
        // 修改 Yes 按鈕顏色
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(getColor(R.color.button_reset_color))
        // 修改 No 按鈕顏色
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(getColor(R.color.button_equalizer_color))
    }

    private fun resetEqualizerSettings() {
        println("Resetting Equalizer Settings...") // Log 提示

        // 1. 重置 Slider UI 回到預設位置 (0dB)
        linearLayoutFrequencySliderContainer.children.forEach { columnLayout ->
            if (columnLayout is LinearLayout) {
                val slider = columnLayout.getChildAt(1) as? Slider
                if (slider != null) {
                    slider.value = 0f //  **  重置 Slider Value 值為 0dB **
                }
            }
        }

        // 2. 重置左右耳等化器增益值回到預設值 (0dB)
        displayFrequencies.forEach { frequency ->
            frequencyGainMapLeft[frequency] = 0f //  **  重置 左耳增益值為 0dB **
            frequencyGainMapRight[frequency] = 0f //  **  重置 右耳增益值為 0dB **
            sliderValueMapLeft[frequency] =
                0f    //  **  重置 左耳 Slider Value 值為 0f **  (同步更新 sliderValueMap)
            sliderValueMapRight[frequency] =
                0f   //  **  重置 右耳 Slider Value 值為 0f **  (同步更新 sliderValueMap)
        }

        // 3. UI 視覺效果回到預設狀態 (例如，預設選中左耳)
        currentSelectedEar = EarType.Left //  **  重設 currentSelectedEar 為 Left **
        textViewLeftEar.isSelected = true
        textViewRightEar.isSelected = false

        // 4. 重新應用等化器增益 (將重置後的增益值應用到 Equalizer 物件)
        applyEqualizerGains(HashMap()) //  **  調用 applyEqualizerGains() 函數，應用重置後的設定 **

        println("Equalizer Settings Reset Complete.") // Log 提示
    }

    private fun navigateToPsapActivity() {
        println("Navigating to PSAP Activity...") // Log 提示

        // ** 建立 Intent 物件，指定從 EqualizerActivity 跳轉到 PsapActivity **
        val intent = Intent(
            this,
            PsapActivity::class.java
        )

        // ** [可選]  可以透過 Intent 傳遞資料到 PsapActivity，例如檔名、等化器設定等 **
        // intent.putExtra("FILENAME", textViewFilename.text.toString())
        // intent.putExtra("EQUALIZER_SETTINGS_LEFT", frequencyGainMapLeft)
        // intent.putExtra("EQUALIZER_SETTINGS_RIGHT", frequencyGainMapRight)

        // ** [修改] 透過 Intent 傳遞 MediaPlayer 的 AudioSessionId **
        intent.putExtra("AUDIO_SESSION_ID", mediaPlayer?.audioSessionId ?: 0) //  <--  傳遞 AudioSessionId

        // ** 啟動 PsapActivity **
        startActivity(intent)

        println("Navigated to PSAP Activity.") // Log 提示
    }
}