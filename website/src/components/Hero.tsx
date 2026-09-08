import React from 'react';
import { Download, Smartphone, ShieldCheck, Cpu, BatteryCharging, CheckCircle2 } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';
import { WidgetSimulator } from './WidgetSimulator';
import confetti from 'canvas-confetti';

interface HeroProps {
  onOpenQrModal: () => void;
}

export const Hero: React.FC<HeroProps> = ({ onOpenQrModal }) => {
  const handleDownloadClick = () => {
    confetti({
      particleCount: 100,
      spread: 70,
      origin: { y: 0.3 },
      colors: ['#52C47C', '#6EE7A0', '#CF7B88', '#F0F3F8']
    });
  };

  return (
    <section className="relative pt-32 pb-20 md:pt-40 md:pb-28 overflow-hidden">
      {/* Ambient background glows */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-saku-matcha/10 rounded-full blur-[140px] pointer-events-none -z-10" />
      <div className="absolute top-1/3 right-10 w-[400px] h-[400px] bg-saku-rose/10 rounded-full blur-[120px] pointer-events-none -z-10" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          
          {/* Left Column: Value Proposition & CTAs */}
          <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
            
            {/* Tag Badge */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#1F222A] border border-saku-border text-xs text-saku-text-secondary">
              <span className="w-2 h-2 rounded-full bg-saku-matcha animate-pulse"></span>
              <span className="font-medium text-white">Saku v1.0.0</span>
              <span className="text-saku-text-muted">•</span>
              <span>Android 8.0+ & AnkiDroid FSRS / SM-2</span>
            </div>

            {/* Main Headline */}
            <div className="space-y-2">
              <p className="font-serif text-sm md:text-base text-saku-rose tracking-widest uppercase">
                画面をつけるたび、言葉が咲く
              </p>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-white tracking-tight leading-[1.12]">
                Passive Japanese <br className="hidden sm:inline" />
                <span className="text-gradient-matcha">Immersion</span> on Your <br className="hidden sm:inline" />
                Home & Lock Screen.
              </h1>
            </div>

            {/* Description */}
            <p className="text-base sm:text-lg text-saku-text-secondary max-w-2xl mx-auto lg:mx-0 font-normal leading-relaxed">
              Transform micro-moments into lasting retention. Review your Japanese flashcards directly on your Android widget and Always-On Display — without opening any app or disturbing your Anki FSRS algorithm.
            </p>

            {/* Key Value Bullets */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 text-xs font-medium text-saku-text-secondary">
              <div className="flex items-center gap-2 bg-[#1F222A]/60 p-2.5 rounded-xl border border-saku-border/60 justify-center sm:justify-start">
                <ShieldCheck className="w-4 h-4 text-saku-matcha shrink-0" />
                <span>100% Private & Offline</span>
              </div>
              <div className="flex items-center gap-2 bg-[#1F222A]/60 p-2.5 rounded-xl border border-saku-border/60 justify-center sm:justify-start">
                <Cpu className="w-4 h-4 text-saku-matcha shrink-0" />
                <span>Native FSRS IPC Sync</span>
              </div>
              <div className="flex items-center gap-2 bg-[#1F222A]/60 p-2.5 rounded-xl border border-saku-border/60 justify-center sm:justify-start">
                <BatteryCharging className="w-4 h-4 text-saku-matcha shrink-0" />
                <span>&lt;0.1% Battery / Day</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="pt-4 flex flex-col sm:flex-row items-center gap-4 justify-center lg:justify-start">
              <a
                href="/Saku.apk"
                download="Saku.apk"
                onClick={handleDownloadClick}
                className="w-full sm:w-auto px-7 py-3.5 rounded-xl bg-saku-matcha hover:bg-saku-matcha-light text-[#091F11] font-bold text-sm tracking-wide transition-all shadow-lg hover:shadow-saku-glow flex items-center justify-center gap-2.5 cursor-pointer font-sans"
              >
                <Download className="w-4 h-4 stroke-[2.5]" />
                <span>Download Saku.apk</span>
                <span className="text-[11px] font-semibold opacity-75 ml-1 px-1.5 py-0.5 rounded bg-black/15">~20 MB</span>
              </a>

              <a
                href="https://github.com/LiebeandSkye/saku/releases"
                target="_blank"
                rel="noopener noreferrer"
                className="w-full sm:w-auto px-6 py-3.5 rounded-xl bg-[#1F222A] hover:bg-[#262A34] text-white border border-saku-border font-medium text-sm transition-colors flex items-center justify-center gap-2 cursor-pointer"
              >
                <GithubIcon className="w-4 h-4" />
                <span>GitHub Releases</span>
              </a>

              <button
                onClick={onOpenQrModal}
                className="w-full sm:w-auto px-4 py-3.5 rounded-xl bg-[#1F222A]/70 hover:bg-[#262A34] text-saku-text-secondary hover:text-white border border-saku-border text-sm font-medium transition-colors flex items-center justify-center gap-2 cursor-pointer"
                title="Scan QR Code to install directly on phone"
              >
                <Smartphone className="w-4 h-4 text-saku-matcha" />
                <span className="hidden sm:inline">Phone QR</span>
                <span className="sm:hidden">Scan Phone QR Code</span>
              </button>
            </div>

            {/* Trust & Verification Row */}
            <div className="pt-2 flex items-center gap-6 justify-center lg:justify-start text-xs text-saku-text-muted">
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                <span>Zero login or account</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                <span>Free & MIT Open Source</span>
              </div>
            </div>

          </div>

          {/* Right Column: Live Interactive Widget Simulator */}
          <div className="lg:col-span-5" id="simulator">
            <WidgetSimulator />
          </div>

        </div>
      </div>
    </section>
  );
};
