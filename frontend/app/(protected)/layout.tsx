"use client";

import Navbar from "@/components/Navbar";
import { useAuthStore } from "@/store/authStore";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function ProtectedLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading) {
    return (
      <div className="page-loader">
        <div className="spinner" style={{ width: 36, height: 36 }} />
      </div>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <div className="dashboard-layout">
      <Navbar />
      <main className="dashboard-content">{children}</main>
    </div>
  );
}
