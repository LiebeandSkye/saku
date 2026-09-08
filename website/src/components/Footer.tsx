import React from 'react';
import { Download, ExternalLink, Star } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-slate-200 dark:border-[#2F3440] py-12 text-slate-600 dark:text-[#9AA1AD] text-xs">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-6">
        {/* Brand */}
        <div className="flex flex-col sm:flex-row items-center sm:items-baseline gap-3 text-center sm:text-left">
          <div className="flex items-baseline gap-2">
            <span className="font-bold text-lg text-slate-900 dark:text-[#E8EAF0] tracking-tight">アンキ</span>
            <span className="text-xs font-semibold tracking-[0.2em] text-slate-500 dark:text-[#9AA1AD]">SAKU</span>
          </div>
          <span className="text-slate-400 dark:text-slate-600 hidden sm:inline">•</span>
          <span>Passive Japanese flashcards for Android. Free & MIT Licensed.</span>
        </div>

        {/* Links */}
        <div className="flex items-center gap-5">
          <a
            href="/Saku.apk"
            download="Saku.apk"
            className="hover:text-slate-900 dark:hover:text-[#E8EAF0] transition-colors flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download APK</span>
          </a>

          <a
            href="https://github.com/LiebeandSkye/saku"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:text-slate-900 dark:hover:text-[#E8EAF0] transition-colors flex items-center gap-1.5 group"
          >
            <GithubIcon className="w-3.5 h-3.5" />
            <span>Star on GitHub</span>
            <Star className="w-3 h-3 text-amber-400 fill-amber-400/80 group-hover:scale-110 transition-transform" />
          </a>

          <a
            href="https://github.com/LiebeandSkye/saku/blob/main/LICENSE"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:text-slate-900 dark:hover:text-[#E8EAF0] transition-colors flex items-center gap-1"
          >
            <span>MIT License</span>
            <ExternalLink className="w-3 h-3 text-slate-400" />
          </a>
        </div>
      </div>
    </footer>
  );
};
