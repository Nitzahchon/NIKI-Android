package com.niki.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private val Navy = Color(0xFF101A35)
private val Purple = Color(0xFF7952D6)
private val LightPurple = Color(0xFFB9A2FF)
private val Background = Color(0xFF080D1B)

data class EditorMedia(
    val uri: Uri,
    val name: String,
    val type: String
)

data class SubtitleClip(
    val text: String,
    val start: Long = 0L,
    val duration: Long = 3000L
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
    EditorScreen()
} 
@Composable
fun EditorScreen() {
    val context = LocalContext.current

    var mediaList by remember { mutableStateOf(listOf<EditorMedia>()) }
    var selectedMedia by remember { mutableStateOf<EditorMedia?>(null) }
    var selectedTool by remember { mutableStateOf("Media") }
    var subtitleText by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    
var mouthFrame by remember {
    mutableStateOf(NikiLipSyncEngine.closedMouth())
}

var mouthPosition by remember {
    mutableStateOf(NikiMouthPosition())
}
    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val newItems = uris.map { uri ->
            val mime = context.contentResolver.getType(uri) ?: ""

            val type = when {
                mime.startsWith("image/") -> "Image"
                mime.startsWith("video/") -> "Video"
                mime.startsWith("audio/") -> "Audio"
                else -> "Media"
            }

            EditorMedia(
                uri = uri,
                name = "Project ${mediaList.size + 1}",
                type = type
            )
        }

        mediaList = mediaList + newItems

        if (selectedMedia == null && newItems.isNotEmpty()) {
            selectedMedia = newItems.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(12.dp)
    ) {

        // HEADER

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "NIKI",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Your Voice. Your Story. Your Victory.",
                    color = LightPurple,
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = {
                    // Export functionality will be connected later.
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Text("Export")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // VIDEO PREVIEW

Box(
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .background(Navy, RoundedCornerShape(16.dp)),
    contentAlignment = Alignment.Center
) {
    if (selectedMedia == null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "NIKI STUDIO",
                color = LightPurple,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Import photos, videos or audio",
                color = Color.White
            )
        }
    } else {
        MediaPreview(
            media = selectedMedia!!,
            subtitle = subtitleText,
            mouthOpenness = mouthFrame.openness,
            mouthPosition = mouthPosition
        )
    }
}
        Spacer(modifier = Modifier.height(10.dp))


        // PLAYBACK CONTROLS

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isPlaying = !isPlaying
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Preview",
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TIMELINE

        Text(
            "PROJECT TIMELINE",
            color = LightPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mediaList.isEmpty()) {
                Text(
                    "Your imported clips will appear here",
                    color = Color.Gray
                )
            }

            mediaList.forEach { item ->
                Card(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .width(105.dp)
                        .fillMaxHeight()
                        .clickable {
                            selectedMedia = item
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (selectedMedia == item)
                                Purple
                            else Navy
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            item.type,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            item.name,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // EDITING TOOLS

        Text(
            "EDITING TOOLS",
            color = LightPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(
                "Media",
                "Audio",
                "Subtitles",
                "Lip Sync",
                "Body Motion",
                "Effects"
            ).forEach { tool ->

                FilterChip(
                    selected = selectedTool == tool,
                    onClick = {
                        selectedTool = tool
                    },
                    label = {
                        Text(tool)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SELECTED TOOL WORKSPACE

        EditorToolPanel(
            selectedTool = selectedTool,
            subtitleText = subtitleText,
            onSubtitleChange = {
                subtitleText = it
            },
            onImportMedia = {
                mediaPicker.launch(
                    arrayOf("image/*", "video/*", "audio/*")
                )
            },
            onAudioSelected = {
                audioUri = it
            },
            onMouthAmplitude = { amplitude ->
                mouthFrame = NikiLipSyncEngine.evaluate(
                    amplitude = amplitude,
                    previousOpenness = mouthFrame.openness,
                    sensitivity = 0.10f,
                    smoothing = 0.55f
                )
            }
        )
    }
}
    
@Composable
fun MediaPreview(
    media: EditorMedia,
    subtitle: String,
    mouthOpenness: Float,
    mouthPosition: NikiMouthPosition
){ 
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        when (media.type) {

            "Image" -> {
                val bitmap = remember(media.uri) {
                    try {
                        context.contentResolver
                            .openInputStream(media.uri)
                            ?.use { BitmapFactory.decodeStream(it) }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "Unable to display image",
                        color = Color.White
                    )
                }
            }

            "Video" -> {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(media.uri)

                            setOnPreparedListener { player ->
                                player.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            "Audio" -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "♫",
                        color = LightPurple,
                        fontSize = 55.sp
                    )

                    Text(
                        "AUDIO ADDED",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        media.name,
                        color = LightPurple,
                        fontSize = 12.sp
                    )
                }
            }

            else -> {
                Text(
                    "Select media to preview",
                    color = Color.White
                )
            }
        }

        // STAGE FOUR: NIKI MOUTH ANIMATION
        if (media.type == "Image") {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val position = mouthPosition.normalized()

                val mouthWidth = size.width * 0.09f

                val mouthHeight = (
                    size.height * 0.025f *
                    mouthOpenness.coerceIn(0f, 1f)
                ).coerceAtLeast(3f)

                drawOval(
                    color = Color(0xFF241018),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        size.width * position.x - mouthWidth / 2,
                        size.height * position.y - mouthHeight / 2
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        mouthWidth,
                        mouthHeight
                    )
                )
            }
        }

        // SUBTITLE DISPLAY
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.65f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun EditorToolPanel(
    selectedTool: String,
    subtitleText: String,
    onSubtitleChange: (String) -> Unit,
    onImportMedia: () -> Unit, 
    onAudioSelected: (Uri) -> Unit,  
    onMouthAmplitude: (Float) -> Unit
) {

    val context = LocalContext.current

    var audioUri by remember { mutableStateOf<Uri?>(null) } 
    
    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioPlaying by remember { mutableStateOf(false) } 
    var audioRecorder by remember { mutableStateOf<MediaRecorder?>(null) } 
    var audioVisualizer by remember {
    mutableStateOf<Visualizer?>(null)
}

var pendingAudioPlay by remember {
    mutableStateOf(false)
}

val mainHandler = remember {
    Handler(Looper.getMainLooper())
}
    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recorderMessage by remember { mutableStateOf("") }

    var mouthMoving by remember { mutableStateOf(false) }
    var bodyMoving by remember { mutableStateOf(false) }
    var effectStrength by remember { mutableFloatStateOf(0.5f) }
    fun startRecording() {
    try {
        val folder = context.getExternalFilesDir(
            android.os.Environment.DIRECTORY_MUSIC
        ) ?: context.filesDir

        val file = File(
            folder,
            "NIKI_${System.currentTimeMillis()}.m4a"
        )

        val recorder = MediaRecorder()

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(file.absolutePath)

        recorder.prepare()
        recorder.start()

        recordingFile = file
        audioRecorder = recorder
        isRecording = true
        recorderMessage = "Recording in progress..."
    } catch (e: Exception) {
        audioRecorder?.release()
        audioRecorder = null
        isRecording = false
        recorderMessage = "Unable to start recording."
    }
}

fun stopRecording() {
    try {
        audioRecorder?.stop()
        audioRecorder?.release()
        audioRecorder = null
        isRecording = false

        recordingFile?.let { file ->
            if (file.exists()) {
                val uri = Uri.fromFile(file)
                audioUri = uri
                onAudioSelected(uri)
                recorderMessage = "Recording saved successfully."
            }
        }
    } catch (e: Exception) {
        recorderMessage = "Recording failed. Please try again."
        audioRecorder?.release()
        audioRecorder = null
        isRecording = false
    }
}

val microphonePermissionLauncher =
    rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            recorderMessage = "Microphone permission was denied."
        }
    } 
    val visualizerPermissionLauncher =
    rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingAudioPlay = true
        } else {
            recorderMessage =
                "Audio will play without mouth animation."
            startAudioPlayback(false)
        }
    }

fun stopAudioVisualizer() {
    try {
        audioVisualizer?.enabled = false
        audioVisualizer?.release()
    } catch (_: Exception) {
    }

    audioVisualizer = null
    onMouthAmplitude(0f)
}

fun attachAudioVisualizer(player: MediaPlayer) {
    stopAudioVisualizer()

    try {
        val visualizer = Visualizer(player.audioSessionId)

        visualizer.captureSize =
            Visualizer.getCaptureSizeRange()[1]

        visualizer.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {

                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    if (waveform == null || waveform.isEmpty()) {
                        return
                    }

                    var peak = 0

                    for (sample in waveform) {
                        val value = kotlin.math.abs(
                            (sample.toInt() and 0xFF) - 128
                        )

                        if (value > peak) {
                            peak = value
                        }
                    }

                    val level = (peak / 128f)
                        .coerceIn(0f, 1f)

                    mainHandler.post {
                        if (audioPlaying) {
                            onMouthAmplitude(level)
                        }
                    }
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                }
            },
            Visualizer.getMaxCaptureRate(),
            true,
            false
        )

        visualizer.enabled = true
        audioVisualizer = visualizer

    } catch (e: Exception) {
        recorderMessage =
            "Audio will play, but sound analysis could not start."
    }
}

fun startAudioPlayback(
    enableVisualizer: Boolean = true
) {
    val uri = audioUri ?: return

    try {
        val player = audioPlayer
            ?: MediaPlayer.create(context, uri)
            ?: run {
                recorderMessage = "Unable to open this audio."
                return
            }

        audioPlayer = player

        player.setOnCompletionListener {
            audioPlaying = false
            stopAudioVisualizer()
        }

        if (player.isPlaying) {
            player.pause()
            audioPlaying = false
            stopAudioVisualizer()
        } else {
            if (
                player.duration > 0 &&
                player.currentPosition >= player.duration
            ) {
                player.seekTo(0)
            }

            if (enableVisualizer) {
                attachAudioVisualizer(player)
            } else {
                stopAudioVisualizer()
            }

            player.start()
            audioPlaying = true
        }

    } catch (e: Exception) {
        audioPlaying = false
        stopAudioVisualizer()
        recorderMessage = "Unable to play audio."
    }
}

LaunchedEffect(pendingAudioPlay) {
    if (pendingAudioPlay) {
        pendingAudioPlay = false
        startAudioPlayback()
    }
}
    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            audioUri = uri
            onAudioSelected(uri)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                onSubtitleChange(
                    if (subtitleText.isBlank()) spokenText
                    else "$subtitleText $spokenText"
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer?.release()
            audioPlayer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState())
            .background(Navy, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {

        when (selectedTool) {

            "Media" -> {
                Text(
                    "MEDIA LIBRARY",
                    color = LightPurple,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onImportMedia,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Photos, Videos or Audio")
                }
            }

            "Audio" -> {
                Text(
                    "AUDIO WORKSPACE",
                    color = LightPurple,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        audioPicker.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Audio")
                }

                Spacer(Modifier.height(8.dp))

Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Button(
        onClick = {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startRecording()
            } else {
                microphonePermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        },
        enabled = !isRecording
    ) {
        Text("Start Recording")
    }

    Button(
        onClick = {
            stopRecording()
        },
        enabled = isRecording
    ) {
        Text("Stop Recording")
    }
}

if (recorderMessage.isNotBlank()) {
    Text(
        recorderMessage,
        color = LightPurple,
        fontSize = 12.sp
    )
}

Spacer(Modifier.height(8.dp))
                if (audioUri != null) {
                    Text(
                        "Audio selected successfully",
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
    onClick = {
        if (audioPlaying) {
            startAudioPlayback()
        } else if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioPlayback()
        } else {
            visualizerPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }
) {
    Text(
        if (audioPlaying) "Pause"
        else "Play Audio + Mouth"
    )
}

                        OutlinedButton(
                            onClick = {
                                audioPlayer?.release()
                                audioPlayer = null
                                audioUri = null
                                audioPlaying = false
                            }
                        ) {
                            Text("Remove")
                        }
                    }
            
               }
          }

            "Subtitles" -> {
                Text(
                    "SUBTITLE STUDIO",
                    color = LightPurple,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtitleText,
                    onValueChange = onSubtitleChange,
                    label = { Text("Write your subtitles") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(
                                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                            ).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE,
                                    Locale.getDefault()
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    "Speak your subtitle"
                                )
                            }

                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            onSubtitleChange(
                                subtitleText.ifBlank {
                                    "Speech recognition is unavailable."
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Speak to Create Subtitles")
                }

                Text(
                    "Your text appears over the preview. Subtitle timing and animation will be added to the timeline in a later stage.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            "Lip Sync" -> {
                Text(
                    "NIKI LIP SYNC",
                    color = LightPurple,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Create a talking-photo animation prototype.",
                    color = Color.White
                )

                Button(
                    onClick = {
                        mouthMoving = !mouthMoving
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (mouthMoving)
                            "Stop Mouth Animation"
                        else
                            "Test Mouth Animation"
                    )
                }

                val mouthSize by animateFloatAsState(
                    targetValue = if (mouthMoving) 1.5f else 1f,
                    label = "mouth"
                )

                Text(
                    "●",
                    color = LightPurple,
                    fontSize = (30 * mouthSize).sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    "This is an animation test, not yet real photo lip synchronization.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
            "Body Motion" -> {
                NikiBodyMotionPanel()
            }

            "Effects" -> {
                Text(
                    "VIDEO EFFECTS",
                    color = LightPurple,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Effect intensity",
                    color = Color.White
                )

                Slider(
                    value = effectStrength,
                    onValueChange = {
                        effectStrength = it
                    },
                    valueRange = 0f..1f
                )

                Text(
                    "${(effectStrength * 100).toInt()}% intensity",
                    color = LightPurple
                )

                Text(
                    "This is an intensity control prototype. Actual video effects will be connected during rendering development.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
