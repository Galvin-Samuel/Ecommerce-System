import React, { useEffect, useState } from 'react';
import { X, Package, Calendar, RefreshCw } from 'lucide-react';
import { Order } from '../../types/order';
import { orderApi } from '../../api/orderApi';

interface OrderHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const OrderHistoryModal: React.FC<OrderHistoryModalProps> = ({ isOpen, onClose }) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await orderApi.getOrders();
      setOrders(data || []);
    } catch (err: any) {
      setError(err?.message || 'Failed to retrieve order history');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchOrders();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-2xl w-full shadow-2xl border border-slate-100 overflow-hidden relative flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Package className="w-5 h-5 text-slate-900" />
            <h2 className="text-lg font-bold text-slate-900">Order History</h2>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {loading && (
            <div className="py-16 text-center text-slate-400 text-xs flex flex-col items-center gap-2">
              <RefreshCw className="w-6 h-6 animate-spin text-brand-500" />
              <span>Retrieving your orders...</span>
            </div>
          )}

          {!loading && error && (
            <div className="py-12 text-center">
              <p className="text-xs text-red-600 mb-3">{error}</p>
              <button
                onClick={fetchOrders}
                className="px-4 py-2 rounded-full bg-slate-900 text-white text-xs font-semibold"
              >
                Retry
              </button>
            </div>
          )}

          {!loading && !error && orders.length === 0 && (
            <div className="py-16 text-center text-slate-500">
              <Package className="w-10 h-10 text-slate-300 mx-auto mb-2" />
              <p className="text-sm font-semibold text-slate-800">No orders yet</p>
              <p className="text-xs text-slate-400 mt-1">When you place orders, they will appear here.</p>
            </div>
          )}

          {!loading && !error && orders.length > 0 && (
            <div className="space-y-4">
              {orders.map((ord) => {
                const dateStr = ord.createdAt
                  ? new Date(ord.createdAt).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    })
                  : 'N/A';

                return (
                  <div
                    key={ord.id}
                    className="p-5 rounded-2xl border border-slate-200/80 hover:border-slate-300 bg-white shadow-xs transition-all"
                  >
                    <div className="flex flex-wrap items-center justify-between pb-3 border-b border-slate-100 gap-2">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-slate-900">
                            #{ord.id}
                          </span>
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-50 text-emerald-700">
                            {ord.status || 'PENDING'}
                          </span>
                        </div>
                        <span className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                          <Calendar className="w-3 h-3" /> Placed on {dateStr}
                        </span>
                      </div>

                      <div className="text-right">
                        <span className="text-xs text-slate-400 block font-normal">Order Total</span>
                        <span className="text-sm font-bold text-slate-900">
                          ${Number(ord.totalAmount).toFixed(2)}
                        </span>
                      </div>
                    </div>

                    {/* Order Line Items */}
                    <div className="pt-3 divide-y divide-slate-50">
                      {ord.items && ord.items.length > 0 ? (
                        ord.items.map((item, idx) => (
                          <div key={idx} className="py-1.5 flex justify-between text-xs text-slate-600 first:pt-0 last:pb-0">
                            <span className="line-clamp-1">
                              {item.quantity}× {item.productName || item.productId}
                            </span>
                            <span className="font-medium text-slate-900 shrink-0">
                              ${Number(item.subtotal || item.unitPrice * item.quantity).toFixed(2)}
                            </span>
                          </div>
                        ))
                      ) : (
                        <p className="text-xs text-slate-400 py-1">Standard cart checkout snapshot</p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
