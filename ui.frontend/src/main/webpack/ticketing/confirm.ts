import { createElement } from './dom';

/**
 * Accessible confirmation dialog before terminal status transitions (Closed, Cancelled).
 */
export function confirmTerminalStatusChange(ticketId: string, status: string): Promise<boolean> {
    return new Promise((resolve) => {
        const host = createElement('div', 'ticket-confirm');
        const overlay = createElement('div', 'ticket-confirm__overlay');
        const dialog = createElement('div', 'ticket-confirm__dialog');
        dialog.setAttribute('role', 'alertdialog');
        dialog.setAttribute('aria-modal', 'true');
        dialog.setAttribute('aria-labelledby', 'ticket-confirm-title');

        const title = createElement('h2', 'ticket-confirm__title', 'Confirm status change');
        title.id = 'ticket-confirm-title';

        const message = createElement('p', 'ticket-confirm__message');
        message.textContent =
            `Move ticket ${ticketId} to ${status}? This is a terminal state and cannot be undone.`;

        const actions = createElement('div', 'ticket-confirm__actions');
        const cancelButton = createElement(
            'button',
            'ticket-confirm__button ticket-confirm__button--secondary',
            'Cancel'
        );
        cancelButton.type = 'button';

        const confirmButton = createElement(
            'button',
            'ticket-confirm__button ticket-confirm__button--primary',
            'Confirm'
        );
        confirmButton.type = 'button';

        let settled = false;
        const close = (confirmed: boolean): void => {
            if (settled) {
                return;
            }
            settled = true;
            document.removeEventListener('keydown', onKeyDown);
            host.remove();
            resolve(confirmed);
        };

        const onKeyDown = (event: KeyboardEvent): void => {
            if (event.key === 'Escape') {
                event.preventDefault();
                close(false);
            }
        };

        overlay.addEventListener('click', () => close(false));
        cancelButton.addEventListener('click', () => close(false));
        confirmButton.addEventListener('click', () => close(true));
        document.addEventListener('keydown', onKeyDown);

        dialog.appendChild(title);
        dialog.appendChild(message);
        actions.appendChild(cancelButton);
        actions.appendChild(confirmButton);
        dialog.appendChild(actions);
        host.appendChild(overlay);
        host.appendChild(dialog);
        document.body.appendChild(host);

        cancelButton.focus();
    });
}
