import React, { useState, useEffect, useCallback } from 'react';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { Navbar } from './components/layout/Navbar';
import { HeroSection } from './components/home/HeroSection';
import { CategoryGrid } from './components/home/CategoryGrid';
import { ProductGrid } from './components/product/ProductGrid';
import { ProductDetailsModal } from './components/product/ProductDetailsModal';
import { CartDrawer } from './components/cart/CartDrawer';
import { AuthModal } from './components/auth/AuthModal';
import { UserProfileModal } from './components/auth/UserProfileModal';
import { CheckoutModal } from './components/order/CheckoutModal';
import { OrderConfirmationModal } from './components/order/OrderConfirmationModal';
import { OrderHistoryModal } from './components/order/OrderHistoryModal';
import { Footer } from './components/layout/Footer';
import { Product } from './types/product';
import { Order } from './types/order';
import { productApi } from './api/productApi';

const MainStorefront: React.FC = () => {
  // Navigation & Filter State
  const [activeCategory, setActiveCategory] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [sortOption, setSortOption] = useState<string>('createdAt,desc');
  const [currentPage, setCurrentPage] = useState<number>(0);

  // Products Data State
  const [products, setProducts] = useState<Product[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Modals & Panels State
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState<boolean>(false);
  const [authModalMode, setAuthModalMode] = useState<'login' | 'register'>('login');
  const [profileModalOpen, setProfileModalOpen] = useState<boolean>(false);
  const [checkoutModalOpen, setCheckoutModalOpen] = useState<boolean>(false);
  const [confirmationOrder, setConfirmationOrder] = useState<Order | null>(null);
  const [orderHistoryOpen, setOrderHistoryOpen] = useState<boolean>(false);

  // Fetch Products from real backend API
  const loadProducts = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      if (searchQuery.trim()) {
        const response = await productApi.searchProducts({
          q: searchQuery.trim(),
          category: activeCategory || undefined,
          page: currentPage,
          size: 12,
          sort: sortOption,
        });
        setProducts(response.items || []);
        setTotalElements(response.totalElements || 0);
        setTotalPages(response.totalPages || 0);
      } else {
        const response = await productApi.getProducts({
          category: activeCategory || undefined,
          page: currentPage,
          size: 12,
          sort: sortOption,
        });
        setProducts(response.items || []);
        setTotalElements(response.totalElements || 0);
        setTotalPages(response.totalPages || 0);
      }
    } catch (err: any) {
      setError(err?.message || 'Unable to connect to backend product service.');
      setProducts([]);
    } finally {
      setIsLoading(false);
    }
  }, [searchQuery, activeCategory, currentPage, sortOption]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  // Handlers
  const handleCategorySelect = (category: string) => {
    setActiveCategory(category);
    setCurrentPage(0);
    const element = document.getElementById('products');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    setCurrentPage(0);
    const element = document.getElementById('products');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleShopNow = () => {
    const element = document.getElementById('products');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleRequireAuth = () => {
    setAuthModalMode('login');
    setAuthModalOpen(true);
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900 font-sans">
      {/* Navigation Bar */}
      <Navbar
        onSearch={handleSearch}
        onSelectCategory={handleCategorySelect}
        onOpenAuth={() => {
          setAuthModalMode('login');
          setAuthModalOpen(true);
        }}
        onOpenProfile={() => setProfileModalOpen(true)}
        onOpenOrders={() => setOrderHistoryOpen(true)}
        onNavigateHome={() => {
          setActiveCategory('');
          setSearchQuery('');
          setCurrentPage(0);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }}
        activeCategory={activeCategory}
      />

      <main className="flex-1">
        {/* Hero Section */}
        <HeroSection onShopClick={handleShopNow} onExploreClick={handleShopNow} />

        {/* Featured Categories */}
        <CategoryGrid
          activeCategory={activeCategory}
          onSelectCategory={handleCategorySelect}
        />

        {/* Product Catalog Grid */}
        <ProductGrid
          products={products}
          isLoading={isLoading}
          error={error}
          onRetry={loadProducts}
          onViewDetails={(prod) => setSelectedProduct(prod)}
          onRequireAuth={handleRequireAuth}
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={(page) => setCurrentPage(page)}
          sortOption={sortOption}
          onSortChange={(sort) => {
            setSortOption(sort);
            setCurrentPage(0);
          }}
          activeCategory={activeCategory}
          searchQuery={searchQuery}
        />
      </main>

      {/* Footer */}
      <Footer />

      {/* Slide-over Cart Drawer */}
      <CartDrawer
        onProceedToCheckout={() => setCheckoutModalOpen(true)}
        onOpenAuth={handleRequireAuth}
      />

      {/* Product Details Modal with Share Feature */}
      <ProductDetailsModal
        product={selectedProduct}
        onClose={() => setSelectedProduct(null)}
        onRequireAuth={handleRequireAuth}
      />

      {/* Authentication Modal (Login / Register) */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        initialMode={authModalMode}
      />

      {/* User Profile Modal */}
      <UserProfileModal
        isOpen={profileModalOpen}
        onClose={() => setProfileModalOpen(false)}
        onOpenOrders={() => setOrderHistoryOpen(true)}
      />

      {/* Checkout Modal */}
      <CheckoutModal
        isOpen={checkoutModalOpen}
        onClose={() => setCheckoutModalOpen(false)}
        onOrderSuccess={(order) => setConfirmationOrder(order)}
      />

      {/* Order Confirmation Modal */}
      <OrderConfirmationModal
        order={confirmationOrder}
        onClose={() => setConfirmationOrder(null)}
        onViewOrders={() => {
          setConfirmationOrder(null);
          setOrderHistoryOpen(true);
        }}
      />

      {/* Order History Modal */}
      <OrderHistoryModal
        isOpen={orderHistoryOpen}
        onClose={() => setOrderHistoryOpen(false)}
      />
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <CartProvider>
        <MainStorefront />
      </CartProvider>
    </AuthProvider>
  );
};

export default App;

