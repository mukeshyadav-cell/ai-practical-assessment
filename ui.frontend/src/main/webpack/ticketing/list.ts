import { fetchTickets, Ticket } from './api';
import {
    createElement,
    formatShortDate,
    priorityToModifier,
    showLoadingMessage,
    showPanelMessage,
    statusToModifier
} from './dom';
import { openTicketFormCreate } from './form';
import { sortTickets } from './sort';
import { navigateToTicketDetail } from './view';

export interface ListState {
    q: string;
    status: string;
    priority: string;
    sort: string;
}

const STATUS_OPTIONS = [
    { value: '', label: 'All' },
    { value: 'Open', label: 'Open' },
    { value: 'In Progress', label: 'In Progress' },
    { value: 'Resolved', label: 'Resolved' },
    { value: 'Closed', label: 'Closed' },
    { value: 'Cancelled', label: 'Cancelled' }
];

const PRIORITY_OPTIONS = [
    { value: '', label: 'All Priorities' },
    { value: 'P1', label: 'P1' },
    { value: 'P2', label: 'P2' },
    { value: 'P3', label: 'P3' },
    { value: 'P4', label: 'P4' }
];

const SORT_OPTIONS = [
    { value: 'newest', label: 'Newest' },
    { value: 'oldest', label: 'Oldest' },
    { value: 'recentlyUpdated', label: 'Recently Updated' },
    { value: 'priority', label: 'Priority' },
    { value: 'ticketId', label: 'Ticket ID' }
];

const DEFAULT_LIST_STATE: ListState = {
    q: '',
    status: '',
    priority: '',
    sort: 'newest'
};

const DEBOUNCE_MS = 300;

let currentState: ListState = { ...DEFAULT_LIST_STATE };

function debounce<T extends (...args: never[]) => void>(fn: T, delay: number): T {
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    return ((...args: Parameters<T>) => {
        if (timeoutId !== null) {
            clearTimeout(timeoutId);
        }
        timeoutId = setTimeout(() => fn(...args), delay);
    }) as T;
}

function showListMessage(container: HTMLElement, className: string, message: string): void {
    showPanelMessage(container, `ticket-list__message ${className}`, message);
}

function hasActiveFilters(state: ListState): boolean {
    return Boolean(state.q.trim() || state.status.trim() || state.priority.trim());
}

function emptyListMessage(state: ListState): string {
    return hasActiveFilters(state) ? 'No tickets match your filters.' : 'No tickets yet.';
}

function formatResultSummary(count: number): string {
    return count === 1 ? 'Showing 1 ticket' : `Showing ${count} tickets`;
}

/**
 * Client-side priority filter — the API has no ?priority= param (status/q are server-side).
 */
function filterTicketsByPriority(tickets: Ticket[], priority: string): Ticket[] {
    const normalizedPriority = priority.trim();
    if (!normalizedPriority) {
        return tickets;
    }

    return tickets.filter((ticket) => ticket.priority === normalizedPriority);
}

function buildTicketCard(ticket: Ticket): HTMLElement {
    const card = createElement('article', 'ticket-card');
    card.setAttribute('role', 'button');
    card.setAttribute('tabindex', '0');
    card.setAttribute('aria-label', `View ticket ${ticket.id}`);

    const header = createElement('div', 'ticket-card__header');
    header.appendChild(createElement('span', 'ticket-card__id', ticket.id));

    const badges = createElement('div', 'ticket-card__badges');
    badges.appendChild(
        createElement(
            'span',
            `ticket-badge ticket-badge--status-${statusToModifier(ticket.status)}`,
            ticket.status
        )
    );
    badges.appendChild(
        createElement(
            'span',
            `ticket-badge ticket-badge--priority-${priorityToModifier(ticket.priority)}`,
            ticket.priority
        )
    );
    header.appendChild(badges);

    card.appendChild(header);
    card.appendChild(createElement('h2', 'ticket-card__title', ticket.title));

    const assignee = ticket.assignedTo?.trim() ? ticket.assignedTo : 'Unassigned';
    const metaText = `Assignee: ${assignee} · Created: ${formatShortDate(ticket.createdAt)}`;
    card.appendChild(createElement('p', 'ticket-card__meta', metaText));

    const openDetail = (): void => navigateToTicketDetail(ticket.id);
    card.addEventListener('click', openDetail);
    card.addEventListener('keydown', (event: KeyboardEvent) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            openDetail();
        }
    });

    return card;
}

export function renderTicketList(
    container: HTMLElement,
    tickets: Ticket[],
    emptyMessage = 'No tickets found.'
): void {
    container.replaceChildren();

    if (tickets.length === 0) {
        showListMessage(container, 'ticket-list__empty', emptyMessage);
        return;
    }

    const grid = createElement('div', 'ticket-list__grid');
    tickets.forEach((ticket) => grid.appendChild(buildTicketCard(ticket)));
    container.appendChild(grid);
}

function populateSelectOptions(
    select: HTMLSelectElement,
    options: Array<{ value: string; label: string }>
): void {
    if (select.options.length > 0) {
        return;
    }

    options.forEach((option) => {
        const optionElement = document.createElement('option');
        optionElement.value = option.value;
        optionElement.textContent = option.label;
        select.appendChild(optionElement);
    });
}

function syncDomFromState(
    searchInput: HTMLInputElement,
    statusSelect: HTMLSelectElement,
    prioritySelect: HTMLSelectElement,
    sortSelect: HTMLSelectElement,
    state: ListState
): void {
    searchInput.value = state.q;
    statusSelect.value = state.status;
    prioritySelect.value = state.priority;
    sortSelect.value = state.sort;
}

function updateResultSummary(summary: HTMLElement, count: number): void {
    summary.textContent = formatResultSummary(count);
}

function updateClearFiltersVisibility(clearButton: HTMLButtonElement, state: ListState): void {
    clearButton.hidden = !hasActiveFilters(state);
}

/**
 * Wires search, status/priority filters, sort, and ticket list rendering when the list view is active.
 */
export function initTicketList(): void {
    const container = document.getElementById('ticket-list-root');
    const searchInput = document.getElementById('ticket-search') as HTMLInputElement | null;
    const statusSelect = document.getElementById('ticket-status-filter') as HTMLSelectElement | null;
    const prioritySelect = document.getElementById('ticket-priority-filter') as HTMLSelectElement | null;
    const sortSelect = document.getElementById('ticket-sort') as HTMLSelectElement | null;
    const summary = document.getElementById('ticket-list-summary');
    const clearFiltersButton = document.getElementById('ticket-list-clear-filters') as HTMLButtonElement | null;

    if (
        !container
        || !searchInput
        || !statusSelect
        || !prioritySelect
        || !sortSelect
        || !summary
        || !clearFiltersButton
    ) {
        console.warn('Ticket list mount points not found; skipping list initialization');
        return;
    }

    currentState = { ...DEFAULT_LIST_STATE };

    populateSelectOptions(statusSelect, STATUS_OPTIONS);
    populateSelectOptions(prioritySelect, PRIORITY_OPTIONS);
    populateSelectOptions(sortSelect, SORT_OPTIONS);
    syncDomFromState(searchInput, statusSelect, prioritySelect, sortSelect, currentState);

    const listHeading = document.getElementById('ticket-list-heading');
    if (listHeading && !document.getElementById('ticket-list-new')) {
        const newTicketButton = createElement('button', 'ticket-list__new', '+ New Ticket');
        newTicketButton.id = 'ticket-list-new';
        newTicketButton.type = 'button';
        newTicketButton.addEventListener('click', () => {
            void openTicketFormCreate(() => refreshList());
        });
        listHeading.insertAdjacentElement('afterend', newTicketButton);
    }

    const refreshList = async (): Promise<void> => {
        showLoadingMessage(container, 'Loading tickets…');
        summary.textContent = 'Loading…';

        try {
            const tickets = await fetchTickets({
                q: currentState.q || undefined,
                status: currentState.status || undefined
            });
            const priorityFilteredTickets = filterTicketsByPriority(tickets, currentState.priority);
            const sortedTickets = sortTickets(priorityFilteredTickets, currentState.sort);

            renderTicketList(container, sortedTickets, emptyListMessage(currentState));
            updateResultSummary(summary, sortedTickets.length);
            updateClearFiltersVisibility(clearFiltersButton, currentState);
        } catch (error: unknown) {
            console.error('Failed to load tickets', error);
            summary.textContent = '';
            clearFiltersButton.hidden = true;
            showListMessage(
                container,
                'ticket-list__error',
                'Unable to load tickets. Please try again later.'
            );
        }
    };

    const debouncedRefresh = debounce(refreshList, DEBOUNCE_MS);

    searchInput.addEventListener('input', () => {
        currentState = {
            ...currentState,
            q: searchInput.value.trim()
        };
        debouncedRefresh();
    });

    statusSelect.addEventListener('change', () => {
        currentState = {
            ...currentState,
            status: statusSelect.value.trim()
        };
        void refreshList();
    });

    prioritySelect.addEventListener('change', () => {
        currentState = {
            ...currentState,
            priority: prioritySelect.value.trim()
        };
        void refreshList();
    });

    sortSelect.addEventListener('change', () => {
        currentState = {
            ...currentState,
            sort: sortSelect.value.trim() || DEFAULT_LIST_STATE.sort
        };
        void refreshList();
    });

    clearFiltersButton.addEventListener('click', () => {
        currentState = { ...DEFAULT_LIST_STATE };
        syncDomFromState(searchInput, statusSelect, prioritySelect, sortSelect, currentState);
        void refreshList();
    });

    void refreshList();
}
