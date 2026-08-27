import './main.scss';
import { initTicketDetail } from './detail';
import { initTicketForm } from './form';
import { initTicketList } from './list';
import { showPendingToast } from './toast';
import { loadCurrentUser } from './userContext';
import { getTicketIdFromUrl, switchView } from './view';

async function initializeTicketingUi(): Promise<void> {
    console.log('ticketing UI loaded');
    await loadCurrentUser();
    initTicketForm();
    switchView();
    showPendingToast();

    if (getTicketIdFromUrl()) {
        initTicketDetail();
    } else {
        initTicketList();
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        void initializeTicketingUi();
    });
} else {
    void initializeTicketingUi();
}
