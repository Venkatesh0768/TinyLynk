"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import api from "@/lib/api";
import Cookies from "js-cookie";
import toast from "react-hot-toast";

export default function Navbar() {
  const { user, clearAuth } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();

  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const isVendor = user?.roles?.includes("ROLE_VENDOR");

  async function handleLogout() {
    try {
      const stored = localStorage.getItem("auth-storage");
      const refreshToken = stored ? JSON.parse(stored)?.state?.refreshToken : null;
      if (refreshToken) {
        await api.post("/api/auth/logout", { refreshToken });
      }
    } catch { /* ignore */ }
    Cookies.remove("access-token");
    Cookies.remove("user-roles");
    clearAuth();
    toast.success("Signed out successfully");
    router.push("/login");
  }

  const initials = user
    ? `${user.firstName?.[0] || ""}${user.lastName?.[0] || ""}`.toUpperCase()
    : "U";

  const getRoleBadgeClass = () => {
    if (isAdmin) return "role-badge admin";
    if (isVendor) return "role-badge vendor";
    return "role-badge user";
  };

  const getPrimaryRole = () => {
    if (isAdmin) return "Admin";
    if (isVendor) return "Vendor";
    return "User";
  };

  return (
    <nav className="navbar">
      <Link href="/dashboard" className="navbar-logo">
        <div className="navbar-logo-icon">A</div>
        <span>AppName</span>
      </Link>

      <div className="navbar-nav">
        <Link
          href="/dashboard"
          className={`navbar-link${pathname === "/dashboard" ? " active" : ""}`}
        >
          Dashboard
        </Link>
        {isVendor && (
          <Link
            href="/vendor"
            className={`navbar-link${pathname.startsWith("/vendor") ? " active" : ""}`}
          >
            Vendor
          </Link>
        )}
        {isAdmin && (
          <Link
            href="/admin"
            className={`navbar-link${pathname.startsWith("/admin") ? " active" : ""}`}
          >
            Admin
          </Link>
        )}
      </div>

      <div className="navbar-spacer" />

      <div className="navbar-user">
        <span className={getRoleBadgeClass()}>{getPrimaryRole()}</span>
        <div className="navbar-avatar">
          {user?.profileImageUrl ? (
            <img src={user.profileImageUrl} alt={user.firstName} />
          ) : (
            initials
          )}
        </div>
        <button
          onClick={handleLogout}
          className="btn btn-secondary btn-sm"
          style={{ whiteSpace: "nowrap" }}
        >
          Sign out
        </button>
      </div>
    </nav>
  );
}
