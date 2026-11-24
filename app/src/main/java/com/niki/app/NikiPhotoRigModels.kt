
package com.niki.app

import android.graphics.Rect

/**
 * Normalized image coordinates.
 * x and y range from 0f to 1f.
 */
data class NikiRigPoint(
    val x: Float,
    val y: Float
)

/**
 * Facial rig information.
 */
data class NikiFaceRig(
    val leftEye: NikiRigPoint? = null,
    val rightEye: NikiRigPoint? = null,
    val nose: NikiRigPoint? = null,
    val mouthLeft: NikiRigPoint? = null,
    val mouthRight: NikiRigPoint? = null,
    val mouthBottom: NikiRigPoint? = null,
    val mouthCenter: NikiRigPoint? = null,
    val mouthWidth: Float = 0f,
    val mouthHeight: Float = 0f,
    val leftEar: NikiRigPoint? = null,
    val rightEar: NikiRigPoint? = null
)

/**
 * A body joint detected or manually positioned on a character.
 */
data class NikiRigJoint(
    val id: String,
    val position: NikiRigPoint,
    val parentJointId: String? = null,
    val confidence: Float = 1f,
    val movable: Boolean = true
)

/**
 * PhotoRig data for one independently animated character.
 *
 * This is deliberately named NikiCharacterRigData to avoid
 * conflicting with the existing NikiCharacterRig object.
 */
 
/**
 * Connection graph for a character's body rig.
 * Defines how joints are connected for independent movement.
 */
data class NikiRigGraph(
    val joints: Map<String, NikiRigJoint> = emptyMap(),
    val connections: Map<String, List<String>> = emptyMap(),
    val rootJointId: String? = null
)

data class NikiCharacterRigData(
    val characterId: String,
    val joints: Map<String, NikiRigJoint> = emptyMap(),
    val boneIds: List<String> = emptyList(),
    val graph: NikiRigGraph? = null,
    val isSegmented: Boolean = false,
    val segmentationStatus: String = "not_available",
    val rigGenerated: Boolean = false
)

/**
 * Individual detected person.
 *
 * Existing facial fields are retained for compatibility.
 */
data class NikiDetectedPerson(
    val id: String,
    val displayName: String,
    val faceBounds: Rect,
    val normalizedFaceBounds: List<Float>,
    val landmarks: Map<String, NikiRigPoint>,
    val headYaw: Float,
    val headPitch: Float,
    val headRoll: Float,
    val smilingProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,

    // Extended facial rig.
    val faceRig: NikiFaceRig? = null,

    // Extended body rig.
    val characterRig: NikiCharacterRigData? = null,

    // Character classification.
    val characterType: String = "unknown",

    // Detection confidence.
    val detectionConfidence: Float = 1f
)

/**
 * Complete NIKI PhotoRig project.
 */
data class NikiPhotoRigProject(
    val projectId: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val people: List<NikiDetectedPerson>,
    val rigVersion: Int = 2,
    val rigType: String = "multi_face_landmark_rig_v2",
    val createdAtMillis: Long = System.currentTimeMillis(),

    val selectedCharacterId: String? = null,
    val animationReady: Boolean = false,
    val backgroundPreserved: Boolean = false
) {
    val personCount: Int
        get() = people.size

    val hasFacialRig: Boolean
        get() = people.any { it.faceRig != null }

    val hasBodyRig: Boolean
        get() = people.any { it.characterRig != null }

    val characterIds: List<String>
        get() = people.map { it.id }
}
