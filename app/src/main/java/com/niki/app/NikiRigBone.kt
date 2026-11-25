
package com.niki.app

/**
 * Individual bone in the NIKI skeletal rig.
 *
 * Coordinates are relative to the parent bone.
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
