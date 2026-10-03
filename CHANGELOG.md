# Changelog

All notable changes to Flick are documented in this file.

## [v0.5.0] - 2026-10-02

### Added
- Prism liquid-glass UI across all screens and the overlay popup, with the Outfit font.
- Appearance settings: System/Light/Dark theme, accent colour, surface brightness.
- Motion & haptics settings: reduce motion, animation speed, motion intensity, haptic strength.
- Popup corner-radius slider (8–48 dp).
- Launcher shortcut: long-press the app icon → Open menu.
- New app icon (adaptive and themed/monochrome layers).
- Release signing config read from the untracked `local.properties`.

### Changed
- Overlay popup is now a floating glass card with a configurable corner radius, instead of an edge-attached sheet.
- Popup opacity now goes down to 10% and controls the panel directly (the dim layer is no longer drawn twice under it).
- Panel and icon animation speeds go up to 400% (previously capped at 100%).
- Toolchain upgraded: AGP 9.1.1, Kotlin 2.4.20, Compose Multiplatform 1.12.1, Gradle 9.3.1, compileSdk/targetSdk 37; Room 2.8.5, Hilt 2.60.1.
- Version 0.5.0 (versionCode 16).

### Removed
- Material 3 theme, dynamic/brand colour mode, and AMOLED mode; previous theme settings are reset.
- Background-blur setting for the popup.
- Space Grotesk font.

## [v0.4.11] - 2026-09-12

### Added
- Persisted overlay menu-scale control with a 60%–140% range ([8c10369]).
- Long-press main-grid reordering and improved bookmark drag handling ([4385270]).

### Changed
- Improved overlay panel scaling, placement, and drag/delete layout ([8c10369], [4385270]).

### Fixed
- Retained voice-interaction components in minified releases and open Android’s default assistant settings when the Assistant role request cannot be completed ([8c10369]).
- Preserved overlay reorder updates reliably ([4385270]).

[8c10369]: https://github.com/Dazaike/flick/commit/8c10369
[4385270]: https://github.com/Dazaike/flick/commit/4385270
