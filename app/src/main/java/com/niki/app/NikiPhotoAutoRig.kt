
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
 * NIKI automatic facial rig detection.
 *
 * Detects faces, maps facial landmarks and generates normalized
 * facial rig information for every detected person.
 *
 * Full-body pose detection, segmentation and image deformation
 * require additional processing components.
 */
object NikiPhotoAutoRig {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(
            FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
        )
        .setLandmarkMode(
            FaceDetectorOptions.LANDMARK_MODE_ALL
        )
        .setClassificationMode(
            FaceDetectorOptions.CLASSIFICATION_MODE_ALL
        )
        .setMinFaceSize(0.06f)
        .enableTracking()
        .build()

    suspend fun createRig(bitmap: Bitmap): NikiPhotoRigProject {

        require(!bitmap.isRecycled) {
            "The selected photo is no longer available."
        }

        require(bitmap.width > 0 && bitmap.height > 0) {
            "The selected photo is empty."
        }

        val detector = FaceDetection.getClient(options)

        try {
            val image = InputImage.fromBitmap(bitmap, 0)

            val faces = detector.process(image)
                .awaitTask()
                .sortedBy {
                    it.boundingBox.centerX()
                }

            val people = faces.mapIndexed { index, face ->
                face.toPersonRig(
                    number = index + 1,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height
                )
            }

            return NikiPhotoRigProject(
                projectId = "niki_${System.currentTimeMillis()}",
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                people = people,
                rigVersion = 2,
                rigType = "character_facial_and_body_rig",
                selectedCharacterId = people.firstOrNull()?.id,
                animationReady = false,
                backgroundPreserved = true
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

        val faceWidth = box.width()
            .toFloat()
            .coerceAtLeast(1f)

        val faceHeight = box.height()
            .toFloat()
            .coerceAtLeast(1f)

        val points = linkedMapOf<String, NikiRigPoint>()

        fun normalizedPoint(
            x: Float,
            y: Float
        ): NikiRigPoint {
            return NikiRigPoint(
                x = (x / imageWidth).coerceIn(0f, 1f),
                y = (y / imageHeight).coerceIn(0f, 1f)
            )
        }

        fun addLandmark(
            name: String,
            landmarkType: Int
        ) {
            val position = getLandmark(landmarkType)?.position
                ?: return

            points[name] = normalizedPoint(
                position.x,
                position.y
            )
        }

        addLandmark("left_eye", FaceLandmark.LEFT_EYE)
        addLandmark("right_eye", FaceLandmark.RIGHT_EYE)
        addLandmark("nose_base", FaceLandmark.NOSE_BASE)
        addLandmark("mouth_left", FaceLandmark.MOUTH_LEFT)
        addLandmark("mouth_right", FaceLandmark.MOUTH_RIGHT)
        addLandmark("mouth_bottom", FaceLandmark.MOUTH_BOTTOM)
        addLandmark("left_ear", FaceLandmark.LEFT_EAR)
        addLandmark("right_ear", FaceLandmark.RIGHT_EAR)

        val leftMouth = points["mouth_left"]
        val rightMouth = points["mouth_right"]
        val bottomMouth = points["mouth_bottom"]

        val mouthCenter = if (
            leftMouth != null && rightMouth != null
        ) {
            NikiRigPoint(
                x = (leftMouth.x + rightMouth.x) / 2f,
                y = (leftMouth.y + rightMouth.y) / 2f
            )
        } else {
            null
        }

        /**
         * Mouth dimensions are relative to the detected face.
         * They are not fixed screen-pixel dimensions.
         */
        val mouthWidth = if (
            leftMouth != null && rightMouth != null
        ) {
            kotlin.math.abs(
                rightMouth.x - leftMouth.x
            ) * imageWidth / faceWidth
        } else {
            0f
        }

        val mouthHeight = if (
            mouthCenter != null && bottomMouth != null
        ) {
            2f * kotlin.math.abs(
                bottomMouth.y - mouthCenter.y
            ) * imageHeight / faceHeight
        } else {
            0f
        }

        val faceRig = NikiFaceRig(
            leftEye = points["left_eye"],
            rightEye = points["right_eye"],
            nose = points["nose_base"],
            mouthLeft = leftMouth,
            mouthRight = rightMouth,
            mouthBottom = bottomMouth,
            mouthCenter = mouthCenter,
            mouthWidth = mouthWidth.coerceIn(0f, 1f),
            mouthHeight = mouthHeight.coerceIn(0f, 1f),
            leftEar = points["left_ear"],
            rightEar = points["right_ear"]
        )

        val normalizedBounds = listOf(
            (box.left.toFloat() / imageWidth).coerceIn(0f, 1f),
            (box.top.toFloat() / imageHeight).coerceIn(0f, 1f),
            (box.right.toFloat() / imageWidth).coerceIn(0f, 1f),
            (box.bottom.toFloat() / imageHeight).coerceIn(0f, 1f)
        )

        return NikiDetectedPerson(
            id = "person_$number",
            displayName = "Person $number",
            faceBounds = box,
            normalizedFaceBounds = normalizedBounds,
            landmarks = points,
            headYaw = headEulerAngleY,
            headPitch = headEulerAngleX,
            headRoll = headEulerAngleZ,
            smilingProbability = smilingProbability,
            leftEyeOpenProbability = leftEyeOpenProbability,
            rightEyeOpenProbability = rightEyeOpenProbability,
            faceRig = faceRig,
            characterRig = null,
            characterType = "unknown",
            detectionConfidence = 1f
        )
    }

    /**
     * Converts Google Play Services Task into a coroutine result.
     */
    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->

            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }

            addOnCanceledListener {
                continuation.cancel()
            }
        }
}
