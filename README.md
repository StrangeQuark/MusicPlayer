# Music Player

A local Android music player written in Java. It reads audio from Android's MediaStore, supports songs, artists, albums, playlists, shuffle/repeat playback, album art lookup, media-session controls, and foreground playback notifications.

## Device Support

The app currently supports Android API 26 and newer. This keeps the Samsung Galaxy S9 supported while allowing the project to compile and target the current Android SDK.

Key SDK settings live in `app/build.gradle`:

- `minSdkVersion 26`: keeps Galaxy S9 support.
- `targetSdkVersion 36`: opts into current Android behavior.
- `compileSdkVersion 'android-36.1'`: builds against the installed current SDK platform.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## Verification

```bash
./gradlew lintDebug testDebugUnitTest
```

## Notes

Playlist data is stored in app-private storage as JSON. Older `playlists.txt` data is migrated on load.

Signing keys and release APKs should not be committed. Keep release artifacts outside the repo or in a private release process.
