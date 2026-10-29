import { AuthProvider } from "@/lib/auth-context";
import { NavBar } from "@/components/NavBar";
import "./globals.css";

export const metadata = {
  title: "Gecili Shop",
  description: "Frontend for the Gecili e-commerce microservices platform",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <NavBar />
          <main className="container">{children}</main>
        </AuthProvider>
      </body>
    </html>
  );
}
