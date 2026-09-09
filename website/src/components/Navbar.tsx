import React, { useState, useEffect } from 'react';
import { Download, Menu, QrCode, Star, X } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';
import { ThemeToggle } from './ThemeToggle';

interface NavbarProps {
  onOpenQrModal: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenQrModal }) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 640) {
        setIsMobileMenuOpen(false);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-200 ${
        isScrolled || isMobileMenuOpen
          ? 'bg-white/95 dark:bg-[#15171C]/95 backdrop-blur-md border-b border-slate-200 dark:border-[#2F3440] py-3.5 shadow-sm'
          : 'bg-transparent py-4 sm:py-5'
      }`}
    >
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Authentic Brand Logo */}
        <a
          href="#"
          onClick={() => setIsMobileMenuOpen(false)}
          className="flex items-baseline gap-2 group cursor-pointer select-none"
        >
          <span className="font-bold text-xl text-slate-900 dark:text-[#E8EAF0] tracking-tight">アンキ</span>
          <span className="text-xs font-semibold tracking-[0.2em] text-slate-500 dark:text-[#9AA1AD]">SAKU</span>
        </a>

        {/* Desktop Actions: Theme, QR, GitHub, Download (preserved for desktop) */}
        <div className="hidden sm:flex items-center gap-3">
          <ThemeToggle />

          <button
            onClick={onOpenQrModal}
            className="px-3 py-1.5 text-xs font-medium text-slate-700 dark:text-[#9AA1AD] hover:text-slate-900 dark:hover:text-[#E8EAF0] bg-slate-100 dark:bg-[#1F222A] hover:bg-slate-200 dark:hover:bg-[#262A34] border border-slate-200 dark:border-[#2F3440] rounded-xl transition-colors cursor-pointer flex items-center gap-1.5"
            title="Scan QR to download on phone"
          >
            <QrCode className="w-3.5 h-3.5 text-emerald-600 dark:text-[#52C47C]" />
            <span>Scan QR</span>
          </button>

          <a
            href="https://github.com/LiebeandSkye/saku"
            target="_blank"
            rel="noopener noreferrer"
            className="px-3 py-1.5 text-xs font-medium text-slate-700 dark:text-[#9AA1AD] hover:text-slate-900 dark:hover:text-[#E8EAF0] bg-slate-100 dark:bg-[#1F222A] hover:bg-slate-200 dark:hover:bg-[#262A34] border border-slate-200 dark:border-[#2F3440] rounded-xl transition-colors cursor-pointer flex items-center gap-1.5 group"
            title="Star Saku on GitHub"
          >
            <GithubIcon className="w-3.5 h-3.5 text-slate-700 dark:text-[#9AA1AD] group-hover:text-slate-900 dark:group-hover:text-[#E8EAF0]" />
            <span>Give me a star!</span>
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

        {/* Mobile Actions: Clean, spacious bar with ThemeToggle, Quick APK CTA, and Hamburger Menu */}
        <div className="flex sm:hidden items-center gap-2">
          <ThemeToggle />

          <a
            href="/Saku.apk"
            download="Saku.apk"
            className="px-3 py-1.5 text-xs font-semibold text-white bg-[#52C47C] active:bg-[#43A869] rounded-xl transition-colors flex items-center gap-1.5 shadow-sm"
          >
            <Download className="w-3.5 h-3.5 stroke-[2.5]" />
            <span>APK</span>
          </a>

          <button
            onClick={() => setIsMobileMenuOpen((prev) => !prev)}
            aria-label={isMobileMenuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={isMobileMenuOpen}
            className="p-2 rounded-xl bg-slate-100 dark:bg-[#1F222A] text-slate-700 dark:text-[#E8EAF0] border border-slate-200 dark:border-[#2F3440] hover:bg-slate-200 dark:hover:bg-[#262A34] transition-colors cursor-pointer flex items-center justify-center"
          >
            {isMobileMenuOpen ? (
              <X className="w-4 h-4" />
            ) : (
              <Menu className="w-4 h-4" />
            )}
          </button>
        </div>
      </div>

      {/* Mobile Dropdown Menu (only visible on mobile when toggled) */}
      {isMobileMenuOpen && (
        <div className="sm:hidden border-t border-slate-200 dark:border-[#2F3440] bg-white/98 dark:bg-[#15171C]/98 backdrop-blur-xl px-4 py-4 space-y-3 mt-3 shadow-xl">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-[#6E7482] px-1">
            Navigation
          </div>
          <nav className="flex flex-col space-y-1">
            <a
              href="#features"
              onClick={() => setIsMobileMenuOpen(false)}
              className="px-3 py-2.5 rounded-xl text-sm font-medium text-slate-700 dark:text-[#E8EAF0] hover:bg-slate-100 dark:hover:bg-[#1F222A] active:bg-slate-200 dark:active:bg-[#262A34] transition-colors flex items-center justify-between"
            >
              <span>Features</span>
              <span className="text-xs text-slate-400 dark:text-[#9AA1AD]">Cards & Reader</span>
            </a>
            <a
              href="#setup"
              onClick={() => setIsMobileMenuOpen(false)}
              className="px-3 py-2.5 rounded-xl text-sm font-medium text-slate-700 dark:text-[#E8EAF0] hover:bg-slate-100 dark:hover:bg-[#1F222A] active:bg-slate-200 dark:active:bg-[#262A34] transition-colors flex items-center justify-between"
            >
              <span>Setup Guide</span>
              <span className="text-xs text-slate-400 dark:text-[#9AA1AD]">3 Easy Steps</span>
            </a>
            <a
              href="#faq"
              onClick={() => setIsMobileMenuOpen(false)}
              className="px-3 py-2.5 rounded-xl text-sm font-medium text-slate-700 dark:text-[#E8EAF0] hover:bg-slate-100 dark:hover:bg-[#1F222A] active:bg-slate-200 dark:active:bg-[#262A34] transition-colors flex items-center justify-between"
            >
              <span>FAQ</span>
              <span className="text-xs text-slate-400 dark:text-[#9AA1AD]">Questions & Answers</span>
            </a>
          </nav>

          <div className="pt-2 border-t border-slate-200 dark:border-[#2F3440] flex flex-col gap-2">
            <a
              href="https://github.com/LiebeandSkye/saku"
              target="_blank"
              rel="noopener noreferrer"
              onClick={() => setIsMobileMenuOpen(false)}
              className="px-3 py-2.5 rounded-xl bg-slate-100 dark:bg-[#1F222A] text-slate-700 dark:text-[#E8EAF0] border border-slate-200 dark:border-[#2F3440] text-sm font-medium flex items-center justify-between transition-colors active:scale-[0.99]"
            >
              <div className="flex items-center gap-2">
                <GithubIcon className="w-4 h-4 text-slate-700 dark:text-[#9AA1AD]" />
                <span>Star on GitHub</span>
              </div>
              <Star className="w-3.5 h-3.5 text-amber-400 fill-amber-400" />
            </a>

            <button
              onClick={() => {
                setIsMobileMenuOpen(false);
                onOpenQrModal();
              }}
              className="px-3 py-2.5 rounded-xl bg-slate-100 dark:bg-[#1F222A] text-slate-700 dark:text-[#E8EAF0] border border-slate-200 dark:border-[#2F3440] text-sm font-medium flex items-center justify-between transition-colors text-left active:scale-[0.99]"
            >
              <div className="flex items-center gap-2">
                <QrCode className="w-4 h-4 text-emerald-600 dark:text-[#52C47C]" />
                <span>Show QR Code</span>
              </div>
              <span className="text-xs text-slate-400 dark:text-[#9AA1AD]">Share with device</span>
            </button>

            <a
              href="/Saku.apk"
              download="Saku.apk"
              onClick={() => setIsMobileMenuOpen(false)}
              className="w-full py-3 px-4 rounded-xl bg-[#52C47C] hover:bg-[#43A869] active:bg-[#3B965D] text-white font-bold text-sm transition-colors flex items-center justify-center gap-2 shadow-sm mt-1"
            >
              <Download className="w-4 h-4 stroke-[2.5]" />
              <span>Download Saku.apk</span>
            </a>
          </div>
        </div>
      )}
    </header>
  );
};
