"use client";

export default function VendorPage() {
  return (
    <>
      <div className="page-title">Vendor Portal</div>
      <p className="page-subtitle">Welcome to your vendor workspace.</p>

      <div className="stats-grid">
        {[
          { label: "Active Listings", value: "—" },
          { label: "Total Orders", value: "—" },
          { label: "Revenue", value: "—" },
          { label: "Rating", value: "—" },
        ].map(({ label, value }) => (
          <div key={label} className="stat-card">
            <div className="stat-value">{value}</div>
            <div className="stat-label">{label}</div>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <div className="card-title">Vendor Hub</div>
        </div>
        <p style={{ color: "var(--text-secondary)", fontSize: "0.9rem", lineHeight: 1.7 }}>
          This is your vendor workspace. You have access to this area because you hold the <span className="role-badge vendor" style={{ display: "inline-flex" }}>VENDOR</span> or <span className="role-badge admin" style={{ display: "inline-flex" }}>ADMIN</span> role.
          <br /><br />
          Connect your inventory management, orders, and analytics here. This area is only visible to authorized vendor accounts.
        </p>
      </div>
    </>
  );
}
