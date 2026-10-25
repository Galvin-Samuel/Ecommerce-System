import React from 'react';
import { Laptop, Watch, Coffee, Home } from 'lucide-react';

interface CategoryGridProps {
  activeCategory: string;
  onSelectCategory: (category: string) => void;
}

export const CategoryGrid: React.FC<CategoryGridProps> = ({ activeCategory, onSelectCategory }) => {
  const categories = [
    {
      name: 'Electronics',
      label: 'Electronics',
      desc: 'High-performance audio, computing & gadgets',
      icon: Laptop,
      count: 'Audio & Tech',
    },
    {
      name: 'Accessories',
      label: 'Accessories',
      desc: 'Minimalist leather, timepieces & carry',
      icon: Watch,
      count: 'Everyday Gear',
    },
    {
      name: 'Lifestyle',
      label: 'Lifestyle',
      desc: 'Desk organization, artisanal items & wellness',
      icon: Coffee,
      count: 'Living & Work',
    },
    {
      name: 'Home Essentials',
      label: 'Home Essentials',
      desc: 'Modern lighting, ceramics & ceramics',
      icon: Home,
      count: 'Modern Space',
    },
  ];

  return (
    <section className="py-12 bg-slate-50 border-b border-slate-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
          <div>
            <span className="text-xs font-semibold text-brand-600 uppercase tracking-wider">Browse by Domain</span>
            <h2 className="text-2xl font-bold text-slate-900 tracking-tight mt-1">Featured Categories</h2>
          </div>

          {activeCategory && (
            <button
              onClick={() => onSelectCategory('')}
              className="text-xs font-medium text-slate-600 hover:text-slate-900 underline flex items-center gap-1 self-start sm:self-auto"
            >
              <span>Clear category filter ({activeCategory})</span>
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {categories.map((cat) => {
            const Icon = cat.icon;
            const isSelected = activeCategory === cat.name;

            return (
              <button
                key={cat.name}
                onClick={() => onSelectCategory(isSelected ? '' : cat.name)}
                className={`text-left p-6 rounded-2xl border transition-all duration-200 group relative ${
                  isSelected
                    ? 'bg-white border-brand-500 shadow-md ring-2 ring-brand-500/20'
                    : 'bg-white border-slate-200/80 hover:border-slate-300 hover:shadow-sm'
                }`}
              >
                <div className="flex items-center justify-between mb-4">
                  <div
                    className={`w-12 h-12 rounded-xl flex items-center justify-center transition-colors ${
                      isSelected
                        ? 'bg-brand-600 text-white'
                        : 'bg-slate-100 text-slate-700 group-hover:bg-brand-50 group-hover:text-brand-600'
                    }`}
                  >
                    <Icon className="w-6 h-6" />
                  </div>
                  <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                    {cat.count}
                  </span>
                </div>

                <h3 className="text-base font-bold text-slate-900 group-hover:text-brand-600 transition-colors">
                  {cat.label}
                </h3>
                <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">
                  {cat.desc}
                </p>

                {isSelected && (
                  <div className="absolute top-3 right-3 w-2 h-2 rounded-full bg-brand-600"></div>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </section>
  );
};
