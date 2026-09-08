import { useState } from 'react';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { BentoFeatures } from './components/BentoFeatures';
import { HowItWorks } from './components/HowItWorks';
import { GradedReaderDemo } from './components/GradedReaderDemo';
import { SetupGuide } from './components/SetupGuide';
import { Compatibility } from './components/Compatibility';
import { DownloadSection } from './components/DownloadSection';
import { FaqSection } from './components/FaqSection';
import { Footer } from './components/Footer';
import { QrModal } from './components/QrModal';

export function App() {
  const [isQrModalOpen, setIsQrModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-[#15171C] text-[#E8EAF0] selection:bg-saku-matcha selection:text-[#091F11] relative overflow-x-hidden font-sans">
      {/* Navigation */}
      <Navbar onOpenQrModal={() => setIsQrModalOpen(true)} />

      {/* Main Page Content */}
      <main>
        <Hero onOpenQrModal={() => setIsQrModalOpen(true)} />
        <BentoFeatures />
        <HowItWorks />
        <GradedReaderDemo />
        <SetupGuide />
        <Compatibility />
        <DownloadSection onOpenQrModal={() => setIsQrModalOpen(true)} />
        <FaqSection />
      </main>

      {/* Footer */}
      <Footer />

      {/* QR Code Phone Download Modal */}
      <QrModal isOpen={isQrModalOpen} onClose={() => setIsQrModalOpen(false)} />
    </div>
  );
}

export default App;
