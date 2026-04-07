import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

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
  adapter: ['fetch', 'xhr', 'http'],
  timeout: 30000,
});

wolfios.interceptors.request.use(async (request: InternalAxiosRequestConfig) => {
  // disable subsequent setting the default header by Axios
  request.headers.set('User-Agent', false);

  return request;
});

wolfios.interceptors.response.use(
  async (response: AxiosResponse) => {
    return response;
  },
  async (error: AxiosError) => {
    if (
      error.response?.status === 401 &&
      error.config &&
      !error.config.url?.includes('/auth/refresh') &&
      !error.config.url?.includes('/auth/logout')
    ) {
      try {
        // Refresh the JWT tokens
        await wolfios.post('/api/auth/refresh', {});

        // Retry the original request and return the response
        // (error.config contains the original request config (url, method, data, headers, etc.))
        return wolfios(error.config);
      } catch (refreshError) {
        console.error('Failed to refresh JWT tokens.', refreshError);

        // Clear cookies
        await wolfios.post('/api/auth/logout', {}).catch();

        // Refresh the page to clear the session
        globalThis.location.reload();
        throw error;
      }
    }

    throw error; // important to propagate the error
  },
);

export { wolfios };
