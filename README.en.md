# Pocket Favorites · 外置收藏夹 1.0

[简体中文](README.md) | **English**

Keep text, links, images, videos, and files from different apps in one local collection. Organize them into folders, then tap an item to copy, share, launch an app, or browse a web page.

## Get started

1. Download `PocketFavorites-v1.0.apk` from [Release 1.0](https://github.com/LuuSir/ResourceTree/releases/tag/v1.0). Existing ResourceTree installations can update in place with the same package ID and signing certificate.
2. Tap **＋ New** on Home to create a folder or item. Set its Content and choose the Action to run when tapped.
3. Open **Menu → App language** and select **Follow system**, **简体中文**, or **English**. The default follows the system, with English as the fallback for other languages. Saved content and clipboard rules retain their original text.

## Features

- Text and single image, video, or file content. Actions include copy, open app, copy and open app, SHARE, and an in-app web browser with outbound app links.
- Long-press drag sorting with spring animations, pinned items, multiple selection, and batch move / copy / delete. Destination selection uses breadcrumbs.
- Global search, folder breadcrumbs, and a searchable app picker with icons.
- Tap the app name at the top left of Home to open graph view. Tap a folder to enter it or an item to run its action. Pinch to zoom, drag to pan, or expand all descendants.
- Clipboard rules support prefixes, wildcards, and candidate package names. Matching content only prefills an editor; it is saved to Home after confirmation.
- A resizable floating button opens an independent clipboard capture window. A Quick Settings tile toggles the button, including after the app process has been reclaimed.
- Import JSON v1 and v2; export JSON v2. Importing a root folder with an existing name appends an independent copy.

Enable capture under **Menu → Floating button**. Edit Quick Settings and add **Pocket Favorites overlay**. Grant overlay permission when prompted; some phones also require permission to open windows from the background.

[Video tutorial on Bilibili (Chinese)](https://www.bilibili.com/video/BV1dQYs6JEbT/?vd_source=6076e72eb03a33f77c50c30c9947e961)

## Build and maintain

Open the repository root in Android Studio with Android SDK 37 and the bundled JBR. With dependencies cached:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --offline --console=plain
```

The Debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. See the [release workflow](workflows/release.md) for signed builds, device data checks, and publishing.

- [Documentation index](reports/README.md)
- [Architecture and entry points](files.md)
- [Technical glossary](glossary.md)
- [Current verification report](reports/resourcetree.md)
- [Outstanding verification](unsure.md)
- [Bilibili favorites export tool](tools/bilibili-export/README.md)

Next steps focus on media collection and device compatibility. The Android package remains `com.example.resouretree`, Room schema v3, and JSON schema v2.
