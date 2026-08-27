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
import { navigateToTicketDetail } from './view';

const STATUS_OPTIONS = [
    { value: '', label: 'All' },
    { value: 'Open', label: 'Open' },
    { value: 'In Progress', label: 'In Progress' },
    { value: 'Resolved', label: 'Resolved' },
    { value: 'Closed', label: 'Closed' },
    { value: 'Cancelled', label: 'Cancelled' }
];

const DEBOUNCE_MS = 300;

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

export function renderTicketList(container: HTMLElement, tickets: Ticket[]): void {
    container.replaceChildren();

    if (tickets.length === 0) {
        showListMessage(container, 'ticket-list__empty', 'No tickets found.');
        return;
    }

    const grid = createElement('div', 'ticket-list__grid');
    tickets.forEach((ticket) => grid.appendChild(buildTicketCard(ticket)));
    container.appendChild(grid);
}

function populateStatusFilter(select: HTMLSelectElement): void {
    if (select.options.length > 0) {
        return;
    }

    STATUS_OPTIONS.forEach((option) => {
        const optionElement = document.createElement('option');
        optionElement.value = option.value;
        optionElement.textContent = option.label;
        select.appendChild(optionElement);
    });
}

function readFilters(searchInput: HTMLInputElement, statusSelect: HTMLSelectElement): {
    q: string;
    status: string;
} {
    return {
        q: searchInput.value.trim(),
        status: statusSelect.value.trim()
    };
}

/**
 * Wires search, status filter, and ticket list rendering when the list view is active.
 */
export function initTicketList(): void {
    const container = document.getElementById('ticket-list-root');
    const searchInput = document.getElementById('ticket-search') as HTMLInputElement | null;
    const statusSelect = document.getElementById('ticket-status-filter') as HTMLSelectElement | null;

    if (!container || !searchInput || !statusSelect) {
        console.warn('Ticket list mount points not found; skipping list initialization');
        return;
    }

    populateStatusFilter(statusSelect);

    const listHeading = document.getElementById('ticket-list-heading');
    if (listHeading && !document.getElementById('ticket-list-new')) {
        const newTicketButton = createElement('button', 'ticket-list__new', '+ New Ticket');
        newTicketButton.id = 'ticket-list-new';
        newTicketButton.type = 'button';
        newTicketButton.addEventListener('click', () => {
            void openTicketFormCreate(() => loadTickets());
        });
        listHeading.insertAdjacentElement('afterend', newTicketButton);
    }

    const loadTickets = async (): Promise<void> => {
        const { q, status } = readFilters(searchInput, statusSelect);
        showLoadingMessage(container, 'Loading tickets…');

        try {
            const tickets = await fetchTickets({
                q: q || undefined,
                status: status || undefined
            });
            renderTicketList(container, tickets);
        } catch (error: unknown) {
            console.error('Failed to load tickets', error);
            showListMessage(
                container,
                'ticket-list__error',
                'Unable to load tickets. Please try again later.'
            );
        }
    };

    const debouncedLoad = debounce(loadTickets, DEBOUNCE_MS);

    searchInput.addEventListener('input', () => debouncedLoad());
    statusSelect.addEventListener('change', () => loadTickets());

    void loadTickets();
}
