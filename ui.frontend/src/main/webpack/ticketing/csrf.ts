const CSRF_TOKEN_URL = '/libs/granite/csrf/token.json';

interface CsrfTokenResponse {
    token?: string;
}

/**
 * Fetches an AEM Granite CSRF token for mutating requests (POST, PUT, DELETE).
 * Required when the session is authenticated — CSRFFilter rejects empty tokens with 403.
 */
export async function fetchCsrfToken(): Promise<string> {
    const response = await fetch(CSRF_TOKEN_URL, {
        headers: {
            Accept: 'application/json'
        }
    });

    if (!response.ok) {
        throw new Error(`Failed to fetch CSRF token: HTTP ${response.status}`);
    }

    const body = await response.json() as CsrfTokenResponse;
    const token = body.token?.trim();

    if (!token) {
        throw new Error('CSRF token response missing token');
    }

    return token;
}

/**
 * Performs fetch with CSRF-Token header for AEM-protected mutating requests.
 */
export async function fetchWithCsrf(url: string, options: RequestInit): Promise<Response> {
    const csrfToken = await fetchCsrfToken();
    const headers = new Headers(options.headers);
    headers.set('CSRF-Token', csrfToken);

    return fetch(url, {
        ...options,
        headers
    });
}
