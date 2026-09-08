import React, { useState } from 'react';
import { ChevronDown } from 'lucide-react';

export const FaqSection: React.FC = () => {
  const [openId, setOpenId] = useState<number | null>(null);

  const faqs = [
    {
      q: 'Does Saku affect my Anki scheduling or FSRS stats?',
      a: 'No. Saku connects to AnkiDroid via the official Android API. Card reviews and grading are processed natively by AnkiDroid, preserving your exact FSRS or SM-2 parameters.',
    },
    {
      q: 'Does Saku require an AnkiWeb login or internet connection?',
      a: 'Zero login and zero internet needed. Saku reads directly from your local AnkiDroid database on your device through Android inter-process communication.',
    },
    {
      q: 'How does it impact battery life?',
      a: 'Negligible (under 0.1% daily). Saku does not run battery-draining background loops; widgets and lock screen notifications update only when your screen turns on.',
    },
    {
      q: 'What Android versions and skins are supported?',
      a: 'Saku runs on Android 8.0 and above. It has been tested and confirmed on Google Pixel, Samsung OneUI, OnePlus OxygenOS, and Xiaomi HyperOS.',
    },
  ];

  return (
    <section id="faq" className="py-16 md:py-24 border-t border-slate-200 dark:border-[#2F3440]">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-10 space-y-2">
          <h2 className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-[#E8EAF0] tracking-tight">
            Frequently Asked Questions
          </h2>
          <p className="text-slate-600 dark:text-[#9AA1AD] text-sm sm:text-base">
            Essential details about privacy, scheduling, and device compatibility.
          </p>
        </div>

        <div className="space-y-3">
          {faqs.map((faq, idx) => {
            const isOpen = openId === idx;
            return (
              <div
                key={idx}
                className="rounded-2xl bg-slate-50 dark:bg-[#1F222A] border border-slate-200 dark:border-[#2F3440] overflow-hidden"
              >
                <button
                  onClick={() => setOpenId(isOpen ? null : idx)}
                  className="w-full text-left p-5 sm:p-6 flex items-center justify-between gap-4 cursor-pointer select-none"
                >
                  <span className="font-bold text-sm sm:text-base text-slate-900 dark:text-[#E8EAF0]">
                    {faq.q}
                  </span>
                  <ChevronDown
                    className={`w-4 h-4 text-slate-500 dark:text-[#9AA1AD] shrink-0 transition-transform duration-200 ${
                      isOpen ? 'rotate-180 text-emerald-600 dark:text-[#52C47C]' : ''
                    }`}
                  />
                </button>

                {isOpen && (
                  <div className="px-5 pb-5 sm:px-6 sm:pb-6 text-xs sm:text-sm text-slate-600 dark:text-[#9AA1AD] leading-relaxed border-t border-slate-200 dark:border-[#2F3440] pt-4">
                    {faq.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
