"use client";

import { useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import api from "@/lib/api";
import { useAuthStore } from "@/store/authStore";
import { User } from "@/lib/types";
import Cookies from "js-cookie";

function OAuth2CallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { setAuth } = useAuthStore();

  useEffect(() => {
    const token = searchParams.get("token");
    if (!token) {
      router.push("/login?error=oauth_failed");
      return;
    }

    (async () => {
      try {
        // Temporarily set token in localStorage for api interceptor
        const tempStorage = { state: { accessToken: token, refreshToken: null }, version: 0 };
        localStorage.setItem("auth-storage", JSON.stringify(tempStorage));

        const res = await api.get<{ data: User }>("/api/user/me");
        const user = res.data.data!;

        // We don't have a refresh token from the OAuth2 redirect, so use empty string
        // The user can use the token for the session, refresh will be handled on next login
        setAuth(user, token, "");

        Cookies.set("access-token", token, {
          expires: 1,
          sameSite: "Strict",
          secure: process.env.NODE_ENV === "production",
        });
        Cookies.set("user-roles", JSON.stringify(user.roles), {
          expires: 1,
          sameSite: "Strict",
          secure: process.env.NODE_ENV === "production",
        });

        if (user.roles.includes("ROLE_ADMIN")) {
          router.push("/admin");
        } else {
          router.push("/dashboard");
        }
      } catch {
        router.push("/login?error=oauth_failed");
      }
    })();
  }, [searchParams, router, setAuth]);

  return (
    <div className="page-loader" style={{ flexDirection: "column", gap: "1.25rem" }}>
      <div className="auth-brand-logo" style={{ margin: 0 }}>A</div>
      <div className="spinner" style={{ width: 36, height: 36 }} />
      <p style={{ color: "var(--text-secondary)", fontSize: "0.95rem" }}>
        Completing sign-in…
      </p>
    </div>
  );
}

export default function OAuth2CallbackPage() {
  return (
    <Suspense fallback={
      <div className="page-loader">
        <div className="spinner" style={{ width: 36, height: 36 }} />
      </div>
    }>
      <OAuth2CallbackContent />
    </Suspense>
  );
}
