package com.example.puretoneequalizer_250223

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

class PureToneTestActivity : ComponentActivity() {

    private lateinit var testEarTextView: TextView
    private lateinit var currentFrequencyTextView: TextView
    private lateinit var hearingResponseArea: CardView // 使用 CardView
    private lateinit var responseView: View // 使用 View
    private lateinit var progressBar: ProgressBar

    private var testingEar: String? = null
    private val frequencies = listOf(1000, 2000, 3000, 4000, 6000, 8000, 1000, 500, 250)
    private var currentFrequencyIndex = 0
    private var currentFrequency = 0
    private var currentIntensity = 40 // 初始音量 (dB HL)
    private val intensityStepDown = 10
    private val intensityStepUp = 5
    private val minIntensity = 0
    private val maxIntensity = 100
    private var heardTone = false
    private var testResults = mutableMapOf<Int, Int?>() // 頻率 -> 聽閾 (使用 Int? 允許 null 表示未測)
    private var testCompleted = false
    private var progress = 0
    private val handler = Handler(Looper.getMainLooper())
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 44100
    private val durationSeconds = 1.5f
    private val numSamples = (durationSeconds * sampleRate).toInt()
    private val buffer = ShortArray(numSamples)
    private var filename: String = ""
    private var findingThreshold = false // 標記是否正在尋找特定頻率的聽閾
    private var responses = mutableListOf<Boolean>() // 記錄在特定頻率和強度下的回應次數


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pure_tone_test)

        testEarTextView = findViewById(R.id.textView_test_ear)
        currentFrequencyTextView = findViewById(R.id.textView_frequency) // 使用正確的 ID
        hearingResponseArea = findViewById(R.id.cardView_response_area) // 使用 CardView 的 ID
        responseView = findViewById(R.id.view_response_area) // 找到 CardView 內部的 View
        progressBar = findViewById(R.id.progressBar_test_progress)

        testingEar = intent.getStringExtra("TESTING_EAR")
        testEarTextView.text = "$testingEar Ear"
        progressBar.max = frequencies.size * 10 // 粗略估計最大測試次數

        val currentTime = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        filename = "${currentTime}_PureTone_${testingEar}_Results.csv"

        responseView.setOnTouchListener { v, event -> // 使用 responseView
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    heardTone = true
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    return@setOnTouchListener true
                }
            }
            false
        }

        startNextFrequency()
    }

    private fun startNextFrequency() {
        if (currentFrequencyIndex < frequencies.size) {
            currentFrequency = frequencies[currentFrequencyIndex]
            currentFrequencyTextView.text = "${currentFrequency} Hz"
            currentIntensity = 40 // 重置起始音量
            testResults[currentFrequency] = null // 初始化該頻率的結果
            findingThreshold = true
            responses.clear()
            startTestTrial()
        } else {
            testCompleted()
        }
    }

    private fun startTestTrial() {
        playSound(currentFrequency, currentIntensity)
        heardTone = false
        updateProgress() // 在每次開始播放聲音時更新進度條
        handler.postDelayed({
            processResponse()
        }, (durationSeconds * 1000 + 500).toLong())
    }

    private fun processResponse() {
        audioTrack?.stop()
        responses.add(heardTone)

        if (findingThreshold) {
            if (heardTone) {
                // 聽到聲音，降低音量
                currentIntensity -= intensityStepDown
                if (currentIntensity < minIntensity) {
                    currentIntensity = minIntensity
                    // 即使已達最小音量仍聽到，則該頻率的聽閾為最小值
                    testResults[currentFrequency] = currentIntensity
                    findingThreshold = false
                    currentFrequencyIndex++
                    startNextFrequency()
                } else {
                    // 再次測試同一頻率更低的音量
                    startTestTrial()
                }
            } else {
                // 沒有聽到聲音，提高音量
                currentIntensity += intensityStepUp
                if (currentIntensity > maxIntensity) {
                    // 即使已達最大音量仍未聽到，則該頻率的聽閾為最大值或標記為未測到
                    testResults[currentFrequency] = -1 // 可以使用 null 或其他值表示
                    findingThreshold = false
                    currentFrequencyIndex++
                    startNextFrequency()
                } else if (responses.count { it } >= 2) {
                    // 至少聽到兩次，則將當前音量作為聽閾
                    testResults[currentFrequency] = currentIntensity
                    findingThreshold = false
                    currentFrequencyIndex++
                    startNextFrequency()
                }
                else {
                    // 再次測試同一頻率更高的音量
                    startTestTrial()
                }
            }
        }
    }

    private fun playSound(frequency: Int, amplitude: Int) {
        val volume = amplitude.toFloat() / 100f // 簡化音量比例

        for (i in 0 until numSamples) {
            val time = i.toFloat() / sampleRate
            buffer[i] = (Short.MAX_VALUE * volume * sin(2 * PI * frequency * time)).toInt().toShort()
        }

        audioTrack?.release()
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            numSamples * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.write(buffer, 0, numSamples)
        audioTrack?.play()
    }

    private fun updateProgress() {
        progress++
        progressBar.progress = progress
    }

    private fun testCompleted() {
        audioTrack?.release()
        // TODO: 實作額外測試邏輯 (重測 1000 Hz, 半倍頻測試)
        saveTestResultsToCSV()
        finish()
    }

    private fun saveTestResultsToCSV() {
        val baseDir = getExternalFilesDir(null)?.absolutePath
        val filePath = "$baseDir/$filename"
        try {
            FileWriter(filePath).use { writer ->
                writer.append("Frequency (Hz),Threshold (dB HL)\n")
                testResults.forEach { (frequency, threshold) ->
                    writer.append("$frequency,${threshold ?: "N/A"}\n")
                }
                Log.d("PureToneTestActivity", "Test results saved to: $filePath")
            }
        } catch (e: IOException) {
            Log.e("PureToneTestActivity", "Error writing test results to CSV: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioTrack?.release()
    }
}