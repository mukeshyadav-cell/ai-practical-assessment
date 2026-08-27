/**
 * Client-side transition map — mirrors com.mysite.core.statemachine.TicketStateMachine.
 * Keep in sync. The server is the source of truth and rejects invalid transitions (409).
 */
export const ALLOWED_TRANSITIONS: Record<string, string[]> = {
    'Open': ['In Progress', 'Cancelled'],
    'In Progress': ['Resolved', 'Cancelled'],
    'Resolved': ['Closed'],
    'Closed': [],
    'Cancelled': []
};

export function getAllowedNextStatuses(currentStatus: string): string[] {
    return ALLOWED_TRANSITIONS[currentStatus] ?? [];
}
