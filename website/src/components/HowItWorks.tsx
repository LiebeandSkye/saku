import React, { useState } from 'react';
import { Database, Smartphone, ShieldCheck, RefreshCw, Layers, Code, Check } from 'lucide-react';

export const HowItWorks: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'flow' | 'code'>('flow');

  return (
    <section id="how-it-works" className="py-24 bg-[#1A1D23]/50 border-y border-saku-border/60 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <p className="font-serif text-sm text-saku-matcha tracking-widest uppercase font-semibold">
            Under the Hood
          </p>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            How Saku Seamlessly Syncs with AnkiDroid
          </h2>
          <p className="text-saku-text-secondary text-base">
            Zero cloud middleman. Saku leverages Android's secure <code className="px-2 py-0.5 rounded bg-[#2C313D] text-xs font-mono text-saku-matcha">ContentProvider</code> IPC mechanism to read and grade flashcards directly on-device.
          </p>

          {/* Tab Switcher */}
          <div className="inline-flex items-center gap-1 bg-[#1F222A] p-1 rounded-xl border border-saku-border mt-4">
            <button
              onClick={() => setActiveTab('flow')}
              className={`px-4 py-2 rounded-lg text-xs font-semibold transition-all cursor-pointer flex items-center gap-2 ${
                activeTab === 'flow' ? 'bg-saku-matcha text-[#091F11] shadow-sm' : 'text-saku-text-secondary hover:text-white'
              }`}
            >
              <Layers className="w-3.5 h-3.5" />
              <span>Architecture Pipeline</span>
            </button>
            <button
              onClick={() => setActiveTab('code')}
              className={`px-4 py-2 rounded-lg text-xs font-semibold transition-all cursor-pointer flex items-center gap-2 ${
                activeTab === 'code' ? 'bg-saku-matcha text-[#091F11] shadow-sm' : 'text-saku-text-secondary hover:text-white'
              }`}
            >
              <Code className="w-3.5 h-3.5" />
              <span>Native Kotlin IPC Code</span>
            </button>
          </div>
        </div>

        {/* Tab 1: Visual Architecture Pipeline */}
        {activeTab === 'flow' && (
          <div className="space-y-12">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 relative">
              
              {/* Step 1: UI Surfaces */}
              <div className="glass-card rounded-2xl p-6 border border-saku-border relative group hover:border-saku-matcha/40 transition-colors">
                <div className="text-xs font-mono text-saku-matcha font-bold mb-2">01 • DISPLAY</div>
                <div className="w-10 h-10 rounded-xl bg-saku-matcha/10 border border-saku-matcha/20 flex items-center justify-center text-saku-matcha mb-4">
                  <Smartphone className="w-5 h-5" />
                </div>
                <h4 className="text-lg font-bold text-white mb-2">Glance & RemoteViews</h4>
                <p className="text-xs text-saku-text-secondary leading-relaxed">
                  Interactive Android widget on Home Screen & high-contrast pinned notification on Lock Screen / AOD.
                </p>
              </div>

              {/* Step 2: Saku Engine */}
              <div className="glass-card rounded-2xl p-6 border border-saku-border relative group hover:border-saku-matcha/40 transition-colors">
                <div className="text-xs font-mono text-saku-rose font-bold mb-2">02 • PARSE</div>
                <div className="w-10 h-10 rounded-xl bg-saku-rose/10 border border-saku-rose/20 flex items-center justify-center text-saku-rose mb-4">
                  <RefreshCw className="w-5 h-5" />
                </div>
                <h4 className="text-lg font-bold text-white mb-2">Saku Core Parser</h4>
                <p className="text-xs text-saku-text-secondary leading-relaxed">
                  Extracts kanji, readings, and meanings while cleaning bracket furigana <code className="text-[11px] font-mono text-saku-matcha">日本[にほん]</code> and HTML tags.
                </p>
              </div>

              {/* Step 3: AnkiDroid IPC */}
              <div className="glass-card rounded-2xl p-6 border border-saku-border relative group hover:border-saku-matcha/40 transition-colors">
                <div className="text-xs font-mono text-saku-slate font-bold mb-2">03 • GRADE</div>
                <div className="w-10 h-10 rounded-xl bg-[#202A36] border border-[#2C3E52] flex items-center justify-center text-saku-slate mb-4">
                  <Database className="w-5 h-5" />
                </div>
                <h4 className="text-lg font-bold text-white mb-2">On-Device IPC</h4>
                <p className="text-xs text-saku-text-secondary leading-relaxed">
                  Grades (Again, Hard, Good, Easy) are updated via <code className="text-[11px] font-mono text-saku-slate">com.ichi2.anki.api</code> directly to AnkiDroid's SQLite.
                </p>
              </div>

              {/* Step 4: AnkiWeb Cloud */}
              <div className="glass-card rounded-2xl p-6 border border-saku-border relative group hover:border-saku-matcha/40 transition-colors">
                <div className="text-xs font-mono text-saku-amber font-bold mb-2">04 • PRESERVE</div>
                <div className="w-10 h-10 rounded-xl bg-saku-amber/10 border border-saku-amber/20 flex items-center justify-center text-saku-amber mb-4">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <h4 className="text-lg font-bold text-white mb-2">FSRS & AnkiWeb</h4>
                <p className="text-xs text-saku-text-secondary leading-relaxed">
                  AnkiDroid preserves memory stability and difficulty curves, syncing effortlessly to your AnkiWeb account.
                </p>
              </div>

            </div>

            {/* Visual Process Bar */}
            <div className="p-6 rounded-2xl bg-[#15171C] border border-saku-border flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-saku-text-secondary">
              <div className="flex items-center gap-3">
                <div className="w-2.5 h-2.5 rounded-full bg-saku-matcha"></div>
                <span>Permission required: <strong>Read/Write AnkiDroid Database</strong> (1-tap prompt)</span>
              </div>
              <div className="flex items-center gap-2 font-mono text-saku-matcha">
                <Check className="w-4 h-4" />
                <span>Zero AnkiWeb Passwords Required</span>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Code Preview */}
        {activeTab === 'code' && (
          <div className="rounded-2xl bg-[#15171C] border border-saku-border overflow-hidden font-mono text-xs shadow-2xl">
            <div className="px-5 py-3 bg-[#1F222A] border-b border-saku-border flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500/80"></div>
                <div className="w-3 h-3 rounded-full bg-green-500/80"></div>
                <span className="ml-2 text-saku-text-muted text-[11px]">com/saku/anki/AnkiDroidHelper.kt</span>
              </div>
              <span className="text-[11px] text-saku-matcha font-medium">100% Native Android IPC</span>
            </div>
            
            <div className="p-6 overflow-x-auto text-saku-text-secondary space-y-4">
              <div>
                <span className="text-saku-text-muted">// 1. Fetching due cards directly from AnkiDroid local database</span>
                <p className="text-white">
                  <span className="text-purple-400">val</span> cursor = context.contentResolver.query(
                  <br />
                  &nbsp;&nbsp;AnkiDroidContract.Cards.CONTENT_URI,
                  <br />
                  &nbsp;&nbsp;arrayOf(<span className="text-green-300">"_id"</span>, <span className="text-green-300">"nid"</span>, <span className="text-green-300">"did"</span>, <span className="text-green-300">"ivl"</span>, <span className="text-green-300">"due"</span>),
                  <br />
                  &nbsp;&nbsp;<span className="text-green-300">"did = ?"</span>,
                  <br />
                  &nbsp;&nbsp;arrayOf(deckId.toString()),
                  <br />
                  &nbsp;&nbsp;<span className="text-green-300">"due ASC LIMIT 30"</span>
                  <br />
                  )
                </p>
              </div>

              <div className="pt-2 border-t border-saku-border/40">
                <span className="text-saku-text-muted">// 2. Submitting grades into AnkiDroid's FSRS / SM-2 scheduler</span>
                <p className="text-white">
                  <span className="text-purple-400">val</span> answerUri = Uri.parse(<span className="text-green-300">"content://com.ichi2.anki.flashcards/cards/$cardId/answer"</span>)
                  <br />
                  <span className="text-purple-400">val</span> values = ContentValues().apply &#123;
                  <br />
                  &nbsp;&nbsp;put(<span className="text-green-300">"ease"</span>, ease) <span className="text-saku-text-muted">// 1: Again, 2: Hard, 3: Good, 4: Easy</span>
                  <br />
                  &#125;
                  <br />
                  context.contentResolver.update(answerUri, values, <span className="text-purple-400">null</span>, <span className="text-purple-400">null</span>)
                </p>
              </div>
            </div>
          </div>
        )}

      </div>
    </section>
  );
};
