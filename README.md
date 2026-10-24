# V Player Android

Native Android video/audio player project using **Kotlin + AndroidX Media3 (ExoPlayer)**. The library interface is native Android; JavaScript is not required for the playback engine. This architecture is more suitable for device media access and reliable playback than wrapping a web page.

## Included
- GitHub Actions workflow builds a debug APK on push or manual workflow dispatch
- MediaStore library scan for device video/audio, with media permission requests
- Optional Android 11+ All files access settings prompt
- Media list, search, music filter, Shorts filter, folder grouping starter, favorites placeholder, history list
- Media3 playback controls, seek ±10 seconds, speed choices (0.5×–2×), next video and portrait/landscape toggle
- Remembers playback position per media URI and resumes on next open
- About dialog with Muhammed Omor and the supplied Hikmah and GitHub URLs

## Build APK using GitHub Actions
1. Create/open `https://github.com/muhammedomor-studio/V-Player`.
2. Upload the contents of this folder to the repository root (so `settings.gradle.kts` is at root).
3. Commit and push to `main` or `master`, or go to **Actions → Build V Player APK → Run workflow**.
4. Open the successful workflow run, find **Artifacts**, and download `V-Player-debug-apk`. Extract it to get `app-debug.apk`.

## Local build
Requires JDK 17 and Gradle 8.9. Run `gradle assembleDebug`.

## Permissions and Android policy
The app requests media permissions and offers the system's special All files access setting on Android 11+. That special permission is sensitive and Google Play restricts its use; request it only if the app's core functionality genuinely needs broad file management. Android can still restrict what a given API/permission exposes. The system document picker is offered for selecting a folder, but this starter does not yet recursively enumerate every document-tree provider.

## Important status
This is a substantial starter implementation, not yet verified by a real Android build or device test in this environment. Please run the GitHub Actions workflow and inspect build logs before treating it as release-ready. More advanced features such as background playback/media notification, subtitle file picker, gesture brightness/volume, audio track selection UI, thumbnails, persistent Room database, casting, and MP3 transcoding are not included yet.
