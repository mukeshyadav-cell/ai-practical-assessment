import { ApiRequestError } from './api';

export type ToastType = 'success' | 'error';

const TOAST_AUTO_HIDE_MS = 3500;
const TOAST_QUEUE_KEY = 'assessment.ticketing.pendingToast';

function ensureToastContainer(): HTMLElement {
    let container = document.getElementById('ticket-toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'ticket-toast-container';
        container.className = 'ticket-toast-container';
        container.setAttribute('aria-live', 'polite');
        container.setAttribute('aria-atomic', 'false');
        document.body.appendChild(container);
    }

    return container;
}

/**
 * Shows a dismissible toast that auto-hides after a few seconds. Multiple toasts stack.
 */
export function showToast(message: string, type: ToastType): void {
    const container = ensureToastContainer();
    const toast = document.createElement('div');
    toast.className = `ticket-toast ticket-toast--${type}`;
    toast.setAttribute('role', 'status');

    const text = document.createElement('p');
    text.className = 'ticket-toast__message';
    text.textContent = message;

    const dismiss = document.createElement('button');
    dismiss.type = 'button';
    dismiss.className = 'ticket-toast__dismiss';
    dismiss.setAttribute('aria-label', 'Dismiss notification');
    dismiss.textContent = '×';

    const removeToast = (): void => {
        toast.remove();
    };

    dismiss.addEventListener('click', removeToast);
    toast.appendChild(text);
    toast.appendChild(dismiss);
    container.appendChild(toast);

    window.setTimeout(removeToast, TOAST_AUTO_HIDE_MS);
}

/**
 * Queues a toast for the next page load (e.g. after ticket create redirect).
 */
export function queueToast(message: string, type: ToastType): void {
    sessionStorage.setItem(TOAST_QUEUE_KEY, JSON.stringify({ message, type }));
}

/**
 * Displays a toast queued before a full page navigation, if any.
 */
export function showPendingToast(): void {
    const raw = sessionStorage.getItem(TOAST_QUEUE_KEY);
    if (!raw) {
        return;
    }

    sessionStorage.removeItem(TOAST_QUEUE_KEY);

    try {
        const parsed = JSON.parse(raw) as { message?: string; type?: ToastType };
        if (parsed.message && (parsed.type === 'success' || parsed.type === 'error')) {
            showToast(parsed.message, parsed.type);
        }
    } catch (error: unknown) {
        console.warn('Failed to read pending toast', error);
    }
}

export function showApiErrorToast(error: unknown, fallback: string): void {
    if (error instanceof ApiRequestError) {
        showToast(error.message, 'error');
        return;
    }

    showToast(fallback, 'error');
}
