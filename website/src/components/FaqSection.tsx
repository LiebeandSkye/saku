import React, { useState } from 'react';
import { FAQS } from '../data/faq';
import { ChevronDown } from 'lucide-react';

export const FaqSection: React.FC = () => {
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [openId, setOpenId] = useState<string | null>('fsrs-preservation');

  const categories = ['All', 'Algorithm & Sync', 'Privacy & Security', 'Android & Lock Screen', 'Decks & Furigana'];

  const filteredFaqs = selectedCategory === 'All'
    ? FAQS
    : FAQS.filter(f => f.category === selectedCategory);

  const toggleAccordion = (id: string) => {
    setOpenId(openId === id ? null : id);
  };

  return (
    <section id="faq" className="py-24 relative">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center mb-16 space-y-3">
          <p className="font-serif text-sm text-saku-matcha tracking-widest uppercase font-semibold">
            Essential Questions Answered
          </p>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Everything You Need to Know
          </h2>
          <p className="text-saku-text-secondary text-base">
            Technical clarity on privacy, FSRS math stability, Android permissions, and deck compatibility.
          </p>

          {/* Category Filter Pills */}
          <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all cursor-pointer ${
                  selectedCategory === cat
                    ? 'bg-saku-matcha text-[#091F11] shadow-sm'
                    : 'bg-[#1F222A] text-saku-text-secondary hover:text-white border border-saku-border'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Accordion List */}
        <div className="space-y-4">
          {filteredFaqs.map((faq) => {
            const isOpen = openId === faq.id;
            return (
              <div
                key={faq.id}
                className={`rounded-2xl border transition-all ${
                  isOpen ? 'bg-[#1F222A] border-saku-matcha/40 shadow-lg' : 'bg-[#15171C] border-saku-border hover:border-saku-border-highlight'
                }`}
              >
                <button
                  onClick={() => toggleAccordion(faq.id)}
                  className="w-full text-left p-5 sm:p-6 flex items-center justify-between gap-4 cursor-pointer"
                  aria-expanded={isOpen}
                >
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-mono font-bold text-saku-matcha px-2 py-0.5 rounded bg-saku-matcha/10 border border-saku-matcha/20 shrink-0">
                      {faq.category.split('&')[0].trim()}
                    </span>
                    <h3 className="text-base font-bold text-white tracking-tight">
                      {faq.question}
                    </h3>
                  </div>

                  <div className={`w-7 h-7 rounded-full bg-[#2C313D] flex items-center justify-center text-saku-text-secondary shrink-0 transition-transform duration-200 ${
                    isOpen ? 'rotate-180 text-white' : ''
                  }`}>
                    <ChevronDown className="w-4 h-4" />
                  </div>
                </button>

                {isOpen && (
                  <div className="px-5 pb-6 sm:px-6 text-sm text-saku-text-secondary leading-relaxed border-t border-saku-border/60 pt-4 animate-fadeIn">
                    <p>{faq.answer}</p>
                  </div>
                )}
              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
};
