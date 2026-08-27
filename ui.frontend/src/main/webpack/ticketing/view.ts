/**
 * Toggles list vs detail views based on the `id` query parameter.
 */
export function switchView(): void {
    const ticketId = getTicketIdFromUrl();
    const listView = document.getElementById('ticket-list-view');
    const detailView = document.getElementById('ticket-detail-view');

    if (listView) {
        listView.hidden = Boolean(ticketId);
    }

    if (detailView) {
        detailView.hidden = !ticketId;
    }
}

export function getTicketIdFromUrl(): string | null {
    return new URLSearchParams(window.location.search).get('id');
}

export function navigateToTicketDetail(ticketId: string): void {
    const params = new URLSearchParams(window.location.search);
    params.set('id', ticketId);
    window.location.search = params.toString();
}

export function navigateToTicketList(): void {
    const params = new URLSearchParams(window.location.search);
    params.delete('id');
    const query = params.toString();
    window.location.search = query ? `?${query}` : '';
}
