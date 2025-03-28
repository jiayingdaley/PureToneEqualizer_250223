package com.example.puretoneequalizer_250223

import android.media.audiofx.Equalizer
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EqualizerChannelTest {

    private val audioSessionId = 0 // 使用預設的 audioSessionId

    private fun getEqualizer(): Equalizer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val equalizer = Equalizer(0, audioSessionId)
            equalizer.enabled = true
            equalizer
        } else {
            println("Equalizer is not supported on this device or SDK version.")
            null
        }
    }

    @Test
    fun testGetNumberOfBands() {
        val equalizer = getEqualizer()
        equalizer?.let {
            val numberOfBands = it.numberOfBands
            println("Number of bands: $numberOfBands")
            assertTrue("Expected more than 0 bands", numberOfBands > 0)
            it.release()
        }
    }

    @Test
    fun testGetBandLevelRange() {
        val equalizer = getEqualizer()
        equalizer?.let {
            val numberOfBands = it.numberOfBands
            val bandLevelRange = it.bandLevelRange
            println("Band level range: min = ${bandLevelRange[0]}, max = ${bandLevelRange[1]} (mB)")
            assertNotNull("Band level range should not be null", bandLevelRange)
            assertEquals("Band level range should have 2 values", 2, bandLevelRange.size)
            assertTrue("Min level should be less than or equal to max level", bandLevelRange[0] <= bandLevelRange[1])
            it.release()
        }
    }

    @Test
    fun testGetCenterFreq() {
        val equalizer = getEqualizer()
        equalizer?.let {
            val numberOfBands = it.numberOfBands
            assertTrue("Expected more than 0 bands to test center frequencies", numberOfBands > 0)
            for (i in 0 until numberOfBands) {
                val centerFreq = it.getCenterFreq(i.toShort())
                println("Center frequency for band $i: $centerFreq mHz (${centerFreq / 1000} Hz)")
                assertTrue("Center frequency should be greater than 0", centerFreq > 0)
            }
            it.release()
        }
    }

    @Test
    fun testGetBand() {
        val equalizer = getEqualizer()
        equalizer?.let {
            val numberOfBands = it.numberOfBands
            println("Number of bands: $numberOfBands")
            assertTrue("Expected more than 0 bands to test getBand()", numberOfBands > 0)

            // 取得所有頻段的中心頻率
            val centerFrequencies = IntArray(numberOfBands.toInt()) // 正確初始化陣列大小
            for (i in 0 until numberOfBands) {
                centerFrequencies[i] = it.getCenterFreq(i.toShort()) / 1000 // 轉換為 Hz
                println("Center frequency for band $i: ${centerFrequencies[i]} Hz")
            }

            // 根據實際的中心頻率選擇測試頻率
            val testFrequencies = mutableListOf<Int>()
            if (centerFrequencies.isNotEmpty()) {
                testFrequencies.add(centerFrequencies.first() / 2) // 略低於最低頻段
                for (freq in centerFrequencies) {
                    testFrequencies.add(freq) // 中心頻率
                }
                testFrequencies.add(centerFrequencies.last() * 2) // 略高於最高頻段
            } else {
                // 如果沒有中心頻率資訊，則使用一些預設值作為備份
                testFrequencies.addAll(listOf(100, 500, 1000, 5000, 10000))
            }

            for (frequency in testFrequencies) {
                val bandIndex = it.getBand(frequency * 1000) // 轉換為 mHz
                println("Band index for frequency ${frequency} Hz: $bandIndex")
                assertTrue("Band index should be within the valid range", bandIndex >= 0 && bandIndex < numberOfBands)

                // 可選：驗證返回的頻段的中心頻率是否接近測試頻率
                if (bandIndex >= 0 && bandIndex < numberOfBands) {
                    val centerFreq = it.getCenterFreq(bandIndex) / 1000 // 轉換為 Hz
                    println("  Center frequency of band $bandIndex: ${centerFreq} Hz")
                    // 您可以定義一個允許的誤差範圍，例如：
                    // val tolerance = frequency * 0.2f // 允許 20% 的誤差
                    // assertTrue("Center frequency should be close to test frequency", Math.abs(centerFreq - frequency) < tolerance)
                }
            }
            it.release()
        }
    }
}
