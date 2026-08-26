import './main.scss';

const TICKETS_ENDPOINT = '/bin/api/v1/tickets';

function switchView(): void {
    const ticketId = new URLSearchParams(window.location.search).get('id');
    const listView = document.getElementById('ticket-list-view');
    const detailView = document.getElementById('ticket-detail-view');

    if (listView) {
        listView.hidden = Boolean(ticketId);
    }

    if (detailView) {
        detailView.hidden = !ticketId;
    }
}

async function logTicketCount(): Promise<void> {
    try {
        const response = await fetch(TICKETS_ENDPOINT, {
            headers: {
                Accept: 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Ticket request failed with HTTP ${response.status}`);
        }

        const payload: unknown = await response.json();
        if (!Array.isArray(payload)) {
            throw new Error('Ticket response was not an array');
        }

        console.log('ticket count', payload.length);
    } catch (error: unknown) {
        console.error('Unable to load ticket count', error);
    }
}

function initializeTicketingUi(): void {
    console.log('ticketing UI loaded');
    switchView();
    void logTicketCount();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeTicketingUi);
} else {
    initializeTicketingUi();
}
