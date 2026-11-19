package com.niki.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun NikiVoiceRecorder(
    onRecordingSaved: (Uri) -> Unit
) {
    val context = LocalContext.current

    var recorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    var player by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var outputFile by remember {
        mutableStateOf<File?>(null)
    }

    var savedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("Ready to record")
    }

    var startTime by remember {
        mutableLongStateOf(0L)
    }

    var elapsedTime by remember {
        mutableLongStateOf(0L)
    }

    val startRecording: () -> Unit = {
        var newRecorder: MediaRecorder? = null

        try {
            val folder = File(
                context.filesDir,
                "recordings"
            )

            folder.mkdirs()

            val file = File(
                folder,
                "niki_voice_${System.currentTimeMillis()}.m4a"
            )

            newRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    MediaRecorder()
                }

            newRecorder.apply {
                setAudioSource(
                    MediaRecorder.AudioSource.MIC
                )

                setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
                )

                setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
                )

                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)

                setOutputFile(file.absolutePath)

                prepare()
                start()
            }

            recorder = newRecorder
            outputFile = file
            savedUri = null
            startTime = SystemClock.elapsedRealtime()
            elapsedTime = 0L
            isRecording = true
            status = "Recording your voice..."
        } catch (e: Exception) {
            newRecorder?.release()
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
                status = "Microphone permission was denied."
            }
        }

    val stopRecording: () -> Unit = {
        val activeRecorder = recorder
        val file = outputFile

        try {
            activeRecorder?.stop()

            if (file != null && file.exists() && file.length() > 0) {
                val uri = Uri.fromFile(file)

                savedUri = uri
                onRecordingSaved(uri)

                status = "Recording saved successfully."
            } else {
                status = "No recording was saved."
            }
        } catch (e: Exception) {
            file?.delete()
            status = "Recording was too short or could not be saved."
        } finally {
            activeRecorder?.release()
            recorder = null
            isRecording = false
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            elapsedTime =
                SystemClock.elapsedRealtime() - startTime

            delay(250)
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
            .fillMaxWidth()
            .background(
                Color(0xFF101A35),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            "RECORD YOUR VOICE",
            color = Color(0xFFB9A2FF),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (isRecording) {
                val seconds = elapsedTime / 1000
                "Recording: %02d:%02d".format(
                    seconds / 60,
                    seconds % 60
                )
            } else {
                status
            },
            color = Color.White,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (isRecording) {
                        stopRecording()
                    } else {
                        val granted =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                        if (granted) {
                            startRecording()
                        } else {
                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (isRecording) "Stop & Save"
                    else "Record Voice"
                )
            }

            OutlinedButton(
                enabled = savedUri != null && !isRecording,
                onClick = {
                    try {
                        if (isPlaying) {
                            player?.pause()
                            isPlaying = false
                        } else {
                            val currentPlayer = player

                            if (currentPlayer == null) {
                                val newPlayer =
                                    MediaPlayer.create(
                                        context,
                                        savedUri
                                    )

                                if (newPlayer != null) {
                                    player = newPlayer

                                    newPlayer.setOnCompletionListener {
                                        it.release()
                                        player = null
                                        isPlaying = false
                                    }

                                    newPlayer.start()
                                    isPlaying = true
                                }
                            } else {
                                currentPlayer.start()
                                isPlaying = true
                            }
                        }
                    } catch (e: Exception) {
                        isPlaying = false
                        status = "Unable to play recording."
                    }
                }
            ) {
                Text(
                    if (isPlaying) "Pause"
                    else "Play"
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            "Recordings are saved inside NIKI.",
            color = Color.LightGray,
            fontSize = 11.sp
        )
    }
}
