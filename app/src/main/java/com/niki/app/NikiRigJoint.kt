
package com.niki.app

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
