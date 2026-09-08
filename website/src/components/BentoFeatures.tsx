import React from 'react';
import { Shield, Brain, Smartphone, Sparkles, CheckCircle2 } from 'lucide-react';

export const BentoFeatures: React.FC = () => {
  return (
    <section id="features" className="py-24 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <p className="font-serif text-sm text-saku-matcha tracking-widest uppercase font-semibold">
            Designed for Effortless Retention
          </p>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Built for Serious Japanese Learners.
          </h2>
          <p className="text-saku-text-secondary text-base">
            No friction, no bloated features, no subscription paywalls. Saku does one thing with uncompromising craftsmanship: delivers your Anki cards where your eyes already go.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          {/* Card 1: Zero Login / On-Device (Col Span 2) */}
          <div className="md:col-span-2 glass-card glass-card-hover rounded-3xl p-8 relative overflow-hidden flex flex-col justify-between">
            <div className="space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-saku-matcha/10 border border-saku-matcha/20 flex items-center justify-center text-saku-matcha">
                <Shield className="w-6 h-6" />
              </div>
              <h3 className="text-2xl font-bold text-white tracking-tight">
                Zero Login. 100% On-Device & Private.
              </h3>
              <p className="text-saku-text-secondary text-sm sm:text-base leading-relaxed max-w-xl">
                Never hand over your AnkiWeb credentials. Saku operates strictly on your phone using Android's native inter-process <code className="px-2 py-0.5 rounded bg-[#2C313D] text-xs font-mono text-saku-matcha">ContentProvider</code> API. With a single tap permission prompt, your flashcard data remains entirely yours.
              </p>
            </div>

            <div className="mt-8 pt-6 border-t border-saku-border flex flex-wrap gap-3 text-xs text-saku-text-secondary">
              <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#15171C] border border-saku-border">
                <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                No cloud relay servers
              </span>
              <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#15171C] border border-saku-border">
                <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                Zero telemetry or ads
              </span>
              <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#15171C] border border-saku-border">
                <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha" />
                100% Offline capability
              </span>
            </div>
          </div>

          {/* Card 2: Algorithm Preservation (Col Span 1) */}
          <div className="glass-card glass-card-hover rounded-3xl p-8 flex flex-col justify-between">
            <div className="space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-[#202A36] border border-[#2C3E52] flex items-center justify-center text-saku-slate">
                <Brain className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white tracking-tight">
                Preserves FSRS & SM-2
              </h3>
              <p className="text-saku-text-secondary text-sm leading-relaxed">
                Widget reviews are submitted directly to AnkiDroid. Your weights, retention factors, stability curves, and AnkiWeb sync stay 100% accurate.
              </p>
            </div>

            <div className="mt-6 p-3 rounded-xl bg-[#15171C] border border-saku-border font-mono text-xs text-saku-text-muted">
              <span className="text-saku-slate">contentResolver.update</span>
              <br />
              <span className="text-saku-matcha">(cardAnswerUri, easeValues)</span>
            </div>
          </div>

          {/* Card 3: Lock Screen & AOD (Col Span 1) */}
          <div className="glass-card glass-card-hover rounded-3xl p-8 flex flex-col justify-between">
            <div className="space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-saku-rose/10 border border-saku-rose/20 flex items-center justify-center text-saku-rose">
                <Smartphone className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white tracking-tight">
                Lock Screen & AOD Display
              </h3>
              <p className="text-saku-text-secondary text-sm leading-relaxed">
                A high-contrast card pinned under your lock screen clock. Review a word whenever you check the time on OnePlus (OxygenOS), Samsung, or Google Pixel.
              </p>
            </div>

            <div className="mt-6 flex items-center gap-2 text-xs font-medium text-saku-rose">
              <span className="w-2 h-2 rounded-full bg-saku-rose animate-ping"></span>
              <span>Glance without unlocking</span>
            </div>
          </div>

          {/* Card 4: AI Graded Reading Generator (Col Span 2) */}
          <div className="md:col-span-2 glass-card glass-card-hover rounded-3xl p-8 flex flex-col justify-between">
            <div className="space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-saku-matcha/10 border border-saku-matcha/20 flex items-center justify-center text-saku-matcha">
                <Sparkles className="w-6 h-6" />
              </div>
              <h3 className="text-2xl font-bold text-white tracking-tight">
                AI Reading Generator (Gemini Flash)
              </h3>
              <p className="text-saku-text-secondary text-sm sm:text-base leading-relaxed max-w-xl">
                Move beyond isolated flashcards. Saku synthesizes customized Japanese reading stories dynamically using the exact vocabulary due in your Anki decks, complete with tap-to-define vocabulary, furigana toggle, and Fish Audio speech.
              </p>
            </div>

            <div className="mt-6 pt-6 border-t border-saku-border flex flex-wrap gap-4 text-xs font-mono text-saku-text-muted">
              <span className="text-saku-matcha font-semibold">✓ Graded to your JLPT level</span>
              <span>✓ Instant dictionary lookup</span>
              <span>✓ English translation drawer</span>
            </div>
          </div>

          {/* Card 5: Battery & Performance (Col Span 3) */}
          <div className="md:col-span-3 glass-card rounded-3xl p-8 border border-saku-border">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-6 text-center">
              <div>
                <div className="text-3xl sm:text-4xl font-extrabold text-saku-matcha font-mono">~20 MB</div>
                <div className="text-xs text-saku-text-muted mt-1 uppercase tracking-wider font-semibold">APK Package Size</div>
              </div>
              <div>
                <div className="text-3xl sm:text-4xl font-extrabold text-white font-mono">&lt; 25 MB</div>
                <div className="text-xs text-saku-text-muted mt-1 uppercase tracking-wider font-semibold">Active RAM (0 on idle)</div>
              </div>
              <div>
                <div className="text-3xl sm:text-4xl font-extrabold text-saku-rose font-mono">&lt; 0.1%</div>
                <div className="text-xs text-saku-text-muted mt-1 uppercase tracking-wider font-semibold">Daily Battery Drain</div>
              </div>
              <div>
                <div className="text-3xl sm:text-4xl font-extrabold text-saku-slate font-mono">100%</div>
                <div className="text-xs text-saku-text-muted mt-1 uppercase tracking-wider font-semibold">Offline Available</div>
              </div>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
};
