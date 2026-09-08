import React from 'react';
import { Heart, ArrowUpRight } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-saku-border/60 bg-[#121418] py-16 text-saku-text-secondary text-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 mb-12">
          
          {/* Brand Col */}
          <div className="md:col-span-2 space-y-4">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-[#1F222A] border border-saku-border flex items-center justify-center p-1">
                <img src="/favicon.svg" alt="Saku" className="w-full h-full object-contain" />
              </div>
              <span className="font-serif font-bold text-lg text-white">Saku • 咲く</span>
            </div>
            <p className="text-saku-text-muted text-xs leading-relaxed max-w-sm">
              Minimal Spaced Repetition Japanese Flashcard Widget for Android. Passive immersion on Home Screen, Lock Screen, and Always-On Display with native AnkiDroid FSRS synchronization.
            </p>
            <div className="flex items-center gap-3 pt-2">
              <a
                href="https://github.com/LiebeandSkye/saku"
                target="_blank"
                rel="noopener noreferrer"
                className="p-2 rounded-xl bg-[#1F222A] text-saku-text-secondary hover:text-white border border-saku-border transition-colors"
                aria-label="GitHub Repository"
              >
                <GithubIcon className="w-4 h-4" />
              </a>
              <span className="text-xs text-saku-text-muted font-mono">
                MIT Licensed • Kotlin 2.0 & Jetpack Glance
              </span>
            </div>
          </div>

          {/* Quick Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-mono uppercase tracking-wider text-white font-bold">Navigation</h4>
            <ul className="space-y-2 text-xs">
              <li><a href="#features" className="hover:text-white transition-colors">Features</a></li>
              <li><a href="#simulator" className="hover:text-white transition-colors">Interactive Widget Simulator</a></li>
              <li><a href="#how-it-works" className="hover:text-white transition-colors">Architecture & IPC</a></li>
              <li><a href="#ai-reader" className="hover:text-white transition-colors">AI Graded Reading</a></li>
              <li><a href="#setup" className="hover:text-white transition-colors">5-Second Setup</a></li>
              <li><a href="#requirements" className="hover:text-white transition-colors">Device Compatibility</a></li>
              <li><a href="#faq" className="hover:text-white transition-colors">FAQ & Permissions</a></li>
            </ul>
          </div>

          {/* Ecosystem & Open Source */}
          <div className="space-y-3">
            <h4 className="text-xs font-mono uppercase tracking-wider text-white font-bold">Open Source</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <a
                  href="https://github.com/LiebeandSkye/saku"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors flex items-center gap-1"
                >
                  <span>Source Code</span>
                  <ArrowUpRight className="w-3 h-3 text-saku-text-muted" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/LiebeandSkye/saku/releases"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors flex items-center gap-1"
                >
                  <span>Releases & Changelogs</span>
                  <ArrowUpRight className="w-3 h-3 text-saku-text-muted" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/ankidroid/Anki-Android"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors flex items-center gap-1"
                >
                  <span>AnkiDroid Project</span>
                  <ArrowUpRight className="w-3 h-3 text-saku-text-muted" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/LiebeandSkye/saku/blob/main/LICENSE"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors flex items-center gap-1"
                >
                  <span>MIT License</span>
                  <ArrowUpRight className="w-3 h-3 text-saku-text-muted" />
                </a>
              </li>
            </ul>
          </div>

        </div>

        {/* Bottom Bar */}
        <div className="pt-8 border-t border-saku-border flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-saku-text-muted">
          <p>© {new Date().getFullYear()} LiebeandSkye / Saku. Built for Japanese learners.</p>
          <div className="flex items-center gap-1">
            <span>Crafted with</span>
            <Heart className="w-3.5 h-3.5 text-saku-rose fill-saku-rose" />
            <span>for lifelong fluency</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
