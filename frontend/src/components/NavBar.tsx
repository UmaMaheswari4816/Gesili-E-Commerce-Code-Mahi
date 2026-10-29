"use client";

import { useAuth } from "@/lib/auth-context";
import Link from "next/link";
import { useRouter } from "next/navigation";

export function NavBar() {
  const { user, logout } = useAuth();
  const router = useRouter();

  return (
    <nav className="topbar">
      <Link href="/">Gesili Shop</Link>
      <Link href="/cart">Cart</Link>
      <span className="spacer" />
      {user ? (
        <div className="row">
          <span className="muted">{user.email}</span>
          <button
            className="secondary"
            onClick={async () => {
              await logout();
              router.push("/");
            }}
          >
            Logout
          </button>
        </div>
      ) : (
        <Link href="/login">Login</Link>
      )}
    </nav>
  );
}
