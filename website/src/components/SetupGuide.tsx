import React from 'react';

export const SetupGuide: React.FC = () => {
  const steps = [
    {
      num: '01',
      title: 'Install the APK',
      description:
        'Download Saku.apk directly to your Android device (Android 8.0+) and install. No Play Store account needed.',
    },
    {
      num: '02',
      title: 'Grant AnkiDroid Access',
      description:
        'Open Saku and tap "Allow" on the AnkiDroid IPC dialog. Select your favorite Japanese deck (Kaishi, Core 2k/6k, Tango).',
    },
    {
      num: '03',
      title: 'Add to Screen',
      description:
        'Add the widget to your Home Screen, or toggle "Lock Screen Card Active" to review flashcards every time you glance at your phone.',
    },
  ];

  return (
    <section id="setup" className="py-16 md:py-24 border-t border-slate-200 dark:border-[#2F3440]">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto text-center mb-12 space-y-2">
          <h2 className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-[#E8EAF0] tracking-tight">
            Simple 3-step setup.
          </h2>
          <p className="text-slate-600 dark:text-[#9AA1AD] text-sm sm:text-base">
            No accounts to create. No server configurations.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {steps.map((s) => (
            <div
              key={s.num}
              className="p-6 sm:p-8 rounded-2xl bg-slate-50 dark:bg-[#1F222A] border border-slate-200 dark:border-[#2F3440] space-y-3"
            >
              <div className="text-xs font-mono font-bold text-emerald-600 dark:text-[#52C47C]">
                {s.num}
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-[#E8EAF0]">
                {s.title}
              </h3>
              <p className="text-xs sm:text-sm text-slate-600 dark:text-[#9AA1AD] leading-relaxed">
                {s.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
