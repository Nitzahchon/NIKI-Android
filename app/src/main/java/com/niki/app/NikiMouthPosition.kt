
package com.niki.app

/**
 * Defines the mouth position on a character's source image.
 *
 * Coordinates are normalized:
 * x = horizontal position (0f to 1f)
 * y = vertical position (0f to 1f)
 * width = mouth width relative to the image width
 */
data class NikiMouthPosition(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val width: Float = 0.06f
) {
    init {
        require(x in 0f..1f) { "Mouth x position must be between 0 and 1." }
        require(y in 0f..1f) { "Mouth y position must be between 0 and 1." }
        require(width in 0f..1f) { "Mouth width must be between 0 and 1." }
    }
}
