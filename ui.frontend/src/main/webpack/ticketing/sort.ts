import { Ticket } from './api';

export type TicketSortKey = 'newest' | 'oldest' | 'recentlyUpdated' | 'priority' | 'ticketId';

const DEFAULT_SORT_KEY: TicketSortKey = 'newest';

const PRIORITY_RANK: Record<string, number> = {
    P1: 1,
    P2: 2,
    P3: 3,
    P4: 4
};

function parseIsoTimestamp(value: string | null | undefined): number | null {
    if (!value?.trim()) {
        return null;
    }

    const timestamp = Date.parse(value.trim());
    return Number.isNaN(timestamp) ? null : timestamp;
}

/**
 * Descending date compare; blank/invalid dates sort last.
 */
function compareDatesDesc(left: string, right: string): number {
    const leftTs = parseIsoTimestamp(left);
    const rightTs = parseIsoTimestamp(right);

    if (leftTs === null && rightTs === null) {
        return 0;
    }
    if (leftTs === null) {
        return 1;
    }
    if (rightTs === null) {
        return -1;
    }

    return rightTs - leftTs;
}

/**
 * Ascending date compare; blank/invalid dates sort last.
 */
function compareDatesAsc(left: string, right: string): number {
    const leftTs = parseIsoTimestamp(left);
    const rightTs = parseIsoTimestamp(right);

    if (leftTs === null && rightTs === null) {
        return 0;
    }
    if (leftTs === null) {
        return 1;
    }
    if (rightTs === null) {
        return -1;
    }

    return leftTs - rightTs;
}

function priorityRank(priority: string): number {
    const normalized = priority?.trim().toUpperCase();
    return PRIORITY_RANK[normalized] ?? Number.MAX_SAFE_INTEGER;
}

function extractTicketNumber(id: string): number | null {
    const match = /^TKT-(\d+)$/i.exec(id.trim());
    if (!match) {
        return null;
    }

    const numericPart = Number.parseInt(match[1], 10);
    return Number.isNaN(numericPart) ? null : numericPart;
}

function compareTicketId(left: string, right: string): number {
    const leftNumber = extractTicketNumber(left);
    const rightNumber = extractTicketNumber(right);

    if (leftNumber !== null && rightNumber !== null) {
        return leftNumber - rightNumber;
    }

    return left.localeCompare(right);
}

function resolveSortKey(sortKey: string): TicketSortKey {
    switch (sortKey) {
        case 'oldest':
        case 'recentlyUpdated':
        case 'priority':
        case 'ticketId':
            return sortKey;
        case 'newest':
        default:
            return DEFAULT_SORT_KEY;
    }
}

/**
 * Returns a new array sorted by the given key. Does not mutate the input.
 */
export function sortTickets(tickets: Ticket[], sortKey: string): Ticket[] {
    const resolvedKey = resolveSortKey(sortKey);
    const sorted = [...tickets];

    sorted.sort((left, right) => {
        switch (resolvedKey) {
            case 'oldest':
                return compareDatesAsc(left.createdAt, right.createdAt);
            case 'recentlyUpdated':
                return compareDatesDesc(left.updatedAt, right.updatedAt);
            case 'priority':
                return priorityRank(left.priority) - priorityRank(right.priority);
            case 'ticketId':
                return compareTicketId(left.id, right.id);
            case 'newest':
            default:
                return compareDatesDesc(left.createdAt, right.createdAt);
        }
    });

    return sorted;
}
