import { fetchWithCsrf } from './csrf';

const TICKETS_ENDPOINT = '/bin/api/v1/tickets';
const USERS_ENDPOINT = '/bin/api/v1/users';
const ME_ENDPOINT = '/bin/api/v1/me';

export interface Ticket {
    id: string;
    title: string;
    description: string;
    priority: string;
    status: string;
    assignedTo: string | null;
    createdBy: string;
    createdAt: string;
    updatedAt: string;
}

export interface Comment {
    id: string;
    ticketId: string;
    message: string;
    createdBy: string;
    createdAt: string;
}

export interface User {
    userId: string;
    displayName: string;
    email: string;
}

export interface CurrentUser {
    userId: string;
    displayName: string;
    email: string;
}

export interface CreateTicketPayload {
    title: string;
    description: string;
    priority: string;
    assignedTo?: string;
    createdBy: string;
}

export interface UpdateTicketPayload {
    title: string;
    description: string;
    priority: string;
}

export interface AddCommentPayload {
    message: string;
    createdBy: string;
}

export interface ApiErrorBody {
    error: string;
    code: string;
}

export class ApiRequestError extends Error {
    readonly status: number;
    readonly code: string;

    constructor(status: number, message: string, code: string) {
        super(message);
        this.name = 'ApiRequestError';
        this.status = status;
        this.code = code;
    }
}

export class TicketNotFoundError extends Error {
    readonly ticketId: string;

    constructor(ticketId: string) {
        super(`Ticket not found: ${ticketId}`);
        this.name = 'TicketNotFoundError';
        this.ticketId = ticketId;
    }
}

async function readJson(response: Response): Promise<unknown> {
    return response.json();
}

async function parseApiResponse(response: Response): Promise<unknown> {
    if (response.ok) {
        return readJson(response);
    }

    let message = `Request failed: HTTP ${response.status}`;
    let code = 'INTERNAL_ERROR';

    try {
        const body = await readJson(response) as ApiErrorBody;
        if (typeof body.error === 'string' && body.error.trim()) {
            message = body.error;
        }
        if (typeof body.code === 'string' && body.code.trim()) {
            code = body.code;
        }
    } catch (parseError: unknown) {
        console.error('Failed to parse API error response', parseError);
    }

    throw new ApiRequestError(response.status, message, code);
}

/**
 * Fetches tickets from the REST API with optional search and status filters.
 *
 * @param params optional query filters (`q` for title search, `status` for status filter)
 * @returns parsed ticket array
 * @throws when the response is not successful or not a JSON array
 */
export async function fetchTickets(params?: { q?: string; status?: string }): Promise<Ticket[]> {
    const searchParams = new URLSearchParams();

    if (params?.q?.trim()) {
        searchParams.set('q', params.q.trim());
    }

    if (params?.status?.trim()) {
        searchParams.set('status', params.status.trim());
    }

    const query = searchParams.toString();
    const url = query ? `${TICKETS_ENDPOINT}?${query}` : TICKETS_ENDPOINT;

    const response = await fetch(url, {
        headers: {
            Accept: 'application/json'
        }
    });

    if (!response.ok) {
        throw new Error(`Failed to fetch tickets: HTTP ${response.status}`);
    }

    const payload: unknown = await readJson(response);
    if (!Array.isArray(payload)) {
        throw new Error('Ticket response was not an array');
    }

    return payload as Ticket[];
}

/**
 * Fetches a single ticket by id.
 *
 * @param id ticket id (e.g. TKT-1001)
 * @throws {@link TicketNotFoundError} when the ticket does not exist (HTTP 404)
 */
export async function fetchTicket(id: string): Promise<Ticket> {
    const url = `${TICKETS_ENDPOINT}/${encodeURIComponent(id)}`;
    const response = await fetch(url, {
        headers: {
            Accept: 'application/json'
        }
    });

    if (response.status === 404) {
        throw new TicketNotFoundError(id);
    }

    if (!response.ok) {
        throw new Error(`Failed to fetch ticket: HTTP ${response.status}`);
    }

    return await readJson(response) as Ticket;
}

/**
 * Fetches comments for a ticket, ordered ascending by createdAt (server-side).
 *
 * @param ticketId parent ticket id
 * @throws {@link TicketNotFoundError} when the ticket does not exist (HTTP 404)
 */
export async function fetchComments(ticketId: string): Promise<Comment[]> {
    const url = `${TICKETS_ENDPOINT}/${encodeURIComponent(ticketId)}/comments`;
    const response = await fetch(url, {
        headers: {
            Accept: 'application/json'
        }
    });

    if (response.status === 404) {
        throw new TicketNotFoundError(ticketId);
    }

    if (!response.ok) {
        throw new Error(`Failed to fetch comments: HTTP ${response.status}`);
    }

    const payload: unknown = await readJson(response);
    if (!Array.isArray(payload)) {
        throw new Error('Comment response was not an array');
    }

    return payload as Comment[];
}

/**
 * Fetches assignable users for the assignee dropdown.
 */
export async function fetchUsers(): Promise<User[]> {
    const response = await fetch(USERS_ENDPOINT, {
        headers: {
            Accept: 'application/json'
        }
    });

    const payload: unknown = await parseApiResponse(response);
    if (!Array.isArray(payload)) {
        throw new Error('User response was not an array');
    }

    return payload as User[];
}

/**
 * Fetches the authenticated AEM user for UI authorship (createdBy).
 */
export async function fetchCurrentUser(): Promise<CurrentUser> {
    const response = await fetch(ME_ENDPOINT, {
        headers: {
            Accept: 'application/json'
        }
    });

    const payload: unknown = await parseApiResponse(response);
    if (!payload || typeof payload !== 'object') {
        throw new Error('Current user response was not an object');
    }

    const body = payload as Record<string, unknown>;
    const userId = typeof body.userId === 'string' ? body.userId.trim() : '';
    if (!userId) {
        throw new Error('Current user response missing userId');
    }

    return {
        userId,
        displayName: typeof body.displayName === 'string' && body.displayName.trim()
            ? body.displayName.trim()
            : userId,
        email: typeof body.email === 'string' ? body.email.trim() : ''
    };
}

/**
 * Creates a ticket. Server sets status to Open; do not send status in the payload.
 */
export async function createTicket(payload: CreateTicketPayload): Promise<Ticket> {
    const body: Record<string, string> = {
        title: payload.title,
        description: payload.description,
        priority: payload.priority,
        createdBy: payload.createdBy
    };

    if (payload.assignedTo?.trim()) {
        body.assignedTo = payload.assignedTo.trim();
    }

    const response = await fetchWithCsrf(TICKETS_ENDPOINT, {
        method: 'POST',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
    });

    return await parseApiResponse(response) as Ticket;
}

/**
 * Updates editable ticket fields (title, description, priority only).
 */
export async function updateTicket(id: string, payload: UpdateTicketPayload): Promise<Ticket> {
    const url = `${TICKETS_ENDPOINT}/${encodeURIComponent(id)}`;
    const response = await fetchWithCsrf(url, {
        method: 'PUT',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            title: payload.title,
            description: payload.description,
            priority: payload.priority
        })
    });

    if (response.status === 404) {
        throw new TicketNotFoundError(id);
    }

    return await parseApiResponse(response) as Ticket;
}

/**
 * Adds a comment to a ticket.
 */
export async function addComment(ticketId: string, payload: AddCommentPayload): Promise<Comment> {
    const url = `${TICKETS_ENDPOINT}/${encodeURIComponent(ticketId)}/comments`;
    const response = await fetchWithCsrf(url, {
        method: 'POST',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            message: payload.message,
            createdBy: payload.createdBy
        })
    });

    if (response.status === 404) {
        throw new TicketNotFoundError(ticketId);
    }

    return await parseApiResponse(response) as Comment;
}

/**
 * Changes ticket status via the state machine endpoint.
 */
export async function changeStatus(id: string, status: string): Promise<Ticket> {
    const url = `${TICKETS_ENDPOINT}/${encodeURIComponent(id)}/status`;
    const response = await fetchWithCsrf(url, {
        method: 'PUT',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status })
    });

    if (response.status === 404) {
        throw new TicketNotFoundError(id);
    }

    return await parseApiResponse(response) as Ticket;
}

/**
 * Reassigns a ticket to an existing seeded user. MVP requires a non-blank assignee (no unassign).
 */
export async function reassignTicket(id: string, assignedTo: string): Promise<Ticket> {
    const url = `${TICKETS_ENDPOINT}/${encodeURIComponent(id)}/assignee`;
    const response = await fetchWithCsrf(url, {
        method: 'PUT',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ assignedTo })
    });

    if (response.status === 404) {
        throw new TicketNotFoundError(id);
    }

    return await parseApiResponse(response) as Ticket;
}
