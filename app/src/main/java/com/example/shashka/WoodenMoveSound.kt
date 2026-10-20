
package com.example.shashka

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Короткий мягкий звук деревянной шашки.
 * Звук создаётся программно — аудиофайлы не нужны.
 */
object WoodenMoveSound {

    private const val SAMPLE_RATE = 22050
    private const val DURATION_MS = 115
    private const val AMPLITUDE = 0.48

    fun play() {
        try {
            val count = SAMPLE_RATE * DURATION_MS / 1000
            val samples = ShortArray(count)
            val random = Random.Default

            for (i in samples.indices) {
                val t = i.toDouble() / SAMPLE_RATE

                // Быстрая атака и мягкое затухание.
                val attack = (t / 0.004).coerceIn(0.0, 1.0)
                val decay = exp(-t * 25.0)
                val envelope = attack * decay

                // Низкий деревянный щелчок.
                val body =
                    sin(2.0 * PI * 185.0 * t) * 0.65 +
                    sin(2.0 * PI * 310.0 * t) * 0.25 +
                    sin(2.0 * PI * 520.0 * t) * 0.10

                // Тихий шум в начале имитирует касание доски.
                val noiseEnvelope = exp(-t * 90.0)
                val noise = (random.nextDouble() * 2.0 - 1.0) *
                    noiseEnvelope * 0.20

                val value = ((body + noise) * envelope *
                    AMPLITUDE).coerceIn(-1.0, 1.0)

                samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        .setChannelMask(
                            AudioFormat.CHANNEL_OUT_MONO
                        )
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)

            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(
                        audioTrack: AudioTrack
                    ) {
                        try {
                            audioTrack.release()
                        } catch (_: Exception) {
                        }
                    }

                    override fun onPeriodicNotification(
                        audioTrack: AudioTrack
                    ) = Unit
                }
            )

            track.play()
        } catch (_: Exception) {
            // Ошибка звука не должна останавливать игру.
        }
    }
}
