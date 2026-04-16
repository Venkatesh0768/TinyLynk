// middleware.ts — Next.js edge middleware for RBAC route protection

import { NextRequest, NextResponse } from "next/server";

const PUBLIC_PATHS = [
  "/login",
  "/register",
  "/verify-otp",
  "/forgot-password",
  "/reset-password",
  "/oauth2",
  "/api",
  "/_next",
  "/favicon.ico",
];

const ADMIN_PATHS = ["/admin"];
const VENDOR_PATHS = ["/vendor"];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Skip public paths
  if (PUBLIC_PATHS.some((p) => pathname.startsWith(p))) {
    return NextResponse.next();
  }

  // Read auth from cookies (set by login page after successful login)
  const token = request.cookies.get("access-token")?.value;
  const rolesRaw = request.cookies.get("user-roles")?.value;

  // Not authenticated: redirect to login
  if (!token) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("callbackUrl", pathname);
    return NextResponse.redirect(loginUrl);
  }

  // Authenticated but trying to visit root — redirect to dashboard
  if (pathname === "/") {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  const roles: string[] = rolesRaw ? JSON.parse(rolesRaw) : [];

  // Admin route protection
  if (ADMIN_PATHS.some((p) => pathname.startsWith(p))) {
    if (!roles.includes("ROLE_ADMIN")) {
      return NextResponse.redirect(new URL("/403", request.url));
    }
  }

  // Vendor route protection
  if (VENDOR_PATHS.some((p) => pathname.startsWith(p))) {
    if (!roles.includes("ROLE_ADMIN") && !roles.includes("ROLE_VENDOR")) {
      return NextResponse.redirect(new URL("/403", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|public/).*)",
  ],
};
