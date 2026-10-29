"use client";

import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { Cart } from "@/lib/types";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function CartPage() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.push("/login");
      return;
    }
    api.getCart().then(setCart).catch((e) => setError(e.message));
  }, [user, loading, router]);

  async function checkout() {
    if (!cart || cart.lines.length === 0) return;
    setPlacing(true);
    setError(null);
    try {
      const order = await api.placeOrder(
        cart.lines.map((l) => ({
          productId: l.productId,
          sku: l.sku,
          quantity: l.quantity,
          unitPrice: l.unitPrice,
        })),
        "INR"
      );
      await api.clearCart();
      router.push(`/orders/${order.id}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Checkout failed");
    } finally {
      setPlacing(false);
    }
  }

  if (loading) return <p className="muted">Loading…</p>;
  if (!cart) return <p className="muted">{error || "Loading cart…"}</p>;

  return (
    <div>
      <h1>Your cart</h1>
      {error && <p className="error">{error}</p>}
      {cart.lines.length === 0 ? (
        <p className="muted">
          Your cart is empty. <Link href="/">Browse products</Link>.
        </p>
      ) : (
        <>
          <div className="card">
            {cart.lines.map((l) => (
              <div className="row" key={l.sku} style={{ borderBottom: "1px solid var(--border)", padding: "0.5rem 0" }}>
                <div style={{ flex: 1 }}>
                  <strong>{l.name || l.sku}</strong>
                  <div className="muted">
                    {l.quantity} × {l.unitPrice.toFixed(2)}
                  </div>
                </div>
                <div className="price">{(l.quantity * l.unitPrice).toFixed(2)}</div>
              </div>
            ))}
            <div className="row" style={{ marginTop: "0.75rem" }}>
              <strong style={{ flex: 1 }}>Total</strong>
              <span className="price">INR {cart.total.toFixed(2)}</span>
            </div>
          </div>
          <button onClick={checkout} disabled={placing} style={{ marginTop: "1rem" }}>
            {placing ? "Placing order…" : "Checkout"}
          </button>
        </>
      )}
    </div>
  );
}
