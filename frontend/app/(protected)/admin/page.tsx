"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";
import { User } from "@/lib/types";

interface PageMeta { totalElements: number; totalPages: number; number: number; }
interface UsersPage { content: User[]; } 

export default function AdminPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [meta, setMeta] = useState<PageMeta>({ totalElements: 0, totalPages: 1, number: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [page, setPage] = useState(0);

  const fetchUsers = async (p = 0) => {
    setLoading(true);
    try {
      const res = await api.get<UsersPage & PageMeta>(`/api/admin/users?page=${p}&size=15&sort=createdAt,desc`);
      const data = res.data as unknown as { content: User[]; totalElements: number; totalPages: number; number: number };
      setUsers(data.content);
      setMeta({ totalElements: data.totalElements, totalPages: data.totalPages, number: data.number });
    } catch {
      setError("Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchUsers(page); }, [page]);

  async function toggleStatus(userId: number, currentEnabled: boolean) {
    setActionLoading(userId);
    try {
      await api.patch(`/api/admin/users/${userId}/status?enabled=${!currentEnabled}`);
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, enabled: !currentEnabled } : u))
      );
    } catch { setError("Failed to update status"); }
    finally { setActionLoading(null); }
  }

  async function promoteToAdmin(userId: number, currentRoles: string[]) {
    setActionLoading(userId);
    const newRoles = currentRoles.includes("ROLE_ADMIN")
      ? currentRoles.filter((r) => r !== "ROLE_ADMIN")
      : [...currentRoles, "ROLE_ADMIN"];
    try {
      await api.patch(`/api/admin/users/${userId}/roles`, { roles: newRoles });
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, roles: newRoles as User["roles"] } : u))
      );
    } catch { setError("Failed to update roles"); }
    finally { setActionLoading(null); }
  }

  const formatDate = (iso?: string) => iso ? new Date(iso).toLocaleDateString() : "—";
  const getRoleLabel = (role: string) => role.replace("ROLE_", "");
  const getRoleClass = (role: string) => {
    if (role === "ROLE_ADMIN") return "role-badge admin";
    if (role === "ROLE_VENDOR") return "role-badge vendor";
    return "role-badge user";
  };

  return (
    <>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "0.35rem" }}>
        <div className="page-title">User Management</div>
        <div style={{ fontSize: "0.85rem", color: "var(--text-secondary)" }}>
          {meta.totalElements} total users
        </div>
      </div>
      <p className="page-subtitle">Manage accounts, roles, and access permissions.</p>

      {error && (
        <div className="alert alert-error" style={{ marginBottom: "1rem" }}>
          <span>✕</span><span>{error}</span>
        </div>
      )}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Roles</th>
              <th>Provider</th>
              <th>Status</th>
              <th>Joined</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} style={{ textAlign: "center", padding: "2rem" }}>
                  <div className="spinner" style={{ margin: "0 auto" }} />
                </td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: "center", color: "var(--text-secondary)", padding: "2rem" }}>
                  No users found
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{user.firstName} {user.lastName}</div>
                    <div style={{ fontSize: "0.8rem", color: "var(--text-secondary)" }}>{user.email}</div>
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: "0.3rem", flexWrap: "wrap" }}>
                      {user.roles.map((r) => (
                        <span key={r} className={getRoleClass(r)}>{getRoleLabel(r)}</span>
                      ))}
                    </div>
                  </td>
                  <td>
                    <span className="role-badge user" style={{ textTransform: "capitalize" }}>
                      {user.provider || "local"}
                    </span>
                  </td>
                  <td>
                    <span style={{
                      fontSize: "0.78rem",
                      fontWeight: 600,
                      color: user.enabled ? "var(--success)" : "var(--error)",
                    }}>
                      {user.enabled ? "● Active" : "● Disabled"}
                    </span>
                  </td>
                  <td style={{ color: "var(--text-secondary)", fontSize: "0.82rem" }}>
                    {formatDate(user.createdAt)}
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: "0.4rem" }}>
                      <button
                        className={`btn btn-sm ${user.enabled ? "btn-danger" : "btn-secondary"}`}
                        onClick={() => toggleStatus(user.id, user.enabled)}
                        disabled={actionLoading === user.id}
                      >
                        {actionLoading === user.id ? <span className="spinner" style={{width:12,height:12}} /> 
                          : user.enabled ? "Disable" : "Enable"}
                      </button>
                      <button
                        className="btn btn-sm btn-secondary"
                        onClick={() => promoteToAdmin(user.id, user.roles as string[])}
                        disabled={actionLoading === user.id}
                        title={user.roles.includes("ROLE_ADMIN") ? "Remove Admin" : "Make Admin"}
                      >
                        {user.roles.includes("ROLE_ADMIN") ? "− Admin" : "+ Admin"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {meta.totalPages > 1 && (
        <div style={{ display: "flex", justifyContent: "center", gap: "0.5rem", marginTop: "1.25rem" }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setPage((p) => p - 1)}
            disabled={page === 0}
          >
            ← Prev
          </button>
          <span style={{ padding: "0.4rem 0.75rem", fontSize: "0.875rem", color: "var(--text-secondary)" }}>
            Page {page + 1} of {meta.totalPages}
          </span>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setPage((p) => p + 1)}
            disabled={page >= meta.totalPages - 1}
          >
            Next →
          </button>
        </div>
      )}
    </>
  );
}
