"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import api from "@/lib/api";

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await api.post("/api/auth/forgot-password", { email });
      setSent(true);
    } catch {
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  if (sent) {
    return (
      <div className="auth-layout">
        <div className="auth-brand-panel">
          <div className="auth-brand-logo">A</div>
          <h1 className="auth-brand-title">Check your email.</h1>
          <p className="auth-brand-subtitle">
            If an account exists for that email, we&apos;ve sent a reset code. Check spam if you don&apos;t see it.
          </p>
          <div className="auth-brand-dots">{Array.from({ length: 42 }).map((_, i) => <span key={i} />)}</div>
        </div>
        <div className="auth-form-panel">
          <div className="auth-form-container">
            <div className="auth-form-header">
              <div className="auth-form-logo-sm" style={{ fontSize: "1.4rem" }}>📬</div>
              <h2 className="auth-form-title">Reset link sent!</h2>
              <p className="auth-form-subtitle">Check your inbox for the OTP code.</p>
            </div>
            <div className="alert alert-success">
              <span>✓</span>
              <span>If <strong>{email}</strong> is registered, a reset code has been sent.</span>
            </div>
            <button
              className="btn btn-primary"
              onClick={() => router.push(`/reset-password?email=${encodeURIComponent(email)}`)}
            >
              Enter Reset Code →
            </button>
            <p className="text-center text-sm text-muted" style={{ marginTop: "1rem" }}>
              <Link href="/login" className="text-link">← Back to login</Link>
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-layout">
      <div className="auth-brand-panel">
        <div className="auth-brand-logo">A</div>
        <h1 className="auth-brand-title">Forgot your password?</h1>
        <p className="auth-brand-subtitle">
          No worries. Enter your email and we&apos;ll send you a reset code instantly.
        </p>
        <div className="auth-brand-dots">{Array.from({ length: 42 }).map((_, i) => <span key={i} />)}</div>
      </div>

      <div className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-form-header">
            <div className="auth-form-logo-sm">🔑</div>
            <h2 className="auth-form-title">Reset your password</h2>
            <p className="auth-form-subtitle">We&apos;ll send a 6-digit code to your email.</p>
          </div>

          {error && <div className="alert alert-error"><span>✕</span><span>{error}</span></div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="email">Email Address</label>
              <input
                id="email"
                type="email"
                className="form-input"
                placeholder="you@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                autoFocus
              />
            </div>

            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" /> : "Send Reset Code"}
            </button>
          </form>

          <p className="text-center text-sm text-muted" style={{ marginTop: "1.5rem" }}>
            Remembered it?{" "}
            <Link href="/login" className="text-link">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
