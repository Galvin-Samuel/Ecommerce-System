import React from 'react';
import { CheckCircle2, ArrowRight, PackageCheck, Calendar, Hash } from 'lucide-react';
import { Order } from '../../types/order';

interface OrderConfirmationModalProps {
  order: Order | null;
  onClose: () => void;
  onViewOrders: () => void;
}

export const OrderConfirmationModal: React.FC<OrderConfirmationModalProps> = ({
  order,
  onClose,
  onViewOrders,
}) => {
  if (!order) return null;

  const formattedDate = order.createdAt
    ? new Date(order.createdAt).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : 'Confirmed';

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl border border-slate-100 overflow-hidden p-8 text-center">
        {/* Animated Check */}
        <div className="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto mb-4 animate-in zoom-in">
          <CheckCircle2 className="w-8 h-8" />
        </div>

        <h2 className="text-xl font-bold text-slate-900 tracking-tight">Order Successfully Placed!</h2>
        <p className="text-xs text-slate-500 mt-1 mb-6">
          Thank you for your purchase. Your order has been snapshot and recorded in the database.
        </p>

        {/* Order Details Card */}
        <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100 text-left space-y-2.5 text-xs mb-6">
          <div className="flex items-center justify-between text-slate-600">
            <span className="flex items-center gap-1.5">
              <Hash className="w-3.5 h-3.5 text-slate-400" /> Order ID:
            </span>
            <code className="font-mono text-[11px] font-bold text-slate-900">{order.id}</code>
          </div>

          <div className="flex items-center justify-between text-slate-600">
            <span className="flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-slate-400" /> Date & Time:
            </span>
            <span className="font-medium text-slate-800">{formattedDate}</span>
          </div>

          <div className="flex items-center justify-between text-slate-600">
            <span className="flex items-center gap-1.5">
              <PackageCheck className="w-3.5 h-3.5 text-slate-400" /> Status:
            </span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-100 text-emerald-800">
              {order.status || 'PENDING'}
            </span>
          </div>

          <div className="border-t border-slate-200 pt-2 flex items-center justify-between text-slate-900 font-bold">
            <span>Total Paid:</span>
            <span className="text-sm font-extrabold text-brand-600">${Number(order.totalAmount).toFixed(2)}</span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="space-y-2">
          <button
            onClick={() => {
              onClose();
              onViewOrders();
            }}
            className="w-full py-3 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold flex items-center justify-center gap-2 shadow-sm transition-all"
          >
            <span>View All My Orders</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={onClose}
            className="w-full py-2.5 px-4 rounded-xl hover:bg-slate-100 text-slate-600 text-xs font-semibold transition-colors"
          >
            Continue Shopping
          </button>
        </div>
      </div>
    </div>
  );
};

