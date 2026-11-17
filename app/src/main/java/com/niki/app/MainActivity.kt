package com.niki.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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
            HomeScreen { selectedStudio = it }
        } else {
            StudioScreen(
                studio = selectedStudio!!,
                onBack = { selectedStudio = null }
            )
        }
    }
}

@Composable
fun HomeScreen(onStudioClick: (Studio) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Navy, Purple))
                )
                .padding(26.dp)
        ) {
            Text(
                "NIKI",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                "Your Voice. Your Story. Your Victory.",
                color = Color.White
            )

            Spacer(Modifier.height(28.dp))

            Text(
                "Create something amazing.",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Your creativity starts here.",
                color = LightPurple
            )
        }

        Column(Modifier.padding(20.dp)) {
            Text(
                "MY STUDIOS",
                fontWeight = FontWeight.Bold,
                color = Purple
            )

            Spacer(Modifier.height(14.dp))

            studios.forEach { studio ->
                StudioCard(studio) {
                    onStudioClick(studio)
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "Made with NIKI ❤️",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StudioCard(studio: Studio, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(studio.icon, fontSize = 32.sp)

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    studio.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Text(
                    studio.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text("›", fontSize = 28.sp, color = Purple)
        }
    }
}

@Composable
fun StudioScreen(studio: Studio, onBack: () -> Unit) {
    when (studio.name) {
        "Voice Studio" -> VoiceStudioScreen(onBack)
        "Subtitle Studio" -> SubtitleStudioScreen(onBack)
        "LipSync Studio" -> LipSyncStudioScreen(onBack)

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = onBack) {
                    Text("← Back to NIKI", color = Purple)
                }

                Spacer(Modifier.height(50.dp))

                Text(studio.icon, fontSize = 65.sp)

                Text(
                    studio.name,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Your creative workspace is being prepared.",
                    color = Purple,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// VOICE STUDIO

@Composable
fun VoiceStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var importedAudio by remember { mutableStateOf<Uri?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to record your voice.") }

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

            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
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
            if (granted) startRecording()
            else status = "Microphone permission was not granted."
        }

    val audioPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                importedAudio = uri
                audioFile = null
                status = "Audio imported successfully."
            }
        }

    fun stopRecording() {
        try {
            recorder?.stop()
            status = "Recording saved successfully!"
        } catch (e: Exception) {
            audioFile?.delete()
            audioFile = null
            status = "Recording could not be saved."
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
        }
    }

    fun playRecording() {
        try {
            player?.release()

            val newPlayer = MediaPlayer()
            val file = audioFile
            val uri = importedAudio

            if (file != null && file.exists()) {
                newPlayer.setDataSource(file.absolutePath)
            } else if (uri != null) {
                newPlayer.setDataSource(context, uri)
            } else {
                status = "No audio selected."
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
            status = "Unable to play this audio."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            player?.release()
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
            "🎙️ Voice Studio",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Text(
            "Record your voice. Create your story.",
            color = Color.Gray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (isRecording) "🔴 Recording in progress"
                    else "NIKI Voice Recorder",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Button(
                    onClick = {
                        val permission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )

                        if (permission == PackageManager.PERMISSION_GRANTED) {
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
                    onClick = { audioPicker.launch(arrayOf("audio/*")) },
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
            colors = CardDefaults.cardColors(LightPurple)
        ) {
            Text(
                status,
                modifier = Modifier.padding(18.dp),
                color = Navy,
                textAlign = TextAlign.Center
            )
        }

        Text(
            "Recordings are stored privately inside NIKI.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// SUBTITLE STUDIO

@Composable
fun SubtitleStudioScreen(onBack: () -> Unit) {
    var subtitle by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("Bottom") }
    var alignment by remember { mutableStateOf("Centre") }
    var effect by remember { mutableStateOf("Normal") }
    var textSize by remember { mutableFloatStateOf(24f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to NIKI", color = Purple)
        }

        Text(
            "✍️ Subtitle Studio",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Text("Create and customize your subtitles.", color = Color.Gray)

        OutlinedTextField(
            value = subtitle,
            onValueChange = { subtitle = it },
            label = { Text("Enter your subtitle") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Text("SUBTITLE PREVIEW", fontWeight = FontWeight.Bold, color = Purple)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Color.DarkGray, RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = when (position) {
                "Top" -> Alignment.TopCenter
                "Middle" -> Alignment.Center
                else -> Alignment.BottomCenter
            }
        ) {
            Text(
                subtitle.ifBlank { "Your subtitles appear here" },
                color = Color.White,
                fontSize = textSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = when (alignment) {
                    "Left" -> TextAlign.Left
                    "Right" -> TextAlign.Right
                    else -> TextAlign.Center
                }
            )
        }

        Text("SUBTITLE POSITION", fontWeight = FontWeight.Bold, color = Navy)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Top", "Middle", "Bottom").forEach {
                FilterChip(
                    selected = position == it,
                    onClick = { position = it },
                    label = { Text(it) }
                )
            }
        }

        Text("TEXT ALIGNMENT", fontWeight = FontWeight.Bold, color = Navy)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Left", "Centre", "Right").forEach {
                FilterChip(
                    selected = alignment == it,
                    onClick = { alignment = it },
                    label = { Text(it) }
                )
            }
        }

        Text("SUBTITLE EFFECT", fontWeight = FontWeight.Bold, color = Navy)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Normal", "Scrolling").forEach {
                FilterChip(
                    selected = effect == it,
                    onClick = { effect = it },
                    label = { Text(it) }
                )
            }
        }

        Text(
            "TEXT SIZE: ${textSize.toInt()} SP",
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Slider(
            value = textSize,
            onValueChange = { textSize = it },
            valueRange = 14f..40f
        )

        Button(
            onClick = { subtitle = "" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Subtitle")
        }

        Text(
            "Your subtitle settings are ready for video integration.",
            color = Purple,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// LIPSYNC STUDIO

@Composable
fun LipSyncStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var selectedImage by remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    var imageName by remember {
        mutableStateOf("No photograph selected")
    }

    var speakerSelected by remember {
        mutableStateOf(false)
    }

    var mouthX by remember { mutableFloatStateOf(0.5f) }
    var mouthY by remember { mutableFloatStateOf(0.65f) }

    var audioFile by remember { mutableStateOf<File?>(null) }
    var importedAudio by remember { mutableStateOf<Uri?>(null) }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    var status by remember {
        mutableStateOf("Choose a photograph to begin.")
    }

    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    val bitmap = context.contentResolver
                        .openInputStream(uri)
                        ?.use { BitmapFactory.decodeStream(it) }

                    if (bitmap != null) {
                        selectedImage = bitmap.asImageBitmap()
                        imageName = "Photograph loaded successfully."
                        speakerSelected = false
                        status = "Tap the mouth area of the person who should speak."
                    } else {
                        status = "Unable to open this photograph."
                    }
                } catch (e: Exception) {
                    status = "Image could not be loaded."
                }
            }
        }

    val audioPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                importedAudio = uri
                audioFile = null
                status = "Audio imported. Ready for playback."
            }
        }

    fun startRecording() {
        try {
            player?.release()
            player = null
            isPlaying = false

            val file = File(
                context.filesDir,
                "NIKI_LipSync_${System.currentTimeMillis()}.m4a"
            )

            val newRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            audioFile = file
            importedAudio = null
            isRecording = true
            status = "Recording your speaker's voice..."
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

    fun stopRecording() {
        try {
            recorder?.stop()
            status = "Voice recording saved!"
        } catch (e: Exception) {
            audioFile?.delete()
            audioFile = null
            status = "Recording could not be saved."
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
        }
    }

    fun playAudio() {
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
                status = "Please record or import audio first."
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
            status = "Speaking... Watch the selected person's mouth!"
        } catch (e: Exception) {
            player?.release()
            player = null
            isPlaying = false
            status = "Unable to play this audio."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            player?.release()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mouth")

    val mouthHeight by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 240,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthOpening"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Button(onClick = onBack) {
            Text("← Back to NIKI")
        }

        Text(
            "👄 LipSync Studio",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Text(
            "Choose who speaks. Give your photograph a voice.",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { imagePicker.launch(arrayOf("image/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose Photograph")
        }

        Text(
            imageName,
            color = Purple,
            textAlign = TextAlign.Center
        )

        Text(
            "SELECT YOUR SPEAKER",
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Text(
            "Tap the mouth area of the person you want to speak. " +
                "If there are several people, select only one.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Color(0xFFE3E0EA), RoundedCornerShape(16.dp))
                .pointerInput(selectedImage) {
                    detectTapGestures { tap ->
                        if (selectedImage != null) {
                            mouthX = (tap.x / size.width).coerceIn(0f, 1f)
                            mouthY = (tap.y / size.height).coerceIn(0f, 1f)
                            speakerSelected = true
                            status = "Speaker selected! You can adjust the mouth position by tapping again."
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                Image(
                    bitmap = selectedImage!!,
                    contentDescription = "Selected speaker photograph",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (speakerSelected) {
                    Canvas(Modifier.fillMaxSize()) {
                        val animatedHeight =
                            if (isPlaying) mouthHeight else 5f

                        drawOval(
                            color = Color(0xFF4B1018),
                            topLeft = Offset(
                                x = mouthX * size.width - 21.dp.toPx(),
                                y = mouthY * size.height - animatedHeight.dp.toPx() / 2
                            ),
                            size = Size(
                                width = 42.dp.toPx(),
                                height = animatedHeight.dp.toPx()
                            )
                        )

                        drawOval(
                            color = Color.White.copy(alpha = 0.8f),
                            topLeft = Offset(
                                x = mouthX * size.width - 21.dp.toPx(),
                                y = mouthY * size.height - animatedHeight.dp.toPx() / 2
                            ),
                            size = Size(
                                width = 42.dp.toPx(),
                                height = animatedHeight.dp.toPx()
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx()
                            )
                        )
                    }
                }
            } else {
                Text(
                    "Your photograph will appear here.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            if (speakerSelected) {
                "✅ Speaker selected. Tap again to reposition the mouth."
            } else {
                "No speaker selected yet."
            },
            color = if (speakerSelected) Purple else Color.Gray,
            textAlign = TextAlign.Center
        )

        HorizontalDivider()

        Text(
            "ADD YOUR VOICE",
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Button(
            onClick = {
                val permission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                )

                if (permission == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    permissionLauncher.launch(
                        Manifest.permission.RECORD_AUDIO
                    )
                }
            },
            enabled = !isRecording && !isPlaying,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎙️ Record Voice")
        }

        Button(
            onClick = { stopRecording() },
            enabled = isRecording,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Recording")
        }

        OutlinedButton(
            onClick = { audioPicker.launch(arrayOf("audio/*")) },
            enabled = !isRecording && !isPlaying,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Audio from Phone")
        }

        Button(
            onClick = { playAudio() },
            enabled = !isRecording &&
                !isPlaying &&
                speakerSelected &&
                (audioFile != null || importedAudio != null),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("▶ Play LipSync")
        }

        OutlinedButton(
            onClick = {
                player?.release()
                player = null
                isPlaying = false
                audioFile?.delete()
                audioFile = null
                importedAudio = null
                status = "Audio cleared."
            },
            enabled = !isRecording &&
                (audioFile != null || importedAudio != null),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Audio")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(LightPurple)
        ) {
            Text(
                status,
                modifier = Modifier.padding(18.dp),
                color = Navy,
                textAlign = TextAlign.Center
            )
        }

        Text(
            "This first version uses a simple animated mouth overlay. " +
                "More advanced facial animation and video export will come later.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
