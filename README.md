# Flick

Android quick-launch overlay: bookmark apps, shortcuts, widgets, contacts, and URLs behind an edge gesture or assistant trigger.

## Features

- System overlay grid for pinned actions
- App, shortcut, widget, contact, and URL pickers
- Icon pack support
- Folders and categories
- Edge-gesture fallback when assistant role is unavailable

## Requirements

- Android 8.0+ (API 26)
- Android Studio Hedgehog or newer
- Overlay, notification, and package-query permissions (requested in-app)

## Build

Open the project root in Android Studio and run the `app` configuration, or:

```bash
./gradlew :app:assembleDebug
```

Install the APK from `app/build/outputs/apk/debug/`.

## Permissions

Flick needs `SYSTEM_ALERT_WINDOW` for the overlay, `QUERY_ALL_PACKAGES` to list launchable apps, and optional permissions for contacts, SMS, and phone actions when you add those bookmark types.

## License

MIT — see [LICENSE](LICENSE) if present, or use at your own discretion for now.
