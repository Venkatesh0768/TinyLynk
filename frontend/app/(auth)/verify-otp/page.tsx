"use client";

import { useState, useRef, useEffect, KeyboardEvent, ClipboardEvent, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import api from "@/lib/api";
import { ApiResponse } from "@/lib/types";

function VerifyOTPContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const email = searchParams.get("email") || "";

  const [otp, setOtp] = useState<string[]>(Array(6).fill(""));
  const [timeLeft, setTimeLeft] = useState(300); // 5 minutes
  const [cooldown, setCooldown] = useState(0);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // Countdown timer
  useEffect(() => {
    if (timeLeft <= 0) return;
    const id = setInterval(() => setTimeLeft((t) => Math.max(0, t - 1)), 1000);
    return () => clearInterval(id);
  }, [timeLeft]);

  // Resend cooldown
  useEffect(() => {
    if (cooldown <= 0) return;
    const id = setInterval(() => setCooldown((c) => Math.max(0, c - 1)), 1000);
    return () => clearInterval(id);
  }, [cooldown]);

  const formatTime = (s: number) => {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${m}:${sec.toString().padStart(2, "0")}`;
  };

  function handleChange(index: number, value: string) {
    if (!/^[0-9]*$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
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

  async function handleVerify() {
    const otpStr = otp.join("");
    if (otpStr.length < 6) { setError("Please enter the complete 6-digit OTP"); return; }

    setError("");
    setLoading(true);

    try {
      await api.post<ApiResponse>("/api/auth/verify-otp", { email, otp: otpStr });
      setSuccess("Email verified! Redirecting to login…");
      setTimeout(() => router.push("/login"), 1800);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e?.response?.data?.message || "Invalid or expired OTP. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    setResending(true);
    setError("");
    try {
      await api.post(`/api/auth/resend-otp?email=${encodeURIComponent(email)}`);
      setSuccess("A new OTP has been sent to your email.");
      setTimeLeft(300);
      setCooldown(60);
      setOtp(Array(6).fill(""));
      setTimeout(() => setSuccess(""), 4000);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e?.response?.data?.message || "Failed to resend OTP.");
    } finally {
      setResending(false);
    }
  }

  return (
    <div className="auth-layout">
      <div className="auth-brand-panel">
        <div className="auth-brand-logo">A</div>
        <h1 className="auth-brand-title">
          Check your<br />inbox.
        </h1>
        <p className="auth-brand-subtitle">
          We sent a 6-digit verification code to your email. The code expires in 5 minutes.
        </p>
        <div className="auth-brand-dots">
          {Array.from({ length: 42 }).map((_, i) => <span key={i} />)}
        </div>
      </div>

      <div className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-form-header">
            <div className="auth-form-logo-sm">✉</div>
            <h2 className="auth-form-title">Verify your email</h2>
            <p className="auth-form-subtitle">
              Enter the 6-digit code sent to{" "}
              <strong style={{ color: "var(--text-primary)" }}>{email}</strong>
            </p>
          </div>

          {error && <div className="alert alert-error"><span>✕</span><span>{error}</span></div>}
          {success && <div className="alert alert-success"><span>✓</span><span>{success}</span></div>}

          <div className="otp-container">
            {otp.map((digit, i) => (
              <input
                key={i}
                ref={(el) => { inputRefs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={1}
                className={`otp-box${digit ? " filled" : ""}`}
                value={digit}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                onPaste={i === 0 ? handlePaste : undefined}
                aria-label={`OTP digit ${i + 1}`}
              />
            ))}
          </div>

          <div className="otp-timer">
            {timeLeft > 0 ? (
              <>Code expires in <strong>{formatTime(timeLeft)}</strong></>
            ) : (
              <span style={{ color: "var(--error)" }}>OTP expired. Please request a new one.</span>
            )}
          </div>

          <button
            className="btn btn-primary"
            onClick={handleVerify}
            disabled={loading || otp.join("").length < 6}
          >
            {loading ? <span className="spinner" /> : "Verify Email"}
          </button>

          <div className="text-center" style={{ marginTop: "1.25rem" }}>
            <button
              className="btn btn-ghost"
              onClick={handleResend}
              disabled={resending || cooldown > 0}
              style={{ fontSize: "0.875rem" }}
            >
              {resending
                ? "Sending…"
                : cooldown > 0
                ? `Resend OTP in ${cooldown}s`
                : "Resend OTP"}
            </button>
          </div>

          <p className="text-center text-sm text-muted" style={{ marginTop: "1rem" }}>
            <Link href="/login" className="text-link">← Back to login</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default function VerifyOTPPage() {
  return (
    <Suspense fallback={<div className="page-loader"><div className="spinner" style={{width:32,height:32}} /></div>}>
      <VerifyOTPContent />
    </Suspense>
  );
}
