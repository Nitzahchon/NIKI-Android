package com.niki.app

/**
 * Lightweight motion-data and interpolation layer for NIKI.
 * A rig renderer must apply these bone rotations to a character skeleton.
 * This evaluator alone does not animate or deform a flat photograph.
 */
data class NikiMotionKeyframe(val progress: Float, val value: Float)
data class NikiBoneChannel(val boneId: String, val keys: List<NikiMotionKeyframe>)
data class NikiMotionPreset(
    val id: String,
    val title: String,
    val durationMs: Long,
    val loop: Boolean,
    val channels: List<NikiBoneChannel>,
    val rootY: List<NikiMotionKeyframe> = emptyList()
)
data class NikiMotionFrame(
    val boneRotations: Map<String, Float>,
    val rootYOffset: Float,
    val progress: Float,
    val finished: Boolean
)

object NikiMotionEngine {
    fun evaluate(preset: NikiMotionPreset, elapsedMs: Long, speed: Float = 1f, intensity: Float = 1f): NikiMotionFrame {
        val duration = preset.durationMs.coerceAtLeast(1L)
        val time = (elapsedMs.coerceAtLeast(0L) * speed.coerceIn(0.1f, 4f)).toLong()
        val finished = !preset.loop && time >= duration
        val progress = if (preset.loop) (time % duration).toFloat() / duration
            else time.coerceAtMost(duration).toFloat() / duration
        val amount = intensity.coerceIn(0f, 2f)
        return NikiMotionFrame(
            preset.channels.associate { it.boneId to sample(it.keys, progress) * amount },
            sample(preset.rootY, progress) * amount,
            progress,
            finished
        )
    }

    fun blend(vararg frames: NikiMotionFrame): NikiMotionFrame {
        val rotations = mutableMapOf<String, Float>()
        var root = 0f
        var progress = 0f
        var finished = true
        frames.forEach { frame ->
            frame.boneRotations.forEach { (bone, angle) -> rotations[bone] = (rotations[bone] ?: 0f) + angle }
            root += frame.rootYOffset
            progress = maxOf(progress, frame.progress)
            finished = finished && frame.finished
        }
        return NikiMotionFrame(rotations, root, progress, finished)
    }

    private fun sample(keys: List<NikiMotionKeyframe>, progress: Float): Float {
        if (keys.isEmpty()) return 0f
        if (keys.size == 1) return keys.first().value
        val p = progress.coerceIn(0f, 1f)
        if (p <= keys.first().progress) return keys.first().value
        if (p >= keys.last().progress) return keys.last().value
        val i = keys.indexOfFirst { it.progress >= p }
        val a = keys[i - 1]
        val b = keys[i]
        val span = (b.progress - a.progress).coerceAtLeast(0.0001f)
        val t = ((p - a.progress) / span).coerceIn(0f, 1f)
        val eased = t * t * (3f - 2f * t)
        return a.value + (b.value - a.value) * eased
    }
}

object NikiMotionLibrary {
    private fun keys(vararg p: Pair<Float, Float>) = p.map { NikiMotionKeyframe(it.first, it.second) }
    private fun bone(id: String, vararg p: Pair<Float, Float>) = NikiBoneChannel(id, keys(*p))

    val presets = listOf(
        NikiMotionPreset("idle", "Idle / Breathing", 3200, true, listOf(
            bone("head", 0f to 0f, .25f to -1.5f, .5f to 0f, .75f to 1f, 1f to 0f),
            bone("torso", 0f to 0f, .25f to 1.2f, .5f to 0f, .75f to -1.2f, 1f to 0f),
            bone("left_upper_arm", 0f to 0f, .25f to 1f, .5f to 0f, .75f to -1f, 1f to 0f),
            bone("right_upper_arm", 0f to 0f, .25f to -1f, .5f to 0f, .75f to 1f, 1f to 0f)
        )),
        NikiMotionPreset("wave", "Wave", 2000, false, listOf(
            bone("right_upper_arm", 0f to 0f, .15f to -35f, .3f to -70f, .8f to -70f, 1f to 0f),
            bone("right_forearm", 0f to 0f, .2f to -75f, .4f to -105f, .7f to -80f, 1f to 0f),
            bone("right_hand", 0f to 0f, .3f to -16f, .5f to 18f, .7f to -16f, 1f to 0f)
        )),
        NikiMotionPreset("greeting", "Palm Greeting", 1800, false, listOf(
            bone("right_upper_arm", 0f to 0f, .25f to -48f, .8f to -48f, 1f to 0f),
            bone("right_forearm", 0f to 0f, .25f to -55f, .8f to -55f, 1f to 0f),
            bone("right_hand", 0f to 0f, .25f to 8f, .8f to 8f, 1f to 0f)
        )),
        NikiMotionPreset("nod", "Head Nod", 1100, false, listOf(
            bone("head", 0f to 0f, .2f to 12f, .45f to -3f, .7f to 9f, 1f to 0f)
        )),
        NikiMotionPreset("point", "Point", 1600, false, listOf(
            bone("right_upper_arm", 0f to 0f, .35f to -35f, .8f to -35f, 1f to 0f),
            bone("right_forearm", 0f to 0f, .35f to -75f, .8f to -75f, 1f to 0f),
            bone("right_hand", 0f to 0f, .35f to 12f, .8f to 12f, 1f to 0f),
            bone("torso", 0f to 0f, .35f to -4f, .8f to -4f, 1f to 0f)
        )),
        NikiMotionPreset("walk", "Walk", 1250, true, listOf(
            bone("right_thigh", 0f to -24f, .25f to 0f, .5f to 24f, .75f to 0f, 1f to -24f),
            bone("left_thigh", 0f to 24f, .25f to 0f, .5f to -24f, .75f to 0f, 1f to 24f),
            bone("right_shin", 0f to 0f, .25f to 8f, .5f to 0f, .75f to 38f, 1f to 0f),
            bone("left_shin", 0f to 0f, .25f to 38f, .5f to 0f, .75f to 8f, 1f to 0f),
            bone("right_upper_arm", 0f to 18f, .5f to -18f, 1f to 18f),
            bone("left_upper_arm", 0f to -18f, .5f to 18f, 1f to -18f)
        ), keys(0f to .08f, .25f to .04f, .5f to .08f, .75f to .04f, 1f to .08f)),
        NikiMotionPreset("run", "Run", 760, true, listOf(
            bone("right_thigh", 0f to -38f, .25f to 5f, .5f to 38f, .75f to -5f, 1f to -38f),
            bone("left_thigh", 0f to 38f, .25f to -5f, .5f to -38f, .75f to 5f, 1f to 38f),
            bone("right_shin", 0f to 8f, .25f to 65f, .5f to 8f, 1f to 2f),
            bone("left_shin", 0f to 65f, .25f to 8f, .5f to 2f, 1f to 65f),
            bone("right_upper_arm", 0f to 35f, .5f to -35f, 1f to 35f),
            bone("left_upper_arm", 0f to -35f, .5f to 35f, 1f to -35f),
            bone("torso", 0f to -8f, 1f to -8f)
        ), keys(0f to 0f, .25f to .13f, .5f to 0f, .75f to .13f, 1f to 0f)),
        NikiMotionPreset("jump", "Jump", 1300, false, listOf(
            bone("right_thigh", 0f to 0f, .3f to -28f, .65f to -12f, 1f to 0f),
            bone("left_thigh", 0f to 0f, .3f to 28f, .65f to 12f, 1f to 0f),
            bone("right_upper_arm", 0f to 0f, .3f to -65f, .65f to -45f, 1f to 0f),
            bone("left_upper_arm", 0f to 0f, .3f to 65f, .65f to 45f, 1f to 0f)
        ), keys(0f to 0f, .3f to -.35f, .55f to -.42f, .8f to -.12f, 1f to 0f)),
        NikiMotionPreset("dance_standing", "Standing Dance", 2400, true, listOf(
            bone("torso", 0f to -5f, .25f to 5f, .5f to -5f, .75f to 5f, 1f to -5f),
            bone("head", 0f to 3f, .25f to -3f, .5f to 3f, .75f to -3f, 1f to 3f),
            bone("left_upper_arm", 0f to -25f, .25f to -55f, .5f to -25f, .75f to -5f, 1f to -25f),
            bone("right_upper_arm", 0f to 25f, .25f to 5f, .5f to 25f, .75f to 55f, 1f to 25f),
            bone("left_forearm", 0f to 20f, .5f to -20f, 1f to 20f),
            bone("right_forearm", 0f to -20f, .5f to 20f, 1f to -20f)
        )),
        NikiMotionPreset("dance_side", "Side-to-Side Dance", 1800, true, listOf(
            bone("torso", 0f to -12f, .25f to 12f, .5f to -12f, .75f to 12f, 1f to -12f),
            bone("head", 0f to 5f, .25f to -5f, .5f to 5f, .75f to -5f, 1f to 5f),
            bone("left_thigh", 0f to -12f, .25f to 12f, .5f to -12f, .75f to 12f, 1f to -12f),
            bone("right_thigh", 0f to 12f, .25f to -12f, .5f to 12f, .75f to -12f, 1f to 12f),
            bone("left_upper_arm", 0f to 35f, .5f to -35f, 1f to 35f),
            bone("right_upper_arm", 0f to -35f, .5f to 35f, 1f to -35f)
        )),
        NikiMotionPreset("dance_full", "Full-Body Rhythmic Dance", 2200, true, listOf(
            bone("torso", 0f to -8f, .25f to 8f, .5f to -8f, .75f to 8f, 1f to -8f),
            bone("head", 0f to 4f, .25f to -4f, .5f to 4f, .75f to -4f, 1f to 4f),
            bone("left_thigh", 0f to -25f, .25f to 10f, .5f to 25f, .75f to -10f, 1f to -25f),
            bone("right_thigh", 0f to 25f, .25f to -10f, .5f to -25f, .75f to 10f, 1f to 25f),
            bone("left_shin", 0f to 0f, .25f to 28f, .5f to 0f, .75f to 28f, 1f to 0f),
            bone("right_shin", 0f to 28f, .25f to 0f, .5f to 28f, .75f to 0f, 1f to 28f),
            bone("left_upper_arm", 0f to -45f, .25f to 35f, .5f to -45f, .75f to 35f, 1f to -45f),
            bone("right_upper_arm", 0f to 45f, .25f to -35f, .5f to 45f, .75f to -35f, 1f to 45f)
        ), keys(0f to 0f, .25f to -.08f, .5f to 0f, .75f to -.08f, 1f to 0f)),
        NikiMotionPreset("dance_hips_shoulders", "Hip & Shoulder Dance", 2000, true, listOf(
            bone("torso", 0f to -10f, .25f to 10f, .5f to -10f, .75f to 10f, 1f to -10f),
            bone("left_thigh", 0f to -18f, .25f to 18f, .5f to -18f, .75f to 18f, 1f to -18f),
            bone("right_thigh", 0f to 18f, .25f to -18f, .5f to 18f, .75f to -18f, 1f to 18f),
            bone("left_upper_arm", 0f to -15f, .25f to -45f, .5f to -15f, .75f to 15f, 1f to -15f),
            bone("right_upper_arm", 0f to 15f, .25f to 45f, .5f to 15f, .75f to -15f, 1f to 15f),
            bone("head", 0f to 2f, .5f to -2f, 1f to 2f)
        )),
        NikiMotionPreset("freestyle", "Freestyle Dance", 2600, true, listOf(
            bone("torso", 0f to -10f, .2f to 7f, .4f to -4f, .6f to 11f, .8f to -7f, 1f to -10f),
            bone("head", 0f to 5f, .2f to -4f, .4f to 3f, .6f to -6f, .8f to 4f, 1f to 5f),
            bone("left_upper_arm", 0f to -40f, .2f to 25f, .4f to -60f, .6f to 20f, .8f to -30f, 1f to -40f),
            bone("right_upper_arm", 0f to 35f, .2f to -25f, .4f to 55f, .6f to -20f, .8f to 40f, 1f to 35f),
            bone("left_thigh", 0f to -20f, .25f to 15f, .5f to -15f, .75f to 20f, 1f to -20f),
            bone("right_thigh", 0f to 20f, .25f to -15f, .5f to 15f, .75f to -20f, 1f to 20f),
            bone("left_shin", 0f to 0f, .25f to 35f, .5f to 5f, .75f to 30f, 1f to 0f),
            bone("right_shin", 0f to 30f, .25f to 5f, .5f to 35f, .75f to 0f, 1f to 30f)
        ), keys(0f to 0f, .2f to -.1f, .4f to 0f, .6f to -.12f, .8f to 0f, 1f to 0f))
    )

    fun find(id: String): NikiMotionPreset? = presets.firstOrNull { it.id == id }
}
