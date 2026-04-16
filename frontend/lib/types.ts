// lib/types.ts — Shared TypeScript interfaces

export type RoleType = "ROLE_USER" | "ROLE_ADMIN" | "ROLE_VENDOR";

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  enabled: boolean;
  provider: string;
  profileImageUrl?: string;
  lastLoginAt?: string;
  createdAt?: string;
  roles: RoleType[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data?: T;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface OTPPayload {
  email: string;
  otp: string;
}

export interface ResetPasswordPayload {
  email: string;
  otp: string;
  newPassword: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
