import React from 'react';
import { Download, QrCode, Star } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';

interface HeroProps {
  onOpenQrModal: () => void;
}

export const Hero: React.FC<HeroProps> = ({ onOpenQrModal }) => {
  return (
    <section className="pt-32 pb-16 md:pt-40 md:pb-24">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-10 items-center">
          {/* Left Column: Focused Copy & CTAs */}
          <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black text-slate-900 dark:text-[#E8EAF0] tracking-tight leading-[1.12]">
              Passive Japanese flashcards on your lock screen.
            </h1>

            <p className="text-base sm:text-lg text-slate-600 dark:text-[#9AA1AD] max-w-xl mx-auto lg:mx-0 leading-relaxed">
              Review your vocabulary every time you turn on your phone.
              Synced directly from AnkiDroid on-device with zero login required.
            </p>

            {/* CTAs */}
            <div className="pt-2 flex flex-wrap items-center gap-3 justify-center lg:justify-start">
              <a
                href="/Saku.apk"
                download="Saku.apk"
                className="px-6 py-3 rounded-xl bg-[#52C47C] hover:bg-[#43A869] text-white font-bold text-sm transition-colors flex items-center gap-2 cursor-pointer shadow-sm"
              >
                <Download className="w-4 h-4 stroke-[2.5]" />
                <span>Download APK</span>
              </a>

              <button
                onClick={onOpenQrModal}
                className="px-4 py-3 rounded-xl bg-slate-100 dark:bg-[#1F222A] hover:bg-slate-200 dark:hover:bg-[#262A34] text-slate-700 dark:text-[#E8EAF0] border border-slate-200 dark:border-[#2F3440] text-sm font-medium transition-colors flex items-center gap-2 cursor-pointer"
              >
                <QrCode className="w-4 h-4 text-emerald-600 dark:text-[#52C47C]" />
                <span>Scan QR</span>
              </button>

              <a
                href="https://github.com/LiebeandSkye/saku"
                target="_blank"
                rel="noopener noreferrer"
                className="px-4 py-3 rounded-xl bg-slate-100 dark:bg-[#1F222A] hover:bg-slate-200 dark:hover:bg-[#262A34] text-slate-700 dark:text-[#E8EAF0] border border-slate-200 dark:border-[#2F3440] text-sm font-medium transition-colors flex items-center gap-2 cursor-pointer group"
                title="Star Saku on GitHub"
              >
                <GithubIcon className="w-4 h-4 text-slate-700 dark:text-[#9AA1AD] group-hover:text-slate-900 dark:group-hover:text-[#E8EAF0]" />
                <span>Give me a star!</span>
                <Star className="w-3.5 h-3.5 text-amber-400 fill-amber-400/80 group-hover:fill-amber-400 group-hover:scale-110 transition-all" />
              </a>
            </div>

            <p className="text-xs text-slate-500 dark:text-[#9AA1AD] pt-1">
              Free & Open Source • Android 8.0+ • AnkiDroid FSRS Compatible
            </p>
          </div>

          {/* Right Column: Authentic App Demo Video */}
          <div className="lg:col-span-5 flex justify-center">
            <div className="relative w-full max-w-[320px] rounded-[36px] bg-[#1F222A] p-2.5 border-2 border-[#2F3440] shadow-2xl overflow-hidden">
              <video
                src="/screenshots/demo.mp4"
                autoPlay
                loop
                muted
                playsInline
                className="w-full h-auto rounded-[28px] object-cover block select-none pointer-events-none"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
