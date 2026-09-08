import React, { useState } from 'react';
import { Download, Link2, Smartphone, CheckCircle2 } from 'lucide-react';

export const SetupGuide: React.FC = () => {
  const [activeStep, setActiveStep] = useState<1 | 2 | 3>(1);

  const steps = [
    {
      step: 1,
      title: "Install Pre-built APK",
      duration: "10 Seconds",
      icon: Download,
      description: "Download Saku.apk directly from this site or GitHub Releases onto your Android phone. Tap the file to install. If Google Play Protect displays an unknown app prompt, tap 'More details' → 'Install anyway'.",
      details: [
        "No Google Play Store account required",
        "100% open source under MIT License",
        "Lightweight ~20 MB standalone package"
      ]
    },
    {
      step: 2,
      title: "1-Tap AnkiDroid Sync",
      duration: "5 Seconds",
      icon: Link2,
      description: "Launch Saku. Tap 'Connect to AnkiDroid (1-Tap)' and press 'Allow' on Android's secure IPC permission dialog. Then choose your preferred Japanese deck (e.g. Kaishi 1.5k, Core 2k/6k, Tango N5/N4, or Wanikani).",
      details: [
        "Zero logins or passwords needed",
        "Instant local ContentProvider handshake",
        "Automatically identifies Japanese card fields"
      ]
    },
    {
      step: 3,
      title: "Place Widget & Lock Screen",
      duration: "5 Seconds",
      icon: Smartphone,
      description: "Long-press your Home Screen → tap Widgets → add Saku. To get passive immersion without unlocking your phone, open Saku and toggle 'Lock Screen & AOD Display' ON.",
      details: [
        "Resize widget freely from 2x2 to full width",
        "Optimized for OxygenOS (OnePlus), OneUI, Pixel UI",
        "Under 0.1% daily battery impact"
      ]
    }
  ];

  return (
    <section id="setup" className="py-24 bg-[#1A1D23]/40 border-t border-saku-border/60 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <p className="font-serif text-sm text-saku-matcha tracking-widest uppercase font-semibold">
            Zero Friction Setup
          </p>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Up and Running in 30 Seconds
          </h2>
          <p className="text-saku-text-secondary text-base">
            No complicated server setup or cloud account migrations. Pair Saku with AnkiDroid on your phone in 3 straightforward steps.
          </p>
        </div>

        {/* Step Selector Buttons */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          {steps.map((item) => {
            const Icon = item.icon;
            const isActive = activeStep === item.step;
            return (
              <button
                key={item.step}
                onClick={() => setActiveStep(item.step as 1 | 2 | 3)}
                className={`text-left p-5 rounded-2xl border transition-all cursor-pointer ${
                  isActive
                    ? 'bg-[#1F222A] border-saku-matcha shadow-lg shadow-saku-matcha/5 ring-1 ring-saku-matcha/40'
                    : 'bg-[#15171C] border-saku-border hover:border-saku-border-highlight text-saku-text-secondary'
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                    isActive ? 'bg-saku-matcha text-[#091F11]' : 'bg-[#1F222A] text-saku-text-muted'
                  }`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <span className="text-[11px] font-mono text-saku-text-muted">{item.duration}</span>
                </div>
                <div className="text-xs font-mono font-bold text-saku-matcha mb-1">STEP 0{item.step}</div>
                <h3 className={`text-base font-bold ${isActive ? 'text-white' : 'text-saku-text-secondary'}`}>
                  {item.title}
                </h3>
              </button>
            );
          })}
        </div>

        {/* Active Step Details Card */}
        {steps.map((item) => {
          if (item.step !== activeStep) return null;
          const Icon = item.icon;
          return (
            <div key={item.step} className="glass-card rounded-3xl p-8 border border-saku-border animate-fadeIn">
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
                <div className="lg:col-span-8 space-y-4">
                  <div className="flex items-center gap-2 text-xs font-mono font-bold text-saku-matcha">
                    <span>STEP 0{item.step} OF 03</span>
                    <span>•</span>
                    <span className="text-saku-text-muted">{item.duration}</span>
                  </div>
                  <h3 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
                    {item.title}
                  </h3>
                  <p className="text-saku-text-secondary text-base leading-relaxed">
                    {item.description}
                  </p>

                  <div className="space-y-2 pt-2">
                    {item.details.map((detail, idx) => (
                      <div key={idx} className="flex items-center gap-2.5 text-sm text-saku-text-secondary">
                        <CheckCircle2 className="w-4 h-4 text-saku-matcha shrink-0" />
                        <span>{detail}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="lg:col-span-4 bg-[#15171C] rounded-2xl p-6 border border-saku-border flex flex-col justify-center items-center text-center space-y-3">
                  <div className="w-14 h-14 rounded-2xl bg-saku-matcha/10 border border-saku-matcha/30 flex items-center justify-center text-saku-matcha">
                    <Icon className="w-7 h-7" />
                  </div>
                  <div className="text-xs font-bold text-white uppercase tracking-wider">
                    {item.title}
                  </div>
                  <p className="text-xs text-saku-text-muted">
                    Instant on-device execution with native Android components.
                  </p>
                </div>
              </div>
            </div>
          );
        })}

      </div>
    </section>
  );
};
