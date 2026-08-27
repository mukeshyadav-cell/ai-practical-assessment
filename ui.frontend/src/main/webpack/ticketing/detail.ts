import {
    addComment,
    ApiRequestError,
    changeStatus,
    Comment,
    fetchComments,
    fetchTicket,
    fetchUsers,
    reassignTicket,
    Ticket,
    TicketNotFoundError,
    User
} from './api';
import {
    createElement,
    formatDateTime,
    priorityToModifier,
    showLoadingMessage,
    showPanelMessage,
    statusToModifier
} from './dom';
import { isTerminalTicketStatus, openTicketFormEdit } from './form';
import { getAllowedNextStatuses } from './transitions';
import { resolveCreatedBy } from './userContext';
import { getTicketIdFromUrl, navigateToTicketList } from './view';

function buildMetaItem(label: string, value: string): HTMLElement {
    const item = createElement('div', 'ticket-detail__meta-item');
    item.appendChild(createElement('dt', 'ticket-detail__meta-label', label));
    item.appendChild(createElement('dd', 'ticket-detail__meta-value', value));
    return item;
}

function buildBackLink(): HTMLElement {
    const back = createElement('button', 'ticket-detail__back', 'Back to list');
    back.type = 'button';
    back.addEventListener('click', () => navigateToTicketList());
    return back;
}

export function renderTicketDetail(container: HTMLElement, ticket: Ticket): void {
    container.replaceChildren();

    const panel = createElement('article', 'ticket-detail__panel');
    panel.appendChild(buildBackLink());

    const header = createElement('header', 'ticket-detail__header');
    header.appendChild(createElement('p', 'ticket-detail__id', ticket.id));

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
    header.appendChild(createElement('h2', 'ticket-detail__title', ticket.title));
    panel.appendChild(header);

    const description = createElement('div', 'ticket-detail__description');
    description.appendChild(createElement('h3', 'ticket-detail__section-title', 'Description'));
    const descriptionBody = createElement('p', 'ticket-detail__description-text');
    descriptionBody.textContent = ticket.description;
    description.appendChild(descriptionBody);
    panel.appendChild(description);

    const assignee = ticket.assignedTo?.trim() ? ticket.assignedTo : 'Unassigned';
    const meta = createElement('dl', 'ticket-detail__meta');
    meta.appendChild(buildMetaItem('Assignee', assignee));
    meta.appendChild(buildMetaItem('Created by', ticket.createdBy));
    meta.appendChild(buildMetaItem('Created', formatDateTime(ticket.createdAt)));
    meta.appendChild(buildMetaItem('Updated', formatDateTime(ticket.updatedAt)));
    panel.appendChild(meta);

    container.appendChild(panel);
}

export function renderComments(container: HTMLElement, comments: Comment[]): void {
    container.replaceChildren();

    const section = createElement('section', 'ticket-comments');
    section.appendChild(createElement('h2', 'ticket-comments__heading', 'Comments'));

    if (comments.length === 0) {
        section.appendChild(createElement('p', 'ticket-comments__empty', 'No comments yet.'));
        container.appendChild(section);
        return;
    }

    const list = createElement('ol', 'ticket-comments__list');
    comments.forEach((comment) => {
        const item = createElement('li', 'ticket-comment');
        const meta = createElement('p', 'ticket-comment__meta');
        meta.textContent = `${comment.createdBy} · ${formatDateTime(comment.createdAt)}`;
        item.appendChild(meta);
        const message = createElement('p', 'ticket-comment__message');
        message.textContent = comment.message;
        item.appendChild(message);
        list.appendChild(item);
    });

    section.appendChild(list);
    container.appendChild(section);
}

function renderAddCommentForm(
    container: HTMLElement,
    ticketId: string,
    onCommentAdded: () => Promise<void>
): void {
    container.replaceChildren();

    const form = createElement('div', 'ticket-add-comment');
    form.appendChild(createElement('h3', 'ticket-add-comment__heading', 'Add Comment'));

    const errorElement = createElement('p', 'ticket-add-comment__error');
    errorElement.hidden = true;
    form.appendChild(errorElement);

    const textarea = document.createElement('textarea');
    textarea.className = 'ticket-add-comment__textarea';
    textarea.rows = 3;
    textarea.placeholder = 'Write a comment…';
    textarea.setAttribute('aria-label', 'Comment message');
    form.appendChild(textarea);

    const addButton = createElement(
        'button',
        'ticket-add-comment__button',
        'Add Comment'
    ) as HTMLButtonElement;
    addButton.type = 'button';

    const showError = (message: string): void => {
        errorElement.textContent = message;
        errorElement.hidden = false;
    };

    const clearError = (): void => {
        errorElement.textContent = '';
        errorElement.hidden = true;
    };

    const submitComment = async (): Promise<void> => {
        const message = textarea.value.trim();
        if (!message) {
            showError('Comment message is required.');
            return;
        }

        clearError();
        addButton.disabled = true;
        addButton.textContent = 'Adding…';

        try {
            await addComment(ticketId, {
                message,
                createdBy: resolveCreatedBy()
            });
            textarea.value = '';
            await onCommentAdded();
        } catch (error: unknown) {
            console.error('Failed to add comment', error);
            if (error instanceof ApiRequestError) {
                showError(error.message);
            } else {
                showError('Unable to add comment. Please try again.');
            }
        } finally {
            addButton.disabled = false;
            addButton.textContent = 'Add Comment';
        }
    };

    addButton.addEventListener('click', () => {
        void submitComment();
    });

    form.appendChild(addButton);
    container.appendChild(form);
}

function renderStatusControl(
    container: HTMLElement,
    ticket: Ticket,
    onStatusChanged: () => void
): void {
    container.replaceChildren();

    const panel = createElement('div', 'ticket-status-control');
    panel.appendChild(createElement('h3', 'ticket-status-control__heading', 'Change Status'));

    const allowedNext = getAllowedNextStatuses(ticket.status);

    if (allowedNext.length === 0) {
        panel.appendChild(
            createElement(
                'p',
                'ticket-status-control__note',
                `This ticket is ${ticket.status} — no further status changes.`
            )
        );
        container.appendChild(panel);
        return;
    }

    const errorElement = createElement('p', 'ticket-status-control__error');
    errorElement.hidden = true;
    panel.appendChild(errorElement);

    const fieldRow = createElement('div', 'ticket-status-control__row');

    const selectLabel = createElement('label', 'ticket-status-control__label', 'New status');
    selectLabel.htmlFor = 'ticket-status-select';
    fieldRow.appendChild(selectLabel);

    const select = document.createElement('select');
    select.id = 'ticket-status-select';
    select.className = 'ticket-status-control__select';
    allowedNext.forEach((status, index) => {
        const option = document.createElement('option');
        option.value = status;
        option.textContent = status;
        if (index === 0) {
            option.selected = true;
        }
        select.appendChild(option);
    });
    fieldRow.appendChild(select);
    panel.appendChild(fieldRow);

    const changeButton = createElement(
        'button',
        'ticket-status-control__button',
        'Change Status'
    ) as HTMLButtonElement;
    changeButton.type = 'button';

    const showError = (message: string): void => {
        errorElement.textContent = message;
        errorElement.hidden = false;
    };

    const clearError = (): void => {
        errorElement.textContent = '';
        errorElement.hidden = true;
    };

    changeButton.addEventListener('click', () => {
        void (async () => {
            const newStatus = select.value.trim();
            if (!newStatus) {
                showError('Select a new status.');
                return;
            }

            clearError();
            changeButton.disabled = true;
            select.disabled = true;
            changeButton.textContent = 'Changing…';

            try {
                await changeStatus(ticket.id, newStatus);
                onStatusChanged();
            } catch (error: unknown) {
                console.error('Failed to change status', error);
                if (error instanceof ApiRequestError) {
                    showError(error.message);
                } else {
                    showError('Unable to change status. Please try again.');
                }
            } finally {
                changeButton.disabled = false;
                select.disabled = false;
                changeButton.textContent = 'Change Status';
            }
        })();
    });

    panel.appendChild(changeButton);
    container.appendChild(panel);
}

/**
 * Populates assignee select with seeded users. MVP reassign requires a valid user — no Unassigned option.
 */
function populateReassignUserOptions(
    select: HTMLSelectElement,
    users: User[],
    currentAssignee?: string | null
): void {
    select.replaceChildren();

    const selectedId = currentAssignee?.trim() || '';
    users.forEach((user) => {
        const option = document.createElement('option');
        option.value = user.userId;
        option.textContent = `${user.displayName} (${user.userId})`;
        if (selectedId === user.userId) {
            option.selected = true;
        }
        select.appendChild(option);
    });
}

async function renderReassignControl(
    container: HTMLElement,
    ticket: Ticket,
    onReassigned: () => void
): Promise<void> {
    container.replaceChildren();

    const panel = createElement('div', 'ticket-reassign-control');
    panel.appendChild(createElement('h3', 'ticket-reassign-control__heading', 'Reassign'));

    if (isTerminalTicketStatus(ticket.status)) {
        panel.appendChild(
            createElement(
                'p',
                'ticket-reassign-control__note',
                'Closed/Cancelled tickets cannot be reassigned.'
            )
        );
        container.appendChild(panel);
        return;
    }

    const errorElement = createElement('p', 'ticket-reassign-control__error');
    errorElement.hidden = true;
    panel.appendChild(errorElement);

    const loadingMessage = createElement('p', 'ticket-reassign-control__loading', 'Loading assignees…');
    panel.appendChild(loadingMessage);
    container.appendChild(panel);

    let users: User[] = [];
    try {
        users = await fetchUsers();
    } catch (error: unknown) {
        console.error('Failed to load users for reassign', error);
        loadingMessage.remove();
        errorElement.textContent = 'Unable to load assignees. Please try again later.';
        errorElement.hidden = false;
        return;
    }

    loadingMessage.remove();

    const fieldRow = createElement('div', 'ticket-reassign-control__row');
    const selectLabel = createElement('label', 'ticket-reassign-control__label', 'Assignee');
    selectLabel.htmlFor = 'ticket-reassign-select';
    fieldRow.appendChild(selectLabel);

    const select = document.createElement('select');
    select.id = 'ticket-reassign-select';
    select.className = 'ticket-reassign-control__select';
    populateReassignUserOptions(select, users, ticket.assignedTo);
    fieldRow.appendChild(select);
    panel.appendChild(fieldRow);

    const reassignButton = createElement(
        'button',
        'ticket-reassign-control__button',
        'Reassign'
    ) as HTMLButtonElement;
    reassignButton.type = 'button';

    const showError = (message: string): void => {
        errorElement.textContent = message;
        errorElement.hidden = false;
    };

    const clearError = (): void => {
        errorElement.textContent = '';
        errorElement.hidden = true;
    };

    reassignButton.addEventListener('click', () => {
        void (async () => {
            const assignedTo = select.value.trim();
            if (!assignedTo) {
                showError('Select an assignee.');
                return;
            }

            clearError();
            reassignButton.disabled = true;
            select.disabled = true;
            reassignButton.textContent = 'Reassigning…';

            try {
                await reassignTicket(ticket.id, assignedTo);
                onReassigned();
            } catch (error: unknown) {
                console.error('Failed to reassign ticket', error);
                if (error instanceof ApiRequestError) {
                    showError(error.message);
                } else {
                    showError('Unable to reassign ticket. Please try again.');
                }
            } finally {
                reassignButton.disabled = false;
                select.disabled = false;
                reassignButton.textContent = 'Reassign';
            }
        })();
    });

    panel.appendChild(reassignButton);
}

function renderNotFound(container: HTMLElement): void {
    container.replaceChildren();
    const panel = createElement('div', 'ticket-detail__not-found');
    panel.appendChild(createElement('p', 'ticket-panel__message ticket-panel__empty', 'Ticket not found.'));
    panel.appendChild(buildBackLink());
    container.appendChild(panel);
}

function renderError(container: HTMLElement, message: string): void {
    container.replaceChildren();
    const panel = createElement('div', 'ticket-detail__error');
    panel.appendChild(createElement('p', 'ticket-panel__message ticket-panel__error', message));
    panel.appendChild(buildBackLink());
    container.appendChild(panel);
}

function renderEditControls(container: HTMLElement, ticket: Ticket, onEdited: () => void): void {
    container.replaceChildren();

    if (isTerminalTicketStatus(ticket.status)) {
        container.appendChild(
            createElement(
                'p',
                'ticket-detail__edit-note',
                'Closed/Cancelled tickets cannot be edited.'
            )
        );
        return;
    }

    const editButton = createElement('button', 'ticket-detail__edit', 'Edit');
    editButton.type = 'button';
    editButton.addEventListener('click', () => {
        void openTicketFormEdit(ticket, onEdited);
    });
    container.appendChild(editButton);
}

/**
 * Loads and renders ticket detail and comments when `?id=` is present in the URL.
 */
export function initTicketDetail(): void {
    const ticketId = getTicketIdFromUrl();
    const detailRoot = document.getElementById('ticket-detail-root');
    const commentsRoot = document.getElementById('ticket-comments-root');
    const editRoot = document.getElementById('ticket-edit-root');
    const statusRoot = document.getElementById('ticket-status-root');
    const reassignRoot = document.getElementById('ticket-reassign-root');
    const addCommentRoot = document.getElementById('ticket-add-comment-root');

    if (!ticketId || !detailRoot || !commentsRoot) {
        console.warn('Ticket detail mount points not found; skipping detail initialization');
        return;
    }

    showLoadingMessage(detailRoot, 'Loading ticket…');
    showLoadingMessage(commentsRoot, 'Loading comments…');

    const reloadComments = async (): Promise<void> => {
        const comments = await fetchComments(ticketId);
        renderComments(commentsRoot, comments);
    };

    const loadDetail = async (): Promise<void> => {
        try {
            const ticket = await fetchTicket(ticketId);
            renderTicketDetail(detailRoot, ticket);

            if (editRoot) {
                renderEditControls(editRoot, ticket, () => {
                    void loadDetail();
                });
            }

            if (statusRoot) {
                renderStatusControl(statusRoot, ticket, () => {
                    void loadDetail();
                });
            }

            if (reassignRoot) {
                void renderReassignControl(reassignRoot, ticket, () => {
                    void loadDetail();
                });
            }

            if (addCommentRoot) {
                renderAddCommentForm(addCommentRoot, ticketId, reloadComments);
            }

            try {
                await reloadComments();
            } catch (commentError: unknown) {
                console.error('Failed to load comments', commentError);
                showPanelMessage(
                    commentsRoot,
                    'ticket-panel__error',
                    'Unable to load comments. Please try again later.'
                );
            }
        } catch (error: unknown) {
            if (error instanceof TicketNotFoundError) {
                renderNotFound(detailRoot);
                commentsRoot.replaceChildren();
                if (addCommentRoot) {
                    addCommentRoot.replaceChildren();
                }
                if (statusRoot) {
                    statusRoot.replaceChildren();
                }
                if (reassignRoot) {
                    reassignRoot.replaceChildren();
                }
                return;
            }

            console.error('Failed to load ticket', error);
            renderError(detailRoot, 'Unable to load ticket. Please try again later.');
            commentsRoot.replaceChildren();
            if (addCommentRoot) {
                addCommentRoot.replaceChildren();
            }
            if (statusRoot) {
                statusRoot.replaceChildren();
            }
            if (reassignRoot) {
                reassignRoot.replaceChildren();
            }
        }
    };

    void loadDetail();
}
