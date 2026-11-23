package com.niki.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

private val Navy = Color(0xFF101A35)
private val Purple = Color(0xFF7952D6)
private val LightPurple = Color(0xFFB9A2FF)
private val Background = Color(0xFF080D1B)

data class EditorMedia(
    val uri: Uri,
    val name: String,
    val type: String,
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

    var mediaList by remember {
        mutableStateOf(emptyList<EditorMedia>())
    }

    var selectedMedia by remember {
        mutableStateOf<EditorMedia?>(null)
    }

    var audioUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var audioPlayer by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var videoView by remember {
        mutableStateOf<VideoView?>(null)
    }

    var mouthOpenness by remember {
        mutableFloatStateOf(0f)
    }

    var mouthPosition by remember {
        mutableStateOf(NikiMouthPosition())
    }

    var selectedPersonIndex by remember {
        mutableIntStateOf(0)
    }

    var detectedPeopleCount by remember {
        mutableIntStateOf(0)
    }

    var recorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    var recordingMessage by remember {
        mutableStateOf("")
    }

    var visualizer by remember {
        mutableStateOf<Visualizer?>(null)
    }

    var selectedTool by remember {
        mutableStateOf("Media")
    }

    var selectedMotion by remember {
        mutableStateOf("idle")
    }

    var motionIntensity by remember {
        mutableFloatStateOf(1f)
    }

    var motionSpeed by remember {
        mutableFloatStateOf(1f)
    }

    var motionElapsed by remember {
        mutableLongStateOf(0L)
    }

    var motionPose by remember {
        mutableStateOf<NikiRigPose?>(null)
    }

    var motionStartedAt by remember {
        mutableLongStateOf(0L)
    }

    var effectStrength by remember {
        mutableFloatStateOf(0.5f)
    }

    var imageBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var photoRig by remember {
        mutableStateOf<NikiPhotoRig?>(null)
    }

    /*
     * Build a lightweight graph using the bone IDs
     * already defined in NikiMotionLibrary.
     *
     * This graph supplies motion data to the adapter.
     * It is not a full photographic character rig.
     */
    val motionGraph = remember {

        val boneIds = NikiMotionLibrary.presets
            .flatMap { preset ->
                preset.channels.map { it.boneId }
            }
            .distinct()

        val bones = boneIds.associateWith { id ->

            NikiRigBone(
                id = id,
                parentId = null,
                x = 0f,
                y = 0f,
                length = 1f,
                restRotationDegrees = 0f,
            )
        }

        NikiRigGraph(
            rootBoneIds = boneIds,
            bones = bones,
        )
    }

    /*
     * Media picker.
     */
    val mediaPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            val imported = uris.mapIndexed { index, uri ->

                val mime = context.contentResolver
                    .getType(uri) ?: ""

                val type = when {
                    mime.startsWith("image/") -> "Image"
                    mime.startsWith("video/") -> "Video"
                    mime.startsWith("audio/") -> "Audio"
                    else -> "Media"
                }

                EditorMedia(
                    uri = uri,
                    name = "Media ${mediaList.size + index + 1}",
                    type = type,
                )
            }

            mediaList = mediaList + imported

            imported.firstOrNull()?.let { item ->

                if (item.type == "Audio") {
                    audioUri = item.uri
                } else {
                    selectedMedia = item
                }
            }
        }

    /*
     * Audio picker.
     */
    val audioPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {
                audioUri = uri
                recordingMessage = "Audio imported successfully."
            }
        }

    /*
     * Recording permission.
     */
    val microphonePermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startNikiRecording(
                    context = context,
                    onRecorderReady = {
                        recorder = it
                        isRecording = true
                        recordingMessage = "Recording in progress..."
                    },
                    onError = {
                        recordingMessage = it
                    },
                )
            } else {
                recordingMessage = "Microphone permission denied."
            }
        }

    /*
     * Audio visualizer permission.
     */
    val visualizerPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                recordingMessage = "Audio analysis permission granted."
            } else {
                recordingMessage =
                    "Audio can play, but mouth analysis may be unavailable."
            }
        }

    /*
     * Load selected image and detect people.
     */
    LaunchedEffect(selectedMedia?.uri) {

        val media = selectedMedia

        imageBitmap = null
        photoRig = null
        detectedPeopleCount = 0
        selectedPersonIndex = 0
        mouthPosition = NikiMouthPosition()
        mouthOpenness = 0f

        if (media?.type == "Image") {

            val bitmap = withContext(Dispatchers.IO) {

                try {
                    context.contentResolver
                        .openInputStream(media.uri)
                        ?.use {
                            BitmapFactory.decodeStream(it)
                        }
                } catch (_: Exception) {
                    null
                }
            }

            imageBitmap = bitmap

            if (bitmap != null) {

                try {

                    val rig = withContext(Dispatchers.Default) {
                        NikiPhotoAutoRig.createRig(bitmap)
                    }

                    photoRig = rig
                    detectedPeopleCount = rig.people.size

                    val person = rig.people.firstOrNull()

                    val left = person?.landmarks?.get("mouth_left")
                    val right = person?.landmarks?.get("mouth_right")
                    val bottom = person?.landmarks?.get("mouth_bottom")

                    if (left != null && right != null) {

                        mouthPosition = NikiMouthPosition(
                            x = (left.x + right.x) / 2f,
                            y = if (bottom != null) {
                                (left.y + right.y + bottom.y) / 3f
                            } else {
                                (left.y + right.y) / 2f
                            },
                            width = abs(right.x - left.x) * 1.25f,
                        )
                    }

                } catch (_: Exception) {
                    recordingMessage =
                        "Automatic face detection was unsuccessful."
                }
            }
        }
    }

    /*
     * Update the mouth position when the selected speaker changes.
     */
    LaunchedEffect(selectedPersonIndex, photoRig) {

        val person = photoRig
            ?.people
            ?.getOrNull(selectedPersonIndex)

        val left = person?.landmarks?.get("mouth_left")
        val right = person?.landmarks?.get("mouth_right")
        val bottom = person?.landmarks?.get("mouth_bottom")

        if (left != null && right != null) {

            mouthPosition = NikiMouthPosition(
                x = (left.x + right.x) / 2f,
                y = if (bottom != null) {
                    (left.y + right.y + bottom.y) / 3f
                } else {
                    (left.y + right.y) / 2f
                },
                width = abs(right.x - left.x) * 1.25f,
            )
        }

        mouthOpenness = 0f
    }

    /*
     * Evaluate the selected motion preset while Preview is playing.
     */
    LaunchedEffect(isPlaying, selectedMotion) {

        if (isPlaying) {

            motionStartedAt = SystemClock.elapsedRealtime()

            while (isPlaying) {

                motionElapsed =
                    SystemClock.elapsedRealtime() - motionStartedAt

                motionPose = NikiRigMotionAdapter.evaluate(
                    graph = motionGraph,
                    presetId = selectedMotion,
                    elapsedMs = motionElapsed,
                    speed = motionSpeed,
                    intensity = motionIntensity,
                )

                delay(33L)
            }

        } else {

            motionPose = NikiRigMotionAdapter.apply(
                graph = NikiRigMotionAdapter.reset(motionGraph),
                frame = NikiMotionEngine.evaluate(
                    preset = NikiMotionLibrary.find(selectedMotion)
                        ?: NikiMotionLibrary.presets.first(),
                    elapsedMs = 0L,
                ),
            )
        }
    }

    /*
     * Stop and release resources when the screen is destroyed.
     */
    DisposableEffect(Unit) {

        onDispose {

            try {
                visualizer?.enabled = false
                visualizer?.release()
            } catch (_: Exception) {
            }

            audioPlayer?.release()
            videoView?.stopPlayback()

            try {
                recorder?.release()
            } catch (_: Exception) {
            }
        }
    }

    /*
     * Playback functions.
     */
    fun stopPlayback() {

        isPlaying = false
        mouthOpenness = 0f

        try {
            audioPlayer?.pause()
            audioPlayer?.seekTo(0)
        } catch (_: Exception) {
        }

        try {
            videoView?.pause()
            videoView?.seekTo(0)
        } catch (_: Exception) {
        }

        try {
            visualizer?.enabled = false
        } catch (_: Exception) {
        }
    }

    fun pausePlayback() {

        isPlaying = false

        try {
            audioPlayer?.pause()
        } catch (_: Exception) {
        }

        try {
            videoView?.pause()
        } catch (_: Exception) {
        }

        try {
            visualizer?.enabled = false
        } catch (_: Exception) {
        }

        mouthOpenness = 0f
    }

    fun playPlayback() {

        val media = selectedMedia

        if (media == null && audioUri == null) {
            recordingMessage = "Import media or audio first."
            return
        }

        try {

            if (media?.type == "Video") {
                videoView?.start()
            }

            if (audioUri != null) {

                if (audioPlayer == null) {

                    audioPlayer = MediaPlayer.create(
                        context,
                        audioUri,
                    )
                }

                audioPlayer?.setOnCompletionListener {
                    isPlaying = false
                    mouthOpenness = 0f
                }

                audioPlayer?.start()
            }

            isPlaying = true

        } catch (e: Exception) {

            recordingMessage =
                "Unable to start preview: ${e.message}"
        }
    }

    /*
     * Remove picture without removing audio.
     */
    fun removePicture() {

        pausePlayback()

        selectedMedia = null
        imageBitmap = null
        photoRig = null
        detectedPeopleCount = 0
        selectedPersonIndex = 0

        mouthPosition = NikiMouthPosition()
        mouthOpenness = 0f
        motionPose = null

        videoView?.stopPlayback()
        videoView = null
    }

    /*
     * Remove audio without removing picture.
     */
    fun removeAudio() {

        pausePlayback()

        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }

        visualizer = null

        audioPlayer?.release()
        audioPlayer = null

        audioUri = null
        mouthOpenness = 0f

        recordingMessage = "Audio removed."
    }

    /*
     * User interface.
     */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(12.dp),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Column {

                Text(
                    "NIKI",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    "Your Voice. Your Story. Your Victory.",
                    color = LightPurple,
                    fontSize = 10.sp,
                )
            }

            Text(
                "STUDIO",
                color = LightPurple,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))

        /*
         * PREVIEW
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {

            when (selectedMedia?.type) {

                "Image" -> {

                    val bitmap = imageBitmap

                    if (bitmap != null) {

                        val headRotation =
                            motionPose
                                ?.graph
                                ?.bones
                                ?.get("head")
                                ?.rotationDegrees ?: 0f

                        val torsoRotation =
                            motionPose
                                ?.graph
                                ?.bones
                                ?.get("torso")
                                ?.rotationDegrees ?: 0f

                        val rootMovement =
                            motionPose?.rootYOffset ?: 0f

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "NIKI photo preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {

                                    rotationZ =
                                        (headRotation * 0.25f +
                                            torsoRotation * 0.35f)
                                            .coerceIn(-12f, 12f)

                                    translationY =
                                        rootMovement * size.height
                                },
                        )

                        /*
                         * Mouth overlay is drawn only for the selected person.
                         * Other detected people are not animated.
                         */
                        Canvas(
                            modifier = Modifier.fillMaxSize(),
                        ) {

                            val position = mouthPosition

                            val centerX =
                                size.width * position.x

                            val centerY =
                                size.height * position.y

                            val mouthWidth =
                                size.width * position.width
                                    .coerceIn(0.005f, 0.3f)

                            val openness =
                                mouthOpenness.coerceIn(0f, 1f)

                            val mouthHeight =
                                (mouthWidth *
                                    (0.08f + openness * 0.55f))
                                    .coerceAtLeast(2f)

                            drawOval(
                                color = Color(0xFF301019),
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    centerX - mouthWidth / 2f,
                                    centerY - mouthHeight / 2f,
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    mouthWidth,
                                    mouthHeight,
                                ),
                            )
                        }

                    } else {

                        Text(
                            "Loading photograph...",
                            color = Color.White,
                        )
                    }
                }

                "Video" -> {

                    key(selectedMedia?.uri) {

                        AndroidView(
                            factory = { ctx ->

                                VideoView(ctx).apply {

                                    setVideoURI(selectedMedia!!.uri)

                                    setOnPreparedListener { player ->
                                        player.isLooping = true
                                        // Do not autoplay.
                                    }

                                    videoView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                "Audio" -> {

                    Text(
                        "AUDIO SELECTED",
                        color = LightPurple,
                        fontWeight = FontWeight.Bold,
                    )
                }

                else -> {

                    Text(
                        "Import a photo, video or audio file",
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        /*
         * PLAYBACK BUTTONS
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {

            Button(
                onClick = {
                    if (isPlaying) pausePlayback()
                    else playPlayback()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple,
                ),
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }

            Spacer(Modifier.width(8.dp))

            OutlinedButton(
                onClick = { stopPlayback() },
            ) {
                Text("Stop")
            }
        }

        Spacer(Modifier.height(10.dp))

        /*
         * MEDIA CONTROLS
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Button(
                onClick = {
                    mediaPicker.launch(
                        arrayOf("image/*", "video/*", "audio/*")
                    )
                },
            ) {
                Text("Import Media")
            }

            OutlinedButton(
                onClick = { removePicture() },
                enabled = selectedMedia?.type == "Image" ||
                    selectedMedia?.type == "Video",
            ) {
                Text("Remove Picture")
            }

            OutlinedButton(
                onClick = { removeAudio() },
                enabled = audioUri != null,
            ) {
                Text("Remove Audio")
            }
        }

        Spacer(Modifier.height(8.dp))

        /*
         * TIMELINE
         */
        Text(
            "PROJECT TIMELINE",
            color = LightPurple,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {

            mediaList.forEach { item ->

                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(100.dp)
                        .clickable {

                            if (item.type == "Audio") {
                                audioUri = item.uri
                            } else {
                                pausePlayback()
                                selectedMedia = item
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (selectedMedia == item) Purple
                            else Navy,
                    ),
                ) {

                    Column(
                        modifier = Modifier.padding(8.dp),
                    ) {

                        Text(
                            item.type,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            item.name,
                            color = LightPurple,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        /*
         * TOOL SELECTOR
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            listOf(
                "Media",
                "Audio",
                "Lip Sync",
                "Body Motion",
                "Effects",
            ).forEach { tool ->

                FilterChip(
                    selected = selectedTool == tool,
                    onClick = {
                        selectedTool = tool
                    },
                    label = {
                        Text(tool)
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        /*
         * TOOL WORKSPACE
         */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .background(
                    Navy,
                    RoundedCornerShape(14.dp),
                )
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        ) {

            when (selectedTool) {

                "Media" -> {

                    Text(
                        "MEDIA LIBRARY",
                        color = LightPurple,
                        fontWeight = FontWeight.Bold,
                    )

                    Button(
                        onClick = {
                            mediaPicker.launch(
                                arrayOf("image/*", "video/*", "audio/*")
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Import Photos, Videos or Audio")
                    }
                }

                "Audio" -> {

                    Text(
                        "AUDIO STUDIO",
                        color = LightPurple,
                        fontWeight = FontWeight.Bold,
                    )

                    Button(
                        onClick = {
                            audioPicker.launch(arrayOf("audio/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Import Audio")
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {

                        Button(
                            onClick = {

                                if (
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO,
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {

                                    startNikiRecording(
                                        context,
                                        onRecorderReady = {
                                            recorder = it
                                            isRecording = true
                                            recordingMessage =
                                                "Recording in progress..."
                                        },
                                        onError = {
                                            recordingMessage = it
                                        },
                                    )

                                } else {

                                    microphonePermission.launch(
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                }
                            },
                            enabled = !isRecording,
                        ) {
                            Text("Record")
                        }

                        Button(
                            onClick = {

                                try {

                                    recorder?.stop()
                                    recorder?.release()
                                    recorder = null

                                    isRecording = false

                                    val file = lastNikiRecordingFile

                                    if (file != null && file.exists()) {

                                        audioUri = Uri.fromFile(file)

                                        recordingMessage =
                                            "Recording saved."
                                    }

                                } catch (_: Exception) {

                                    recordingMessage =
                                        "Recording could not be saved."
                                }
                            },
                            enabled = isRecording,
                        ) {
                            Text("Stop Recording")
                        }
                    }

                    if (audioUri != null) {

                        Text(
                            "Audio is ready for preview.",
                            color = Color.White,
                        )

                        Button(
                            onClick = { playPlayback() },
                        ) {
                            Text("Play Audio")
                        }
                    }

                    Text(
                        recordingMessage,
                        color = LightPurple,
                        fontSize = 12.sp,
                    )
                }

                "Lip Sync" -> {

                    Text(
                        "NIKI LIP SYNC",
                        color = LightPurple,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        "Detected people: $detectedPeopleCount",
                        color = Color.White,
                    )

                    if (detectedPeopleCount > 1) {

                        Text(
                            "Select the person speaking:",
                            color = Color.White,
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(
                                rememberScrollState()
                            ),
                        ) {

                            repeat(detectedPeopleCount) { index ->

                                FilterChip(
                                    selected =
                                        selectedPersonIndex == index,
                                    onClick = {
                                        selectedPersonIndex = index
                                    },
                                    label = {
                                        Text("Person ${index + 1}")
                                    },
                                )

                                Spacer(Modifier.width(5.dp))
                            }
                        }
                    }

                    Text(
                        "Mouth position X",
                        color = Color.White,
                    )

                    Slider(
                        value = mouthPosition.x.coerceIn(0f, 1f),
                        onValueChange = {
                            mouthPosition = mouthPosition.copy(x = it)
                        },
                    )

                    Text(
                        "Mouth position Y",
                        color = Color.White,
                    )

                    Slider(
                        value = mouthPosition.y.coerceIn(0f, 1f),
                        onValueChange = {
                            mouthPosition = mouthPosition.copy(y = it)
                        },
                    )

                    Text(
                        "Mouth width",
                        color = Color.White,
                    )

                    Slider(
                        value = mouthPosition.width.coerceIn(0.01f, 0.3f),
                        onValueChange = {
                            mouthPosition = mouthPosition.copy(width = it)
                        },
                        valueRange = 0.01f..0.3f,
                    )

                    Text(
                        "Mouth movement responds to audio analysis when the audio visualizer is connected.",
                        color = LightPurple,
                        fontSize = 11.sp,
                    )
                }

                "Body Motion" -> {

                    Text(
                        "BODY MOTION STUDIO",
                        color = LightPurple,
                        fontWeight = FontWeight.Bold,
                    )

                    NikiMotionLibrary.presets.forEach { preset ->

                        FilterChip(
                            selected = selectedMotion == preset.id,
                            onClick = {
                                selectedMotion = preset.id
                                motionElapsed = 0L
                            },
                            label = {
                                Text(preset.title)
                            },
                        )
                    }

                    Text(
                        "Motion intensity",
                        color = Color.White,
                    )

                    Slider(
                        value = motionIntensity,
                        onValueChange = {
                            motionIntensity = it
                        },
                        valueRange = 0f..2f,
                    )

                    Text(
                        "${(motionIntensity * 100).toInt()}%",
                        color = LightPurple,
                    )

                    Text(
                        "Motion speed",
                        color = Color.White,
                    )

                    Slider(
                        value = motionSpeed,
                        onValueChange = {
                            motionSpeed = it
                        },
                        valueRange = 0.1f..4f,
                    )

                    Text(
                        "Motion is evaluated by NikiMotionEngine and applied through NikiRigMotionAdapter. The current photo preview applies whole-image movement, not independent limb deformation.",
                        color = LightPurple,
                        fontSize = 11.sp,
                    )
                }

                "Effects" -> {

                    Text(
                        "EFFECTS",
                        color = LightPurple,
                        fontWeight = FontWeight.Bold,
                    )

                    Slider(
                        value = effectStrength,
                        onValueChange = {
                            effectStrength = it
                        },
                        valueRange = 0f..1f,
                    )

                    Text(
                        "Effect intensity: ${(effectStrength * 100).toInt()}%",
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/*
 * Recording helper.
 */
private var lastNikiRecordingFile: File? = null

private fun startNikiRecording(
    context: android.content.Context,
    onRecorderReady: (MediaRecorder) -> Unit,
    onError: (String) -> Unit,
) {

    try {

        val folder = context.getExternalFilesDir(
            android.os.Environment.DIRECTORY_MUSIC
        ) ?: context.filesDir

        val file = File(
            folder,
            "NIKI_${System.currentTimeMillis()}.m4a",
        )

        val mediaRecorder = MediaRecorder()

        mediaRecorder.setAudioSource(
            MediaRecorder.AudioSource.MIC
        )

        mediaRecorder.setOutputFormat(
            MediaRecorder.OutputFormat.MPEG_4
        )

        mediaRecorder.setAudioEncoder(
            MediaRecorder.AudioEncoder.AAC
        )

        mediaRecorder.setOutputFile(file.absolutePath)

        mediaRecorder.prepare()
        mediaRecorder.start()

        lastNikiRecordingFile = file

        onRecorderReady(mediaRecorder)

    } catch (e: Exception) {

        onError(
            "Unable to start recording: ${e.message}"
        )
    }
}
