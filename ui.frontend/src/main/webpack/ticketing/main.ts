import './main.scss';
import { initTicketDetail } from './detail';
import { initTicketForm } from './form';
import { initTicketList } from './list';
import { getTicketIdFromUrl, switchView } from './view';

function initializeTicketingUi(): void {
    console.log('ticketing UI loaded');
    initTicketForm();
    switchView();

    if (getTicketIdFromUrl()) {
        initTicketDetail();
    } else {
        initTicketList();
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeTicketingUi);
} else {
    initializeTicketingUi();
}
