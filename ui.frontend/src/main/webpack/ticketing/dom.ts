export function createElement<K extends keyof HTMLElementTagNameMap>(
    tag: K,
    className?: string,
    text?: string
): HTMLElementTagNameMap[K] {
    const element = document.createElement(tag);
    if (className) {
        element.className = className;
    }
    if (text !== undefined) {
        element.textContent = text;
    }
    return element;
}

export function formatShortDate(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
        return iso;
    }

    return date.toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

export function formatDateTime(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
        return iso;
    }

    return date.toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

export function statusToModifier(status: string): string {
    const modifiers: Record<string, string> = {
        Open: 'open',
        'In Progress': 'in-progress',
        Resolved: 'resolved',
        Closed: 'closed',
        Cancelled: 'cancelled'
    };

    return modifiers[status] || 'unknown';
}

export function priorityToModifier(priority: string): string {
    return priority.toLowerCase();
}

export function showPanelMessage(container: HTMLElement, className: string, message: string): void {
    container.replaceChildren();
    const messageElement = createElement('p', `ticket-panel__message ${className}`, message);
    container.appendChild(messageElement);
}

export function showLoadingMessage(container: HTMLElement, message: string): void {
    showPanelMessage(container, 'ticket-panel__loading', message);
}
