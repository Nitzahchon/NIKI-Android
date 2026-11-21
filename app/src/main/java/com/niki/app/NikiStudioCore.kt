package com.niki.app

enum class NikiMediaType {
    IMAGE, VIDEO, AUDIO, CHARACTER
}

enum class NikiTrackType {
    VIDEO, AUDIO, SUBTITLE, MOTION, EFFECT
}

enum class NikiSubtitleAnimation {
    NONE,
    SLIDE_DOWN,
    TICKER,
    FADE,
    WORD_BY_WORD
}

data class NikiTrack(
    val id: String,
    val name: String,
    val type: NikiTrackType,
    val muted: Boolean = false,
    val locked: Boolean = false
)

data class NikiTimelineClip(
    val id: String,
    val trackId: String,
    val mediaUri: String?,
    val mediaType: NikiMediaType,
    val startMs: Long,
    val durationMs: Long,
    val sourceInMs: Long = 0L,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val enabled: Boolean = true
) {
    val endMs: Long
        get() = startMs + durationMs
}

data class NikiWordTiming(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class NikiSubtitleCue(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val words: List<NikiWordTiming> = emptyList(),
    val x: Float = 0.5f,
    val y: Float = 0.88f,
    val animation: NikiSubtitleAnimation =
        NikiSubtitleAnimation.NONE,
    val fontSize: Float = 22f
)

data class NikiVoiceSettings(
    val deepVoice: Float = 0f,
    val lightVoice: Float = 0f,
    val volume: Float = 1f,
    val speed: Float = 1f
)

data class NikiRigBone(
    val id: String,
    val parentId: String?,
    val x: Float,
    val y: Float,
    val length: Float,
    val restRotation: Float = 0f,
    val rotation: Float = restRotation
)

data class NikiCharacter(
    val id: String,
    val imageUri: String,
    val rig: NikiRigGraph?
)

data class NikiStudioProject(
    val id: String,
    val name: String,
    val width: Int = 1080,
    val height: Int = 1920,
    val frameRate: Int = 30,
    val tracks: List<NikiTrack> = emptyList(),
    val clips: List<NikiTimelineClip> = emptyList(),
    val subtitles: List<NikiSubtitleCue> = emptyList(),
    val characters: List<NikiCharacter> = emptyList(),
    val voiceSettings: NikiVoiceSettings =
        NikiVoiceSettings()
)

data class NikiTimelineSnapshot(
    val timeMs: Long,
    val activeClips: List<NikiTimelineClip>,
    val activeSubtitles: List<NikiSubtitleCue>
)

object NikiTimelineEngine {

    fun projectDuration(
        project: NikiStudioProject
    ): Long {
        val clipEnd = project.clips.maxOfOrNull {
            it.endMs
        } ?: 0L

        val subtitleEnd =
            project.subtitles.maxOfOrNull {
                it.endMs
            } ?: 0L

        return maxOf(clipEnd, subtitleEnd)
    }

    fun snapshot(
        project: NikiStudioProject,
        timeMs: Long
    ): NikiTimelineSnapshot {

        val time = timeMs.coerceIn(
            0L,
            projectDuration(project)
        )

        val activeClips = project.clips.filter {
            it.enabled &&
            time >= it.startMs &&
            time < it.endMs
        }

        val activeSubtitles =
            project.subtitles.filter {
                time >= it.startMs &&
                time < it.endMs
            }

        return NikiTimelineSnapshot(
            timeMs = time,
            activeClips = activeClips,
            activeSubtitles = activeSubtitles
        )
    }
}

object NikiProjectValidator {

    fun validate(
        project: NikiStudioProject
    ): List<String> {

        val errors = mutableListOf<String>()

        if (project.name.isBlank()) {
            errors.add("Project name is missing.")
        }

        if (project.width <= 0 ||
            project.height <= 0
        ) {
            errors.add("Invalid video dimensions.")
        }

        if (project.frameRate !in 1..120) {
            errors.add("Invalid frame rate.")
        }

        val trackIds = project.tracks.map {
            it.id
        }.toSet()

        project.clips.forEach { clip ->

            if (clip.trackId !in trackIds) {
                errors.add(
                    "Clip ${clip.id} has no valid track."
                )
            }

            if (clip.startMs < 0 ||
                clip.durationMs <= 0
            ) {
                errors.add(
                    "Clip ${clip.id} has invalid timing."
                )
            }

            if (!clip.speed.isFinite() ||
                clip.speed !in 0.1f..4f
            ) {
                errors.add(
                    "Clip ${clip.id} has invalid speed."
                )
            }
        }

        project.subtitles.forEach { cue ->

            if (cue.startMs < 0 ||
                cue.endMs <= cue.startMs
            ) {
                errors.add(
                    "Subtitle ${cue.id} has invalid timing."
                )
            }

            cue.words.forEach { word ->
                if (word.startMs < cue.startMs ||
                    word.endMs > cue.endMs ||
                    word.endMs <= word.startMs
                ) {
                    errors.add(
                        "Invalid word timing in ${cue.id}."
                    )
                }
            }
        }

        return errors
    }
}
