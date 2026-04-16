"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import api from "@/lib/api";
import { useAuthStore } from "@/store/authStore";
import { AuthResponse } from "@/lib/types";
import Cookies from "js-cookie";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:5000";

export default function LoginPage() {
  const router = useRouter();
  const { setAuth } = useAuthStore();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [errorType, setErrorType] = useState<"error" | "locked">("error");

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await api.post<AuthResponse>("/api/auth/login", {
        email,
        password,
      });

      const { accessToken, refreshToken, user } = res.data;

      setAuth(user, accessToken, refreshToken);

      // Set cookies for middleware
      Cookies.set("access-token", accessToken, {
        expires: 1,
        sameSite: "Strict",
        secure: process.env.NODE_ENV === "production",
      });
      Cookies.set("user-roles", JSON.stringify(user.roles), {
        expires: 1,
        sameSite: "Strict",
        secure: process.env.NODE_ENV === "production",
      });

      // Role-based redirect
      if (user.roles.includes("ROLE_ADMIN")) {
        router.push("/admin");
      } else {
        router.push("/dashboard");
      }
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      const status = axiosErr?.response?.status;
      const msg =
        axiosErr?.response?.data?.message || "An error occurred. Please try again.";

      if (status === 403 && msg.toLowerCase().includes("verify")) {
        router.push(`/verify-otp?email=${encodeURIComponent(email)}`);
        return;
      }

      setErrorType(status === 423 ? "locked" : "error");
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-layout">
      {/* Brand panel */}
      <div className="auth-brand-panel">
        <div className="auth-brand-logo">A</div>
        <h1 className="auth-brand-title">
          Enterprise-grade<br />authentication.
        </h1>
        <p className="auth-brand-subtitle">
          Secure, role-based access management built for the scale of modern product companies.
        </p>
        <div className="auth-brand-dots">
          {Array.from({ length: 42 }).map((_, i) => (
            <span key={i} />
          ))}
        </div>
      </div>

      {/* Form panel */}
      <div className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-form-header">
            <div className="auth-form-logo-sm">A</div>
            <h2 className="auth-form-title">Welcome back</h2>
            <p className="auth-form-subtitle">Sign in to your account</p>
          </div>

          {/* OAuth2 Buttons */}
          <div className="oauth-row">
            <a
              href={`${API_BASE}/oauth2/authorization/google`}
              className="oauth-btn"
            >
              <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Google
            </a>
            <a
              href={`${API_BASE}/oauth2/authorization/github`}
              className="oauth-btn"
            >
              <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" fill="currentColor">
                <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
              </svg>
              GitHub
            </a>
          </div>

          <div className="divider">or continue with email</div>

          {error && (
            <div className={`alert alert-${errorType}`}>
              <span>⚠</span>
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                className="form-input"
                placeholder="you@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>

            <div className="form-group">
              <div className="flex justify-between items-center mb-1">
                <label className="form-label" htmlFor="password" style={{ marginBottom: 0 }}>
                  Password
                </label>
                <Link href="/forgot-password" className="text-link" style={{ fontSize: "0.78rem" }}>
                  Forgot password?
                </Link>
              </div>
              <div className="form-input-wrapper">
                <input
                  id="password"
                  type={showPwd ? "text" : "password"}
                  className="form-input"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="current-password"
                  style={{ paddingRight: "2.75rem" }}
                />
                <button
                  type="button"
                  className="form-input-icon toggle-btn"
                  onClick={() => setShowPwd((v) => !v)}
                  aria-label={showPwd ? "Hide password" : "Show password"}
                >
                  {showPwd ? (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? <span className="spinner" /> : "Sign in"}
            </button>
          </form>

          <p className="text-center text-sm text-muted" style={{ marginTop: "1.5rem" }}>
            Don&apos;t have an account?{" "}
            <Link href="/register" className="text-link">
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
