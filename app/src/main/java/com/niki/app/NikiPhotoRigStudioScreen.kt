package com.niki.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Embed this screen in the existing NIKI navigation.
 *
 * onSendToStudio receives BOTH the original bitmap and its generated rig data.
 * The host NIKI Studio should retain the project object instead of importing
 * only the bitmap, otherwise the rig would be lost.
 */
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
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose a photo, then tap AUTO RIG.") }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = "Loading photo…"
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                    if (loaded == null) {
                        status = "NIKI could not open that photo. Please choose another."
                    } else {
                        bitmap = loaded
                        rig = null
                        status = "Photo ready. Tap AUTO RIG to detect the people."
                    }
                } catch (error: Exception) {
                    status = "Could not load photo: ${error.localizedMessage ?: "unknown error"}"
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
            "NIKI PHOTO RIG STUDIO",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text("Prepare one photo for animation. NIKI will detect visible faces together.")

        OutlinedButton(
            onClick = { galleryPicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Choose Photo") }

        bitmap?.let { photo ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "Photo selected for rigging",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        status = "NIKI is detecting every visible face…"
                        try {
                            val result = withContext(Dispatchers.Default) {
                                NikiPhotoAutoRig.createRig(photo)
                            }
                            rig = result
                            status = if (result.personCount == 0) {
                                "No clear faces were detected. Try a brighter, front-facing photo."
                            } else {
                                "Auto Rig complete: ${result.personCount} separate ${if (result.personCount == 1) "person" else "people"} detected."
                            }
                        } catch (error: Exception) {
                            status = "Auto Rig failed: ${error.localizedMessage ?: "Please try another photo."}"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("AUTO RIG — ONE TAP") }

            rig?.let { project ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Detected people: ${project.personCount}", fontWeight = FontWeight.Bold)
                        project.people.forEach { person ->
                            Text(
                                "${person.displayName} • ${person.landmarks.size} facial landmarks"
                            )
                        }
                        Text(
                            "Each detected face has its own rig record. You can select people later in NIKI Studio."
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onEditPhoto(photo) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Edit Photo") }
                    Button(
                        onClick = { onSendToStudio(project, photo) },
                        modifier = Modifier.weight(1f),
                        enabled = project.personCount > 0
                    ) { Text("Send to NIKI Studio") }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = try {
                                withContext(Dispatchers.IO) {
                                    savePhotoAndRig(context, photo, project)
                                }
                                "Photo and rig data saved in your NIKI folder."
                            } catch (error: Exception) {
                                "Save failed: ${error.localizedMessage ?: "unknown error"}"
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Photo + Rig Data to Phone") }
            }
        }

        if (busy) CircularProgressIndicator()
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Current stage: automatic multi-face detection and facial landmark rig data. Full-body joints, photo deformation, breathing, blinking animation and lip-sync rendering are later stages.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.heightIn(min = 8.dp))
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
        put(MediaStore.Images.Media.DISPLAY_NAME, "${project.projectId}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, folder)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)
        ?: error("Could not create image file.")
    resolver.openOutputStream(imageUri)?.use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
            error("Could not write the photo.")
        }
    } ?: error("Could not open image output.")
    if (Build.VERSION.SDK_INT >= 29) {
        val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        resolver.update(imageUri, done, null, null)
    }

    val rigJson = project.toJson().toString(2)
    if (Build.VERSION.SDK_INT >= 29) {
        val jsonValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${project.projectId}.json")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NIKI")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val jsonUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, jsonValues)
            ?: error("Photo saved, but NIKI could not create the rig-data file.")
        resolver.openOutputStream(jsonUri)?.bufferedWriter()?.use { it.write(rigJson) }
            ?: error("Photo saved, but NIKI could not write the rig-data file.")
        val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        resolver.update(jsonUri, done, null, null)
    } else {
        val directory = java.io.File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "NIKI"
        ).apply { mkdirs() }
        java.io.File(directory, "${project.projectId}.json").writeText(rigJson)
    }
}

private fun NikiPhotoRigProject.toJson(): JSONObject {
    val peopleArray = JSONArray()
    people.forEach { person ->
        val landmarksJson = JSONObject()
        person.landmarks.forEach { (name, point) ->
            landmarksJson.put(name, JSONObject().put("x", point.x).put("y", point.y))
        }
        peopleArray.put(
            JSONObject()
                .put("id", person.id)
                .put("displayName", person.displayName)
                .put("faceBoundsNormalized", JSONArray(person.normalizedFaceBounds))
                .put("landmarks", landmarksJson)
                .put("headYaw", person.headYaw)
                .put("headPitch", person.headPitch)
                .put("headRoll", person.headRoll)
                .put("smilingProbability", person.smilingProbability ?: JSONObject.NULL)
                .put("leftEyeOpenProbability", person.leftEyeOpenProbability ?: JSONObject.NULL)
                .put("rightEyeOpenProbability", person.rightEyeOpenProbability ?: JSONObject.NULL)
        )
    }
    return JSONObject()
        .put("projectId", projectId)
        .put("rigVersion", rigVersion)
        .put("rigType", rigType)
        .put("sourceWidth", sourceWidth)
        .put("sourceHeight", sourceHeight)
        .put("createdAtMillis", createdAtMillis)
        .put("people", peopleArray)
}
