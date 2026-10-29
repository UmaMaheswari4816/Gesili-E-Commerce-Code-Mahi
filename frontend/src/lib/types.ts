// Types mirroring the backend DTOs (manually kept in sync here; see README for how to
// GENERATE these from the aggregated OpenAPI spec instead).

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface UserResponse {
  id: string;
  email: string;
  displayName: string | null;
  roles: string[];
}

export interface ProductSearchResult {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  price: number;
  currency: string;
  categoryName: string | null;
  categorySlug: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CartLine {
  productId: string;
  sku: string;
  name: string | null;
  quantity: number;
  unitPrice: number;
}

export interface Cart {
  userId: string;
  lines: CartLine[];
  total: number;
}

export interface CheckoutParams {
  provider: string;
  providerRef: string;
  keyId: string | null;
  amount: number;
  currency: string;
}

export interface Order {
  id: string;
  userId: string;
  status:
    | "CREATED"
    | "PENDING_INVENTORY"
    | "PAYMENT_PENDING"
    | "CONFIRMED"
    | "PAYMENT_TIMEOUT"
    | "CANCELLED";
  totalAmount: number;
  currency: string;
  paymentDeadline: string | null;
  cancelReason: string | null;
  items: { productId: string; sku: string; quantity: number; unitPrice: number }[];
  checkout: CheckoutParams | null;
}

// RFC 7807 problem detail returned by every service on error.
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  correlationId?: string;
  errors?: { field: string; message: string; rejected: unknown }[];
}
