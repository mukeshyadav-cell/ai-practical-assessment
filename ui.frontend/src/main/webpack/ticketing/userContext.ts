const DEFAULT_CREATED_BY = 'agent-1';

/**
 * Resolves createdBy for write operations (tickets, comments).
 * TODO: Replace default with authenticated AEM user when a client-safe user id is exposed on the page.
 */
export function resolveCreatedBy(): string {
    const bodyUser = document.body?.dataset?.currentUser?.trim();
    if (bodyUser) {
        return bodyUser;
    }

    return DEFAULT_CREATED_BY;
}
