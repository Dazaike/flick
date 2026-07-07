# Flick

Android quick-launch overlay for pinning apps, shortcuts, widgets, contacts, URLs, and settings panels behind an edge gesture or assistant trigger.

## Features

- System overlay grid with smooth panel and icon animations
- Long-press drag-and-drop reordering in the overlay and main grid
- Drag-to-merge folders in the main app
- App, shortcut, widget, contact, URL, and settings-panel pickers
- Icon pack support
- Folders and categories
- Edge-gesture fallback when the assistant role is unavailable

## Requirements

- Android 12+ (API 31)
- Android Studio Hedgehog or newer (to build from source)
- Overlay, notification, and package-query permissions (requested in-app)

## Install

Download the latest APK from [GitHub Releases](https://github.com/Dazaike/flick/releases) and sideload it on your device. Release builds are unsigned; allow installation from unknown sources if prompted.

## Build

Open the project root in Android Studio and run the `app` configuration, or:

```bash
./gradlew :app:assembleDebug
```

Install the APK from `app/build/outputs/apk/debug/`.

For a release build:

```bash
./gradlew :app:assembleRelease
```

## Permissions

Flick needs `SYSTEM_ALERT_WINDOW` for the overlay, `QUERY_ALL_PACKAGES` to list launchable apps, and optional permissions for contacts, SMS, and phone actions when you add those bookmark types.

## License

MIT
