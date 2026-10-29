// Typed API client for the Gesili gateway, with automatic access-token attachment and a
// transparent refresh-token retry on 401. Tokens live in localStorage (fine for a learning
// SPA; a production app would use httpOnly cookies to resist XSS).
import type {
  Cart,
  Order,
  Page,
  ProblemDetail,
  ProductSearchResult,
  TokenResponse,
  UserResponse,
} from "./types";

const BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") || "http://localhost:8080";

const ACCESS_KEY = "gesili.access";
const REFRESH_KEY = "gesili.refresh";

export const tokenStore = {
  get access() {
    return typeof window === "undefined" ? null : localStorage.getItem(ACCESS_KEY);
  },
  get refresh() {
    return typeof window === "undefined" ? null : localStorage.getItem(REFRESH_KEY);
  },
  set(tokens: TokenResponse) {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export class ApiError extends Error {
  constructor(public status: number, public problem: ProblemDetail | null) {
    super(problem?.detail || problem?.title || `HTTP ${status}`);
  }
}

async function parseError(res: Response): Promise<ApiError> {
  let problem: ProblemDetail | null = null;
  try {
    problem = (await res.json()) as ProblemDetail;
  } catch {
    /* non-JSON error body */
  }
  return new ApiError(res.status, problem);
}

// Low-level fetch with auth + one transparent refresh retry on 401.
async function request<T>(
  path: string,
  init: RequestInit = {},
  auth = true,
  _retried = false
): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (auth && tokenStore.access) {
    headers.set("Authorization", `Bearer ${tokenStore.access}`);
  }

  const res = await fetch(`${BASE}${path}`, { ...init, headers });

  if (res.status === 401 && auth && !_retried && tokenStore.refresh) {
    // Access token likely expired — rotate and retry once.
    const refreshed = await tryRefresh();
    if (refreshed) {
      return request<T>(path, init, auth, true);
    }
  }
  if (!res.ok) {
    throw await parseError(res);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

async function tryRefresh(): Promise<boolean> {
  const refreshToken = tokenStore.refresh;
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) {
      tokenStore.clear();
      return false;
    }
    tokenStore.set((await res.json()) as TokenResponse);
    return true;
  } catch {
    tokenStore.clear();
    return false;
  }
}

export const api = {
  // ---- auth ----
  async register(email: string, password: string, displayName: string) {
    return request<UserResponse>(
      "/api/v1/auth/register",
      { method: "POST", body: JSON.stringify({ email, password, displayName }) },
      false
    );
  },
  async login(email: string, password: string) {
    const tokens = await request<TokenResponse>(
      "/api/v1/auth/login",
      { method: "POST", body: JSON.stringify({ email, password }) },
      false
    );
    tokenStore.set(tokens);
    return tokens;
  },
  async me() {
    return request<UserResponse>("/api/v1/auth/me");
  },
  async logout() {
    try {
      await request<void>("/api/v1/auth/logout", { method: "POST" });
    } finally {
      tokenStore.clear();
    }
  },

  // ---- catalog (public) ----
  async search(q: string, page = 0, size = 20) {
    const qs = new URLSearchParams({ q, page: String(page), size: String(size) });
    return request<Page<ProductSearchResult>>(`/api/v1/products/search?${qs}`, {}, false);
  },

  // ---- cart ----
  async getCart() {
    return request<Cart>("/api/v1/cart");
  },
  async addToCart(item: {
    productId: string;
    sku: string;
    name: string;
    quantity: number;
    unitPrice: number;
  }) {
    return request<Cart>("/api/v1/cart/items", {
      method: "POST",
      body: JSON.stringify(item),
    });
  },
  async clearCart() {
    return request<void>("/api/v1/cart", { method: "DELETE" });
  },

  // ---- orders ----
  async placeOrder(
    items: { productId: string; sku: string; quantity: number; unitPrice: number }[],
    currency = "INR"
  ) {
    return request<Order>("/api/v1/orders", {
      method: "POST",
      body: JSON.stringify({ items, currency }),
    });
  },
  async getOrder(id: string) {
    return request<Order>(`/api/v1/orders/${id}`);
  },
};
