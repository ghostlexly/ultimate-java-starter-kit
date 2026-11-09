import "server-only";

import { wolfiosServer, WolfiosServerError } from "@/lib/wolfios.server";
import type { MeResponse } from "@/types/auth";

/**
 * Fetches the current authenticated user on the server, forwarding the
 * browser's session cookie.
 *
 * - Returns the user when the session is valid.
 * - Returns `null` only when the backend explicitly rejected the session
 *   (401/403) — the user really is logged out.
 * - Rethrows anything else (5xx, Caddy's 503, network error): the backend is
 *   unavailable, NOT the user logged out. Layouts must not redirect to signin
 *   in that case — let the error reach the nearest `error.tsx` instead.
 */
export async function getServerUser(): Promise<MeResponse | null> {
  try {
    const { data } = await wolfiosServer<MeResponse>("/api/auth/me");

    return data;
  } catch (error) {
    const isUnauthenticated =
      error instanceof WolfiosServerError &&
      (error.status === 401 || error.status === 403);

    if (isUnauthenticated) {
      return null;
    }

    throw error;
  }
}
