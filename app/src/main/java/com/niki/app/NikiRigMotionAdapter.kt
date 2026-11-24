
package com.niki.app

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
    val rootYOffset: Float = 0f,
    val progress: Float = 0f,
    val finished: Boolean = false,
)

data class NikiRigValidation(
    val valid: Boolean,
    val errors: List<String>,
)

object NikiRigMotionAdapter {

    /**
     * Applies motion relative to the original rest pose.
     * Each frame is calculated independently to prevent rotation drift.
     */
    fun apply(
        graph: NikiRigGraph,
        frame: NikiMotionFrame,
    ): NikiRigPose {

        val posedBones = graph.bones.mapValues { (id, bone) ->
            val delta = frame.boneRotations[id] ?: 0f

            bone.copy(
                rotationDegrees =
                    bone.restRotationDegrees + delta
            )
        }

        return NikiRigPose(
            graph = graph.copy(bones = posedBones),
            rootYOffset = frame.rootYOffset
                .takeIf { it.isFinite() } ?: 0f,
            progress = frame.progress
                .takeIf { it.isFinite() }
                ?.coerceIn(0f, 1f) ?: 0f,
            finished = frame.finished,
        )
    }

    /**
     * Evaluates a motion preset and applies its frame.
     */
    fun evaluate(
        graph: NikiRigGraph,
        presetId: String,
        elapsedMs: Long,
        speed: Float = 1f,
        intensity: Float = 1f,
    ): NikiRigPose? {

        if (!validate(graph).valid) {
            return null
        }

        if (!speed.isFinite() || speed <= 0f) {
            return null
        }

        if (!intensity.isFinite() || intensity < 0f) {
            return null
        }

        val preset = NikiMotionLibrary.find(presetId)
            ?: return null

        val frame = NikiMotionEngine.evaluate(
            preset = preset,
            elapsedMs = elapsedMs.coerceAtLeast(0L),
            speed = speed,
            intensity = intensity,
        )

        return apply(graph, frame)
    }

    /**
     * Restores every bone to its original rotation.
     */
    fun reset(graph: NikiRigGraph): NikiRigGraph {
        return graph.copy(
            bones = graph.bones.mapValues { (_, bone) ->
                bone.copy(
                    rotationDegrees = bone.restRotationDegrees
                )
            }
        )
    }

    /**
     * Validates the rig before animation.
     */
    fun validate(graph: NikiRigGraph): NikiRigValidation {

        val errors = mutableListOf<String>()

        if (graph.bones.isEmpty()) {
            errors += "The rig has no bones."
        }

        if (graph.rootBoneIds.isEmpty()) {
            errors += "The rig has no root bone."
        }

        if (graph.rootBoneIds.size != graph.rootBoneIds.distinct().size) {
            errors += "The root list contains duplicate bone IDs."
        }

        graph.rootBoneIds.forEach { rootId ->
            val root = graph.bones[rootId]

            if (root == null) {
                errors += "Root '$rootId' does not exist."
            } else if (root.parentId != null) {
                errors += "Root '$rootId' must not have a parent."
            }
        }

        graph.bones.forEach { (key, bone) ->

            if (key != bone.id) {
                errors += "Bone map key '$key' does not match '${bone.id}'."
            }

            if (!bone.x.isFinite() || !bone.y.isFinite()) {
                errors += "Bone '${bone.id}' has an invalid position."
            }

            if (!bone.length.isFinite() || bone.length < 0f) {
                errors += "Bone '${bone.id}' has an invalid length."
            }

            if (!bone.restRotationDegrees.isFinite()) {
                errors += "Bone '${bone.id}' has an invalid rest rotation."
            }

            if (!bone.rotationDegrees.isFinite()) {
                errors += "Bone '${bone.id}' has an invalid current rotation."
            }

            if (
                bone.parentId != null &&
                bone.parentId !in graph.bones
            ) {
                errors +=
                    "Bone '${bone.id}' references missing parent '${bone.parentId}'."
            }
        }

        // Detect cycles in the parent hierarchy.
        graph.bones.keys.forEach { startId ->

            val visited = mutableSetOf<String>()
            var current: String? = startId

            while (current != null) {

                if (!visited.add(current)) {
                    errors += "Hierarchy cycle involving '$current'."
                    break
                }

                current = graph.bones[current]?.parentId
            }
        }

        // Confirm that every parentless bone is listed as a root.
        val actualRoots = graph.bones.values
            .filter { it.parentId == null }
            .map { it.id }
            .toSet()

        if (actualRoots != graph.rootBoneIds.toSet()) {
            errors += "The root list does not match the parentless bones."
        }

        return NikiRigValidation(
            valid = errors.isEmpty(),
            errors = errors.distinct(),
        )
    }
}
