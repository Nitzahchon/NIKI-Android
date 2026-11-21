package com.niki.app

import android.os.Handler
import android.os.Looper

class NikiLipSyncController {

    private val audioMonitor = NikiAudioLevelMonitor()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var previousOpenness = 0f

    fun start(
        audioSessionId: Int,
        onFrame: (NikiMouthFrame) -> Unit
    ): Boolean {

        previousOpenness = 0f

        return audioMonitor.start(audioSessionId) { amplitude ->

            val frame = NikiLipSyncEngine.evaluate(
                amplitude = amplitude,
                previousOpenness = previousOpenness
            )

            previousOpenness = frame.openness

            mainHandler.post {
                onFrame(frame)
            }
        }
    }

    fun stop() {
        audioMonitor.stop()
        previousOpenness = 0f

        mainHandler.post {
            // Return to the closed-mouth state.
        }
    }
}
