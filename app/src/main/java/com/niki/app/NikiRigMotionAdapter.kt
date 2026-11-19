package com.niki.app

/**
 * Converts NIKI motion frames into an immutable, parent-relative rig pose.
 *
 * The bone IDs intentionally match KinetiFrame's HUMAN ActorTemplates IDs
 * (torso, head, left_upper_arm, left_forearm, etc.). This adapter is kept
 * independent of KinetiFrame classes until their complete :engine module and
 * Gradle dependency graph can be integrated into the NIKI project.
 */
data class NikiRigBone(
    val id: String,
    val parentId: String?,
    val x: Float,
    val y: Float,
    val length: Float,
    val restRotationDegrees: Float = 0f,
    val rotationDegrees: Float = restRotationDegrees,
    val visible: Boolean = true,
    val drawOrder: Int = 0,
)

data class NikiRigGraph(
    val rootBoneIds: List<String>,
    val bones: Map<String, NikiRigBone>,
)

data class NikiRigPose(
    val graph: NikiRigGraph,
    /** Vertical translation to apply to the character root(s), in rig units. */
    val rootYOffset: Float = 0f,
    val progress: Float = 0f,
    val finished: Boolean = false,
)

data class NikiRigValidation(val valid: Boolean, val errors: List<String>)

object NikiRigMotionAdapter {
    /**
     * Applies local bone rotations from NikiMotionEngine to a rig graph.
     * Every result is calculated from restRotationDegrees, not from the
     * previous animated frame, so repeated preview frames cannot accumulate drift.
     */
    fun apply(graph: NikiRigGraph, frame: NikiMotionFrame): NikiRigPose {
        val posedBones = graph.bones.mapValues { (id, bone) ->
            val delta = frame.boneRotations[id] ?: 0f
            bone.copy(rotationDegrees = bone.restRotationDegrees + delta)
        }
        return NikiRigPose(
            graph = graph.copy(bones = posedBones),
            rootYOffset = frame.rootYOffset,
            progress = frame.progress,
            finished = frame.finished,
        )
    }

    /** Evaluates a named preset and applies it to the supplied rig. */
    fun evaluate(
        graph: NikiRigGraph,
        presetId: String,
        elapsedMs: Long,
        speed: Float = 1f,
        intensity: Float = 1f,
    ): NikiRigPose? {
        val preset = NikiMotionLibrary.find(presetId) ?: return null
        val frame = NikiMotionEngine.evaluate(preset, elapsedMs, speed, intensity)
        return apply(graph, frame)
    }

    /** Restores every bone to its saved rest rotation. */
    fun reset(graph: NikiRigGraph): NikiRigGraph = graph.copy(
        bones = graph.bones.mapValues { (_, bone) ->
            bone.copy(rotationDegrees = bone.restRotationDegrees)
        },
    )

    /** Checks IDs, roots, parent references, lengths and hierarchy cycles before rendering. */
    fun validate(graph: NikiRigGraph): NikiRigValidation {
        val errors = mutableListOf<String>()
        if (graph.bones.isEmpty()) errors += "The rig has no bones."
        if (graph.rootBoneIds.isEmpty()) errors += "The rig has no root bone."
        graph.rootBoneIds.forEach { rootId ->
            val root = graph.bones[rootId]
            if (root == null) errors += "Root '$rootId' does not exist."
            else if (root.parentId != null) errors += "Root '$rootId' must not have a parent."
        }
        graph.bones.forEach { (key, bone) ->
            if (key != bone.id) errors += "Bone map key '$key' does not match bone ID '${bone.id}'."
            if (!bone.length.isFinite() || bone.length < 0f) errors += "Bone '${bone.id}' has an invalid length."
            if (!bone.x.isFinite() || !bone.y.isFinite()) errors += "Bone '${bone.id}' has an invalid position."
            if (!bone.restRotationDegrees.isFinite()) errors += "Bone '${bone.id}' has an invalid rest rotation."
            if (bone.parentId != null && bone.parentId !in graph.bones) {
                errors += "Bone '${bone.id}' references missing parent '${bone.parentId}'."
            }
        }
        graph.bones.keys.forEach { startId ->
            val visited = mutableSetOf<String>()
            var current: String? = startId
            while (current != null) {
                if (!visited.add(current)) {
                    errors += "The hierarchy contains a cycle involving '$current'."
                    break
                }
                current = graph.bones[current]?.parentId
            }
        }
        val actualRoots = graph.bones.values.filter { it.parentId == null }.map { it.id }.toSet()
        if (actualRoots != graph.rootBoneIds.toSet()) {
            errors += "The root list does not match the parentless bones."
        }
        return NikiRigValidation(errors.isEmpty(), errors.distinct())
    }
}
