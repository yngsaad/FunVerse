package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance synthesized sound and haptics manager.
 * Operates 100% offline with zero assets or network dependency.
 */
class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    fun playTap() {
        if (isHapticsEnabled) {
            triggerVibration(25, 60)
        }
        if (!isSoundEnabled) return
        scope.launch {
            playTone(frequency = 784.0, durationMs = 50, decayRate = 35.0, volume = 0.35f) // G5 note
        }
    }

    fun playSwap() {
        if (isHapticsEnabled) {
            triggerVibration(30, 80)
        }
        if (!isSoundEnabled) return
        scope.launch {
            playSlideTone(startFreq = 523.25, endFreq = 659.25, durationMs = 90, volume = 0.4f)
        }
    }

    fun playInvalid() {
        if (isHapticsEnabled) {
            triggerVibration(60, 160)
        }
        if (!isSoundEnabled) return
        scope.launch {
            playTone(frequency = 220.0, durationMs = 120, decayRate = 18.0, volume = 0.45f)
        }
    }

    fun playMatch(comboLevel: Int, matchSize: Int) {
        if (isHapticsEnabled) {
            val strength = (80 + comboLevel * 30).coerceAtMost(255)
            triggerVibration(45, strength)
        }
        if (!isSoundEnabled) return
        scope.launch {
            // Ascending pitch base depending on combo level
            val baseFreqMultiplier = 1.0 + (comboLevel.coerceAtMost(5) * 0.15)
            val root = 523.25 * baseFreqMultiplier // C5 based
            val third = root * 1.25992 // E
            val fifth = root * 1.49831 // G
            val octave = root * 2.0 // C6

            if (matchSize >= 4) {
                // Sparkle chord for 4+ match
                playChord(listOf(root, third, fifth, octave), durationMs = 180, volume = 0.5f)
            } else {
                playArpeggio(listOf(root, third, fifth), noteDurationMs = 50, volume = 0.45f)
            }
        }
    }

    fun playVictory() {
        if (isHapticsEnabled) {
            triggerVibration(120, 200)
        }
        if (!isSoundEnabled) return
        scope.launch {
            // Triumphant fanfare: C5, E5, G5, C6 high sustain
            val notes = listOf(523.25, 659.25, 783.99, 1046.50)
            playArpeggio(notes, noteDurationMs = 90, volume = 0.55f)
        }
    }

    fun playGameOver() {
        if (isHapticsEnabled) {
            triggerVibration(90, 150)
        }
        if (!isSoundEnabled) return
        scope.launch {
            val notes = listOf(440.0, 392.0, 349.23, 311.13)
            playArpeggio(notes, noteDurationMs = 110, volume = 0.4f)
        }
    }

    fun playReshuffle() {
        if (!isSoundEnabled) return
        scope.launch {
            val notes = listOf(350.0, 420.0, 500.0, 590.0)
            playArpeggio(notes, noteDurationMs = 40, volume = 0.35f)
        }
    }

    private fun triggerVibration(durationMs: Long, amplitude: Int) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration fails or device lacks permission
        }
    }

    private fun playTone(
        frequency: Double,
        durationMs: Int,
        decayRate: Double = 20.0,
        volume: Float = 0.5f
    ) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-decayRate * t)
            val sample = (sin(2.0 * PI * frequency * t) * envelope * Short.MAX_VALUE * volume).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playAudioBuffer(buffer, sampleRate)
    }

    private fun playSlideTone(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float = 0.5f
    ) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * currentFreq / sampleRate
            val envelope = sin(PI * t) // smooth sine envelope
            val sample = (sin(phase) * envelope * Short.MAX_VALUE * volume).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playAudioBuffer(buffer, sampleRate)
    }

    private fun playArpeggio(notes: List<Double>, noteDurationMs: Int, volume: Float) {
        val sampleRate = 22050
        val noteSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt()
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        notes.forEachIndexed { noteIndex, freq ->
            val offset = noteIndex * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = exp(-18.0 * t)
                val sample = (sin(2.0 * PI * freq * t) * envelope * Short.MAX_VALUE * volume).toInt()
                buffer[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        playAudioBuffer(buffer, sampleRate)
    }

    private fun playChord(frequencies: List<Double>, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        val freqCount = frequencies.size.toDouble().coerceAtLeast(1.0)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-15.0 * t)
            var mixedSample = 0.0
            for (freq in frequencies) {
                mixedSample += sin(2.0 * PI * freq * t)
            }
            mixedSample /= freqCount
            val sample = (mixedSample * envelope * Short.MAX_VALUE * volume).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playAudioBuffer(buffer, sampleRate)
    }

    private fun playAudioBuffer(buffer: ShortArray, sampleRate: Int) {
        var track: AudioTrack? = null
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = buffer.size * 2
            val finalBufferSize = bufferSize.coerceAtLeast(minBufSize)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(finalBufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            // Automatically clean up after playback ends
            val sleepMs = ((buffer.size.toDouble() / sampleRate) * 1000).toLong() + 50
            Thread.sleep(sleepMs)
        } catch (_: Exception) {
            // Audio error non-fatal in game
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }
}
