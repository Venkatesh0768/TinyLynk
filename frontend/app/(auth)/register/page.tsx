"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import api from "@/lib/api";
import { ApiResponse } from "@/lib/types";

function getPasswordStrength(pwd: string): { level: number; label: string } {
  let score = 0;
  if (pwd.length >= 8) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  const labels = ["", "Weak", "Fair", "Good", "Strong"];
  return { level: score, label: labels[score] || "" };
}

export default function RegisterPage() {
  const router = useRouter();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const strength = getPasswordStrength(password);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setFieldErrors({});

    if (password !== confirmPassword) {
      setFieldErrors({ confirmPassword: "Passwords do not match" });
      return;
    }

    setLoading(true);

    try {
      await api.post<ApiResponse>("/api/auth/signup", {
        email,
        password,
        firstName,
        lastName,
      });

      router.push(`/verify-otp?email=${encodeURIComponent(email)}&type=signup`);
    } catch (err: unknown) {
      const e2 = err as {
        response?: { data?: { message?: string; errors?: Record<string, string> } };
      };
      const data = e2?.response?.data;
      if (data?.errors) {
        setFieldErrors(data.errors);
      } else {
        setError(data?.message || "Registration failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-layout">
      <div className="auth-brand-panel">
        <div className="auth-brand-logo">A</div>
        <h1 className="auth-brand-title">
          Join thousands of<br />teams worldwide.
        </h1>
        <p className="auth-brand-subtitle">
          Get started with a free account. No credit card required.
          Enterprise features available on day one.
        </p>
        <div className="auth-brand-dots">
          {Array.from({ length: 42 }).map((_, i) => <span key={i} />)}
        </div>
      </div>

      <div className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-form-header">
            <div className="auth-form-logo-sm">A</div>
            <h2 className="auth-form-title">Create your account</h2>
            <p className="auth-form-subtitle">Free forever. No credit card needed.</p>
          </div>

          {error && (
            <div className="alert alert-error">
              <span>✕</span>
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
              <div className="form-group">
                <label className="form-label" htmlFor="firstName">First Name</label>
                <input
                  id="firstName"
                  type="text"
                  className={`form-input${fieldErrors.firstName ? " error" : ""}`}
                  placeholder="John"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                  autoComplete="given-name"
                />
                {fieldErrors.firstName && (
                  <p className="form-error">{fieldErrors.firstName}</p>
                )}
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="lastName">Last Name</label>
                <input
                  id="lastName"
                  type="text"
                  className={`form-input${fieldErrors.lastName ? " error" : ""}`}
                  placeholder="Doe"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                  autoComplete="family-name"
                />
                {fieldErrors.lastName && (
                  <p className="form-error">{fieldErrors.lastName}</p>
                )}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="email">Work Email</label>
              <input
                id="email"
                type="email"
                className={`form-input${fieldErrors.email ? " error" : ""}`}
                placeholder="you@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
              {fieldErrors.email && <p className="form-error">{fieldErrors.email}</p>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="password">Password</label>
              <div className="form-input-wrapper">
                <input
                  id="password"
                  type={showPwd ? "text" : "password"}
                  className={`form-input${fieldErrors.password ? " error" : ""}`}
                  placeholder="Min. 8 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={8}
                  autoComplete="new-password"
                  style={{ paddingRight: "2.75rem" }}
                />
                <button
                  type="button"
                  className="form-input-icon toggle-btn"
                  onClick={() => setShowPwd((v) => !v)}
                  aria-label="Toggle password visibility"
                >
                  {showPwd ? (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
              {password && (
                <div className="password-strength">
                  <div className="strength-bars">
                    {[1, 2, 3, 4].map((n) => (
                      <div
                        key={n}
                        className={`strength-bar${strength.level >= n ? ` active level-${strength.level}` : ""}`}
                      />
                    ))}
                  </div>
                  <span className="strength-label">{strength.label}</span>
                </div>
              )}
              {fieldErrors.password && <p className="form-error">{fieldErrors.password}</p>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="confirmPassword">Confirm Password</label>
              <input
                id="confirmPassword"
                type="password"
                className={`form-input${fieldErrors.confirmPassword ? " error" : ""}`}
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                autoComplete="new-password"
              />
              {fieldErrors.confirmPassword && (
                <p className="form-error">{fieldErrors.confirmPassword}</p>
              )}
            </div>

            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" /> : "Create Account"}
            </button>
          </form>

          <p className="text-center text-sm text-muted" style={{ marginTop: "1.5rem" }}>
            Already have an account?{" "}
            <Link href="/login" className="text-link">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
