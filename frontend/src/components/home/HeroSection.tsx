import React from 'react';
import { ArrowDown, Sparkles } from 'lucide-react';

interface HeroSectionProps {
  onShopClick: () => void;
  onExploreClick: () => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({ onShopClick, onExploreClick }) => {
  return (
    <section className="relative overflow-hidden bg-white border-b border-slate-100 py-12 lg:py-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
          {/* Hero Copy */}
          <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-50 border border-brand-100 text-brand-700 text-xs font-semibold tracking-wide uppercase">
              <Sparkles className="w-3.5 h-3.5 text-brand-600" />
              <span>Curated Selection • 2026 Edition</span>
            </div>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-slate-900 tracking-tight leading-[1.1]">
              Find Your Next <br />
              <span className="text-brand-600">Favorite.</span>
            </h1>

            <p className="text-base sm:text-lg text-slate-600 max-w-xl mx-auto lg:mx-0 font-normal leading-relaxed">
              Discover everyday essentials, thoughtfully selected for you. From high-performance electronics to minimalist workspace accessories.
            </p>

            <div className="flex flex-wrap items-center justify-center lg:justify-start gap-3.5 pt-2">
              <button
                onClick={onShopClick}
                className="px-6 py-3 rounded-full bg-slate-900 hover:bg-slate-800 text-white text-sm font-semibold shadow-sm hover:shadow transition-all flex items-center gap-2"
              >
                <span>Shop Collection</span>
                <ArrowDown className="w-4 h-4" />
              </button>

              <button
                onClick={onExploreClick}
                className="px-6 py-3 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-800 text-sm font-semibold transition-all"
              >
                Explore Products
              </button>
            </div>
          </div>

          {/* Hero Visual Card (Apple-style sleek showcase) */}
          <div className="lg:col-span-5 relative">
            <div className="relative mx-auto max-w-md rounded-2xl bg-gradient-to-tr from-slate-100 to-slate-50 p-6 border border-slate-200/80 shadow-sm">
              <div className="aspect-[4/3] rounded-xl overflow-hidden bg-white shadow-inner flex items-center justify-center relative">
                <img
                  src="https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80"
                  alt="Premium Wireless Headphones"
                  className="w-full h-full object-cover object-center transform hover:scale-105 transition-transform duration-500"
                  onError={(e) => {
                    // Fallback to minimal SVG representation if image fails to load
                    (e.target as HTMLElement).style.display = 'none';
                  }}
                />
              </div>

              {/* Floating Product Tag */}
              <div className="mt-4 flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-brand-600 uppercase tracking-wider">Featured Today</span>
                  <h3 className="text-sm font-bold text-slate-900">Acoustic Studio Wireless</h3>
                </div>
                <div className="text-right">
                  <span className="text-xs text-slate-400 line-through mr-1.5">$299</span>
                  <span className="text-sm font-bold text-slate-900">$249.00</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

