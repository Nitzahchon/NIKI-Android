package com.niki.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File

private val Navy = Color(0xFF10172A)
private val Purple = Color(0xFF7755DD)
private val LightPurple = Color(0xFFE9E2FF)
private val Background = Color(0xFFF6F5FA)

data class Studio(
    val name: String,
    val icon: String,
    val description: String
)

private val studios = listOf(
    Studio("Voice Studio", "🎙️", "Record and transform your voice"),
    Studio("Subtitle Studio", "✍️", "Create and animate your subtitles"),
    Studio("LipSync Studio", "👄", "Bring photographs to life"),
    Studio("BodyMotion Studio", "🕺", "Create gestures and body movements"),
    Studio("Video Studio", "🎬", "Combine your creations into videos")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                NikiApp()
            }
        }
    }
}

@Composable
fun NikiApp() {
    var selectedStudio by remember {
        mutableStateOf<Studio?>(null)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        if (selectedStudio == null) {
            HomeScreen(
                onStudioClick = { selectedStudio = it }
            )
        } else {
            StudioScreen(
                studio = selectedStudio!!,
                onBack = { selectedStudio = null }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onStudioClick: (Studio) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Navy, Purple)
                    )
                )
                .padding(26.dp)
        ) {
            Text(
                text = "NIKI",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "Your Voice. Your Story. Your Victory.",
                fontSize = 15.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Create something amazing.",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Your creativity starts here.",
                fontSize = 14.sp,
                color = LightPurple
            )
        }

        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "MY STUDIOS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Purple
            )

            Spacer(modifier = Modifier.height(14.dp))

            studios.forEach { studio ->
                StudioCard(
                    studio = studio,
                    onClick = { onStudioClick(studio) }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Made with NIKI ❤️",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun StudioCard(
    studio: Studio,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = studio.icon,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = studio.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Text(
                    text = studio.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = Purple
            )
        }
    }
}

@Composable
fun StudioScreen(
    studio: Studio,
    onBack: () -> Unit
) {
    if (studio.name == "Voice Studio") {
        VoiceStudioScreen(onBack)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Back", color = Purple)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = studio.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
            }

            Spacer(modifier = Modifier.height(65.dp))

            Text(
                text = studio.icon,
                fontSize = 65.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = studio.name,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = studio.description,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Your creative workspace is being prepared.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Purple
            )
        }
    }
}

@Composable
fun VoiceStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var recorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    var player by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var audioFile by remember {
        mutableStateOf<File?>(null)
    }

    var importedAudio by remember {
        mutableStateOf<Uri?>(null)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("Ready to record your voice.")
    }

    fun startRecording() {
        try {
            player?.release()
            player = null
            isPlaying = false

            val file = File(
                context.filesDir,
                "NIKI_Voice_${System.currentTimeMillis()}.m4a"
            )

            val newRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

            newRecorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            newRecorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            newRecorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )

            newRecorder.setOutputFile(file.absolutePath)

            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            audioFile = file
            importedAudio = null
            isRecording = true
            status = "Recording your voice..."
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            isRecording = false
            status = "Recording failed: ${e.message}"
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startRecording()
            } else {
                status = "Microphone permission was not granted."
            }
        }

    val audioPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                importedAudio = uri
                audioFile = null
                status = "Audio file imported successfully."
            }
        }

    fun stopRecording() {
        try {
            recorder?.stop()
            status = "Recording saved successfully!"
        } catch (e: Exception) {
            status = "Recording could not be saved."
            audioFile?.delete()
            audioFile = null
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
        }
    }

    fun playRecording() {
        try {
            player?.release()
            player = null

            val newPlayer = MediaPlayer()

            val file = audioFile
            val uri = importedAudio

            if (file != null && file.exists()) {
                newPlayer.setDataSource(file.absolutePath)
            } else if (uri != null) {
                newPlayer.setDataSource(context, uri)
            } else {
                status = "No audio recording selected."
                newPlayer.release()
                return
            }

            newPlayer.setOnCompletionListener {
                isPlaying = false
                status = "Playback completed."
                it.release()
                player = null
            }

            newPlayer.prepare()
            newPlayer.start()

            player = newPlayer
            isPlaying = true
            status = "Playing your audio..."
        } catch (e: Exception) {
            player?.release()
            player = null
            isPlaying = false
            status = "Unable to play this audio file."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.release()
                player?.release()
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("← Back to NIKI")
        }

        Text(
            text = "🎙️ Voice Studio",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Text(
            text = "Record your voice. Create your story.",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isRecording) {
                        "🔴 Recording in progress"
                    } else {
                        "NIKI Voice Recorder"
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Button(
                    onClick = {
                        val permission =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )

                        if (permission ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            startRecording()
                        } else {
                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    },
                    enabled = !isRecording,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Recording")
                }

                Button(
                    onClick = { stopRecording() },
                    enabled = isRecording,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Recording")
                }

                OutlinedButton(
                    onClick = {
                        audioPicker.launch(arrayOf("audio/*"))
                    },
                    enabled = !isRecording,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Audio from Phone")
                }

                Button(
                    onClick = { playRecording() },
                    enabled = !isRecording &&
                        !isPlaying &&
                        (audioFile != null || importedAudio != null),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Play Audio")
                }

                OutlinedButton(
                    onClick = {
                        player?.release()
                        player = null
                        isPlaying = false

                        audioFile?.delete()
                        audioFile = null
                        importedAudio = null

                        status = "Audio selection cleared."
                    },
                    enabled = !isRecording &&
                        (audioFile != null || importedAudio != null),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete / Clear Audio")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = LightPurple
            )
        ) {
            Text(
                text = status,
                modifier = Modifier.padding(18.dp),
                color = Navy,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "Recordings made in NIKI are stored privately inside the app on this device.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
