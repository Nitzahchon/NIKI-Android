
package com.niki.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun NikiPhotoRigStudioScreen(
    onSendToStudio: (NikiPhotoRigProject, Bitmap) -> Unit,
    onEditPhoto: (Bitmap) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rig by remember { mutableStateOf<NikiPhotoRigProject?>(null) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf("Choose a photo, then tap AUTO RIG.")
    }

    val selectedPerson = rig?.people?.firstOrNull {
        it.id == selectedPersonId
    } ?: rig?.people?.firstOrNull()

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = "Loading photo..."

                try {
                    val loaded = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }

                    if (loaded == null) {
                        status = "NIKI could not open that photo. Choose another."
                    } else {
                        bitmap = loaded
                        rig = null
                        selectedPersonId = null
                        status = "Photo ready. Tap AUTO RIG."
                    }
                } catch (error: Exception) {
                    status = "Could not load photo: ${
                        error.localizedMessage ?: "unknown error"
                    }"
                } finally {
                    busy = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "NIKI PHOTO RIG STUDIO",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Prepare a character photo for rigging and animation."
        )

        OutlinedButton(
            onClick = { galleryPicker.launch("image/*") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CHOOSE PHOTO")
        }

        bitmap?.let { photo ->

            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            photo.width.toFloat() /
                                photo.height.toFloat()
                        )
                ) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = "Selected character photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    selectedPerson?.let { person ->
                        FacialRigOverlay(
                            landmarks = person.landmarks,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Text(
                "Facial rig preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "The coloured points show detected facial landmarks. " +
                    "The mouth outline shows the detected mouth position."
            )

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        status = "NIKI is detecting visible faces..."

                        try {
                            val result = withContext(Dispatchers.Default) {
                                NikiPhotoAutoRig.createRig(photo)
                            }

                            rig = result
                            selectedPersonId =
                                result.people.firstOrNull()?.id

                            status = if (result.personCount == 0) {
                                "No clear faces detected. Try a brighter, " +
                                    "front-facing photo."
                            } else {
                                "Auto Rig complete: ${result.personCount} " +
                                    "person(s) detected."
                            }
                        } catch (error: Exception) {
                            status = "Auto Rig failed: ${
                                error.localizedMessage
                                    ?: "Please try another photo."
                            }"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AUTO RIG — ONE TAP")
            }

            rig?.let { project ->

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "RIGGING RESULTS",
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text("Detected people: ${project.personCount}")

                        Text(
                            "Facial rig detected: ${
                                if (project.hasFacialRig) "Yes" else "Landmarks only"
                            }"
                        )

                        Text(
                            "Body segmentation: Not yet available"
                        )

                        Text(
                            "Select a detected person to inspect their facial landmarks."
                        )
                    }
                }

                if (project.people.size > 1) {
                    Text(
                        "SELECT CHARACTER",
                        fontWeight = FontWeight.Bold
                    )

                    project.people.forEach { person ->
                        OutlinedButton(
                            onClick = {
                                selectedPersonId = person.id
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (selectedPerson?.id == person.id) {
                                    "✓ ${person.displayName} — Selected"
                                } else {
                                    person.displayName
                                }
                            )
                        }
                    }
                }

                selectedPerson?.let { person ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                person.displayName,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Facial landmarks: ${person.landmarks.size}"
                            )

                            Text(
                                "Head rotation — " +
                                    "Yaw: ${person.headYaw.toInt()}°, " +
                                    "Pitch: ${person.headPitch.toInt()}°, " +
                                    "Roll: ${person.headRoll.toInt()}°"
                            )

                            Text(
                                if (
                                    person.landmarks.containsKey("mouth_left") &&
                                    person.landmarks.containsKey("mouth_right")
                                ) {
                                    "Mouth position detected."
                                } else {
                                    "Mouth landmarks were not fully detected."
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onEditPhoto(photo) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EDIT PHOTO")
                    }

                    Button(
                        onClick = {
                            onSendToStudio(project, photo)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = project.personCount > 0
                    ) {
                        Text("SEND TO STUDIO")
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Saving photo and rig data..."

                            try {
                                withContext(Dispatchers.IO) {
                                    savePhotoAndRig(
                                        context,
                                        photo,
                                        project
                                    )
                                }

                                status =
                                    "Photo and rig data saved successfully."
                            } catch (error: Exception) {
                                status = "Save failed: ${
                                    error.localizedMessage ?: "unknown error"
                                }"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE PHOTO + RIG DATA")
                }
            }
        }

        if (busy) {
            CircularProgressIndicator()
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = status,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            "Current PhotoRig stage: multi-face detection and facial " +
                "landmark positioning. Independent body movement, " +
                "character segmentation, mouth deformation and " +
                "lip-sync animation require the animation renderer.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.heightIn(min = 8.dp))
    }
}

@Composable
private fun FacialRigOverlay(
    landmarks: Map<String, NikiRigPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        fun point(name: String): Offset? {
            val value = landmarks[name] ?: return null

            return Offset(
                x = value.x.coerceIn(0f, 1f) * canvasWidth,
                y = value.y.coerceIn(0f, 1f) * canvasHeight
            )
        }

        val mouthLeft = point("mouth_left")
        val mouthRight = point("mouth_right")
        val mouthBottom = point("mouth_bottom")

        // Draw the mouth outline when both mouth corners are detected.
        if (mouthLeft != null && mouthRight != null) {
            val left = minOf(mouthLeft.x, mouthRight.x)
            val right = maxOf(mouthLeft.x, mouthRight.x)
            val centerY = (mouthLeft.y + mouthRight.y) / 2f
            val mouthWidth = (right - left).coerceAtLeast(2f)

            val topY = centerY - mouthWidth * 0.16f
            val bottomY = mouthBottom?.y
                ?.coerceAtLeast(centerY + mouthWidth * 0.08f)
                ?: (centerY + mouthWidth * 0.16f)

            drawOval(
                color = Color.Red,
                topLeft = Offset(left, topY),
                size = Size(
                    width = mouthWidth,
                    height = (bottomY - topY).coerceAtLeast(2f)
                ),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Draw the other detected facial landmarks.
        landmarks.forEach { (name, rigPoint) ->
            if (
                name != "mouth_left" &&
                name != "mouth_right" &&
                name != "mouth_bottom"
            ) {
                val position = Offset(
                    x = rigPoint.x.coerceIn(0f, 1f) * canvasWidth,
                    y = rigPoint.y.coerceIn(0f, 1f) * canvasHeight
                )

                drawCircle(
                    color = Color.Cyan,
                    radius = 4.dp.toPx(),
                    center = position
                )
            }
        }

        // Make the mouth corners visible.
        listOfNotNull(mouthLeft, mouthRight, mouthBottom).forEach {
            drawCircle(
                color = Color.Red,
                radius = 4.dp.toPx(),
                center = Offset(
                    it.x.coerceIn(0f, canvasWidth),
                    it.y.coerceIn(0f, canvasHeight)
                )
            )
        }
    }
}

private fun savePhotoAndRig(
    context: android.content.Context,
    bitmap: Bitmap,
    project: NikiPhotoRigProject
) {
    val resolver = context.contentResolver
    val folder = Environment.DIRECTORY_PICTURES + "/NIKI"

    val imageValues = ContentValues().apply {
        put(
            MediaStore.Images.Media.DISPLAY_NAME,
            "${project.projectId}.jpg"
        )
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, folder)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val imageUri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        imageValues
    ) ?: error("Could not create image file.")

    resolver.openOutputStream(imageUri)?.use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
            error("Could not write the photo.")
        }
    } ?: error("Could not open image output.")

    if (Build.VERSION.SDK_INT >= 29) {
        val done = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(imageUri, done, null, null)
    }

    val rigJson = project.toJson().toString(2)

    if (Build.VERSION.SDK_INT >= 29) {
        val jsonValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "${project.projectId}.json"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/NIKI"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val jsonUri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            jsonValues
        ) ?: error("Photo saved, but rig-data file could not be created.")

        resolver.openOutputStream(jsonUri)?.bufferedWriter()?.use {
            it.write(rigJson)
        } ?: error("Photo saved, but rig-data file could not be written.")

        val done = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        resolver.update(jsonUri, done, null, null)
    } else {
        val directory = java.io.File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "NIKI"
        ).apply {
            mkdirs()
        }

        java.io.File(
            directory,
            "${project.projectId}.json"
        ).writeText(rigJson)
    }
}

private fun NikiPhotoRigProject.toJson(): JSONObject {
    val peopleArray = JSONArray()

    people.forEach { person ->
        val landmarksJson = JSONObject()

        person.landmarks.forEach { (name, point) ->
            landmarksJson.put(
                name,
                JSONObject()
                    .put("x", point.x)
                    .put("y", point.y)
            )
        }

        val personJson = JSONObject()
            .put("id", person.id)
            .put("displayName", person.displayName)
            .put(
                "faceBoundsNormalized",
                JSONArray(person.normalizedFaceBounds)
            )
            .put("landmarks", landmarksJson)
            .put("headYaw", person.headYaw)
            .put("headPitch", person.headPitch)
            .put("headRoll", person.headRoll)
            .put(
                "smilingProbability",
                person.smilingProbability ?: JSONObject.NULL
            )
            .put(
                "leftEyeOpenProbability",
                person.leftEyeOpenProbability ?: JSONObject.NULL
            )
            .put(
                "rightEyeOpenProbability",
                person.rightEyeOpenProbability ?: JSONObject.NULL
            )
            .put("characterType", person.characterType)
            .put("detectionConfidence", person.detectionConfidence)

        peopleArray.put(personJson)
    }

    return JSONObject()
        .put("projectId", projectId)
        .put("rigVersion", rigVersion)
        .put("rigType", rigType)
        .put("sourceWidth", sourceWidth)
        .put("sourceHeight", sourceHeight)
        .put("createdAtMillis", createdAtMillis)
        .put("selectedCharacterId", selectedCharacterId) 
        .put("animationReady", animationReady)
        .put("backgroundPreserved", backgroundPreserved)
 }
