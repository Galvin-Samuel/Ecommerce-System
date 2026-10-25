import React from 'react';
import { Product } from '../../types/product';
import { ProductCard } from './ProductCard';
import { RefreshCw, PackageX, ChevronLeft, ChevronRight, SlidersHorizontal } from 'lucide-react';

interface ProductGridProps {
  products: Product[];
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
  onViewDetails: (product: Product) => void;
  onRequireAuth: () => void;
  currentPage: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (newPage: number) => void;
  sortOption: string;
  onSortChange: (newSort: string) => void;
  activeCategory: string;
  searchQuery: string;
}

export const ProductGrid: React.FC<ProductGridProps> = ({
  products,
  isLoading,
  error,
  onRetry,
  onViewDetails,
  onRequireAuth,
  currentPage,
  totalPages,
  totalElements,
  onPageChange,
  sortOption,
  onSortChange,
  activeCategory,
  searchQuery,
}) => {
  return (
    <section id="products" className="py-12 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      {/* Grid Controls Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
            {searchQuery
              ? `Results for "${searchQuery}"`
              : activeCategory
              ? `${activeCategory} Collection`
              : 'All Products'}
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            {isLoading ? 'Checking catalog...' : `Showing ${products.length} of ${totalElements} available items`}
          </p>
        </div>

        {/* Sorting Dropdown */}
        <div className="flex items-center gap-2 self-start sm:self-auto">
          <SlidersHorizontal className="w-4 h-4 text-slate-400" />
          <span className="text-xs text-slate-500 font-medium">Sort by:</span>
          <select
            value={sortOption}
            onChange={(e) => onSortChange(e.target.value)}
            className="text-xs font-semibold text-slate-800 bg-white border border-slate-200 rounded-lg px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 cursor-pointer"
          >
            <option value="createdAt,desc">Newest Arrivals</option>
            <option value="price,asc">Price: Low to High</option>
            <option value="price,desc">Price: High to Low</option>
            <option value="name,asc">Name: A to Z</option>
          </select>
        </div>
      </div>

      {/* Loading Skeleton State */}
      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 pt-8">
          {[...Array(8)].map((_, index) => (
            <div
              key={index}
              className="bg-white rounded-2xl border border-slate-100 p-4 animate-pulse flex flex-col gap-4"
            >
              <div className="aspect-square bg-slate-200 rounded-xl w-full"></div>
              <div className="h-4 bg-slate-200 rounded w-3/4"></div>
              <div className="h-3 bg-slate-100 rounded w-full"></div>
              <div className="flex items-center justify-between pt-2">
                <div className="h-5 bg-slate-200 rounded w-1/4"></div>
                <div className="h-8 bg-slate-200 rounded-full w-16"></div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Error State */}
      {!isLoading && error && (
        <div className="py-16 text-center max-w-md mx-auto">
          <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-3">
            <RefreshCw className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-slate-900">Failed to load catalog products</h3>
          <p className="text-xs text-slate-500 mt-1 mb-5">{error}</p>
          <button
            onClick={onRetry}
            className="px-5 py-2 rounded-full bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold shadow-sm transition-all inline-flex items-center gap-1.5"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Retry Connection</span>
          </button>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && products.length === 0 && (
        <div className="py-20 text-center max-w-md mx-auto">
          <div className="w-14 h-14 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-4">
            <PackageX className="w-7 h-7" />
          </div>
          <h3 className="text-base font-bold text-slate-900">No products found</h3>
          <p className="text-xs text-slate-500 mt-1 mb-6 leading-relaxed">
            {searchQuery
              ? `No matches for "${searchQuery}". Try adjusting your keywords or category filters.`
              : 'Our product catalog is currently being updated. Please check back soon.'}
          </p>
          {searchQuery && (
            <button
              onClick={() => onPageChange(0)}
              className="px-4 py-2 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold transition-all"
            >
              Clear Search Query
            </button>
          )}
        </div>
      )}

      {/* Product Grid */}
      {!isLoading && !error && products.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 pt-8">
            {products.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onViewDetails={onViewDetails}
                onRequireAuth={onRequireAuth}
              />
            ))}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="mt-12 flex items-center justify-center gap-3">
              <button
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="p-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                aria-label="Previous Page"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              <span className="text-xs font-semibold text-slate-700 px-3 py-1.5 bg-slate-100 rounded-lg">
                Page {currentPage + 1} of {totalPages}
              </span>

              <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="p-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                aria-label="Next Page"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
};

