import React from 'react';
import { ShieldCheck, Truck, RotateCcw, Heart } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-slate-200 mt-20">
      {/* Features Bar */}
      <div className="border-b border-slate-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-center sm:text-left">
            <div className="flex items-center gap-4 justify-center sm:justify-start">
              <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center shrink-0">
                <Truck className="w-6 h-6" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900">Complimentary Shipping</h4>
                <p className="text-xs text-slate-500 mt-0.5">Prompt delivery on all orders nationwide.</p>
              </div>
            </div>

            <div className="flex items-center gap-4 justify-center sm:justify-start">
              <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center shrink-0">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900">Authentic Guarantee</h4>
                <p className="text-xs text-slate-500 mt-0.5">Verified products directly from certified sellers.</p>
              </div>
            </div>

            <div className="flex items-center gap-4 justify-center sm:justify-start">
              <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center shrink-0">
                <RotateCcw className="w-6 h-6" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900">Effortless Returns</h4>
                <p className="text-xs text-slate-500 mt-0.5">30-day seamless satisfaction guarantee.</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links & Architecture Meta */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div className="col-span-2 md:col-span-1">
            <div className="flex items-center gap-2 mb-3">
              <div className="w-6 h-6 rounded-md bg-slate-900 text-white flex items-center justify-center text-xs font-bold">
                N
              </div>
              <span className="text-base font-bold tracking-tight text-slate-900">NOVA</span>
            </div>
            <p className="text-xs text-slate-500 leading-relaxed">
              Engineered with clean architectural patterns. Powered by Java 21, Spring Boot, MongoDB, and Redis distributed caching.
            </p>
          </div>

          <div>
            <h5 className="text-xs font-semibold text-slate-900 uppercase tracking-wider mb-3">Explore</h5>
            <ul className="space-y-2 text-xs text-slate-600">
              <li><a href="#products" className="hover:text-slate-900 transition-colors">Featured Electronics</a></li>
              <li><a href="#products" className="hover:text-slate-900 transition-colors">Everyday Accessories</a></li>
              <li><a href="#products" className="hover:text-slate-900 transition-colors">Lifestyle Essentials</a></li>
            </ul>
          </div>

          <div>
            <h5 className="text-xs font-semibold text-slate-900 uppercase tracking-wider mb-3">System Design</h5>
            <ul className="space-y-2 text-xs text-slate-600">
              <li><span className="text-emerald-600 font-medium">● Backend API (Port 8080)</span></li>
              <li><span>MongoDB 7.0 Document Store</span></li>
              <li><span>Redis 7.4 Sliding Rate Limiter</span></li>
              <li><span>Snowflake 64-bit ID Generation</span></li>
            </ul>
          </div>

          <div>
            <h5 className="text-xs font-semibold text-slate-900 uppercase tracking-wider mb-3">Project Demo</h5>
            <p className="text-xs text-slate-500 leading-relaxed mb-3">
              Scalable E-Commerce System Capstone Project.
            </p>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 text-slate-600 text-[11px] font-medium">
              <span>Production-Ready Clean UI</span>
            </div>
          </div>
        </div>

        <div className="border-t border-slate-100 mt-10 pt-6 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-400 gap-4">
          <p>© {new Date().getFullYear()} NOVA Storefront. All rights reserved.</p>
          <p className="flex items-center gap-1">
            Built with <Heart className="w-3.5 h-3.5 text-brand-500 fill-brand-500" /> for Scalable System Design
          </p>
        </div>
      </div>
    </footer>
  );
};

