# Saku (咲く) — Minimal Japanese Spaced Repetition Widget for Android

A clean, typography-focused Android application and widget system that brings your active Japanese flashcards directly to your **Home Screen**, **Lock Screen**, and **Always-On Display (AOD)**, syncing in real-time with **AnkiDroid**.

---

## Key Features

1. **Native AnkiDroid Integration (Zero-Login / 100% On-Device)**
   * Connects directly to the AnkiDroid database via Android's `ContentProvider` API.
   * **1-Tap Permission Prompt**: No username, password, or third-party cloud required.
   * **Preserves FSRS & SM-2**: Intervals, retention curves, and due dates stay 100% managed by AnkiDroid and AnkiWeb.
2. **Minimal & Informational Aesthetic**
   * **Kanji / Headword**: Large, legible font in a framed dark card.
   * **Furigana / Kana + Romaji**: Clean reading guide.
   * **Definition & Context**: English glossary and contextual compound expressions (e.g. `日本 • Japan`).
3. **OxygenOS (OnePlus) Lock Screen & Always-On Display (AOD)**
   * Pinned minimal notification card visible on Lock Screen and AOD right below the clock.
   * Review cards (*Again*, *Hard*, *Good*, *Easy*) directly from the notification shade or lockscreen.
4. **Home Screen Jetpack Glance Widget**
   * Modern, resizable Jetpack Compose widget for your Home Screen and OnePlus Shelf.

---

## How to Build & Run

### Method 1: In Android Studio
1. Open Android Studio.
2. Select **Open** $\rightarrow$ navigate to this directory (`Saku`).
3. Allow Gradle to sync.
4. Connect your Android / OnePlus device (with USB debugging enabled) and click **Run (▶)**.

### Method 2: Command Line (Gradle)
```bash
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## First-Time Setup on your Phone

1. **Install & Open AnkiDroid**: Make sure you have AnkiDroid installed with your Japanese deck loaded.
2. **Open Saku**:
   * Tap **"Connect to AnkiDroid (1-Tap)"** and select **Allow**.
   * Select your preferred Japanese deck from the list.
3. **Add Home Screen Widget**:
   * Long-press your home screen $\rightarrow$ select **Widgets** $\rightarrow$ choose **Saku**.
4. **Lock Screen / AOD Display**:
   * In Saku, ensure **"Lock Screen & AOD Display"** toggle is ON.
   * In OxygenOS settings, ensure notifications are allowed on the Lock Screen (*Settings > Notifications & Status Bar > Lock Screen > Show app and notification content*).
