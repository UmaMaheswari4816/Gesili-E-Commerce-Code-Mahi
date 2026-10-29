"use client";

import { api } from "@/lib/api";
import type { Order } from "@/lib/types";
import { useParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";

const TERMINAL = new Set(["CONFIRMED", "CANCELLED", "PAYMENT_TIMEOUT"]);

export default function OrderStatusPage() {
  const params = useParams<{ id: string }>();
  const orderId = params.id;
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function poll() {
      try {
        const o = await api.getOrder(orderId);
        if (cancelled) return;
        setOrder(o);
        // Stop polling once the saga reaches a state the user acts on or a terminal state.
        if (o.status === "PAYMENT_PENDING" || TERMINAL.has(o.status)) {
          if (timer.current) clearInterval(timer.current);
        }
      } catch (e) {
        if (!cancelled) setError((e as Error).message);
      }
    }

    poll();
    timer.current = setInterval(poll, 2000); // poll every 2s (per the design)
    return () => {
      cancelled = true;
      if (timer.current) clearInterval(timer.current);
    };
  }, [orderId]);

  if (error) return <p className="error">{error}</p>;
  if (!order) return <p className="muted">Loading order…</p>;

  return (
    <div>
      <h1>Order {order.id.slice(0, 8)}…</h1>
      <div className="card">
        <p>
          Status: <span className="badge">{order.status}</span>
        </p>
        <p className="price">
          {order.currency} {order.totalAmount.toFixed(2)}
        </p>

        {order.status === "PENDING_INVENTORY" && (
          <p className="muted">Reserving stock… this page updates automatically.</p>
        )}

        {order.status === "PAYMENT_PENDING" && order.checkout && (
          <div>
            <p>Reserved! Complete payment to confirm your order.</p>
            <p className="muted">
              Provider: {order.checkout.provider} · Ref: {order.checkout.providerRef}
            </p>
            {/* In a real app, this opens the provider's checkout (Razorpay/Stripe). */}
            <button
              onClick={() =>
                alert(
                  "Open provider checkout here. The provider webhook then confirms the order; this page will reflect CONFIRMED."
                )
              }
            >
              Pay {order.currency} {order.checkout.amount.toFixed(2)}
            </button>
          </div>
        )}

        {order.status === "CONFIRMED" && <p>✅ Your order is confirmed. Thank you!</p>}
        {order.status === "CANCELLED" && (
          <p className="error">Order cancelled{order.cancelReason ? ` (${order.cancelReason})` : ""}.</p>
        )}
        {order.status === "PAYMENT_TIMEOUT" && (
          <p className="error">Payment timed out; the order was cancelled.</p>
        )}
      </div>
    </div>
  );
}
