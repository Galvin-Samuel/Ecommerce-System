import React, { useState } from 'react';
import { ShoppingBag, User as UserIcon, Search, Menu, X, ArrowRight } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';

interface NavbarProps {
  onSearch: (query: string) => void;
  onSelectCategory: (category: string) => void;
  onOpenAuth: () => void;
  onOpenProfile: () => void;
  onOpenOrders: () => void;
  onNavigateHome: () => void;
  activeCategory: string;
}

export const Navbar: React.FC<NavbarProps> = ({
  onSearch,
  onSelectCategory,
  onOpenAuth,
  onOpenProfile,
  onOpenOrders,
  onNavigateHome,
  activeCategory,
}) => {
  const { user, isAuthenticated } = useAuth();
  const { totalItems, openCart } = useCart();
  const [searchQuery, setSearchQuery] = useState('');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch(searchQuery.trim());
  };

  const categories = ['All', 'Electronics', 'Accessories', 'Lifestyle', 'Home Essentials'];

  return (
    <header className="sticky top-0 z-40 bg-white/90 backdrop-blur-md border-b border-slate-100 transition-all">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">
          {/* Brand Logo */}
          <div className="flex items-center gap-8">
            <button
              onClick={onNavigateHome}
              className="flex items-center gap-2 text-left group focus:outline-none"
            >
              <div className="w-8 h-8 rounded-lg bg-slate-900 flex items-center justify-center text-white font-bold tracking-tighter text-lg shadow-sm group-hover:bg-brand-600 transition-colors">
                N
              </div>
              <span className="text-xl font-bold tracking-tight text-slate-900">
                NOVA
              </span>
            </button>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-1">
              {categories.map((cat) => {
                const isSelected = (cat === 'All' && !activeCategory) || activeCategory === cat;
                return (
                  <button
                    key={cat}
                    onClick={() => onSelectCategory(cat === 'All' ? '' : cat)}
                    className={`px-3 py-1.5 text-sm font-medium rounded-full transition-colors ${
                      isSelected
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                    }`}
                  >
                    {cat}
                  </button>
                );
              })}
            </nav>
          </div>

          {/* Search Bar - Desktop */}
          <div className="hidden lg:flex items-center flex-1 max-w-xs mx-6">
            <form onSubmit={handleSearchSubmit} className="relative w-full">
              <input
                type="text"
                placeholder="Search products..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2 text-sm bg-slate-100 border border-transparent rounded-full focus:bg-white focus:border-slate-300 focus:outline-none focus:ring-2 focus:ring-brand-500/20 text-slate-900 placeholder-slate-400 transition-all"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            </form>
          </div>

          {/* Actions: Account & Cart */}
          <div className="flex items-center gap-3">
            {/* Account Button */}
            {isAuthenticated ? (
              <button
                onClick={onOpenProfile}
                className="flex items-center gap-2 py-1.5 px-3 rounded-full border border-slate-200 hover:border-slate-300 text-slate-700 hover:text-slate-900 bg-white text-sm font-medium transition-all"
                title="Account Settings"
              >
                <div className="w-6 h-6 rounded-full bg-brand-100 text-brand-700 flex items-center justify-center text-xs font-semibold">
                  {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <span className="hidden sm:inline max-w-[100px] truncate">{user?.name}</span>
              </button>
            ) : (
              <button
                onClick={onOpenAuth}
                className="flex items-center gap-1.5 py-1.5 px-4 rounded-full bg-slate-900 hover:bg-slate-800 text-white text-sm font-medium transition-all shadow-sm"
              >
                <UserIcon className="w-4 h-4" />
                <span>Sign In</span>
              </button>
            )}

            {/* Shopping Cart Button */}
            <button
              onClick={openCart}
              className="relative p-2 rounded-full text-slate-700 hover:text-slate-900 hover:bg-slate-100 transition-all focus:outline-none"
              aria-label="Shopping Cart"
            >
              <ShoppingBag className="w-6 h-6" />
              {totalItems > 0 && (
                <span className="absolute -top-1 -right-1 bg-brand-600 text-white text-[11px] font-bold w-5 h-5 rounded-full flex items-center justify-center shadow-sm animate-in zoom-in">
                  {totalItems > 99 ? '99+' : totalItems}
                </span>
              )}
            </button>

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-2 text-slate-600 hover:text-slate-900 focus:outline-none"
              aria-label="Toggle Navigation"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {/* Mobile Search & Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden py-4 border-t border-slate-100 animate-in slide-in-from-top duration-200">
            <form onSubmit={handleSearchSubmit} className="relative mb-3">
              <input
                type="text"
                placeholder="Search products..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2 text-sm bg-slate-100 rounded-full focus:bg-white focus:ring-2 focus:ring-brand-500/20 text-slate-900 placeholder-slate-400"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            </form>

            <div className="flex flex-col gap-1 pb-2">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider px-2 py-1">
                Categories
              </span>
              {categories.map((cat) => (
                <button
                  key={cat}
                  onClick={() => {
                    onSelectCategory(cat === 'All' ? '' : cat);
                    setMobileMenuOpen(false);
                  }}
                  className={`text-left px-3 py-2 text-sm rounded-lg font-medium transition-colors ${
                    (cat === 'All' && !activeCategory) || activeCategory === cat
                      ? 'bg-slate-900 text-white'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>

            {isAuthenticated && (
              <div className="pt-2 border-t border-slate-100 flex flex-col gap-1">
                <button
                  onClick={() => {
                    onOpenOrders();
                    setMobileMenuOpen(false);
                  }}
                  className="flex items-center justify-between px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 rounded-lg"
                >
                  <span>My Orders</span>
                  <ArrowRight className="w-4 h-4 text-slate-400" />
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  );
};

