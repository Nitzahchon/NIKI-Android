package com.niki.app

/**
 * NIKI's starter human rig. Coordinates are local to the parent bone's end joint,
 * and IDs intentionally follow KinetiFrame's HUMAN actor semantic IDs wherever possible.
 * The extra pelvis root gives both legs and the torso a shared attachment point.
 */
object NikiCharacterRig {
    fun createHuman(): NikiRigGraph {
        val bones = listOf(
            NikiRigBone("pelvis", null, 0f, 0f, 0f, 0f),
            NikiRigBone("torso", "pelvis", 0f, 0f, 112f, -90f),
            NikiRigBone("head", "torso", 0f, 0f, 56f, 0f),
            NikiRigBone("left_upper_arm", "torso", 0f, -16f, 68f, -120f),
            NikiRigBone("left_forearm", "left_upper_arm", 0f, 0f, 58f, 18f),
            NikiRigBone("left_hand", "left_forearm", 0f, 0f, 25f, -8f),
            NikiRigBone("right_upper_arm", "torso", 0f, 16f, 68f, 120f),
            NikiRigBone("right_forearm", "right_upper_arm", 0f, 0f, 58f, -18f),
            NikiRigBone("right_hand", "right_forearm", 0f, 0f, 25f, 8f),
            NikiRigBone("left_thigh", "pelvis", -14f, 0f, 76f, 105f),
            NikiRigBone("left_shin", "left_thigh", 0f, 0f, 72f, 8f),
            NikiRigBone("left_foot", "left_shin", 0f, 0f, 28f, 78f),
            NikiRigBone("right_thigh", "pelvis", 14f, 0f, 76f, 75f),
            NikiRigBone("right_shin", "right_thigh", 0f, 0f, 72f, -8f),
            NikiRigBone("right_foot", "right_shin", 0f, 0f, 28f, -78f),
        ).associateBy { it.id }
        return NikiRigGraph(rootBoneIds = listOf("pelvis"), bones = bones)
    }

    /** Resolve parent-relative bones into stage-space line segments for the preview renderer. */
    fun worldTransforms(graph: NikiRigGraph): Map<String, NikiRigWorldBone> {
        val result = linkedMapOf<String, NikiRigWorldBone>()
        val visiting = mutableSetOf<String>()

        fun resolve(id: String): NikiRigWorldBone {
            result[id]?.let { return it }
            require(visiting.add(id)) { "Rig hierarchy cycle at '$id'." }
            val bone = requireNotNull(graph.bones[id]) { "Missing bone '$id'." }
            val parent = bone.parentId?.let(::resolve)
            val parentAngle = parent?.angleDegrees ?: 0f
            val radians = Math.toRadians(parentAngle.toDouble())
            val localX = bone.x * kotlin.math.cos(radians).toFloat() -
                bone.y * kotlin.math.sin(radians).toFloat()
            val localY = bone.x * kotlin.math.sin(radians).toFloat() +
                bone.y * kotlin.math.cos(radians).toFloat()
            val startX = (parent?.endX ?: 0f) + localX
            val startY = (parent?.endY ?: 0f) + localY
            val angle = parentAngle + bone.rotationDegrees
            val angleRadians = Math.toRadians(angle.toDouble())
            val endX = startX + bone.length * kotlin.math.cos(angleRadians).toFloat()
            val endY = startY + bone.length * kotlin.math.sin(angleRadians).toFloat()
            val world = NikiRigWorldBone(startX, startY, endX, endY, angle)
            result[id] = world
            visiting.remove(id)
            return world
        }

        graph.bones.keys.forEach(::resolve)
        return result
    }
}

data class NikiRigWorldBone(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val angleDegrees: Float,
)
