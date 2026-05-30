# Music Player

A local Android music player written in Java. It reads audio from Android's MediaStore, supports songs, artists, albums, playlists, shuffle/repeat playback, album art lookup, media-session controls, and foreground playback notifications.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## Verification

```bash
./gradlew lintDebug testDebugUnitTest
```
