import React, { useState, useEffect } from 'react';
import { Download, QrCode, Star } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';
import { ThemeToggle } from './ThemeToggle';

interface NavbarProps {
  onOpenQrModal: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenQrModal }) => {
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-200 ${
        isScrolled
          ? 'bg-white/90 dark:bg-[#15171C]/90 backdrop-blur-md border-b border-slate-200 dark:border-[#2F3440] py-3.5 shadow-sm'
          : 'bg-transparent py-5'
      }`}
    >
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Authentic Brand Logo */}
        <a href="#" className="flex items-baseline gap-2 group cursor-pointer select-none">
          <span className="font-bold text-xl text-slate-900 dark:text-[#E8EAF0] tracking-tight">アンキ</span>
          <span className="text-xs font-semibold tracking-[0.2em] text-slate-500 dark:text-[#9AA1AD]">SAKU</span>
        </a>

        {/* Right Actions: Theme, QR, GitHub, Download */}
        <div className="flex items-center gap-2 sm:gap-3">
          <ThemeToggle />

          <button
            onClick={onOpenQrModal}
            className="px-3 py-1.5 text-xs font-medium text-slate-700 dark:text-[#9AA1AD] hover:text-slate-900 dark:hover:text-[#E8EAF0] bg-slate-100 dark:bg-[#1F222A] hover:bg-slate-200 dark:hover:bg-[#262A34] border border-slate-200 dark:border-[#2F3440] rounded-xl transition-colors cursor-pointer flex items-center gap-1.5"
            title="Scan QR to download on phone"
          >
            <QrCode className="w-3.5 h-3.5 text-emerald-600 dark:text-[#52C47C]" />
            <span className="hidden sm:inline">Scan QR</span>
          </button>

          <a
            href="https://github.com/LiebeandSkye/saku"
            target="_blank"
            rel="noopener noreferrer"
            className="px-3 py-1.5 text-xs font-medium text-slate-700 dark:text-[#9AA1AD] hover:text-slate-900 dark:hover:text-[#E8EAF0] bg-slate-100 dark:bg-[#1F222A] hover:bg-slate-200 dark:hover:bg-[#262A34] border border-slate-200 dark:border-[#2F3440] rounded-xl transition-colors cursor-pointer flex items-center gap-1.5 group"
            title="Star Saku on GitHub"
          >
            <GithubIcon className="w-3.5 h-3.5 text-slate-700 dark:text-[#9AA1AD] group-hover:text-slate-900 dark:group-hover:text-[#E8EAF0]" />
            <span className="hidden sm:inline">Give me a star!</span>
            <Star className="w-3 h-3 text-amber-400 fill-amber-400/80 group-hover:fill-amber-400 group-hover:scale-110 transition-all" />
          </a>

          <a
            href="/Saku.apk"
            download="Saku.apk"
            className="px-4 py-1.5 text-xs font-semibold text-white bg-[#52C47C] hover:bg-[#43A869] rounded-xl transition-colors flex items-center gap-1.5 cursor-pointer shadow-sm"
          >
            <Download className="w-3.5 h-3.5 stroke-[2.5]" />
            <span>Download</span>
          </a>
        </div>
      </div>
    </header>
  );
};
