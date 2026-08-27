import { CurrentUser, fetchCurrentUser } from './api';

const FALLBACK_CREATED_BY = 'unknown-user';

let currentUser: CurrentUser | null = null;
let loadPromise: Promise<CurrentUser> | null = null;

function updateCurrentUserDisplay(user: CurrentUser): void {
    const display = document.getElementById('ticket-current-user');
    if (display) {
        display.textContent = `Logged in as ${user.displayName}`;
    }
}

/**
 * Loads and caches the authenticated AEM user from GET /bin/api/v1/me.
 * Safe to call multiple times; subsequent calls reuse the cached value.
 */
export async function loadCurrentUser(): Promise<CurrentUser> {
    if (currentUser) {
        return currentUser;
    }

    if (loadPromise) {
        return loadPromise;
    }

    loadPromise = (async (): Promise<CurrentUser> => {
        try {
            const user = await fetchCurrentUser();
            currentUser = user;
            updateCurrentUserDisplay(user);
            return user;
        } catch (error: unknown) {
            console.warn('Failed to load current user; using fallback for createdBy', error);
            const fallback: CurrentUser = {
                userId: FALLBACK_CREATED_BY,
                displayName: FALLBACK_CREATED_BY,
                email: ''
            };
            currentUser = fallback;
            updateCurrentUserDisplay(fallback);
            return fallback;
        } finally {
            loadPromise = null;
        }
    })();

    return loadPromise;
}

/**
 * Resolves createdBy for write operations (tickets, comments).
 * Call {@link loadCurrentUser} during app init before using this.
 */
export function resolveCreatedBy(): string {
    if (!currentUser?.userId?.trim()) {
        console.warn('resolveCreatedBy called before current user loaded; using fallback');
        return FALLBACK_CREATED_BY;
    }

    return currentUser.userId;
}

export function getCurrentUser(): CurrentUser | null {
    return currentUser;
}
