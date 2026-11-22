package com.niki.app

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Coordinates are normalized to the source image (0f..1f), so the rig remains
 * correctly positioned when the image is displayed at a different size.
 */
data class NikiRigPoint(
    val x: Float,
    val y: Float
)

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
    val rightEyeOpenProbability: Float?
)

data class NikiPhotoRigProject(
    val projectId: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val people: List<NikiDetectedPerson>,
    val rigVersion: Int = 1,
    val rigType: String = "multi_face_landmark_rig",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val personCount: Int get() = people.size
}
