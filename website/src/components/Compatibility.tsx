import React, { useState } from 'react';
import { Smartphone, CheckCircle2 } from 'lucide-react';

export const Compatibility: React.FC = () => {
  const [selectedBrand, setSelectedBrand] = useState<'oneplus' | 'samsung' | 'pixel' | 'xiaomi' | 'other'>('oneplus');

  const brandGuides = {
    oneplus: {
      name: "OnePlus / OxygenOS",
      status: "Optimized & Highly Recommended",
      badgeClass: "bg-saku-matcha/15 text-saku-matcha border-saku-matcha/30",
      description: "Saku was specifically tailored for OxygenOS lock screens. The high-contrast card pins seamlessly below the lock screen clock and transitions naturally to the Always-On Display (AOD).",
      tips: [
        "Go to Settings → Notifications → Ensure 'Show sensitive content on Lock Screen' is allowed.",
        "Set battery usage for Saku to 'Don't optimize' to prevent OxygenOS from killing the AOD notification."
      ]
    },
    samsung: {
      name: "Samsung Galaxy / OneUI",
      status: "Fully Supported",
      badgeClass: "bg-saku-slate/15 text-saku-slate border-saku-slate/30",
      description: "Compatible with OneUI 3.0 through OneUI 7.0+. Both the Glance AppWidget and Lock Screen notification render cleanly.",
      tips: [
        "Settings → Battery and Device Care → Battery → Background usage limits → Add Saku to 'Never sleeping apps'.",
        "Settings → Lock screen → Notifications → Choose 'Detailed' view for full card visibility."
      ]
    },
    pixel: {
      name: "Google Pixel / Stock Android",
      status: "100% Native Support",
      badgeClass: "bg-saku-matcha/15 text-saku-matcha border-saku-matcha/30",
      description: "Pixel phones enjoy the purest Jetpack Glance widget experience. The lock screen card pins directly below the 'At a Glance' weather widget.",
      tips: [
        "Settings → Apps → Saku → Battery → Set to 'Unrestricted'.",
        "Enable Android 13+ 'Post Notifications' permission when prompted on first launch."
      ]
    },
    xiaomi: {
      name: "Xiaomi / HyperOS & MIUI",
      status: "Supported with Permissions",
      badgeClass: "bg-saku-amber/15 text-saku-amber border-saku-amber/30",
      description: "HyperOS and MIUI have strict aggressive RAM killers that require toggling Autostart permissions.",
      tips: [
        "Long-press Saku icon → App Info → Enable 'Autostart'.",
        "App Info → Battery Saver → Change from 'MIUI Battery saver' to 'No restrictions'.",
        "App Info → Other permissions → Enable 'Show on Lock screen'."
      ]
    },
    other: {
      name: "Motorola, Sony, Asus, Nothing",
      status: "Standard Android Support",
      badgeClass: "bg-saku-matcha/15 text-saku-matcha border-saku-matcha/30",
      description: "Any device running Android 8.0+ (API level 26 or higher) with AnkiDroid installed will run Saku smoothly.",
      tips: [
        "Ensure AnkiDroid is installed and has at least one deck created.",
        "Allow Saku to access AnkiDroid via the 1-tap permission prompt."
      ]
    }
  };

  const activeGuide = brandGuides[selectedBrand];

  return (
    <section id="requirements" className="py-24 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <p className="font-serif text-sm text-saku-matcha tracking-widest uppercase font-semibold">
            System & Device Compatibility
          </p>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Requirements & Device Optimization
          </h2>
          <p className="text-saku-text-secondary text-base">
            Engineered with minimal dependencies to ensure compatibility across older and newest Android phones.
          </p>
        </div>

        {/* Requirements Table Grid */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-12">
          
          <div className="glass-card rounded-2xl p-6 border border-saku-border">
            <div className="text-xs font-mono text-saku-matcha font-bold mb-2">OPERATING SYSTEM</div>
            <h4 className="text-lg font-bold text-white mb-1">Android 8.0+</h4>
            <p className="text-xs text-saku-text-muted">API Level 26 (Oreo) up to Android 15 (Vanilla Ice Cream).</p>
          </div>

          <div className="glass-card rounded-2xl p-6 border border-saku-border">
            <div className="text-xs font-mono text-saku-slate font-bold mb-2">COMPANION APP</div>
            <h4 className="text-lg font-bold text-white mb-1">AnkiDroid</h4>
            <p className="text-xs text-saku-text-muted">Free & open source on F-Droid or Play Store with 1+ deck.</p>
          </div>

          <div className="glass-card rounded-2xl p-6 border border-saku-border">
            <div className="text-xs font-mono text-saku-rose font-bold mb-2">HARDWARE IMPACT</div>
            <h4 className="text-lg font-bold text-white mb-1">&lt; 25 MB RAM</h4>
            <p className="text-xs text-saku-text-muted">Zero idle background overhead. ~20 MB storage.</p>
          </div>

          <div className="glass-card rounded-2xl p-6 border border-saku-border">
            <div className="text-xs font-mono text-saku-amber font-bold mb-2">AI EXTENSION (OPTIONAL)</div>
            <h4 className="text-lg font-bold text-white mb-1">Gemini API Key</h4>
            <p className="text-xs text-saku-text-muted">Only needed for the AI Reading Generator story generator.</p>
          </div>

        </div>

        {/* Interactive Manufacturer Selector */}
        <div className="glass-card rounded-3xl p-6 sm:p-10 border border-saku-border">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
            <div>
              <h3 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
                <Smartphone className="w-5 h-5 text-saku-matcha" />
                <span>Device-Specific Optimization Guide</span>
              </h3>
              <p className="text-xs text-saku-text-muted mt-1">
                Select your phone brand to see verified tips for widget and lock screen pinning:
              </p>
            </div>

            {/* Brand Pills */}
            <div className="flex flex-wrap gap-2">
              {(['oneplus', 'samsung', 'pixel', 'xiaomi', 'other'] as const).map((brandKey) => (
                <button
                  key={brandKey}
                  onClick={() => setSelectedBrand(brandKey)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                    selectedBrand === brandKey
                      ? 'bg-saku-matcha text-[#091F11] shadow-sm'
                      : 'bg-[#15171C] text-saku-text-secondary hover:text-white border border-saku-border'
                  }`}
                >
                  {brandGuides[brandKey].name.split('/')[0].trim()}
                </button>
              ))}
            </div>
          </div>

          {/* Active Brand Guide Card */}
          <div className="p-6 rounded-2xl bg-[#15171C] border border-saku-border space-y-4 animate-fadeIn">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <h4 className="text-lg font-bold text-white">{activeGuide.name}</h4>
              <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${activeGuide.badgeClass}`}>
                {activeGuide.status}
              </span>
            </div>

            <p className="text-sm text-saku-text-secondary leading-relaxed">
              {activeGuide.description}
            </p>

            <div className="pt-2 space-y-2">
              <div className="text-xs font-mono uppercase tracking-wider text-saku-matcha font-bold">
                Recommended Device Settings:
              </div>
              {activeGuide.tips.map((tip, idx) => (
                <div key={idx} className="flex items-start gap-2.5 text-xs sm:text-sm text-saku-text-secondary">
                  <CheckCircle2 className="w-4 h-4 text-saku-matcha shrink-0 mt-0.5" />
                  <span>{tip}</span>
                </div>
              ))}
            </div>
          </div>

        </div>

      </div>
    </section>
  );
};
