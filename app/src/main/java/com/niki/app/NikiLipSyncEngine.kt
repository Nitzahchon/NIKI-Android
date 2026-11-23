package com.niki.app

data class NikiMouthPosition(
    val x: Float = 0.5f,
    val y: Float = 0.62f,
    val width: Float = 0.09f
) {
    fun normalized() = copy(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        width = width.coerceIn(0.015f, 0.3f)
    )
}

data class NikiMouthFrame(
    val openness: Float,
    val isSpeaking: Boolean
)

object NikiLipSyncEngine {

    fun evaluate(
        amplitude: Float,
        previousOpenness: Float = 0f,
        sensitivity: Float = 0.12f,
        smoothing: Float = 0.45f
    ): NikiMouthFrame {

        val level = amplitude.coerceIn(0f, 1f)
        val threshold = sensitivity.coerceIn(0f, 0.95f)

        val target = (
            (level - threshold) / (1f - threshold)
        ).coerceIn(0f, 1f)

        val smooth = smoothing.coerceIn(0.01f, 1f)

        val openness = previousOpenness.coerceIn(0f, 1f) +
            (target - previousOpenness.coerceIn(0f, 1f)) * smooth

        return NikiMouthFrame(
            openness = openness.coerceIn(0f, 1f),
            isSpeaking = openness > 0.08f
        )
    }

    fun closedMouth(): NikiMouthFrame {
        return NikiMouthFrame(
            openness = 0f,
            isSpeaking = false
        )
    }
}
