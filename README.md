# مترونوم (Metronome) — `com.pouyaApp.metoronome_2`

Modern Persian metronome for Android, modeled on the standard metronome behavior of
[metronome-online.org](https://metronome-online.org). Kotlin + Jetpack Compose (Material 3).

## Features

- **Standard metronome model** — BPM = beats-per-minute of the denominator note; time signature
  as numerator / denominator (e.g. 6/8), per-beat accent pattern
- **Physical & digital views** — switchable classic swinging-pendulum metronome or a digital beat
  counter (مترونوم فیزیکی / مترونوم دیجیتال)
- **Asymmetric / additive rhythms** — additive groupings like 7/8 (2+2+3) and 14/16 (3+3+4+4);
  ready-made presets plus a custom group editor
- **Subdivisions** — none, duple, triplet, sixteenth
- **Meter presets** — 2/4, 3/4, 4/4, 6/8, 9/8, 12/8
- **Tempo markings** — Largo … Prestissimo, shown for the current BPM
- **Native audio engine** — lightweight high-priority `HandlerThread` scheduler + `SoundPool`
  (no libpd/Pure Data, no native `.so` dependencies)
- **7 sound packs**, per-beat volume, flash on beat (`CameraManager` torch), screen-color sync
- **RTL Persian UI**, Material 3 dark theme

## Build

Requirements: JDK 17+, Android SDK 35, `local.properties` with `sdk.dir`.

```bash
# Run unit tests
./gradlew test

# Debug APK
./gradlew assembleDebug

# Signed release APK (uses ../keystore.properties if present)
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`

## Publish checklist

- [x] `applicationId` = `com.pouyaApp.metoronome_2`
- [x] `versionCode` 11 / `versionName` 3.0.0
- [x] Signed release build configured via parent `keystore.properties`
- [x] Min SDK 24, target SDK 35
- [x] ProGuard/R8 enabled for release
- [ ] Upload to Cafe Bazaar / Google Play Console

## Project layout

```
app/src/main/java/com/pouyaApp/metoronome/
  audio/   MetronomeEngine, SoundManager, FlashManager
  logic/   MetronomeTiming (settings, groupings, presets, tempo markings), RhythmScreenColors, BeatKind
  ui/      MetronomeScreen, theme
  MainActivity.kt, MetronomeViewModel.kt
app/src/main/res/raw/   click sounds (.ogg)
```
