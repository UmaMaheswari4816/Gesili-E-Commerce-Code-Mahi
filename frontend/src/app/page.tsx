"use client";

import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { ProductSearchResult } from "@/lib/types";
import { useEffect, useState } from "react";

export default function HomePage() {
  const { user } = useAuth();
  const [q, setQ] = useState("");
  const [results, setResults] = useState<ProductSearchResult[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function runSearch(query: string) {
    setLoading(true);
    setError(null);
    try {
      const page = await api.search(query);
      setResults(page.content);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Search failed");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    runSearch("");
  }, []);

  async function add(p: ProductSearchResult) {
    if (!user) {
      setError("Please log in to add items to your cart.");
      return;
    }
    try {
      await api.addToCart({
        productId: p.id,
        sku: p.sku,
        name: p.name,
        quantity: 1,
        unitPrice: p.price,
      });
      alert(`Added ${p.name} to cart`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Add to cart failed");
    }
  }

  return (
    <div>
      <h1>Products</h1>
      <form
        className="row"
        onSubmit={(e) => {
          e.preventDefault();
          runSearch(q);
        }}
      >
        <input
          placeholder="Search products…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>
      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="muted">Loading…</p>
      ) : results.length === 0 ? (
        <p className="muted">No products found. (Is product-service running with data?)</p>
      ) : (
        <div className="grid" style={{ marginTop: "1rem" }}>
          {results.map((p) => (
            <div className="card" key={p.id}>
              <div className="badge">{p.categoryName || "Catalog"}</div>
              <h3>{p.name}</h3>
              <p className="muted">{p.description}</p>
              <p className="price">
                {p.currency} {p.price.toFixed(2)}
              </p>
              <button onClick={() => add(p)}>Add to cart</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
