import React from 'react';
import { X, Trash2, ShoppingBag, ArrowRight, ShieldCheck } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';

interface CartDrawerProps {
  onProceedToCheckout: () => void;
  onOpenAuth: () => void;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({
  onProceedToCheckout,
  onOpenAuth,
}) => {
  const { cart, isOpen, closeCart, updateQuantity, removeFromCart, isLoading, clearCart } =
    useCart();
  const { isAuthenticated } = useAuth();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden animate-in fade-in duration-200">
      {/* Backdrop */}
      <div
        onClick={closeCart}
        className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity"
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col border-l border-slate-100 animate-in slide-in-from-right duration-300">
          {/* Header */}
          <div className="p-6 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShoppingBag className="w-5 h-5 text-slate-900" />
              <h2 className="text-lg font-bold text-slate-900">Your Cart</h2>
              {cart && cart.totalItems > 0 && (
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700">
                  {cart.totalItems} {cart.totalItems === 1 ? 'item' : 'items'}
                </span>
              )}
            </div>

            <button
              onClick={closeCart}
              className="p-2 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
              aria-label="Close cart"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {!isAuthenticated ? (
              <div className="py-16 text-center">
                <div className="w-12 h-12 rounded-full bg-brand-50 text-brand-600 flex items-center justify-center mx-auto mb-3">
                  <ShoppingBag className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-slate-900">Sign in to view your cart</h3>
                <p className="text-xs text-slate-500 mt-1 mb-6 max-w-xs mx-auto">
                  Sign in or create an account to access your saved items and seamless checkout.
                </p>
                <button
                  onClick={() => {
                    closeCart();
                    onOpenAuth();
                  }}
                  className="px-6 py-2.5 rounded-full bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold shadow-sm transition-all"
                >
                  Sign In to Account
                </button>
              </div>
            ) : !cart || cart.items.length === 0 ? (
              <div className="py-20 text-center">
                <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-3">
                  <ShoppingBag className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-slate-900">Your cart is empty</h3>
                <p className="text-xs text-slate-500 mt-1 mb-6">
                  Explore our curated collection and add items to your cart.
                </p>
                <button
                  onClick={closeCart}
                  className="px-5 py-2.5 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold transition-all"
                >
                  Continue Shopping
                </button>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {cart.items.map((item) => (
                  <div key={item.id || item.productId} className="py-4 flex gap-4 first:pt-0 last:pb-0">
                    {/* Item Image */}
                    <div className="w-20 h-20 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-100 flex items-center justify-center">
                      <img
                        src={
                          item.productImageUrl ||
                          `https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=200&auto=format&fit=crop&q=80`
                        }
                        alt={item.productName}
                        className="w-full h-full object-cover object-center"
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = 'none';
                        }}
                      />
                    </div>

                    {/* Info & Quantity */}
                    <div className="flex-1 flex flex-col justify-between">
                      <div className="flex justify-between items-start">
                        <div>
                          <h4 className="text-sm font-semibold text-slate-900 line-clamp-1">
                            {item.productName}
                          </h4>
                          <span className="text-xs text-slate-500">
                            ${Number(item.unitPrice).toFixed(2)} each
                          </span>
                        </div>
                        <button
                          onClick={() => removeFromCart(item.productId)}
                          disabled={isLoading}
                          className="text-slate-400 hover:text-red-600 transition-colors p-1"
                          title="Remove item"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>

                      <div className="flex items-center justify-between mt-3">
                        <div className="flex items-center border border-slate-200 rounded-lg overflow-hidden bg-slate-50">
                          <button
                            onClick={() => {
                              if (item.quantity > 1) {
                                updateQuantity(item.productId, item.quantity - 1);
                              } else {
                                removeFromCart(item.productId);
                              }
                            }}
                            disabled={isLoading}
                            className="px-2.5 py-0.5 text-slate-600 hover:bg-slate-200 text-xs font-semibold disabled:opacity-40"
                          >
                            -
                          </button>
                          <span className="px-2.5 py-0.5 text-xs font-bold text-slate-800 min-w-[1.5rem] text-center">
                            {item.quantity}
                          </span>
                          <button
                            onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                            disabled={isLoading}
                            className="px-2.5 py-0.5 text-slate-600 hover:bg-slate-200 text-xs font-semibold disabled:opacity-40"
                          >
                            +
                          </button>
                        </div>

                        <span className="text-sm font-bold text-slate-900">
                          ${Number(item.subtotal || item.unitPrice * item.quantity).toFixed(2)}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer / Summary */}
          {isAuthenticated && cart && cart.items.length > 0 && (
            <div className="p-6 bg-slate-50 border-t border-slate-100 space-y-4">
              <div className="space-y-1.5 text-xs">
                <div className="flex justify-between text-slate-500">
                  <span>Subtotal ({cart.totalItems} items)</span>
                  <span className="font-semibold text-slate-800">
                    ${Number(cart.totalPrice).toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-500">
                  <span>Standard Shipping</span>
                  <span className="font-medium text-emerald-600">Complimentary</span>
                </div>
                <div className="border-t border-slate-200 pt-2 flex justify-between text-sm font-bold text-slate-900">
                  <span>Estimated Total</span>
                  <span className="text-base text-slate-900">
                    ${Number(cart.totalPrice).toFixed(2)}
                  </span>
                </div>
              </div>

              <div className="flex flex-col gap-2 pt-1">
                <button
                  onClick={() => {
                    closeCart();
                    onProceedToCheckout();
                  }}
                  disabled={isLoading}
                  className="w-full py-3.5 px-4 rounded-full bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold flex items-center justify-center gap-2 shadow-sm transition-all"
                >
                  <span>Proceed to Checkout</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>

                <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1">
                  <span className="flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                    Encrypted Snapshot Checkout
                  </span>
                  <button
                    onClick={clearCart}
                    disabled={isLoading}
                    className="hover:text-red-600 transition-colors"
                  >
                    Clear cart
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

