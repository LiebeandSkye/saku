import React, { useState, useEffect } from 'react';
import { Download, Menu, X, Smartphone, Sparkles } from 'lucide-react';
import { GithubIcon } from './icons/GithubIcon';
import confetti from 'canvas-confetti';

interface NavbarProps {
  onOpenQrModal: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenQrModal }) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleDownloadClick = () => {
    confetti({
      particleCount: 80,
      spread: 60,
      origin: { y: 0.2 },
      colors: ['#52C47C', '#CF7B88', '#E06C75', '#6EE7A0', '#F0F3F8']
    });
  };

  return (
    <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
      isScrolled ? 'glass-nav py-3 shadow-lg shadow-black/30' : 'bg-transparent py-5'
    }`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Brand Logo */}
        <a href="#" className="flex items-center gap-3 group">
          <div className="w-10 h-10 rounded-xl bg-[#1F222A] border border-saku-border group-hover:border-saku-matcha/50 flex items-center justify-center transition-colors shadow-sm overflow-hidden p-1.5">
            <img src="/favicon.svg" alt="Saku Icon" className="w-full h-full object-contain" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-serif font-bold text-lg text-white tracking-wide">Saku</span>
            </div>
            <p className="text-[11px] text-saku-text-muted hidden sm:block">Japanese Flashcard Widget</p>
          </div>
        </a>

        {/* Desktop Nav Links */}
        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-saku-text-secondary">
          <a href="#features" className="hover:text-white transition-colors cursor-pointer">Features</a>
          <a href="#simulator" className="hover:text-white transition-colors cursor-pointer flex items-center gap-1.5">
            <Smartphone className="w-3.5 h-3.5 text-saku-matcha" />
            <span>Interactive Widget</span>
          </a>
          <a href="#how-it-works" className="hover:text-white transition-colors cursor-pointer">Architecture</a>
          <a href="#ai-reader" className="hover:text-white transition-colors cursor-pointer flex items-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5 text-saku-rose" />
            <span>AI Stories</span>
          </a>
          <a href="#setup" className="hover:text-white transition-colors cursor-pointer">Setup</a>
          <a href="#faq" className="hover:text-white transition-colors cursor-pointer">FAQ</a>
        </nav>

        {/* Action Buttons */}
        <div className="hidden lg:flex items-center gap-3">
          <button
            onClick={onOpenQrModal}
            className="px-3 py-2 text-xs font-medium text-saku-text-secondary hover:text-white bg-[#1F222A] hover:bg-[#262A34] border border-saku-border rounded-lg transition-colors cursor-pointer flex items-center gap-1.5"
            title="Scan QR to download on your phone"
          >
            <Smartphone className="w-3.5 h-3.5 text-saku-matcha" />
            <span>Scan QR</span>
          </button>

          <a
            href="https://github.com/LiebeandSkye/saku"
            target="_blank"
            rel="noopener noreferrer"
            className="p-2 text-saku-text-secondary hover:text-white bg-[#1F222A] hover:bg-[#262A34] border border-saku-border rounded-lg transition-colors cursor-pointer"
            aria-label="GitHub Repository"
          >
            <GithubIcon className="w-4 h-4" />
          </a>

          <a
            href="/Saku.apk"
            download="Saku.apk"
            onClick={handleDownloadClick}
            className="px-4 py-2 text-xs font-semibold text-[#091F11] bg-saku-matcha hover:bg-saku-matcha-light rounded-lg transition-all duration-200 shadow-sm hover:shadow-saku-glow flex items-center gap-2 cursor-pointer font-sans"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download APK</span>
          </a>
        </div>

        {/* Mobile Hamburger Button */}
        <div className="flex md:hidden items-center gap-2">
          <a
            href="/Saku.apk"
            download="Saku.apk"
            onClick={handleDownloadClick}
            className="px-3 py-1.5 text-xs font-semibold text-[#091F11] bg-saku-matcha rounded-lg flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5" />
            <span>APK</span>
          </a>

          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2 rounded-lg bg-[#1F222A] text-saku-text-secondary hover:text-white border border-saku-border cursor-pointer"
            aria-label="Toggle Menu"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden glass-nav border-t border-saku-border px-6 py-5 mt-3 space-y-4 animate-fadeIn">
          <nav className="flex flex-col space-y-3 text-sm font-medium">
            <a
              href="#features"
              onClick={() => setMobileMenuOpen(false)}
              className="text-saku-text-secondary hover:text-white py-1"
            >
              Features
            </a>
            <a
              href="#simulator"
              onClick={() => setMobileMenuOpen(false)}
              className="text-saku-text-secondary hover:text-white py-1 flex items-center gap-2"
            >
              <Smartphone className="w-4 h-4 text-saku-matcha" />
              Interactive Widget Demo
            </a>
            <a
              href="#how-it-works"
              onClick={() => setMobileMenuOpen(false)}
              className="text-saku-text-secondary hover:text-white py-1"
            >
              Architecture & IPC
            </a>
            <a
              href="#ai-reader"
              onClick={() => setMobileMenuOpen(false)}
              className="text-saku-text-secondary hover:text-white py-1 flex items-center gap-2"
            >
              <Sparkles className="w-4 h-4 text-saku-rose" />
              AI Graded Stories
            </a>
            <a
              href="#setup"
              onClick={() => setMobileMenuOpen(false)}
              className="text-saku-text-secondary hover:text-white py-1"
            >
              5-Second Setup Guide
            </a>
            <a
              href="#requirements"
              onClick={() => setMobileMenuOpen(false)}
              className="text-saku-text-secondary hover:text-white py-1"
            >
              Device Compatibility
            </a>
            <a
              href="#faq"
              onClick={() => setMobileMenuOpen(false)}
              className="text-saku-text-secondary hover:text-white py-1"
            >
              FAQ & Permissions
            </a>
          </nav>

          <div className="pt-4 border-t border-saku-border flex flex-col gap-2.5">
            <a
              href="/Saku.apk"
              download="Saku.apk"
              onClick={() => {
                handleDownloadClick();
                setMobileMenuOpen(false);
              }}
              className="w-full py-2.5 text-center text-sm font-semibold text-[#091F11] bg-saku-matcha rounded-lg flex items-center justify-center gap-2"
            >
              <Download className="w-4 h-4" />
              Download Saku.apk (~20 MB)
            </a>
            <a
              href="https://github.com/LiebeandSkye/saku"
              target="_blank"
              rel="noopener noreferrer"
              className="w-full py-2.5 text-center text-sm font-medium text-white bg-[#1F222A] border border-saku-border rounded-lg flex items-center justify-center gap-2"
            >
              <GithubIcon className="w-4 h-4" />
              View on GitHub
            </a>
          </div>
        </div>
      )}
    </header>
  );
};
