import React, { useState } from 'react';
import { Product } from '../../types/product';
import { X, ShoppingBag, Share2, Copy, Check, CheckCircle2, Shield, Truck, AlertCircle } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { shortUrlApi } from '../../api/shortUrlApi';

interface ProductDetailsModalProps {
  product: Product | null;
  onClose: () => void;
  onRequireAuth: () => void;
}

export const ProductDetailsModal: React.FC<ProductDetailsModalProps> = ({
  product,
  onClose,
  onRequireAuth,
}) => {
  const { addToCart, isLoading } = useCart();
  const { isAuthenticated } = useAuth();
  const [quantity, setQuantity] = useState(1);
  const [isAdding, setIsAdding] = useState(false);
  const [addedSuccess, setAddedSuccess] = useState(false);
  const [shareLoading, setShareLoading] = useState(false);
  const [shortUrl, setShortUrl] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [imageError, setImageError] = useState(false);

  if (!product) return null;

  const isOutOfStock = product.stockQuantity <= 0;
  const isLowStock = product.stockQuantity > 0 && product.stockQuantity <= 5;

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      onClose();
      onRequireAuth();
      return;
    }

    setIsAdding(true);
    try {
      await addToCart(product.id, quantity);
      setAddedSuccess(true);
      setTimeout(() => setAddedSuccess(false), 2000);
    } catch {
      // handled
    } finally {
      setIsAdding(false);
    }
  };

  const handleShare = async () => {
    setShareLoading(true);
    try {
      // Generate short URL via backend POST /api/v1/short-urls
      const currentUrl = `${window.location.origin}/#product-${product.id}`;
      const response = await shortUrlApi.createShortUrl(currentUrl);
      setShortUrl(response.shortUrl);
    } catch {
      // Fallback to direct URL if shortener endpoint is unavailable or errors
      setShortUrl(`${window.location.origin}/#product-${product.id}`);
    } finally {
      setShareLoading(false);
    }
  };

  const handleCopyLink = () => {
    if (shortUrl) {
      navigator.clipboard.writeText(shortUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const displayImage =
    !imageError && product.imageUrl
      ? product.imageUrl
      : `https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800&auto=format&fit=crop&q=80`;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-3xl w-full shadow-2xl border border-slate-100 overflow-hidden relative">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 p-2 rounded-full bg-white/80 hover:bg-slate-100 text-slate-500 hover:text-slate-800 transition-colors"
          aria-label="Close details"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-0">
          {/* Image Pane */}
          <div className="bg-slate-50 p-8 flex items-center justify-center relative border-b md:border-b-0 md:border-r border-slate-100">
            <div className="aspect-square w-full max-w-sm rounded-2xl overflow-hidden bg-white shadow-sm flex items-center justify-center">
              <img
                src={displayImage}
                alt={product.name}
                onError={() => setImageError(true)}
                className="w-full h-full object-cover object-center"
              />
            </div>
          </div>

          {/* Details Pane */}
          <div className="p-8 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold uppercase tracking-wider bg-brand-50 text-brand-700">
                  {product.category || 'General'}
                </span>
                {isOutOfStock ? (
                  <span className="text-[11px] font-bold text-red-600 uppercase tracking-wider flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" /> Out of Stock
                  </span>
                ) : isLowStock ? (
                  <span className="text-[11px] font-bold text-amber-600 uppercase tracking-wider">
                    Only {product.stockQuantity} Remaining
                  </span>
                ) : (
                  <span className="text-[11px] font-medium text-emerald-600 flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" /> In Stock ({product.stockQuantity})
                  </span>
                )}
              </div>

              <h2 className="text-2xl font-bold text-slate-900 tracking-tight leading-snug">
                {product.name}
              </h2>

              <div className="mt-3 flex items-baseline gap-2">
                <span className="text-3xl font-extrabold text-slate-900">
                  ${Number(product.price).toFixed(2)}
                </span>
                <span className="text-xs text-slate-400">USD, taxes calculated at checkout</span>
              </div>

              <div className="mt-5 pt-5 border-t border-slate-100">
                <h4 className="text-xs font-semibold text-slate-900 uppercase tracking-wider mb-2">
                  Description
                </h4>
                <p className="text-xs text-slate-600 leading-relaxed">
                  {product.description ||
                    'Premium everyday essential crafted for reliability, aesthetics, and lasting utility.'}
                </p>
              </div>

              {/* Quantity Selector */}
              {!isOutOfStock && (
                <div className="mt-6 flex items-center gap-4">
                  <span className="text-xs font-semibold text-slate-700">Quantity:</span>
                  <div className="flex items-center border border-slate-200 rounded-lg overflow-hidden bg-slate-50">
                    <button
                      onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                      disabled={quantity <= 1}
                      className="px-3 py-1 text-slate-600 hover:bg-slate-200 text-sm font-semibold disabled:opacity-30"
                    >
                      -
                    </button>
                    <span className="px-3 py-1 text-xs font-bold text-slate-800 min-w-[2rem] text-center">
                      {quantity}
                    </span>
                    <button
                      onClick={() => setQuantity((q) => Math.min(product.stockQuantity, q + 1))}
                      disabled={quantity >= product.stockQuantity}
                      className="px-3 py-1 text-slate-600 hover:bg-slate-200 text-sm font-semibold disabled:opacity-30"
                    >
                      +
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Actions: Add to Cart & Share */}
            <div className="mt-8 space-y-3">
              <button
                onClick={handleAddToCart}
                disabled={isOutOfStock || isAdding || isLoading}
                className={`w-full py-3.5 px-6 rounded-full text-sm font-semibold flex items-center justify-center gap-2 shadow-sm transition-all ${
                  addedSuccess
                    ? 'bg-emerald-600 text-white'
                    : isOutOfStock
                    ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                    : 'bg-slate-900 hover:bg-slate-800 text-white'
                }`}
              >
                {addedSuccess ? (
                  <>
                    <Check className="w-4 h-4" />
                    <span>Added to Cart!</span>
                  </>
                ) : (
                  <>
                    <ShoppingBag className="w-4 h-4" />
                    <span>{isOutOfStock ? 'Currently Unavailable' : `Add to Cart • $${(Number(product.price) * quantity).toFixed(2)}`}</span>
                  </>
                )}
              </button>

              {/* Share via URL Shortener */}
              <div className="pt-2">
                {!shortUrl ? (
                  <button
                    onClick={handleShare}
                    disabled={shareLoading}
                    className="w-full py-2.5 px-4 rounded-full border border-slate-200 hover:border-slate-300 text-slate-700 text-xs font-semibold flex items-center justify-center gap-2 transition-colors bg-white"
                  >
                    <Share2 className="w-3.5 h-3.5 text-slate-500" />
                    <span>{shareLoading ? 'Generating Share Link...' : 'Share Product Link'}</span>
                  </button>
                ) : (
                  <div className="flex items-center gap-2 p-2 bg-slate-50 rounded-xl border border-slate-200 text-xs">
                    <input
                      type="text"
                      readOnly
                      value={shortUrl}
                      className="bg-transparent flex-1 text-slate-600 font-mono text-[11px] outline-none px-1"
                    />
                    <button
                      onClick={handleCopyLink}
                      className="px-3 py-1 bg-brand-600 hover:bg-brand-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1 transition-colors"
                    >
                      {copied ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                      <span>{copied ? 'Copied' : 'Copy'}</span>
                    </button>
                  </div>
                )}
              </div>

              {/* Trust Badges */}
              <div className="pt-4 flex items-center justify-between text-[11px] text-slate-400 border-t border-slate-100">
                <span className="flex items-center gap-1">
                  <Truck className="w-3.5 h-3.5" /> Express shipping
                </span>
                <span className="flex items-center gap-1">
                  <Shield className="w-3.5 h-3.5" /> Direct warranty
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

