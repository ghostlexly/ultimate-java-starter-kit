import { type NextRequest, NextResponse } from "next/server";

/**
 * Combined proxy (Next.js 16 replacement for middleware.ts) handling:
 *
 * 1. **i18n routing** — next-intl locale detection and redirects.
 * 2. **Server-side token refresh** — when the short-lived access-token cookie
 *    has expired (browser deleted it) but the long-lived refresh-token cookie
 *    is still present, we call the backend's refresh endpoint and set new
 *    cookies **before** the page renders. This way `getServerUser()` (and
 *    every other `wolfiosServer` call) always has a valid access token during
 *    SSR. Without this, the user would appear logged-out on every SSR render
 *    once the 15-minute access token expires, even though they have a valid
 *    7-day refresh token.
 */

const BACKEND_BASE_URL = process.env.BACKEND_BASE_URL;

const ACCESS_TOKEN_COOKIE = "lunisoft_access_token";
const REFRESH_TOKEN_COOKIE = "lunisoft_refresh_token";

// Match the backend's AuthConstants durations.
const ACCESS_TOKEN_MAX_AGE = 15 * 60; // 900 s
const REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 604 800 s

interface RefreshedTokens {
  accessToken: string;
  refreshToken: string;
}

/**
 * Calls the backend refresh endpoint when needed.
 * Returns the new token pair, or `null` when no refresh is needed/possible.
 */
async function refreshTokensIfNeeded(
  request: NextRequest,
): Promise<RefreshedTokens | null> {
  const hasAccessToken = request.cookies.has(ACCESS_TOKEN_COOKIE);
  const refreshToken = request.cookies.get(REFRESH_TOKEN_COOKIE);

  // Nothing to do: either the access token is still alive, or the user is
  // fully anonymous (no refresh token either).
  if (hasAccessToken || !refreshToken) {
    return null;
  }

  try {
    const refreshResponse = await fetch(
      `${BACKEND_BASE_URL}/api/auth/refresh`,
      {
        method: "POST",
        headers: {
          cookie: `${REFRESH_TOKEN_COOKIE}=${refreshToken.value}`,
          "Content-Type": "application/json",
        },
        body: "{}",
      },
    );

    if (!refreshResponse.ok) {
      // Refresh token is invalid / expired — let the page render as anonymous.
      return null;
    }

    return (await refreshResponse.json()) as RefreshedTokens;
  } catch {
    // Network error reaching the backend — proceed without refreshing.
    return null;
  }
}

export default async function proxy(request: NextRequest) {
  const refreshedTokens = await refreshTokensIfNeeded(request);

  const response = NextResponse.next();

  if (refreshedTokens) {
    const isSecure = process.env.NODE_ENV === "production";

    // `response.cookies.set()` sets the cookie on both:
    //   1. the outgoing Response → browser stores the refreshed tokens
    //   2. the forwarded Request  → Server Components see them via `cookies()`
    response.cookies.set(ACCESS_TOKEN_COOKIE, refreshedTokens.accessToken, {
      httpOnly: true,
      secure: isSecure,
      path: "/",
      maxAge: ACCESS_TOKEN_MAX_AGE,
    });
    response.cookies.set(REFRESH_TOKEN_COOKIE, refreshedTokens.refreshToken, {
      httpOnly: true,
      secure: isSecure,
      path: "/",
      maxAge: REFRESH_TOKEN_MAX_AGE,
    });
  }

  return response;
}

export const config = {
  // Match all pathnames except for
  // - … if they start with `/api`, `/trpc`, `/_next` or `/_vercel`
  // - … the ones containing a dot (e.g. `favicon.ico`)
  matcher: "/((?!api|trpc|_next|_vercel|.*\\..*).*)",
};
