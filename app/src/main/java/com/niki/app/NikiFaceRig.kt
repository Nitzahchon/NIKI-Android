
package com.niki.app

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
