import React, { useState } from 'react';

export const Features: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'cards' | 'reading' | 'jisho'>('cards');

  const tabs = [
    {
      id: 'cards' as const,
      label: 'Lock Screen & Cards',
      tagline: 'Passive Repetition',
      title: 'Study without opening an app.',
      description:
        'Saku surfaces your active Anki cards on your Lock Screen, Widget, and Always-On Display. Powered by native Android IPC with AnkiDroid—zero battery drain, no background polling, and 100% on-device privacy.',
      image: '/screenshots/cards_tab.png',
      imageAlt: 'Saku Lock Screen Cards Tab',
      points: [
        'Direct SQLite IPC sync with AnkiDroid',
        'FSRS & SM-2 scheduling fully preserved',
        'Zero cloud login or AnkiWeb credentials needed',
      ],
    },
    {
      id: 'reading' as const,
      label: 'AI Reading Practice',
      tagline: 'Contextual Immersion',
      title: 'Stories generated from your vocabulary.',
      description:
        'Transform static flashcard knowledge into reading comprehension. Generate graded short stories matched to your JLPT level and learned cards, complete with audio narration and furigana.',
      image: '/screenshots/reading_tab.png',
      imageAlt: 'Saku AI Graded Reader Tab',
      points: [
        'Personalized to your studied Anki deck cards',
        'Natural speech synthesis via Fish Audio',
        'Interactive word breakdown and translations',
      ],
    },
    {
      id: 'jisho' as const,
      label: 'Jisho Dictionary',
      tagline: 'Instant Reference',
      title: 'Fast, built-in Japanese lookup.',
      description:
        'Look up unfamiliar words instantly without switching away. Search kanji, readings, and definitions with clear JLPT and WaniKani level indicators.',
      image: '/screenshots/jisho_tab.png',
      imageAlt: 'Saku Jisho Dictionary Tab',
      points: [
        'Instant search for kanji, readings, and romaji',
        'JLPT level & commonality tagging',
        'Offline capability for quick daily lookup',
      ],
    },
  ];

  const current = tabs.find((t) => t.id === activeTab) || tabs[0];

  return (
    <section id="features" className="py-16 md:py-24 border-t border-slate-200 dark:border-[#2F3440]">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="max-w-2xl mx-auto text-center mb-12 space-y-3">
          <h2 className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-[#E8EAF0] tracking-tight">
            Designed for genuine daily retention.
          </h2>
          <p className="text-slate-600 dark:text-[#9AA1AD] text-sm sm:text-base leading-relaxed">
            Everything you need for passive immersion, organized around your real study routine.
          </p>
        </div>

        {/* 3-Tab Selector Pill */}
        <div className="flex justify-center mb-10">
          <div className="inline-flex p-1 rounded-2xl bg-slate-100 dark:bg-[#1F222A] border border-slate-200 dark:border-[#2F3440]">
            {tabs.map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl transition-colors cursor-pointer ${
                    isActive
                      ? 'bg-white dark:bg-[#262A34] text-slate-900 dark:text-[#E8EAF0] shadow-sm'
                      : 'text-slate-600 dark:text-[#9AA1AD] hover:text-slate-900 dark:hover:text-[#E8EAF0]'
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Active Feature Display */}
        <div className="rounded-3xl bg-slate-50 dark:bg-[#1F222A] border border-slate-200 dark:border-[#2F3440] p-6 sm:p-10 md:p-12">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
            {/* Details */}
            <div className="lg:col-span-7 space-y-5">
              <span className="text-xs font-semibold tracking-wider uppercase text-emerald-600 dark:text-[#52C47C]">
                {current.tagline}
              </span>

              <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-[#E8EAF0] tracking-tight">
                {current.title}
              </h3>

              <p className="text-sm sm:text-base text-slate-600 dark:text-[#9AA1AD] leading-relaxed">
                {current.description}
              </p>

              <ul className="space-y-2.5 pt-2 text-xs sm:text-sm font-medium text-slate-700 dark:text-[#E8EAF0]">
                {current.points.map((point) => (
                  <li key={point} className="flex items-center gap-2.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#52C47C] shrink-0" />
                    <span>{point}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Screenshot Frame */}
            <div className="lg:col-span-5 flex justify-center">
              <div className="w-full max-w-[280px] rounded-[32px] bg-white dark:bg-[#15171C] p-2 border border-slate-200 dark:border-[#2F3440] shadow-xl overflow-hidden">
                <img
                  src={current.image}
                  alt={current.imageAlt}
                  className="w-full h-auto rounded-[26px] object-cover block select-none"
                  loading="lazy"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
