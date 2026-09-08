import React from 'react';
import { X, Smartphone, CheckCircle2, ExternalLink } from 'lucide-react';

interface QrModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const QrModal: React.FC<QrModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  // Direct APK download link or GitHub releases link for mobile scanner
  const downloadUrl = "https://github.com/LiebeandSkye/saku/releases";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-sm glass-card rounded-3xl p-6 sm:p-8 border border-saku-border shadow-2xl space-y-5"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-[#1F222A] text-saku-text-muted hover:text-white border border-saku-border transition-colors cursor-pointer"
          aria-label="Close modal"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Modal Header */}
        <div className="text-center space-y-1">
          <div className="w-12 h-12 rounded-2xl bg-saku-matcha/10 border border-saku-matcha/30 flex items-center justify-center text-saku-matcha mx-auto mb-3">
            <Smartphone className="w-6 h-6" />
          </div>
          <h3 className="text-xl font-bold text-white tracking-tight">Scan with Phone Camera</h3>
          <p className="text-xs text-saku-text-muted">
            Point your Android camera to download Saku.apk directly onto your phone.
          </p>
        </div>

        {/* QR Code Container (High Contrast White Card for reliable scanning) */}
        <div className="bg-white p-6 rounded-2xl flex flex-col items-center justify-center shadow-inner">
          <img 
            src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(downloadUrl)}`} 
            alt="Scan to Download Saku" 
            className="w-44 h-44 object-contain"
            loading="lazy"
          />
          <span className="text-[11px] font-mono text-gray-700 font-semibold mt-2">
            saku • direct mobile download
          </span>
        </div>

        {/* Info bullets */}
        <div className="space-y-2 text-xs text-saku-text-secondary">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha shrink-0" />
            <span>Universal Android 8.0+ Release</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-3.5 h-3.5 text-saku-matcha shrink-0" />
            <span>Direct GitHub verified binary</span>
          </div>
        </div>

        {/* Action Link */}
        <a
          href={downloadUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="w-full py-2.5 rounded-xl bg-saku-matcha hover:bg-saku-matcha-light text-[#091F11] font-bold text-xs flex items-center justify-center gap-1.5 transition-colors"
        >
          <span>Open Download URL</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </a>
      </div>
    </div>
  );
};
