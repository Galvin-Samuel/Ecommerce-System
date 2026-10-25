import React, { useState } from 'react';
import { X, Check, ShieldCheck, AlertCircle, Loader2 } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { orderApi } from '../../api/orderApi';
import { Order } from '../../types/order';

interface CheckoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  onOrderSuccess: (order: Order) => void;
}

export const CheckoutModal: React.FC<CheckoutModalProps> = ({
  isOpen,
  onClose,
  onOrderSuccess,
}) => {
  const { cart, refreshCart } = useCart();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !cart) return null;

  const handlePlaceOrder = async () => {
    setError(null);
    setLoading(true);

    try {
      // Backend snapshots the user's cart into an immutable order document
      const order = await orderApi.createOrder();
      await refreshCart();
      onClose();
      onOrderSuccess(order);
    } catch (err: any) {
      setError(err?.message || 'Failed to place order. Please review your cart.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-lg w-full shadow-2xl border border-slate-100 overflow-hidden relative p-8">
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={loading}
          className="absolute top-4 right-4 p-2 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
          aria-label="Close checkout"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="mb-6">
          <span className="text-xs font-semibold text-brand-600 uppercase tracking-wider">
            Review & Finalize
          </span>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight mt-1">
            Order Confirmation
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Snapshotting current items directly into your customer order record.
          </p>
        </div>

        {error && (
          <div className="mb-5 p-3 rounded-xl bg-red-50 border border-red-100 text-red-700 text-xs flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {/* Item Summary List */}
        <div className="max-h-60 overflow-y-auto divide-y divide-slate-100 border border-slate-100 rounded-2xl p-4 bg-slate-50/50 mb-6">
          {cart.items.map((item) => (
            <div key={item.id || item.productId} className="py-2.5 flex justify-between items-center text-xs first:pt-0 last:pb-0">
              <div className="pr-4">
                <p className="font-semibold text-slate-900 line-clamp-1">{item.productName}</p>
                <p className="text-slate-500 text-[11px]">
                  Qty: {item.quantity} × ${Number(item.unitPrice).toFixed(2)}
                </p>
              </div>
              <span className="font-bold text-slate-900 shrink-0">
                ${Number(item.subtotal || item.unitPrice * item.quantity).toFixed(2)}
              </span>
            </div>
          ))}
        </div>

        {/* Totals Breakdown */}
        <div className="space-y-2 border-t border-slate-100 pt-4 text-xs mb-6">
          <div className="flex justify-between text-slate-600">
            <span>Subtotal</span>
            <span className="font-semibold text-slate-900">${Number(cart.totalPrice).toFixed(2)}</span>
          </div>
          <div className="flex justify-between text-slate-600">
            <span>Standard Shipping</span>
            <span className="font-semibold text-emerald-600">Free</span>
          </div>
          <div className="border-t border-slate-100 pt-2 flex justify-between text-sm font-bold text-slate-900">
            <span>Total Payable</span>
            <span className="text-base text-slate-900">${Number(cart.totalPrice).toFixed(2)}</span>
          </div>
        </div>

        {/* Confirmation Button */}
        <button
          onClick={handlePlaceOrder}
          disabled={loading || cart.items.length === 0}
          className="w-full py-3.5 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold flex items-center justify-center gap-2 shadow-sm transition-all disabled:opacity-60"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Placing Order with Backend...</span>
            </>
          ) : (
            <>
              <Check className="w-4 h-4" />
              <span>Place Order • ${Number(cart.totalPrice).toFixed(2)}</span>
            </>
          )}
        </button>

        <p className="mt-3 text-[11px] text-center text-slate-400 flex items-center justify-center gap-1">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
          Transactional cart-to-order persistence
        </p>
      </div>
    </div>
  );
};

