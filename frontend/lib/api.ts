// lib/api.ts — Axios instance with JWT attach + automatic token refresh

import axios, { AxiosInstance, InternalAxiosRequestConfig } from "axios";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:5000";

const api: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

// ─── Request interceptor: attach Bearer token ──────────────────────────────
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (typeof window !== "undefined") {
    try {
      const stored = localStorage.getItem("auth-storage");
      if (stored) {
        const parsed = JSON.parse(stored);
        const token = parsed?.state?.accessToken;
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      }
    } catch {
      // ignore parse errors
    }
  }
  return config;
});

// ─── Response interceptor: 401 → refresh token → retry ────────────────────
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  failedQueue = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        let refreshToken: string | null = null;
        const stored = localStorage.getItem("auth-storage");
        if (stored) {
          const parsed = JSON.parse(stored);
          refreshToken = parsed?.state?.refreshToken;
        }

        if (!refreshToken) throw new Error("No refresh token");

        const res = await axios.post(`${API_URL}/api/auth/refresh-token`, {
          refreshToken,
        });

        const newAccessToken: string = res.data.accessToken;

        // Update store
        const stored2 = localStorage.getItem("auth-storage");
        if (stored2) {
          const parsed = JSON.parse(stored2);
          parsed.state.accessToken = newAccessToken;
          localStorage.setItem("auth-storage", JSON.stringify(parsed));
        }

        processQueue(null, newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (err) {
        processQueue(err, null);
        // Clear stored auth and redirect to login
        localStorage.removeItem("auth-storage");
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
