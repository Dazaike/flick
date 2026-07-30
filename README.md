# Flick

Android quick-launch overlay for pinning apps, shortcuts, widgets, contacts, URLs, and settings panels behind an edge gesture or assistant trigger.

**Latest release:** [v0.4.8](https://github.com/Dazaike/flick/releases/tag/v0.4.8)

## Features

- System overlay grid with smooth panel and icon animations
- Overall panel scale control (70%–150%) in settings
- Long-press drag-and-drop reordering in the overlay and main grid
- Drag-to-merge folders in the main app
- App, shortcut, widget, contact, URL, and settings-panel pickers
- Icon pack support
- Folders and categories
- Edge-gesture fallback when the assistant role is unavailable

## Changelog

### [0.4.8](https://github.com/Dazaike/flick/releases/tag/v0.4.8)

- Overall panel scale setting (70%–150%) in Popup settings
- Scales icons, labels, spacing, and panel chrome together
- Vertical offset remains absolute screen dp
- Release APKs are now signed (fixes install certificate errors)

### [0.4.7](https://github.com/Dazaike/flick/releases/tag/v0.4.7)

- Faster, clip-free overlay popup animations
- Separate panel and icon animation speed controls
- Smoother icon fade-in reveals
- Restored long-press drag-and-drop reordering in the overlay panel

## Requirements

- Android 12+ (API 31)
- Android Studio Hedgehog or newer (to build from source)
- Overlay, notification, and package-query permissions (requested in-app)

## Install

Download the latest APK from [GitHub Releases](https://github.com/Dazaike/flick/releases) and sideload it on your device. Release APKs are signed with the project keystore in [`keystore/`](keystore/). Allow installation from unknown sources if prompted.

If an upgrade fails with a signature or certificate error (for example after installing an older unsigned or debug build), uninstall the existing Flick app first, then install the new APK.

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
