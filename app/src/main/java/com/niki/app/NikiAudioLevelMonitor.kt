package com.niki.app

import android.media.audiofx.Visualizer
import kotlin.math.sqrt

class NikiAudioLevelMonitor {

    private var visualizer: Visualizer? = null

    fun start(
        audioSessionId: Int,
        onLevel: (Float) -> Unit
    ): Boolean {
        stop()

        return try {
            val monitor = Visualizer(audioSessionId)

            val captureSize = Visualizer
                .getCaptureSizeRange()[1]

            monitor.captureSize = captureSize

            monitor.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {

                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform == null || waveform.isEmpty()) {
                            onLevel(0f)
                            return
                        }

                        var sum = 0.0

                        for (sample in waveform) {
                            val value =
                                (sample.toInt() and 0xFF) - 128

                            sum += value * value
                        }

                        val rms = sqrt(sum / waveform.size)

                        val level = (rms / 90.0)
                            .toFloat()
                            .coerceIn(0f, 1f)

                        onLevel(level)
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // Not needed for this first version.
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )

            monitor.enabled = true
            visualizer = monitor
            true

        } catch (_: Exception) {
            stop()
            false
        }
    }

    fun stop() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }

        visualizer = null
    }
}
