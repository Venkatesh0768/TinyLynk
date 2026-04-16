"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";

export default function Home() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuthStore();

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace("/login");
    } else if (user?.roles?.includes("ROLE_ADMIN")) {
      router.replace("/admin");
    } else {
      router.replace("/dashboard");
    }
  }, [isAuthenticated, isLoading, user, router]);

  return (
    <div className="page-loader">
      <div className="spinner" style={{ width: 36, height: 36 }} />
    </div>
  );
}
