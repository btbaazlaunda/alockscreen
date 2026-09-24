# Lull — one-tap sleep mode for Android

Tap once at bedtime and Lull turns off **Wi-Fi, mobile data and Bluetooth** and turns on **battery saver**
(and, if you like, **Do Not Disturb**). Tap again in the morning and everything goes back to how it was.

- **Three ways to switch:** the app, a home screen widget and a Quick Settings tile.
- **Restores, not resets:** waking only turns back on what was on before you slept.
- **Private:** no internet permission, no analytics, no data collected.

## Why Lull needs Shizuku

Since Android 10, ordinary apps are not allowed to switch Wi-Fi, mobile data, Bluetooth or battery saver.
Apps that claim to do it without help just open the settings screen for you.

Lull uses [Shizuku](https://shizuku.rikka.app), a free, open-source app that lets other apps run a small set
of system commands with the same rights as a USB debugging connection. No root is needed.
Lull runs a fixed list of commands only (`svc wifi|data|bluetooth`, `cmd power set-mode`,
`cmd notification set_dnd`) in a short-lived process that exits right after each switch.

One-time setup, which the app walks you through:

1. Install Shizuku from Google Play.
2. Start it. On Android 11+ this uses Wireless debugging and needs no computer.
3. Allow Lull when asked.

Without root, Shizuku stops when the phone restarts. Open Shizuku and tap **Start** again after a restart.

## Project layout

```
app/src/main/java/com/btbaazlaunda/lull/
├── core/        Feature, SleepController (toggle logic), SleepStore (DataStore), DeviceState (reads state)
├── shizuku/     ShizukuGateway (binding, status), ShellService (runs in Shizuku's process)
├── ui/          MainActivity, MainViewModel, Compose screens and theme
├── widget/      Glance home screen widget
└── tile/        Quick Settings tile
```

`SleepController` is the one entry point the app, widget and tile all use. It records which features were
awake when sleep started and restores exactly those on wake. It has unit tests in
`app/src/test/.../SleepControllerTest.kt`.

## Building

Requirements: JDK 17+ and the Android SDK (Android Studio sets both up).

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android lint
```

## Releasing on Google Play

1. **Pick the final application ID.** It is `com.btbaazlaunda.lull` in `app/build.gradle.kts` and can't be
   changed after the first upload.
2. **Create an upload key** and a `keystore.properties` file in the project root (it is git-ignored):
   ```properties
   storeFile=/absolute/path/to/upload-key.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```
3. **Build the bundle:** `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
   Release builds are minified and resource-shrunk with R8.
4. **Play Console:**
   - *Data safety:* "No data collected, no data shared".
   - *Privacy policy:* host [`PRIVACY.md`](PRIVACY.md), for example with GitHub Pages, and link it.
   - *Store listing:* say clearly that the app needs Shizuku, and explain why. Reviewers look for this.
   - *Assets:* a 512 × 512 icon (export `ic_launcher` from Android Studio's Image Asset tool), a
     1024 × 500 feature graphic and phone screenshots.
5. Bump `versionCode` / `versionName` in `app/build.gradle.kts` for every upload.
