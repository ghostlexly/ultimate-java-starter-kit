import 'server-only';

import { wolfiosServer } from '@/lib/wolfios.server';
import type { MeResponse } from '@/types/auth';

/**
 * Fetches the current authenticated user on the server, forwarding the
 * browser's session cookie. Returns null when the user is not authenticated
 * or when the backend call fails — never throws, so layouts can stay simple.
 */
export async function getServerUser(): Promise<MeResponse | null> {
  try {
    const { data } = await wolfiosServer<MeResponse>('/api/auth/me');

    return data;
  } catch {
    return null;
  }
}
