# V Player — Android project

Package: `studio.muhammedomor.vplayer`

## Build

The project uses JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21 and AndroidX Media3 1.5.1.

```bash
gradle --no-daemon assembleDebug
gradle --no-daemon assembleRelease bundleRelease
```

GitHub Actions builds a debug APK, release APK and Android App Bundle (AAB). The release build is **not signed for Play upload** unless you configure signing credentials in GitHub Actions and add a release signing configuration. Never commit a keystore or passwords.

## Implemented in this revision

- Media3 ExoPlayer playback with Media3 HLS, DASH and RTSP modules included.
- Existing MediaStore library and playback-position/history persistence.
- Playback speed selector from 0.25× to 4×.
- Horizontal swipe seeking and vertical left/right brightness/volume gestures.
- Audio-track selection where the stream exposes selectable tracks.
- External subtitle picker for common text subtitle formats supported by Media3.
- Fit/crop/stretch display modes.
- Picture-in-picture entry and multitasking manifest configuration.
- Modern media permissions, internet permission, release R8/minification rules and CI build of APK/AAB.

## Important platform and format limits

This repository is an Android player foundation, not a certification that all 40 requested commercial features are complete. Playback depends on Android's available decoders and the actual media codec/container. Media3 does not automatically guarantee AVI/FLV/ASS/SSA, 8K/120 fps, HDR10 or Dolby Vision on every device; Dolby Vision/HDR output is device, display, stream and decoder dependent. Software decoding for arbitrary codecs is not bundled. RTMP is not provided by the included Media3 modules. Network URLs, background audio service, persistent Room schema, sleep timer, AB repeat, screenshot/gallery capture, private vault, SMB/FTP/WebDAV, casting/DLNA, EQ/bass/virtualizer, online subtitle provider, analytics/crash reporting, ads, billing and full localization still require separately designed and tested modules and service credentials.

The manifest intentionally does not request `MANAGE_EXTERNAL_STORAGE`: broad file access is highly restricted by Google Play policy. Use the system Photo Picker/Storage Access Framework or MediaStore access for user-selected media.

## Before Play Store release

1. Add and test the remaining features listed above on a representative device matrix.
2. Configure a private release keystore using GitHub Actions secrets and verify signing.
3. Add privacy policy, data safety disclosures, content rating and store listing.
4. Test Android 8–16 behavior, media permissions, PiP, audio focus, Bluetooth routing, subtitles and malformed media files.
5. Run `gradle lintRelease test` and physical-device regression tests. CI compilation alone does not prove runtime correctness.
