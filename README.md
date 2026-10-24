# VPlayer — Ultimate Android Video Player Upgrade

VPlayer is a modern, lightweight, and powerful Android Video Player built with Kotlin, AndroidX Media3 (ExoPlayer), Jetpack components, and Material 3 design.

## Features
- **AndroidX Media3 ExoPlayer Engine**: Supports MP4, MKV, WebM, MOV, TS, HDR, and high-fps videos with hardware acceleration.
- **Advanced Touch Controls**: Double-tap for 10s seek, vertical swipe gestures for volume and brightness control.
- **Video Scaling**: Responsive Fit, Crop, and Stretch aspect ratio scaling modes.
- **Picture-in-Picture (PiP)**: Seamless background and mini-player video playback on supported Android versions.
- **Subtitle Integration**: External subtitle picker supporting `.srt` files.
- **Playback Resumption**: Remembers exact saved playback positions for all media files.
- **Scoped Storage**: Handles modern Android `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` permissions cleanly.

## Build and CI/CD Instructions
1. Push this project to GitHub (`main` or `master` branch).
2. GitHub Actions workflow `.github/workflows/android.yml` will trigger automatically.
3. Once the build finishes, open the **Actions** tab on GitHub, click on the latest run, and download **VPlayer-debug-apk** from the Artifacts section.
4. Transfer the `app-debug.apk` file to your phone and install it directly!
