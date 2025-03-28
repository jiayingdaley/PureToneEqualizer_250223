package com.example.puretoneequalizer_250223.audio_effects

import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioEffectAvailabilityTest {
    @Test
    fun testNoiseSuppressorAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val isAvailable = NoiseSuppressor.isAvailable()
            println("NoiseSuppressor.isAvailable(): $isAvailable")
            // 您可以根據需要添加更嚴格的斷言，例如 assertTrue(isAvailable)
        } else {
            println("NoiseSuppressor is not available on SDK version ${Build.VERSION.SDK_INT}")
        }
    }

    @Test
    fun testAutomaticGainControlAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val isAvailable = AutomaticGainControl.isAvailable()
            println("AutomaticGainControl.isAvailable(): $isAvailable")
            // 您可以根據需要添加更嚴格的斷言，例如 assertTrue(isAvailable)
        } else {
            println("AutomaticGainControl is not available on SDK version ${Build.VERSION.SDK_INT}")
        }
    }

    @Test
    fun testLoudnessEnhancerAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val effects = AudioEffect.queryEffects()
            val isAvailable = effects.any { it.type == AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER }

            println("LoudnessEnhancer available: $isAvailable")
            assertTrue(isAvailable)
        } else {
            println("LoudnessEnhancer is not available on SDK version ${Build.VERSION.SDK_INT}")
        }
    }

}