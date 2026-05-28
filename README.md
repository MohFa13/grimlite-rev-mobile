# Grimlite Rev Mobile Overlay - AQW Pocket Base

This repository builds a two-APK package:

1. `AQWPocket-working-armv8.apk` - the working AQW Pocket Android client from the official AQW mobile releases.
2. `GrimliteRevOverlay.apk` - a Grimlite-style floating overlay UI.

## Added in this version

- Landscape-compatible overlay layout.
- `.gbot` file picker from Android storage.
- Local `.gbot` text loader.
- Basic `.gbot` parser that counts and previews non-comment commands.
- Script name, line count, command count, and command preview.
- Start/Stop/Next Command preview controls.
- Saved script/config using Android SharedPreferences.

## Important limitation

The overlay loads and previews `.gbot` files locally. It does not execute bot/cheat gameplay automation, send packets, or bypass game rules.

## Usage

1. Install `AQWPocket-working-armv8.apk`.
2. Install `GrimliteRevOverlay.apk`.
3. Open Grimlite Rev Overlay.
4. Grant overlay permission.
5. Tap `Load .gbot Script` and choose a `.gbot` file.
6. Tap `Start Overlay`.
7. Open AQW Pocket.
8. Use the floating `☰ Grimlite` button in landscape mode.
