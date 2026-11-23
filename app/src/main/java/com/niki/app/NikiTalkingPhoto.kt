--- a/app/src/main/java/com/niki/app/MainActivity.kt
+++ b/app/src/main/java/com/niki/app/MainActivity.kt
@@ -192,10 +192,10 @@
             Button(
                 onClick = {
-                    isPlaying = !isPlaying
+                    selectedTool = "Lip Sync"
                 },
                 colors = ButtonDefaults.buttonColors(
                     containerColor = Purple
                 )
             ) {
-                Text(if (isPlaying) "Pause" else "Play")
+                Text("Talking Photo")
             }
@@ -312,6 +312,7 @@
         // SELECTED TOOL WORKSPACE
         EditorToolPanel(
             selectedTool = selectedTool,
+            hasPhoto = selectedMedia?.type == "Image",
             subtitleText = subtitleText,
             onSubtitleChange = {
                 subtitleText = it
@@ -467,6 +468,7 @@
 @Composable
 fun EditorToolPanel(
     selectedTool: String,
+    hasPhoto: Boolean,
     subtitleText: String,
     onSubtitleChange: (String) -> Unit,
     onImportMedia: () -> Unit,
@@ -573,6 +575,6 @@
     onMouthAmplitude(0f)
 }
-fun attachAudioVisualizer(player: MediaPlayer) {
+fun attachAudioVisualizer(player: MediaPlayer): Boolean {
     stopAudioVisualizer()
 
     try {
@@ -623,8 +625,10 @@
 
         visualizer.enabled = true
         audioVisualizer = visualizer
+        true
     } catch (e: Exception) {
         recorderMessage =
-            "Audio will play, but sound analysis could not start."
+            "Mouth tracking could not start: ${e.message ?: "audio visualizer unavailable"}"
+        false
     }
 }
@@ -669,11 +673,16 @@
                 player.seekTo(0)
             }
 
-            if (enableVisualizer) {
-                attachAudioVisualizer(player)
-            } else {
+            if (!enableVisualizer) {
                 stopAudioVisualizer()
+                recorderMessage = "NIKI needs microphone/audio permission to animate the photo."
+                return
+            }
+            if (!attachAudioVisualizer(player)) {
+                audioPlaying = false
+                return
             }
             player.start()
             audioPlaying = true
+            recorderMessage = "Talking photo is playing."
         }
@@ -692,9 +701,7 @@
         if (granted) {
             pendingAudioPlay = true
         } else {
-            recorderMessage =
-                "Audio will play without mouth animation."
-            startAudioPlayback(false)
+            recorderMessage = "Permission denied. Allow microphone/audio permission to use Talking Photo."
         }
     }
 LaunchedEffect(pendingAudioPlay) {
@@ -701,6 +708,27 @@
         startAudioPlayback()
     }
 }
+
+fun playTalkingPhoto() {
+    if (!hasPhoto) {
+        recorderMessage = "First import and select a photo in the Media tab."
+        return
+    }
+    if (audioUri == null) {
+        recorderMessage = "Record or import a voice recording in the Audio tab first."
+        return
+    }
+    if (ContextCompat.checkSelfPermission(
+            context,
+            Manifest.permission.RECORD_AUDIO
+        ) == PackageManager.PERMISSION_GRANTED
+    ) {
+        startAudioPlayback()
+    } else {
+        visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
+    }
+}
+
     val audioPicker = rememberLauncherForActivityResult(
         ActivityResultContracts.OpenDocument()
     ) { uri ->
@@ -930,41 +958,30 @@
             "Lip Sync" -> {
                 Text(
                     "NIKI LIP SYNC",
                     color = LightPurple,
                     fontWeight = FontWeight.Bold
                 )
                 Spacer(Modifier.height(8.dp))
 
-                Text(
-                    "Create a talking-photo animation prototype.",
-                    color = Color.White
-                )
                 Button(
                     onClick = {
-                        mouthMoving = !mouthMoving
+                        playTalkingPhoto()
                     },
+                    enabled = hasPhoto && audioUri != null,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     Text(
-                        if (mouthMoving)
-                            "Stop Mouth Animation"
-                        else
-                            "Test Mouth Animation"
+                        if (audioPlaying) "Pause Talking Photo"
+                        else "Play Voice + Animate Photo"
                     )
                 }
-                val mouthSize by animateFloatAsState(
-                    targetValue = if (mouthMoving) 1.5f else 1f,
-                    label = "mouth"
-                )
-
-                Text(
-                    "●",
-                    color = LightPurple,
-                    fontSize = (30 * mouthSize).sp,
-                    modifier = Modifier.align(Alignment.CenterHorizontally)
-                )
                 Text(
-                    "This is an animation test, not yet real photo lip synchronization.",
+                    when {
+                        !hasPhoto -> "Step 1: Open Media and import/select a photo."
+                        audioUri == null -> "Step 2: Open Audio and record or import your voice."
+                        recorderMessage.isNotBlank() -> recorderMessage
+                        else -> "Ready. Tap Play Voice + Animate Photo. The photo mouth responds to the playing audio."
+                    },
                     color = Color.LightGray,
                     fontSize = 12.sp
                 )
*** End Patch
