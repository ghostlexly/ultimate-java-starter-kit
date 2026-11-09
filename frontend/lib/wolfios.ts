import type { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from "axios";
import axios from "axios";

/**
 * Wolfios - Axios instance for Client Components
 *
 * This instance handles automatic token refresh on 401 errors and works with browser cookies.
 * Use this for Client Components and client-side API calls.
 *
 * For Server Components, use `wolfiosServer` from "@/lib/wolfios/wolfios.server" instead.
 *
 * @example
 * ```tsx
 * 'use client';
 * import { wolfios } from "@/lib/wolfios/wolfios";
 *
 * export function MyComponent() {
 *   const fetchData = async () => {
 *     const data = await wolfios.get("/api/data").then(res => res.data);
 *   };
 * }
 * ```
 */
const wolfios = axios.create({
  adapter: ["fetch", "xhr", "http"],
  timeout: 30000,
  paramsSerializer: {
    indexes: null, // arrays serialize as status=A&status=B (no brackets)
  },
});

/**
 * True only when the refresh endpoint itself answered 401: the refresh token is
 * invalid or the session was revoked/expired server-side. Network errors,
 * timeouts and 5xx (backend down) return false — the session may still be valid.
 */
const isSessionInvalid = (refreshError: unknown): boolean => {
  return (
    axios.isAxiosError(refreshError) && refreshError.response?.status === 401
  );
};

/** Clears the session cookies (best effort) and reloads the page. */
const logoutAndReload = async (): Promise<void> => {
  await wolfios.post("/api/auth/logout", {}).catch(() => undefined);

  globalThis.location.reload();
};

wolfios.interceptors.request.use(
  async (request: InternalAxiosRequestConfig) => {
    // disable subsequent setting the default header by Axios
    request.headers.set("User-Agent", false);

    return request;
  },
);

wolfios.interceptors.response.use(
  async (response: AxiosResponse) => {
    return response;
  },
  async (error: AxiosError) => {
    // Original request config (url, method, data, headers, etc.)
    const originalRequest = error.config;
    const isUnauthorized = error.response?.status === 401;
    const isAuthEndpoint =
      originalRequest?.url?.includes("/auth/refresh") ||
      originalRequest?.url?.includes("/auth/logout");

    if (!isUnauthorized || !originalRequest || isAuthEndpoint) {
      throw error; // important to propagate the error
    }

    try {
      await wolfios.post("/api/auth/refresh", {});
    } catch (refreshError) {
      console.error("Failed to refresh JWT tokens.", refreshError);

      // Only a 401 from the refresh endpoint means the session is really over.
      // Otherwise (backend down, network issue) keep the cookies and just fail this request.
      if (isSessionInvalid(refreshError)) {
        await logoutAndReload();
      }

      throw error;
    }

    // Retry the original request with the fresh tokens
    return wolfios(originalRequest);
  },
);

export { wolfios };
