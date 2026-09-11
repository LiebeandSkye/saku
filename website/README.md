# 🌸 Saku Showcase & Download Website

Responsive, high-aesthetic web portal for **Saku • 咲く** (Minimal Spaced Repetition Japanese Flashcard Widget for Android).

Built with **React 19 + TypeScript + Tailwind CSS + Vite** following **UI/UX Pro Max** design intelligence.

---

## 🌟 Key Sections & Features

- **Interactive Widget Simulator**: Live replica of the Android Glance widget. Flip cards, test Anki ratings (`Again`, `Hard`, `Good`, `Easy`), toggle furigana, and switch between Dim, OLED Dark, and Light skins.
- **Dual Download Channels**:
  - Direct verified APK download (~20 MB) automatically pointing to the latest release.
  - GitHub Releases link with tags, source code, and release notes.
  - QR Code scanner modal for scanning and downloading directly on mobile devices.
- **Bento Grid Feature Showcase**:
  - 100% On-Device & Zero Login (Android IPC `com.ichi2.anki.api`).
  - Native FSRS & SM-2 algorithm preservation.
  - OxygenOS (OnePlus), OneUI, Pixel Lock Screen & AOD display.
  - Ultra Lightweight (< 25 MB RAM, < 0.1% battery/day).
- **Architecture Pipeline & Native Code**:
  - Interactive pipeline: UI Surfaces $\rightarrow$ Saku Core $\rightarrow$ On-Device IPC $\rightarrow$ AnkiDroid $\rightarrow$ AnkiWeb.
  - Real Kotlin code tab for queries and answer updates.
- **Interactive Graded Reader Demo**:
  - Gemini Flash generated story simulator.
  - Click-to-lookup dictionary popup with definitions and JLPT tags.
  - Furigana toggle and English translation drawer.
- **5-Second Setup Wizard**: Tabbed 3-step walkthrough.
- **Device Compatibility Guide**: Interactive brand selector (OnePlus, Samsung, Pixel, Xiaomi, etc.) with verified optimization tips.
- **Technical FAQ**: Filterable accordion addressing permissions, sideloading, FSRS safety, and deck compatibility.

---

## 🛠️ Development & Build

### Install dependencies
```bash
npm install
```

### Run local development server
```bash
npm run dev
```

### Build for production
```bash
npm run build
```

The production assets will be output to `website/dist/`.

### Preview production build
```bash
npm run preview
```
