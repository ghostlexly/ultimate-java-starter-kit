/**
 * Post-login destination logic, shared by the login page server guard and the
 * client login flow. Safe to import from both Server and Client Components.
 */

/**
 * Default landing page per role after login. Roles missing from this map
 * (e.g. CUSTOMER for now) fall back to the home page. Extend it as new
 * role-specific areas are created.
 */
const ROLE_HOME_PATHS: Record<string, string> = {
  ADMIN: '/admin-area',
};

const DEFAULT_HOME_PATH = '/';

/**
 * Only allows internal absolute paths (e.g. `/reservation`) as a post-login
 * destination. Anything else (external URLs, protocol-relative `//evil.com`,
 * backslash tricks) returns `null` to prevent open redirects.
 */
export function sanitizeRedirectPath(path: string | undefined): string | null {
  const isInternalPath =
    typeof path === 'string' && path.startsWith('/') && !path.startsWith('//') && !path.includes('\\');

  if (!isInternalPath) {
    return null;
  }

  return path;
}

/**
 * Resolves where a user should land after login.
 *
 * An explicit (sanitized) `?redirect` destination always wins, whatever the
 * role. Otherwise the user goes to their role's home page.
 */
export function getPostLoginPath(role: string, redirectPath: string | null): string {
  if (redirectPath) {
    return redirectPath;
  }

  return ROLE_HOME_PATHS[role] ?? DEFAULT_HOME_PATH;
}
