package com.niki.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
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
                    subtitle = subtitleText
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
) {
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
    onAudioSelected: (Uri) -> Unit
) {
    val context = LocalContext.current

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioPlaying by remember { mutableStateOf(false) }

    var mouthMoving by remember { mutableStateOf(false) }
    var bodyMoving by remember { mutableStateOf(false) }
    var effectStrength by remember { mutableFloatStateOf(0.5f) }

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
                                try {
                                    if (audioPlaying) {
                                        audioPlayer?.pause()
                                        audioPlaying = false
                                    } else {
                                        if (audioPlayer == null) {
                                            audioPlayer =
                                                MediaPlayer.create(
                                                    context,
                                                    audioUri
                                                )
                                        }

                                        audioPlayer?.setOnCompletionListener {
                                            audioPlaying = false
                                        }

                                        audioPlayer?.start()
                                        audioPlaying = true
                                    }
                                } catch (e: Exception) {
                                    audioPlaying = false
                                }
                            }
                        ) {
                            Text(
                                if (audioPlaying) "Pause" else "Play Audio"
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

                Text(
                    "Voice recording and advanced audio editing will be connected to this workspace in the next development stage.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
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
