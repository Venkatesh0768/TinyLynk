import Link from "next/link";

export default function ForbiddenPage() {
  return (
    <div className="error-page">
      <div className="error-code">403</div>
      <h1 className="error-title">Access Denied</h1>
      <p className="error-subtitle">
        You don&apos;t have permission to view this page.
        This area requires elevated privileges.
      </p>
      <Link href="/dashboard" className="btn btn-primary" style={{ width: "auto" }}>
        ← Back to Dashboard
      </Link>
    </div>
  );
}
