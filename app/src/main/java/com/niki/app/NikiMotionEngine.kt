package com.niki.app

import kotlin.math.PI
import kotlin.math.sin

data class NikiMotionPreset(
    val id: String,
    val title: String,
    val durationMs: Long = 2400L,
    val looping: Boolean = true,
)

data class NikiMotionFrame(
    val boneRotations: Map<String, Float>,
    val rootYOffset: Float = 0f,
    val progress: Float = 0f,
    val finished: Boolean = false,
)

object NikiMotionLibrary {

    val presets = listOf(
        NikiMotionPreset("idle", "Idle"),
        NikiMotionPreset("wave", "Wave"),
        NikiMotionPreset("nod", "Nod"),
        NikiMotionPreset("point", "Point"),
        NikiMotionPreset("walk", "Walk"),
        NikiMotionPreset("dance", "Dance"),
        NikiMotionPreset("jump", "Jump", 1200L, false),
    )

    fun find(id: String): NikiMotionPreset? {
        return presets.find { it.id == id }
    }
}

object NikiMotionEngine {

    fun evaluate(
        preset: NikiMotionPreset,
        elapsedMs: Long,
        speed: Float = 1f,
        intensity: Float = 1f,
    ): NikiMotionFrame {

        val duration = preset.durationMs.coerceAtLeast(1L)
        val adjustedTime = elapsedMs.toFloat() * speed.coerceIn(0.1f, 3f)
        val progress = if (preset.looping) {
    (adjustedTime % duration.toFloat()) / duration.toFloat()
} else {
    adjustedTime.coerceAtMost(duration.toFloat()) / duration.toFloat()
}

        val finished = !preset.looping && adjustedTime >= duration.toFloat()

        val phase = progress * 2f * PI.toFloat()
        val wave = sin(phase)
        val strength = intensity.coerceIn(0f, 2f)

        val rotations = mutableMapOf<String, Float>()

        var rootY = 0f

        when (preset.id) {

            "idle" -> {
                rotations["torso"] = wave * 2f * strength
                rotations["head"] = wave * 2f * strength
                rootY = wave * 1.5f * strength
            }

            "wave" -> {
                rotations["right_upper_arm"] = -125f
                rotations["right_forearm"] =
                    -45f + wave * 35f * strength
                rotations["right_hand"] = wave * 20f * strength
                rotations["torso"] = wave * 3f * strength
            }

            "nod" -> {
                rotations["head"] = wave * 18f * strength
                rotations["torso"] = wave * 2f * strength
            }

            "point" -> {
                rotations["right_upper_arm"] = -145f
                rotations["right_forearm"] = -10f
                rotations["right_hand"] = -5f
            }

            "walk" -> {
                rotations["left_thigh"] = wave * 28f * strength
                rotations["right_thigh"] = -wave * 28f * strength
                rotations["left_shin"] =
                    (-wave).coerceAtLeast(0f) * 35f * strength
                rotations["right_shin"] =
                    wave.coerceAtLeast(0f) * 35f * strength
                rotations["left_upper_arm"] = -wave * 18f * strength
                rotations["right_upper_arm"] = wave * 18f * strength
                rootY = kotlin.math.abs(wave) * 3f * strength
            }

            "dance" -> {
                rotations["torso"] = wave * 18f * strength
                rotations["head"] = -wave * 12f * strength
                rotations["left_upper_arm"] = wave * 45f * strength
                rotations["right_upper_arm"] = -wave * 45f * strength
                rotations["left_thigh"] = wave * 22f * strength
                rotations["right_thigh"] = -wave * 22f * strength
                rootY = kotlin.math.abs(wave) * 8f * strength
            }

            "jump" -> {
                val jump = sin(progress * PI.toFloat())
                    .coerceAtLeast(0f)

                rootY = -65f * jump * strength
                rotations["left_thigh"] = -20f * jump * strength
                rotations["right_thigh"] = 20f * jump * strength
                rotations["left_upper_arm"] = -35f * jump * strength
                rotations["right_upper_arm"] = 35f * jump * strength
            }
        }

        return NikiMotionFrame(
            boneRotations = rotations,
            rootYOffset = rootY,
            progress = progress,
            finished = finished,
        )
    }
}
