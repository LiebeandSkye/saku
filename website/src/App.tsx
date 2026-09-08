import { useState } from 'react';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { Features } from './components/Features';
import { SetupGuide } from './components/SetupGuide';
import { FaqSection } from './components/FaqSection';
import { Footer } from './components/Footer';
import { QrModal } from './components/QrModal';

export function App() {
  const [isQrModalOpen, setIsQrModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-[#FAFBFC] dark:bg-[#15171C] text-slate-900 dark:text-[#E8EAF0] selection:bg-[#52C47C] selection:text-[#091F11] relative overflow-x-hidden font-sans">
      {/* Navigation */}
      <Navbar onOpenQrModal={() => setIsQrModalOpen(true)} />

      {/* Main Page Content */}
      <main>
        <Hero onOpenQrModal={() => setIsQrModalOpen(true)} />
        <Features />
        <SetupGuide />
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
