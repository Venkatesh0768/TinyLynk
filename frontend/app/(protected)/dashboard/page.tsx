"use client";

import { useEffect, useState } from "react";
import { useAuthStore } from "@/store/authStore";
import api from "@/lib/api";
import Link from "next/link";

interface DashboardStats {
  totalUsers?: number;
  enabledUsers?: number;
  adminCount?: number;
  vendorCount?: number;
}

export default function DashboardPage() {
  const { user } = useAuthStore();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  useEffect(() => {
    if (isAdmin) {
      api.get<{ data: DashboardStats }>("/api/admin/dashboard")
        .then((res) => setStats(res.data.data ?? null))
        .catch(() => {});
    }
  }, [isAdmin]);

  const initials = user
    ? `${user.firstName?.[0] || ""}${user.lastName?.[0] || ""}`.toUpperCase()
    : "U";

  const formatDate = (iso?: string) =>
    iso ? new Date(iso).toLocaleString() : "Never";

  const getRoleClass = (role: string) => {
    if (role === "ROLE_ADMIN") return "role-badge admin";
    if (role === "ROLE_VENDOR") return "role-badge vendor";
    return "role-badge user";
  };

  const formatRole = (role: string) => role.replace("ROLE_", "");

  return (
    <>
      <div className="page-title">Welcome back, {user?.firstName}! 👋</div>
      <p className="page-subtitle">Here&apos;s an overview of your account.</p>

      {/* Profile Card */}
      <div className="profile-card">
        <div className="profile-avatar-lg">
          {user?.profileImageUrl ? (
            <img src={user.profileImageUrl} alt={user?.firstName} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
          ) : (
            initials
          )}
        </div>
        <div className="profile-info">
          <div className="profile-name">{user?.firstName} {user?.lastName}</div>
          <div className="profile-email">{user?.email}</div>
          <div className="profile-roles">
            {user?.roles?.map((role) => (
              <span key={role} className={getRoleClass(role)}>{formatRole(role)}</span>
            ))}
          </div>
        </div>
        <div style={{ textAlign: "right" }}>
          <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginBottom: "0.3rem" }}>Last Login</div>
          <div style={{ fontSize: "0.875rem", color: "var(--text-secondary)" }}>{formatDate(user?.lastLoginAt)}</div>
          <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "0.5rem", marginBottom: "0.3rem" }}>Provider</div>
          <span className="role-badge user" style={{ textTransform: "capitalize" }}>{user?.provider || "local"}</span>
        </div>
      </div>

      {/* Admin Stats */}
      {isAdmin && stats && (
        <>
          <h3 className="card-title" style={{ marginBottom: "1rem" }}>System Overview</h3>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-value">{stats.totalUsers ?? "—"}</div>
              <div className="stat-label">Total Users</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.enabledUsers ?? "—"}</div>
              <div className="stat-label">Active Users</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.adminCount ?? "—"}</div>
              <div className="stat-label">Admins</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.vendorCount ?? "—"}</div>
              <div className="stat-label">Vendors</div>
            </div>
          </div>
        </>
      )}

      {/* Quick Links */}
      <div className="card">
        <div className="card-header">
          <div className="card-title">Quick Actions</div>
        </div>
        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
          {isAdmin && (
            <Link href="/admin" className="btn btn-primary" style={{ width: "auto" }}>
              Manage Users →
            </Link>
          )}
          {user?.roles?.includes("ROLE_VENDOR") && (
            <Link href="/vendor" className="btn btn-secondary" style={{ width: "auto" }}>
              Vendor Panel →
            </Link>
          )}
        </div>
      </div>

      {/* Account Info */}
      <div className="card" style={{ marginTop: "1rem" }}>
        <div className="card-header">
          <div className="card-title">Account Details</div>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem", fontSize: "0.875rem" }}>
          {[
            { label: "Email", value: user?.email },
            { label: "Email Verified", value: user?.emailVerified ? "✓ Verified" : "✗ Not Verified" },
            { label: "Account Status", value: user?.enabled ? "Active" : "Disabled" },
            { label: "Auth Provider", value: user?.provider || "local" },
            { label: "Member Since", value: formatDate(user?.createdAt) },
            { label: "Last Login", value: formatDate(user?.lastLoginAt) },
          ].map(({ label, value }) => (
            <div key={label}>
              <div style={{ color: "var(--text-muted)", fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: "0.25rem" }}>{label}</div>
              <div style={{ color: "var(--text-primary)", fontWeight: 500 }}>{value}</div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
