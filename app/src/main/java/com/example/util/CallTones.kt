package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Call sounds:
 *  - [Ringback]: the "tuuu... tuuu..." the CALLER hears while the other phone is ringing.
 *  - [IncomingRing]: the phone ringtone + vibration the RECEIVER hears.
 */
object CallTones {

    /** Loops a generated two-tone ringback (440 Hz + 480 Hz, 1.5 s on / 2.5 s off). */
    class Ringback {
        private var track: AudioTrack? = null

        fun start() {
            if (track != null) return
            try {
                val rate = 16000
                val onSamples = (rate * 1.5).toInt()
                val offSamples = (rate * 2.5).toInt()
                val total = onSamples + offSamples
                val pcm = ShortArray(total)
                for (i in 0 until onSamples) {
                    val t = i.toDouble() / rate
                    // short fade in/out to avoid clicks
                    val fade = minOf(1.0, i / (rate * 0.02), (onSamples - i) / (rate * 0.02))
                    val v = (sin(2 * PI * 440 * t) + sin(2 * PI * 480 * t)) / 2.0
                    pcm[i] = (v * fade * Short.MAX_VALUE * 0.6).toInt().toShort()
                }
                val t = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(total * 2)
                    .build()
                t.write(pcm, 0, total)
                t.setLoopPoints(0, total, -1)
                t.setVolume(1f)
                t.play()
                track = t
            } catch (e: Exception) {
                track = null
            }
        }

        fun stop() {
            try { track?.stop() } catch (e: Exception) { }
            try { track?.release() } catch (e: Exception) { }
            track = null
        }
    }

    /** Default phone ringtone (falls back to notification / alarm sound) + vibration. */
    class IncomingRing(private val context: Context) {
        private var ringtone: Ringtone? = null
        private var vibrator: Vibrator? = null

        fun start() {
            if (ringtone != null) return
            val types = intArrayOf(
                RingtoneManager.TYPE_RINGTONE,
                RingtoneManager.TYPE_NOTIFICATION,
                RingtoneManager.TYPE_ALARM
            )
            for (type in types) {
                try {
                    val uri = RingtoneManager.getActualDefaultRingtoneUri(context, type)
                        ?: RingtoneManager.getDefaultUri(type)
                    val r = RingtoneManager.getRingtone(context, uri) ?: continue
                    r.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (Build.VERSION.SDK_INT >= 28) r.isLooping = true
                    r.play()
                    ringtone = r
                    break
                } catch (e: Exception) {
                    // try next sound type
                }
            }
            try {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (v != null && v.hasVibrator()) {
                    val pattern = longArrayOf(0, 900, 700)
                    if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    else @Suppress("DEPRECATION") v.vibrate(pattern, 0)
                    vibrator = v
                }
            } catch (e: Exception) { }
        }

        fun stop() {
            try { ringtone?.stop() } catch (e: Exception) { }
            ringtone = null
            try { vibrator?.cancel() } catch (e: Exception) { }
            vibrator = null
        }
    }
}
