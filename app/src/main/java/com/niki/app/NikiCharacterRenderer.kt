package com.niki.app

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Live Body Motion test panel. It evaluates NIKI presets, passes each frame through
 * NikiRigMotionAdapter, and draws the resulting articulated human rig on a Compose Canvas.
 * This is a rig-preview renderer; photo cutout artwork binding is a later integration stage.
 */
@Composable
fun NikiBodyMotionPanel() {
    val rig = remember { NikiCharacterRig.createHuman() }
    var selectedPresetId by remember { mutableStateOf("wave") }
    var isPlaying by remember { mutableStateOf(true) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1f) }
    var intensity by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(selectedPresetId) {
        elapsedMs = 0L
        isPlaying = true
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startedAt = SystemClock.elapsedRealtime() - elapsedMs
            while (true) {
                elapsedMs = SystemClock.elapsedRealtime() - startedAt
                delay(16L)
            }
        }
    }

    val pose = remember(rig, selectedPresetId, elapsedMs, speed, intensity) {
        NikiRigMotionAdapter.evaluate(
            graph = rig,
            presetId = selectedPresetId,
            elapsedMs = elapsedMs,
            speed = speed,
            intensity = intensity,
        ) ?: NikiRigMotionAdapter.apply(
            rig,
            NikiMotionEngine.evaluate(NikiMotionLibrary.presets.first(), elapsedMs, speed, intensity),
        )
    }

    LaunchedEffect(pose.finished) {
        if (pose.finished) isPlaying = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("BODY MOTION PREVIEW", color = Color(0xFFB9A2FF), fontSize = 12.sp)
        NikiCharacterRenderer(pose, Modifier.fillMaxWidth().height(176.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NikiMotionLibrary.presets.forEach { preset ->
                FilterChip(
                    selected = selectedPresetId == preset.id,
                    onClick = { selectedPresetId = preset.id },
                    label = { Text(preset.title, fontSize = 11.sp) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (isPlaying) {
                        isPlaying = false
                    } else {
                        if (pose.finished) elapsedMs = 0L
                        isPlaying = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7952D6)),
            ) { Text(if (isPlaying) "Pause" else "Play") }
            OutlinedButton(onClick = { elapsedMs = 0L; isPlaying = false }) { Text("Reset") }
        }

        Text("Speed ${(speed * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2f)
        Text("Intensity ${(intensity * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
        Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 0.25f..1.5f)
        Text(
            "This preview animates the NIKI human rig. Attaching separated photo artwork is the next renderer stage.",
            color = Color.LightGray,
            fontSize = 10.sp,
        )
    }
}

/** Draws the articulated rig, including joints and a head silhouette, from the evaluated pose. */
@Composable
fun NikiCharacterRenderer(pose: NikiRigPose, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF101A35), RoundedCornerShape(12.dp))
            .padding(4.dp),
    ) {
        val world = NikiCharacterRig.worldTransforms(pose.graph)
        val scale = minOf(size.width / 250f, size.height / 380f)
        val originX = size.width / 2f
        // Keep the complete head-to-feet rig inside the preview canvas.
        val originY = size.height * 0.46f + pose.rootYOffset * scale
        fun point(x: Float, y: Float) = Offset(originX + x * scale, originY + y * scale)

        val limbColor = Color(0xFFE5DCFF)
        val jointColor = Color(0xFFB9A2FF)
        val lineWidth = (7f * scale).coerceIn(3f, 8f)

        fun segment(id: String) {
            val bone = world[id] ?: return
            drawLine(
                color = limbColor,
                start = point(bone.startX, bone.startY),
                end = point(bone.endX, bone.endY),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round,
            )
            drawCircle(jointColor, radius = (4.5f * scale).coerceIn(2.5f, 5f), center = point(bone.startX, bone.startY))
            drawCircle(jointColor, radius = (4.5f * scale).coerceIn(2.5f, 5f), center = point(bone.endX, bone.endY))
        }

        // Shoulder and hip bars make the joint connections easier to read.
        val leftShoulder = world["left_upper_arm"]?.let { point(it.startX, it.startY) }
        val rightShoulder = world["right_upper_arm"]?.let { point(it.startX, it.startY) }
        if (leftShoulder != null && rightShoulder != null) {
            drawLine(limbColor, leftShoulder, rightShoulder, lineWidth, cap = StrokeCap.Round)
        }
        val leftHip = world["left_thigh"]?.let { point(it.startX, it.startY) }
        val rightHip = world["right_thigh"]?.let { point(it.startX, it.startY) }
        if (leftHip != null && rightHip != null) {
            drawLine(limbColor, leftHip, rightHip, lineWidth, cap = StrokeCap.Round)
        }

        listOf(
            "torso", "left_upper_arm", "left_forearm", "left_hand",
            "right_upper_arm", "right_forearm", "right_hand",
            "left_thigh", "left_shin", "left_foot",
            "right_thigh", "right_shin", "right_foot",
        ).forEach(::segment)

        world["head"]?.let { head ->
            val center = point(
                (head.startX + head.endX) / 2f,
                (head.startY + head.endY) / 2f,
            )
            drawCircle(
                color = limbColor,
                radius = 19f * scale,
                center = center,
                style = Stroke(width = lineWidth),
            )
            drawCircle(jointColor, radius = 3f * scale, center = point(head.startX, head.startY))
        }
    }
}
