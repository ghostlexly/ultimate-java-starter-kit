import { AxiosError } from 'axios';
import type { FieldValues, Path, UseFormReturn } from 'react-hook-form';

interface ApiViolation {
  /** Backend field name (e.g. "firstName", "phone"). */
  path?: string;
  message?: string;
  code?: string;
}

interface ApiErrorBody {
  message?: string;
  code?: string;
  violations?: ApiViolation[];
}

/**
 * Extracts the API error body from an unknown error, if it originates from Axios.
 */
function getApiErrorBody(error: unknown): ApiErrorBody | undefined {
  if (error instanceof AxiosError) {
    return error.response?.data as ApiErrorBody | undefined;
  }

  return undefined;
}

/**
 * Extracts a map of field → error message from an API error's violations.
 * Returns an empty object if the error has no field-level violations.
 */
export function getFieldErrors(error: unknown): Record<string, string> {
  const body = getApiErrorBody(error);
  const result: Record<string, string> = {};

  if (!body?.violations?.length) {
    return result;
  }

  for (const violation of body.violations) {
    if (violation.path && violation.message) {
      result[violation.path] = violation.message;
    }
  }

  return result;
}

/**
 * Returns true if the API error contains any field-level violations.
 */
export function hasFieldErrors(error: unknown): boolean {
  return Object.keys(getFieldErrors(error)).length > 0;
}

/**
 * Extracts a human-readable top-level error message from an API error.
 * Falls back to the provided default if none can be found.
 */
export function getErrorMessage(error: unknown, fallback: string): string {
  const body = getApiErrorBody(error);

  if (body?.message) {
    return body.message;
  }

  return fallback;
}

/**
 * Applies backend validation violations onto a react-hook-form instance.
 * Each field-level violation is registered via `form.setError`, and the first
 * one is focused so the user lands on it. Call this inside a mutation's
 * `onError` once `hasFieldErrors(error)` is true.
 */
export function handleApiErrors<TValues extends FieldValues>(
  form: UseFormReturn<TValues>,
  error: unknown,
): void {
  const entries = Object.entries(getFieldErrors(error));

  entries.forEach(([field, message], index) => {
    form.setError(
      field as Path<TValues>,
      { type: 'server', message },
      // Auto-focus the first field with an error so the user lands on it
      { shouldFocus: index === 0 },
    );
  });
}
