"use client";

import { useState, useRef, useEffect, KeyboardEvent, ClipboardEvent, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import api from "@/lib/api";

function ResetPasswordContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const email = searchParams.get("email") || "";

  const [otp, setOtp] = useState<string[]>(Array(6).fill(""));
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  function handleChange(index: number, value: string) {
    if (!/^[0-9]*$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);
    if (value && index < 5) inputRefs.current[index + 1]?.focus();
  }

  function handleKeyDown(index: number, e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Backspace" && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  }

  function handlePaste(e: ClipboardEvent<HTMLInputElement>) {
    e.preventDefault();
    const digits = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
    const newOtp = [...Array(6).fill("")];
    digits.split("").forEach((d, i) => { newOtp[i] = d; });
    setOtp(newOtp);
    inputRefs.current[Math.min(digits.length, 5)]?.focus();
  }

  async function handleReset() {
    setError("");
    if (otp.join("").length < 6) { setError("Enter the 6-digit code"); return; }
    if (newPassword.length < 8) { setError("Password must be at least 8 characters"); return; }
    if (newPassword !== confirmPassword) { setError("Passwords do not match"); return; }

    setLoading(true);
    try {
      await api.post("/api/auth/reset-password", {
        email,
        otp: otp.join(""),
        newPassword,
      });
      router.push("/login?reset=success");
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e?.response?.data?.message || "Failed to reset password. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-layout">
      <div className="auth-brand-panel">
        <div className="auth-brand-logo">A</div>
        <h1 className="auth-brand-title">Create a new<br />password.</h1>
        <p className="auth-brand-subtitle">Enter the code from your email and choose a strong new password.</p>
        <div className="auth-brand-dots">{Array.from({ length: 42 }).map((_, i) => <span key={i} />)}</div>
      </div>

      <div className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-form-header">
            <div className="auth-form-logo-sm">🔐</div>
            <h2 className="auth-form-title">Set new password</h2>
            <p className="auth-form-subtitle">
              Code sent to <strong style={{ color: "var(--text-primary)" }}>{email}</strong>
            </p>
          </div>

          {error && <div className="alert alert-error"><span>✕</span><span>{error}</span></div>}

          <div className="form-group">
            <label className="form-label">Verification Code</label>
            <div className="otp-container" style={{ margin: "0.5rem 0 1.1rem", justifyContent: "flex-start", gap: "0.5rem" }}>
              {otp.map((digit, i) => (
                <input
                  key={i}
                  ref={(el) => { inputRefs.current[i] = el; }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  className={`otp-box${digit ? " filled" : ""}`}
                  style={{ width: "44px", height: "52px", fontSize: "1.25rem" }}
                  value={digit}
                  onChange={(e) => handleChange(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  onPaste={i === 0 ? handlePaste : undefined}
                  aria-label={`Code digit ${i + 1}`}
                />
              ))}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="newPwd">New Password</label>
            <div className="form-input-wrapper">
              <input
                id="newPwd"
                type={showPwd ? "text" : "password"}
                className="form-input"
                placeholder="Min. 8 characters"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                style={{ paddingRight: "2.75rem" }}
              />
              <button type="button" className="form-input-icon toggle-btn" onClick={() => setShowPwd(v => !v)}>
                {showPwd ? (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                  </svg>
                )}
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="confirmPwd">Confirm Password</label>
            <input
              id="confirmPwd"
              type="password"
              className={`form-input${confirmPassword && confirmPassword !== newPassword ? " error" : ""}`}
              placeholder="••••••••"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          <button className="btn btn-primary" onClick={handleReset} disabled={loading}>
            {loading ? <span className="spinner" /> : "Reset Password"}
          </button>

          <p className="text-center text-sm text-muted" style={{ marginTop: "1.25rem" }}>
            <Link href="/login" className="text-link">← Back to login</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="page-loader"><div className="spinner" style={{width:32,height:32}} /></div>}>
      <ResetPasswordContent />
    </Suspense>
  );
}
