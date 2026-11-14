package com.niki.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NikiHome() }
    }
}

@Composable
private fun NikiHome() {
    val context = LocalContext.current
    var mode by remember { mutableStateOf("Record My Voice") }
    var words by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Your next story starts here.") }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioPath by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val output = File(context.cacheDir, "niki_recording.m4a")
                val media = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
                media.setAudioSource(MediaRecorder.AudioSource.MIC)
                media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                media.setOutputFile(output.absolutePath)
                media.prepare()
                media.start()
                recorder = media
                audioPath = output.absolutePath
                recording = true
                status = "Recording your voice…"
            } catch (e: Exception) {
                status = "Could not start recording. Please try again."
            }
        } else status = "Microphone permission is needed to record."
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF5B3FD1),
            secondary = Color(0xFFEC6B8B),
            background = Color(0xFFF7F5FC),
            surface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("NIKI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Your Voice. Your Story. Your Victory.",
                        style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }

                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Create a story", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = mode == "Record My Voice", onClick = { mode = "Record My Voice" },
                                label = { Text("Record My Voice") })
                            FilterChip(selected = mode == "Type My Words", onClick = { mode = "Type My Words" },
                                label = { Text("Type My Words") })
                        }
                        if (mode == "Record My Voice") {
                            Text("Record narration for your video. You can stop and replay it later as the project grows.")
                            Button(
                                onClick = {
                                    if (!recording) {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            try {
                                                val output = File(context.cacheDir, "niki_recording.m4a")
                                                val media = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
                                                media.setAudioSource(MediaRecorder.AudioSource.MIC)
                                                media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                media.setOutputFile(output.absolutePath)
                                                media.prepare(); media.start()
                                                recorder = media; audioPath = output.absolutePath
                                                recording = true; status = "Recording your voice…"
                                            } catch (_: Exception) { status = "Recording could not start." }
                                        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
                                        recorder = null; recording = false
                                        status = "Recording saved in this session."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (recording) "Stop recording" else "Start recording") }
                            Text(if (audioPath != null) "A recording has been created in the app's temporary storage." else "Your recording will appear here.")
                        } else {
                            Text("Write or paste the words you want to display as subtitles.")
                            OutlinedTextField(
                                value = words, onValueChange = { words = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                                label = { Text("Your story or subtitle text") },
                                placeholder = { Text("Start typing…") }
                            )
                            Text("This starter version lets you draft text. Automatic speech transcription and timed subtitle generation are planned features.")
                        }
                    }
                }

                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE8FF))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Subtitle preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Box(
                            Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF252235), RoundedCornerShape(14.dp)).padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                words.ifBlank { "Your subtitles will appear here." },
                                color = Color.White, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(status, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                Text(
                    "Starter build: recording and text preview are included. Automatic transcription, subtitle timing, media import, effects, music, video rendering and MP4 export still need to be implemented.",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
            }
        }
    }
}
