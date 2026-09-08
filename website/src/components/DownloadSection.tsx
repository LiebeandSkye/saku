import React from 'react';
import { Download, Smartphone, CheckCircle2, AlertCircle, ArrowDownToLine } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';
import confetti from 'canvas-confetti';

interface DownloadSectionProps {
  onOpenQrModal: () => void;
}

export const DownloadSection: React.FC<DownloadSectionProps> = ({ onOpenQrModal }) => {
  const handleDownload = () => {
    confetti({
      particleCount: 120,
      spread: 80,
      origin: { y: 0.6 },
      colors: ['#52C47C', '#6EE7A0', '#CF7B88', '#F0F3F8']
    });
  };

  return (
    <section id="download" className="py-24 bg-gradient-to-b from-transparent via-[#1A1D23]/60 to-transparent relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <p className="font-serif text-sm text-saku-matcha tracking-widest uppercase font-semibold">
            Ready to Immersion?
          </p>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Download Saku for Android
          </h2>
          <p className="text-saku-text-secondary text-base">
            Choose your preferred download channel. Get the direct pre-compiled APK or fetch the latest GitHub release.
          </p>
        </div>

        {/* Download Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch max-w-5xl mx-auto">
          
          {/* Card 1: Direct APK Download (Col Span 7) */}
          <div className="lg:col-span-7 glass-card rounded-3xl p-8 sm:p-10 border-2 border-saku-matcha/40 flex flex-col justify-between shadow-2xl relative overflow-hidden group">
            <div className="absolute top-0 right-0 px-4 py-1.5 bg-saku-matcha text-[#091F11] font-bold text-xs rounded-bl-2xl uppercase tracking-wider font-mono">
              Recommended
            </div>

            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-saku-matcha/10 border border-saku-matcha/30 flex items-center justify-center text-saku-matcha">
                  <ArrowDownToLine className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-2xl font-bold text-white tracking-tight">Direct APK Package</h3>
                  <p className="text-xs text-saku-text-muted font-mono">Saku.apk • Universal Release</p>
                </div>
              </div>

              <p className="text-saku-text-secondary text-sm leading-relaxed">
                Download the verified release binary directly to your Android phone or computer. Instant installation, zero third-party telemetry.
              </p>

              {/* Package Specs */}
              <div className="grid grid-cols-3 gap-3 py-3 font-mono text-xs text-saku-text-secondary border-y border-saku-border">
                <div>
                  <span className="text-saku-text-muted block text-[10px] uppercase">Version</span>
                  <span className="text-white font-bold">v1.0.0 (Latest)</span>
                </div>
                <div>
                  <span className="text-saku-text-muted block text-[10px] uppercase">File Size</span>
                  <span className="text-saku-matcha font-bold">19.3 MB</span>
                </div>
                <div>
                  <span className="text-saku-text-muted block text-[10px] uppercase">Target OS</span>
                  <span className="text-white font-bold">Android 8.0+</span>
                </div>
              </div>
            </div>

            {/* CTAs */}
            <div className="pt-6 space-y-3">
              <a
                href="/Saku.apk"
                download="Saku.apk"
                onClick={handleDownload}
                className="w-full py-4 rounded-2xl bg-saku-matcha hover:bg-saku-matcha-light text-[#091F11] font-extrabold text-sm tracking-wide transition-all shadow-lg hover:shadow-saku-glow flex items-center justify-center gap-2 cursor-pointer font-sans"
              >
                <Download className="w-5 h-5 stroke-[2.5]" />
                <span>Download Saku.apk (Direct)</span>
              </a>

              <button
                onClick={onOpenQrModal}
                className="w-full py-3 rounded-xl bg-[#15171C] hover:bg-[#262A34] text-xs font-semibold text-saku-text-secondary hover:text-white border border-saku-border transition-colors flex items-center justify-center gap-2 cursor-pointer"
              >
                <Smartphone className="w-4 h-4 text-saku-matcha" />
                <span>Viewing on PC? Scan Phone QR Code to Download</span>
              </button>
            </div>
          </div>

          {/* Card 2: GitHub Releases (Col Span 5) */}
          <div className="lg:col-span-5 glass-card rounded-3xl p-8 sm:p-10 border border-saku-border flex flex-col justify-between shadow-xl">
            <div className="space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-[#1F222A] border border-saku-border flex items-center justify-center text-white">
                <GithubIcon className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white tracking-tight">GitHub Releases</h3>
              <p className="text-saku-text-secondary text-sm leading-relaxed">
                Access tagged releases, raw source code archives, changelogs, and report issues on GitHub.
              </p>

              <div className="space-y-2 pt-2 text-xs text-saku-text-secondary font-mono">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                  <span>Full Kotlin 2.0 source code</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                  <span>Release notes & SHA-256 tags</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                  <span>MIT Open-Source License</span>
                </div>
              </div>
            </div>

            <div className="pt-6">
              <a
                href="https://github.com/LiebeandSkye/saku/releases"
                target="_blank"
                rel="noopener noreferrer"
                className="w-full py-3.5 rounded-2xl bg-[#1F222A] hover:bg-[#262A34] text-white border border-saku-border font-semibold text-xs tracking-wide transition-colors flex items-center justify-center gap-2 cursor-pointer"
              >
                <GithubIcon className="w-4 h-4" />
                <span>Browse Releases on GitHub</span>
              </a>
            </div>
          </div>

        </div>

        {/* Sideloading Notice Banner */}
        <div className="mt-8 max-w-5xl mx-auto p-5 rounded-2xl bg-[#15171C] border border-saku-border/80 flex flex-col sm:flex-row items-start sm:items-center gap-4 text-xs text-saku-text-secondary">
          <div className="w-9 h-9 rounded-xl bg-saku-amber/10 border border-saku-amber/20 flex items-center justify-center text-saku-amber shrink-0">
            <AlertCircle className="w-5 h-5" />
          </div>
          <div className="space-y-1">
            <span className="font-bold text-white">First time sideloading on Android?</span>
            <p className="text-saku-text-muted">
              Because Saku is distributed outside the Google Play Store, Google Play Protect may show an "Unrecognized app" banner. Simply tap <strong>"More details"</strong> → <strong>"Install anyway"</strong>.
            </p>
          </div>
        </div>

      </div>
    </section>
  );
};
