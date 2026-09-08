export interface FaqItem {
  id: string;
  category: "Algorithm & Sync" | "Privacy & Security" | "Android & Lock Screen" | "Decks & Furigana";
  question: string;
  answer: string;
}

export const FAQS: FaqItem[] = [
  {
    id: "fsrs-preservation",
    category: "Algorithm & Sync",
    question: "Does answering cards from the Saku widget preserve my FSRS / SM-2 scheduling weights?",
    answer: "Yes, 100%! Saku does not invent its own review algorithm. It forwards your rating (Again: 1, Hard: 2, Good: 3, Easy: 4) directly to AnkiDroid via the official com.ichi2.anki.api ContentProvider IPC. AnkiDroid executes your active scheduling algorithm (FSRS weights, stability, difficulty, retention rate, or legacy SM-2), updates the local SQLite database, and queues standard synchronization with AnkiWeb."
  },
  {
    id: "offline-login",
    category: "Privacy & Security",
    question: "Do I need to create an account or provide my AnkiWeb username/password?",
    answer: "Zero logins or passwords required. Saku runs 100% on your device and never asks for AnkiWeb credentials or personal data. It uses Android's native inter-process ContentProvider permission. With a single tap ('Allow Saku to access AnkiDroid'), it securely reads and grades cards on-device."
  },
  {
    id: "lockscreen-oxygenos",
    category: "Android & Lock Screen",
    question: "How does the Lock Screen & Always-On Display (AOD) feature work on OnePlus / OxygenOS / Pixel?",
    answer: "Saku uses Android's media/pinned notification surface (NotificationActionReceiver & LockScreenCardService) rendered in high contrast. On OxygenOS (OnePlus), Samsung OneUI, and Google Pixel, this notification is pinned directly below your lock screen clock and mirrors to the Always-On Display, letting you glance at and review your due card every time you look at your phone without unlocking it."
  },
  {
    id: "battery-drain",
    category: "Privacy & Security",
    question: "Will having widgets and lock screen notifications drain my phone's battery?",
    answer: "No. Saku consumes less than 0.1% battery per day. It does not run background polling loops or periodic wake locks. The widget updates only when you tap an interaction button or when a card is graded. Memory footprint is under 25 MB RAM and immediately drops to 0 when idle."
  },
  {
    id: "supported-note-types",
    category: "Decks & Furigana",
    question: "Which Japanese decks and note types are supported?",
    answer: "Saku includes a smart Japanese regex parser that supports Kaishi 1.5k/2.3k, Core 2k/6k/10k, Tango N5 through N1, Wanikani, RTK, and custom note types. It automatically parses bracket furigana like 漢字[かんじ], strips HTML tags (<br>, <div>), and ignores audio tags ([sound:...]) so your cards render clean and clutter-free."
  },
  {
    id: "ai-reader-details",
    category: "Algorithm & Sync",
    question: "How does the AI Reading Generator work?",
    answer: "When you want contextual reading practice, Saku takes your currently due or studied vocabulary cards from AnkiDroid and sends a lightweight prompt to Google Gemini Flash using your own Gemini API key. It crafts an engaging story calibrated to your JLPT level containing your target vocabulary, complete with interactive tap-to-define lookup and optional Fish Audio high-definition Japanese voice synthesis."
  },
  {
    id: "play-protect-warning",
    category: "Privacy & Security",
    question: "Why does Android Play Protect show an 'Unknown App' warning when sideloading?",
    answer: "Since Saku is an open-source project distributed directly via GitHub and this website (bypassing Google Play Store's developer fee and censorship), Google Play Protect flags any newly sideloaded APK. Saku is 100% open source under the MIT license, and you can inspect every line of Kotlin code on GitHub. Simply tap 'More details' → 'Install anyway'."
  }
];
