package com.niki.app

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * First-pass automatic photo rigging.
 *
 * Detects every face returned by ML Kit and creates an independent facial
 * landmark rig for each face. It deliberately does not claim to create a
 * complete body skeleton: that requires a separate multi-person pose model
 * and a renderer/deformation stage.
 */
object NikiPhotoAutoRig {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.06f)
        .enableTracking()
        .build()

    suspend fun createRig(bitmap: Bitmap): NikiPhotoRigProject {
        require(bitmap.width > 0 && bitmap.height > 0) { "The selected photo is empty." }

        val detector = FaceDetection.getClient(options)
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).awaitTask()
                .sortedBy { it.boundingBox.centerX() }

            val people = faces.mapIndexed { index, face ->
                face.toPersonRig(index + 1, bitmap.width, bitmap.height)
            }

            return NikiPhotoRigProject(
                projectId = "niki_${System.currentTimeMillis()}",
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                people = people
            )
        } finally {
            detector.close()
        }
    }

    private fun Face.toPersonRig(
        number: Int,
        imageWidth: Int,
        imageHeight: Int
    ): NikiDetectedPerson {
        val box = Rect(boundingBox)
        val points = linkedMapOf<String, NikiRigPoint>()

        fun add(name: String, landmarkType: Int) {
            val point = getLandmark(landmarkType)?.position ?: return
            points[name] = NikiRigPoint(
                x = (point.x / imageWidth).coerceIn(0f, 1f),
                y = (point.y / imageHeight).coerceIn(0f, 1f)
            )
        }

        add("left_eye", FaceLandmark.LEFT_EYE)
        add("right_eye", FaceLandmark.RIGHT_EYE)
        add("nose_base", FaceLandmark.NOSE_BASE)
        add("mouth_left", FaceLandmark.MOUTH_LEFT)
        add("mouth_right", FaceLandmark.MOUTH_RIGHT)
        add("mouth_bottom", FaceLandmark.MOUTH_BOTTOM)
        add("left_ear", FaceLandmark.LEFT_EAR)
        add("right_ear", FaceLandmark.RIGHT_EAR)

        return NikiDetectedPerson(
            id = "person_$number",
            displayName = "Person $number",
            faceBounds = box,
            normalizedFaceBounds = listOf(
                (box.left.toFloat() / imageWidth).coerceIn(0f, 1f),
                (box.top.toFloat() / imageHeight).coerceIn(0f, 1f),
                (box.right.toFloat() / imageWidth).coerceIn(0f, 1f),
                (box.bottom.toFloat() / imageHeight).coerceIn(0f, 1f)
            ),
            landmarks = points,
            headYaw = headEulerAngleY,
            headPitch = headEulerAngleX,
            headRoll = headEulerAngleZ,
            smilingProbability = smilingProbability,
            leftEyeOpenProbability = leftEyeOpenProbability,
            rightEyeOpenProbability = rightEyeOpenProbability
        )
    }

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
}
