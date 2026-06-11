import "server-only";

import { cookies } from "next/headers";

/**
 * Wolfios (server) — minimal fetch helper for Server Components.
 *
 * Forwards the browser's cookies (HTTP-only session cookies included) to the
 * backend so authenticated endpoints work during SSR. Routes through the same
 * Caddy reverse proxy the browser uses, so dev and prod both "just work" from
 * inside the docker network (caddy → backend container in prod, caddy →
 * host.docker.internal in dev).
 *
 * Override with BACKEND_BASE_URL when running Next outside Docker.
 *
 * Use this from Server Components only. For Client Components, use
 * `wolfios` from "@/lib/wolfios".
 */

const BACKEND_BASE_URL = process.env.BACKEND_BASE_URL;

interface ServerFetchOptions extends RequestInit {
  // Disables Next's fetch cache for the call. Defaults to "no-store" because
  // every server request is per-user.
  cache?: RequestCache;
}

export async function wolfiosServer<T>(
  path: string,
  options: ServerFetchOptions = {},
): Promise<{ data: T; status: number }> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  const response = await fetch(`${BACKEND_BASE_URL}${path}`, {
    cache: "no-store",
    ...options,
    headers: {
      ...(cookieHeader ? { cookie: cookieHeader } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error = new Error(`wolfiosServer ${path} failed: ${response.status}`);
    (error as Error & { status?: number }).status = response.status;
    throw error;
  }

  const data = (await response.json()) as T;

  return { data, status: response.status };
}
