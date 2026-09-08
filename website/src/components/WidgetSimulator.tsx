import React, { useState } from 'react';
import { SAMPLE_FLASHCARDS } from '../data/flashcards';
import { Volume2, RotateCcw, Check, Eye, Layers, Clock } from 'lucide-react';

export const WidgetSimulator: React.FC = () => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isRevealed, setIsRevealed] = useState(false);
  const [showFurigana, setShowFurigana] = useState(true);
  const [widgetTheme, setWidgetTheme] = useState<'dim' | 'dark' | 'light'>('dim');
  const [dueCount, setDueCount] = useState(14);
  const [feedback, setFeedback] = useState<string | null>(null);

  const card = SAMPLE_FLASHCARDS[currentIndex];

  const handleReveal = () => {
    setIsRevealed(!isRevealed);
  };

  const handleGrade = (rating: 'again' | 'hard' | 'good' | 'easy', interval: string) => {
    const labels = {
      again: 'Again (<1m) • Card queued for relearn',
      hard: `Hard (${interval}) • Retention factor adjusted`,
      good: `Good (${interval}) • Scheduled in AnkiDroid`,
      easy: `Easy (${interval}) • Interval extended`
    };

    setFeedback(labels[rating]);
    setDueCount(prev => Math.max(0, prev - 1));

    setTimeout(() => {
      setFeedback(null);
      setIsRevealed(false);
      setCurrentIndex((prev) => (prev + 1) % SAMPLE_FLASHCARDS.length);
    }, 600);
  };

  const handlePlayAudio = (e: React.MouseEvent) => {
    e.stopPropagation();
    if ('speechSynthesis' in window) {
      const utterance = new SpeechSynthesisUtterance(card.reading || card.headword);
      utterance.lang = 'ja-JP';
      window.speechSynthesis.speak(utterance);
    }
  };

  // Widget theme color schemes matching Saku Android App
  const themeStyles = {
    dim: {
      container: 'bg-[#1F222A] text-[#E8EAF0] border-[#2F3440]',
      surfaceElevated: 'bg-[#262A34]',
      badge: 'bg-[#2C313D] text-[#9AA1AD] border-[#384359]',
      btnBorder: 'border-[#2F3440]',
      textMuted: 'text-[#9AA1AD]',
      textSubtle: 'text-[#6E7482]',
      accentGood: 'bg-[#233127] text-[#6EE7A0] border-[#374D3D]',
      accentAgain: 'bg-[#332024] text-[#E06C75] border-[#4A262D]',
      accentHard: 'bg-[#33281D] text-[#E5C07B] border-[#4A3A26]',
      accentEasy: 'bg-[#202A36] text-[#7B9EC7] border-[#2C3E52]',
    },
    dark: {
      container: 'bg-[#090B0E] text-[#F0F3F8] border-[#262D3D]',
      surfaceElevated: 'bg-[#13171F]',
      badge: 'bg-[#1B202B] text-[#A2AAB8] border-[#262D3D]',
      btnBorder: 'border-[#262D3D]',
      textMuted: 'text-[#A2AAB8]',
      textSubtle: 'text-[#656D7E]',
      accentGood: 'bg-[#1B3824] text-[#52C47C] border-[#2E633D]',
      accentAgain: 'bg-[#2D161A] text-[#E06C75] border-[#421E24]',
      accentHard: 'bg-[#2B2213] text-[#E5C07B] border-[#423318]',
      accentEasy: 'bg-[#152233] text-[#61AFEF] border-[#20344D]',
    },
    light: {
      container: 'bg-[#FFFFFF] text-[#141916] border-[#D6E2D8] shadow-xl',
      surfaceElevated: 'bg-[#F2F6F3]',
      badge: 'bg-[#E5EDE6] text-[#475549] border-[#D6E2D8]',
      btnBorder: 'border-[#D6E2D8]',
      textMuted: 'text-[#475549]',
      textSubtle: 'text-[#6B7B6E]',
      accentGood: 'bg-[#E4F3E8] text-[#2D7D46] border-[#BEE4C8]',
      accentAgain: 'bg-[#FFEAEF] text-[#D93848] border-[#FFCCD4]',
      accentHard: 'bg-[#FEF3C7] text-[#B45309] border-[#FDE68A]',
      accentEasy: 'bg-[#EFF6FF] text-[#1D4ED8] border-[#BFDBFE]',
    }
  };

  const currentThemeStyle = themeStyles[widgetTheme];

  return (
    <div className="w-full max-w-md mx-auto">
      {/* Simulator Toolbar Controls */}
      <div className="flex items-center justify-between gap-2 px-3 py-2 mb-3 bg-[#1F222A]/80 border border-saku-border rounded-xl text-xs backdrop-blur-md">
        <div className="flex items-center gap-1.5 text-saku-text-secondary">
          <Layers className="w-3.5 h-3.5 text-saku-matcha" />
          <span className="font-mono font-medium">Live Widget Preview</span>
        </div>

        {/* Theme Toggles */}
        <div className="flex items-center gap-1 bg-[#15171C] p-1 rounded-lg border border-saku-border/60">
          <button
            onClick={() => setWidgetTheme('dim')}
            className={`px-2 py-0.5 rounded text-[11px] font-medium transition-all ${
              widgetTheme === 'dim' ? 'bg-[#2C313D] text-white shadow-xs' : 'text-saku-text-muted hover:text-white'
            }`}
          >
            Dim
          </button>
          <button
            onClick={() => setWidgetTheme('dark')}
            className={`px-2 py-0.5 rounded text-[11px] font-medium transition-all ${
              widgetTheme === 'dark' ? 'bg-[#1F222A] text-white shadow-xs' : 'text-saku-text-muted hover:text-white'
            }`}
          >
            OLED
          </button>
          <button
            onClick={() => setWidgetTheme('light')}
            className={`px-2 py-0.5 rounded text-[11px] font-medium transition-all ${
              widgetTheme === 'light' ? 'bg-white text-gray-900 shadow-xs' : 'text-saku-text-muted hover:text-white'
            }`}
          >
            Light
          </button>
        </div>

        {/* Furigana Switcher */}
        <button
          onClick={() => setShowFurigana(!showFurigana)}
          className={`px-2 py-1 rounded-lg border text-[11px] font-medium flex items-center gap-1 transition-colors ${
            showFurigana ? 'bg-saku-matcha/10 border-saku-matcha/30 text-saku-matcha' : 'border-saku-border text-saku-text-muted'
          }`}
          title="Toggle Furigana reading hints"
        >
          <span>振仮名</span>
          <span className="text-[9px]">{showFurigana ? 'ON' : 'OFF'}</span>
        </button>
      </div>

      {/* Realistic Android Home Screen Widget Frame */}
      <div className={`relative rounded-3xl p-5 border-2 transition-all duration-300 ${currentThemeStyle.container}`}>
        
        {/* Top Meta Bar */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${currentThemeStyle.badge}`}>
              Kaishi 1.5k
            </span>
            <span className={`text-[11px] font-mono flex items-center gap-1 ${currentThemeStyle.textSubtle}`}>
              <Clock className="w-3 h-3 text-saku-amber" />
              <span>{dueCount} Due</span>
            </span>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={handlePlayAudio}
              className={`p-1.5 rounded-lg border ${currentThemeStyle.btnBorder} hover:opacity-80 transition-opacity cursor-pointer`}
              title="Listen to Japanese pronunciation"
            >
              <Volume2 className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => {
                setIsRevealed(false);
                setCurrentIndex((prev) => (prev + 1) % SAMPLE_FLASHCARDS.length);
              }}
              className={`p-1.5 rounded-lg border ${currentThemeStyle.btnBorder} hover:opacity-80 transition-opacity cursor-pointer`}
              title="Next Card"
            >
              <RotateCcw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Card Main Surface (Click to Flip / Reveal) */}
        <div
          onClick={handleReveal}
          className={`rounded-2xl p-6 mb-4 border transition-all duration-200 cursor-pointer text-center relative overflow-hidden select-none ${
            currentThemeStyle.surfaceElevated
          } ${currentThemeStyle.btnBorder} hover:border-saku-matcha/50`}
        >
          {/* JLPT Tag */}
          <div className="absolute top-3 right-3 text-[10px] font-bold px-1.5 py-0.5 rounded bg-saku-matcha/20 text-saku-matcha">
            {card.jlpt}
          </div>

          {/* Prompt / Headword */}
          <div className="py-2">
            {showFurigana ? (
              <ruby className="font-serif text-4xl sm:text-5xl font-bold tracking-wider inline-block">
                {card.headword}
                <rt className="text-sm font-sans">{card.reading}</rt>
              </ruby>
            ) : (
              <span className="font-serif text-4xl sm:text-5xl font-bold tracking-wider inline-block">
                {card.headword}
              </span>
            )}
          </div>

          <p className={`text-xs mt-1 font-mono ${currentThemeStyle.textSubtle}`}>
            {card.partOfSpeech}
          </p>

          {/* Reveal status or Back Details */}
          {!isRevealed ? (
            <div className="mt-4 pt-3 border-t border-current/10 flex items-center justify-center gap-1.5 text-xs text-saku-matcha font-medium">
              <Eye className="w-3.5 h-3.5" />
              <span>Tap to reveal answer & rating</span>
            </div>
          ) : (
            <div className="mt-4 pt-3 border-t border-current/10 text-left animate-fadeIn">
              <div className="mb-2">
                <span className="text-xs font-mono uppercase tracking-wider text-saku-matcha font-semibold">Meaning:</span>
                <p className="text-base font-medium leading-snug">{card.meaning}</p>
              </div>

              <div className="bg-black/10 rounded-lg p-2.5 text-xs">
                <p className="font-serif text-sm mb-1">{card.exampleSentence}</p>
                <p className={`text-[11px] ${currentThemeStyle.textSubtle}`}>{card.exampleMeaning}</p>
              </div>
            </div>
          )}
        </div>

        {/* Bottom Rating Buttons (Grading directly with Anki FSRS/SM-2) */}
        {isRevealed ? (
          <div className="grid grid-cols-4 gap-2">
            <button
              onClick={() => handleGrade('again', card.dueIntervals.again)}
              className={`py-2 px-1 rounded-xl border text-center font-medium transition-all transform active:scale-95 cursor-pointer ${currentThemeStyle.accentAgain}`}
            >
              <div className="text-xs font-bold">Again</div>
              <div className="text-[10px] opacity-80">{card.dueIntervals.again}</div>
            </button>

            <button
              onClick={() => handleGrade('hard', card.dueIntervals.hard)}
              className={`py-2 px-1 rounded-xl border text-center font-medium transition-all transform active:scale-95 cursor-pointer ${currentThemeStyle.accentHard}`}
            >
              <div className="text-xs font-bold">Hard</div>
              <div className="text-[10px] opacity-80">{card.dueIntervals.hard}</div>
            </button>

            <button
              onClick={() => handleGrade('good', card.dueIntervals.good)}
              className={`py-2 px-1 rounded-xl border text-center font-medium transition-all transform active:scale-95 cursor-pointer ${currentThemeStyle.accentGood}`}
            >
              <div className="text-xs font-bold">Good</div>
              <div className="text-[10px] opacity-80">{card.dueIntervals.good}</div>
            </button>

            <button
              onClick={() => handleGrade('easy', card.dueIntervals.easy)}
              className={`py-2 px-1 rounded-xl border text-center font-medium transition-all transform active:scale-95 cursor-pointer ${currentThemeStyle.accentEasy}`}
            >
              <div className="text-xs font-bold">Easy</div>
              <div className="text-[10px] opacity-80">{card.dueIntervals.easy}</div>
            </button>
          </div>
        ) : (
          <button
            onClick={handleReveal}
            className="w-full py-2.5 rounded-xl bg-saku-matcha text-[#091F11] font-semibold text-xs transition-all hover:bg-saku-matcha-light active:scale-98 shadow-sm flex items-center justify-center gap-2 cursor-pointer"
          >
            <Eye className="w-4 h-4" />
            <span>Show Answer (Anki Review)</span>
          </button>
        )}

        {/* Live IPC Feedback Notification */}
        {feedback && (
          <div className="absolute inset-x-4 bottom-5 p-3 rounded-xl bg-saku-matcha text-[#091F11] font-semibold text-xs text-center shadow-lg animate-bounce flex items-center justify-center gap-1.5">
            <Check className="w-4 h-4" />
            <span>{feedback}</span>
          </div>
        )}
      </div>

      <p className="text-[11px] text-center text-saku-text-muted mt-3">
        ⚡ Exact replica of Saku's Android Glance Home Screen Widget. All buttons are fully clickable.
      </p>
    </div>
  );
};
