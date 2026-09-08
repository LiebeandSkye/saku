import React, { useState } from 'react';
import { SAMPLE_STORIES } from '../data/stories';
import type { StoryWord } from '../data/stories';
import { Volume2, Globe, X } from 'lucide-react';

export const GradedReaderDemo: React.FC = () => {
  const story = SAMPLE_STORIES[0];
  const [showFurigana, setShowFurigana] = useState(true);
  const [showTranslation, setShowTranslation] = useState(false);
  const [selectedWord, setSelectedWord] = useState<StoryWord | null>(null);

  const handleWordClick = (token: StoryWord) => {
    setSelectedWord(token);
  };

  const handlePlayStory = () => {
    if ('speechSynthesis' in window) {
      const fullText = story.tokens.map(t => t.word).join('');
      const utterance = new SpeechSynthesisUtterance(fullText);
      utterance.lang = 'ja-JP';
      window.speechSynthesis.speak(utterance);
    }
  };

  return (
    <section id="ai-reader" className="py-24 relative overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <p className="font-serif text-sm text-saku-rose tracking-widest uppercase font-semibold">
            Contextual Immersion
          </p>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            AI Graded Reading Generator
          </h2>
          <p className="text-saku-text-secondary text-base">
            Never memorize isolated words without context. Saku uses <strong>Google Gemini Flash</strong> to generate short, natural Japanese passages containing your active Anki due vocabulary.
          </p>
        </div>

        {/* Interactive Reader Card */}
        <div className="max-w-3xl mx-auto glass-card rounded-3xl p-6 sm:p-10 border border-saku-border shadow-2xl relative">
          
          {/* Reader Top Bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-saku-border">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-saku-rose/15 text-saku-rose border border-saku-rose/30">
                  {story.level}
                </span>
                <span className="text-xs text-saku-text-muted">
                  {story.targetCount} Anki Due Words Embedded
                </span>
              </div>
              <h3 className="text-xl font-bold text-white mt-1 font-serif">
                {story.title}
              </h3>
            </div>

            {/* Controls */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowFurigana(!showFurigana)}
                className={`px-3 py-1.5 rounded-xl border text-xs font-medium transition-colors cursor-pointer flex items-center gap-1.5 ${
                  showFurigana ? 'bg-saku-matcha/10 border-saku-matcha/40 text-saku-matcha' : 'bg-[#15171C] border-saku-border text-saku-text-muted'
                }`}
                title="Toggle Furigana reading hints"
              >
                <span>振仮名</span>
                <span className="text-[10px]">{showFurigana ? 'ON' : 'OFF'}</span>
              </button>

              <button
                onClick={handlePlayStory}
                className="p-2 rounded-xl bg-[#15171C] border border-saku-border text-saku-text-secondary hover:text-white transition-colors cursor-pointer"
                title="Play Japanese Audio via TTS"
              >
                <Volume2 className="w-4 h-4 text-saku-slate" />
              </button>
            </div>
          </div>

          {/* Passage Body */}
          <div className="py-8 leading-loose tracking-wide text-lg sm:text-xl font-serif select-text">
            {story.tokens.map((token, index) => {
              const isTarget = token.isVocab;
              return (
                <span
                  key={index}
                  onClick={() => handleWordClick(token)}
                  className={`inline-block px-0.5 rounded transition-all cursor-pointer ${
                    isTarget
                      ? 'bg-saku-matcha/15 text-saku-matcha font-semibold border-b-2 border-saku-matcha hover:bg-saku-matcha/25'
                      : 'hover:bg-white/10'
                  }`}
                  title={token.meaning ? `${token.word}: ${token.meaning}` : undefined}
                >
                  {showFurigana && token.furigana ? (
                    <ruby>
                      {token.word}
                      <rt>{token.furigana}</rt>
                    </ruby>
                  ) : (
                    token.word
                  )}
                </span>
              );
            })}
          </div>

          {/* Interactive Word Inspector Popover */}
          {selectedWord && (
            <div className="mb-6 p-4 rounded-2xl bg-[#15171C] border border-saku-matcha/40 shadow-xl flex items-start justify-between gap-4 animate-fadeIn">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-xl font-bold font-serif text-white">{selectedWord.word}</span>
                  {selectedWord.furigana && (
                    <span className="text-xs font-mono text-saku-matcha px-2 py-0.5 rounded bg-saku-matcha/10 border border-saku-matcha/20">
                      【{selectedWord.furigana}】
                    </span>
                  )}
                  {selectedWord.isVocab && (
                    <span className="text-[10px] uppercase font-bold text-saku-amber bg-saku-amber/15 px-2 py-0.5 rounded border border-saku-amber/30">
                      Target Anki Card
                    </span>
                  )}
                </div>
                <p className="text-sm text-saku-text-secondary font-medium">
                  {selectedWord.meaning || 'Japanese particle/connective'}
                </p>
              </div>

              <button
                onClick={() => setSelectedWord(null)}
                className="p-1 rounded-lg text-saku-text-muted hover:text-white transition-colors cursor-pointer"
                aria-label="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Translation Toggle & Bottom Bar */}
          <div className="pt-6 border-t border-saku-border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <button
              onClick={() => setShowTranslation(!showTranslation)}
              className="px-4 py-2 rounded-xl bg-[#15171C] hover:bg-[#1F222A] text-xs font-semibold text-saku-text-secondary hover:text-white border border-saku-border transition-colors flex items-center gap-2 cursor-pointer"
            >
              <Globe className="w-3.5 h-3.5 text-saku-slate" />
              <span>{showTranslation ? 'Hide English Translation' : 'Reveal English Translation'}</span>
            </button>

            <div className="text-xs text-saku-text-muted flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-saku-matcha"></span>
              <span>Green underline indicates words currently due in AnkiDroid</span>
            </div>
          </div>

          {/* Collapsible Translation */}
          {showTranslation && (
            <div className="mt-4 p-4 rounded-xl bg-[#15171C]/90 border border-saku-border text-sm text-saku-text-secondary leading-relaxed animate-fadeIn">
              <p className="italic text-saku-text-muted mb-1 text-xs uppercase font-mono">English Translation:</p>
              <p>{story.englishTranslation}</p>
            </div>
          )}

        </div>

      </div>
    </section>
  );
};
