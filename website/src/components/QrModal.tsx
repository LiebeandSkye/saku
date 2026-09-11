import React from 'react';
import { X, QrCode, ExternalLink, Download } from 'lucide-react';
import { APK_DOWNLOAD_URL, GITHUB_RELEASES_URL } from '../constants';

interface QrModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const QrModal: React.FC<QrModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  const downloadUrl = APK_DOWNLOAD_URL;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-sm bg-white dark:bg-[#1F222A] rounded-3xl p-6 sm:p-7 border border-slate-200 dark:border-[#2F3440] shadow-2xl space-y-5"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-xl bg-slate-100 dark:bg-[#15171C] text-slate-500 hover:text-slate-900 dark:hover:text-[#E8EAF0] border border-slate-200 dark:border-[#2F3440] transition-colors cursor-pointer"
          aria-label="Close modal"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Modal Header */}
        <div className="text-center space-y-1">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-[#15171C] border border-emerald-200 dark:border-[#2F3440] flex items-center justify-center text-emerald-600 dark:text-[#52C47C] mx-auto mb-2">
            <QrCode className="w-5 h-5" />
          </div>
          <h3 className="text-lg font-bold text-slate-900 dark:text-[#E8EAF0]">
            Scan with Phone Camera
          </h3>
          <p className="text-xs text-slate-500 dark:text-[#9AA1AD]">
            Download Saku.apk directly onto your Android device.
          </p>
        </div>

        {/* QR Code */}
        <div className="bg-white p-5 rounded-2xl flex flex-col items-center justify-center border border-slate-200">
          <img
            src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(downloadUrl)}`}
            alt="Scan to Download Saku"
            className="w-40 h-40 object-contain"
            loading="lazy"
          />
          <span className="text-[11px] font-mono text-slate-600 font-semibold mt-2">
            GitHub Verified Release
          </span>
        </div>

        {/* Action Links */}
        <div className="space-y-2">
          <a
            href={APK_DOWNLOAD_URL}
            className="w-full py-2.5 rounded-xl bg-[#52C47C] hover:bg-[#43A869] text-white font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Direct Download APK</span>
          </a>

          <a
            href={GITHUB_RELEASES_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="w-full py-2 rounded-xl text-slate-500 dark:text-[#9AA1AD] hover:text-slate-900 dark:hover:text-[#E8EAF0] text-xs font-medium flex items-center justify-center gap-1 transition-colors"
          >
            <span>View Release on GitHub</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </div>
    </div>
  );
};
