# NIKI — Android starter project

**Tagline:** Your Voice. Your Story. Your Victory.

This is a complete, correctly structured Android Gradle starter project for the NIKI app. It includes the Android app module, Kotlin source, manifest, Gradle configuration, and a GitHub Actions workflow that builds a debug APK in the cloud.

## Included in this ZIP

- `app/` — Android application module
- `app/src/main/AndroidManifest.xml` — app manifest and microphone permission
- `app/src/main/java/com/niki/app/MainActivity.kt` — starter app screen and basic voice recording/text preview
- `build.gradle.kts` — root build configuration
- `settings.gradle.kts` — project/module configuration
- `gradle.properties` — Android/Gradle settings
- `.github/workflows/build-apk.yml` — cloud APK build workflow
- `.gitignore` — generated-file exclusions

## Build the APK using GitHub on a phone

1. Create or open a GitHub repository for NIKI.
2. Upload the **contents inside this folder** (not the ZIP file itself). Keep the `.github` folder and its `workflows` subfolder.
3. Commit the files to the `main` branch.
4. Open the repository's **Actions** tab.
5. Select **Build NIKI APK**, then tap **Run workflow** (or let the workflow run after the upload).
6. When the workflow finishes successfully, open the run and download the `NIKI-debug-apk` artifact.

The workflow installs Gradle in GitHub's cloud runner, so a local `gradlew` script and wrapper JAR are not required for this GitHub build route.

## Current implementation status

This is a buildable starter, not the finished NIKI product. It currently provides the app shell, Record My Voice screen with basic microphone recording, Type My Words text entry, and a subtitle preview. Recording is kept in temporary app storage for this starter.

The following agreed product capabilities still require implementation and testing: importing existing audio/video, automatic speech recognition/transcription, synchronized/timed subtitles, subtitle editing and styling, voice effects, text-to-speech, background selection, music/audio mixing, video preview/timeline editing, and MP4 rendering/export.

## Technical setup

- Android application ID: `com.niki.app`
- Minimum Android version: API 23
- Compile/target SDK: 35
- Java: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
