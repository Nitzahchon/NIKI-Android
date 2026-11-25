
package com.niki.app

/**
 * Complete NIKI rig graph.
 *
 * Supports:
 * 1. Bone-based skeletal animation.
 * 2. Joint-based PhotoRig connections.
 */
data class NikiRigGraph(
    // Joint-based structure.
    val joints: Map<String, NikiRigJoint> = emptyMap(),
    val connections: Map<String, List<String>> = emptyMap(),
    val rootJointId: String? = null,

    // Bone-based skeletal structure.
    val rootBoneIds: List<String> = emptyList(),
    val bones: Map<String, NikiRigBone> = emptyMap()
)
