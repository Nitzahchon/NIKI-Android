
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
 * PhotoRig data for one independently animated character.
 */
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

    val faceRig: NikiFaceRig? = null,
    val characterRig: NikiCharacterRigData? = null,
    val characterType: String = "unknown",
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
