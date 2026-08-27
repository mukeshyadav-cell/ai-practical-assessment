import {
    createTicket,
    fetchUsers,
    Ticket,
    updateTicket,
    User
} from './api';
import { createElement } from './dom';
import { queueToast, showApiErrorToast, showToast } from './toast';
import { resolveCreatedBy } from './userContext';

const PRIORITY_OPTIONS = ['P1', 'P2', 'P3', 'P4'];

type FormMode = 'create' | 'edit';

interface FormState {
    mode: FormMode;
    ticket?: Ticket;
    onSuccess?: () => void;
}

let formRoot: HTMLElement | null = null;
let overlay: HTMLElement | null = null;
let errorElement: HTMLElement | null = null;
let saveButton: HTMLButtonElement | null = null;
let titleInput: HTMLInputElement | null = null;
let descriptionInput: HTMLTextAreaElement | null = null;
let prioritySelect: HTMLSelectElement | null = null;
let assigneeSelect: HTMLSelectElement | null = null;
let assigneeField: HTMLElement | null = null;
let currentState: FormState | null = null;

function setFormVisible(visible: boolean): void {
    if (!formRoot) {
        return;
    }

    if (visible) {
        formRoot.hidden = false;
        formRoot.classList.add('ticket-form--open');
    } else {
        formRoot.hidden = true;
        formRoot.classList.remove('ticket-form--open');
        formRoot.replaceChildren();
        overlay = null;
        errorElement = null;
        saveButton = null;
        titleInput = null;
        descriptionInput = null;
        prioritySelect = null;
        assigneeSelect = null;
        assigneeField = null;
        currentState = null;
    }
}

function showFormError(message: string): void {
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.hidden = false;
    }
}

function clearFormError(): void {
    if (errorElement) {
        errorElement.textContent = '';
        errorElement.hidden = true;
    }
}

function validateForm(): string | null {
    if (!titleInput || !descriptionInput || !prioritySelect) {
        return 'Form is not ready.';
    }

    if (!titleInput.value.trim()) {
        return 'Title is required.';
    }

    if (!descriptionInput.value.trim()) {
        return 'Description is required.';
    }

    if (!prioritySelect.value.trim()) {
        return 'Priority is required.';
    }

    return null;
}

function populatePriorityOptions(select: HTMLSelectElement, selected?: string): void {
    select.replaceChildren();
    PRIORITY_OPTIONS.forEach((priority) => {
        const option = document.createElement('option');
        option.value = priority;
        option.textContent = priority;
        if (selected === priority) {
            option.selected = true;
        }
        select.appendChild(option);
    });
}

function populateAssigneeOptions(select: HTMLSelectElement, users: User[], selected?: string | null): void {
    select.replaceChildren();

    const unassigned = document.createElement('option');
    unassigned.value = '';
    unassigned.textContent = 'Unassigned';
    select.appendChild(unassigned);

    users.forEach((user) => {
        const option = document.createElement('option');
        option.value = user.userId;
        option.textContent = `${user.displayName} (${user.userId})`;
        if (selected === user.userId) {
            option.selected = true;
        }
        select.appendChild(option);
    });
}

function buildFormShell(mode: FormMode): void {
    if (!formRoot) {
        return;
    }

    formRoot.replaceChildren();
    overlay = createElement('div', 'ticket-form__overlay');
    overlay.addEventListener('click', () => closeTicketForm());

    const dialog = createElement('div', 'ticket-form__dialog', undefined);
    dialog.setAttribute('role', 'dialog');
    dialog.setAttribute('aria-modal', 'true');
    dialog.addEventListener('click', (event) => event.stopPropagation());

    const heading = createElement(
        'h2',
        'ticket-form__heading',
        mode === 'create' ? 'New ticket' : 'Edit ticket'
    );
    dialog.appendChild(heading);

    errorElement = createElement('p', 'ticket-form__error');
    errorElement.hidden = true;
    dialog.appendChild(errorElement);

    const form = createElement('form', 'ticket-form__fields');
    form.addEventListener('submit', (event) => {
        event.preventDefault();
        void submitTicketForm();
    });

    const titleLabel = createElement('label', 'ticket-form__label', 'Title');
    titleLabel.htmlFor = 'ticket-form-title';
    titleInput = document.createElement('input');
    titleInput.id = 'ticket-form-title';
    titleInput.name = 'title';
    titleInput.type = 'text';
    titleInput.required = true;
    titleInput.className = 'ticket-form__input';
    form.appendChild(titleLabel);
    form.appendChild(titleInput);

    const descriptionLabel = createElement('label', 'ticket-form__label', 'Description');
    descriptionLabel.htmlFor = 'ticket-form-description';
    descriptionInput = document.createElement('textarea');
    descriptionInput.id = 'ticket-form-description';
    descriptionInput.name = 'description';
    descriptionInput.required = true;
    descriptionInput.rows = 5;
    descriptionInput.className = 'ticket-form__textarea';
    form.appendChild(descriptionLabel);
    form.appendChild(descriptionInput);

    const priorityLabel = createElement('label', 'ticket-form__label', 'Priority');
    priorityLabel.htmlFor = 'ticket-form-priority';
    prioritySelect = document.createElement('select');
    prioritySelect.id = 'ticket-form-priority';
    prioritySelect.name = 'priority';
    prioritySelect.className = 'ticket-form__select';
    populatePriorityOptions(prioritySelect);
    form.appendChild(priorityLabel);
    form.appendChild(prioritySelect);

    assigneeField = createElement('div', 'ticket-form__assignee-field');
    const assigneeLabel = createElement('label', 'ticket-form__label', 'Assignee');
    assigneeLabel.htmlFor = 'ticket-form-assignee';
    assigneeSelect = document.createElement('select');
    assigneeSelect.id = 'ticket-form-assignee';
    assigneeSelect.name = 'assignedTo';
    assigneeSelect.className = 'ticket-form__select';
    assigneeField.appendChild(assigneeLabel);
    assigneeField.appendChild(assigneeSelect);
    form.appendChild(assigneeField);

    const actions = createElement('div', 'ticket-form__actions');
    const cancelButton = createElement('button', 'ticket-form__button ticket-form__button--secondary', 'Cancel');
    cancelButton.type = 'button';
    cancelButton.addEventListener('click', () => closeTicketForm());

    saveButton = createElement('button', 'ticket-form__button ticket-form__button--primary', 'Save') as HTMLButtonElement;
    saveButton.type = 'submit';
    actions.appendChild(cancelButton);
    actions.appendChild(saveButton);
    form.appendChild(actions);

    dialog.appendChild(form);

    formRoot.appendChild(overlay);
    formRoot.appendChild(dialog);
}

async function loadAssigneeOptions(selected?: string | null): Promise<void> {
    if (!assigneeSelect) {
        return;
    }

    try {
        const users = await fetchUsers();
        populateAssigneeOptions(assigneeSelect, users, selected);
    } catch (error: unknown) {
        console.error('Failed to load users for assignee dropdown', error);
        populateAssigneeOptions(assigneeSelect, [], selected);
        showFormError('Unable to load assignees. You can still save without changing assignee.');
    }
}

export function closeTicketForm(): void {
    setFormVisible(false);
}

export async function openTicketFormCreate(onSuccess?: () => void): Promise<void> {
    if (!formRoot) {
        console.warn('Ticket form root not found');
        return;
    }

    currentState = { mode: 'create', onSuccess };
    buildFormShell('create');

    if (assigneeField) {
        assigneeField.hidden = false;
    }

    clearFormError();
    setFormVisible(true);
    await loadAssigneeOptions();
}

export async function openTicketFormEdit(ticket: Ticket, onSuccess?: () => void): Promise<void> {
    if (!formRoot) {
        console.warn('Ticket form root not found');
        return;
    }

    currentState = { mode: 'edit', ticket, onSuccess };
    buildFormShell('edit');

    if (assigneeField) {
        assigneeField.hidden = true;
    }

    if (titleInput) {
        titleInput.value = ticket.title;
    }
    if (descriptionInput) {
        descriptionInput.value = ticket.description;
    }
    if (prioritySelect) {
        populatePriorityOptions(prioritySelect, ticket.priority);
    }

    clearFormError();
    setFormVisible(true);
}

async function submitTicketForm(): Promise<void> {
    if (!currentState || !saveButton) {
        return;
    }

    const validationError = validateForm();
    if (validationError) {
        showFormError(validationError);
        return;
    }

    clearFormError();
    saveButton.disabled = true;
    saveButton.textContent = 'Saving…';

    try {
        if (currentState.mode === 'create') {
            if (!titleInput || !descriptionInput || !prioritySelect) {
                return;
            }

            const assignedTo = assigneeSelect?.value.trim() || undefined;
            const ticket = await createTicket({
                title: titleInput.value.trim(),
                description: descriptionInput.value.trim(),
                priority: prioritySelect.value,
                assignedTo,
                createdBy: resolveCreatedBy()
            });

            closeTicketForm();
            queueToast(`Ticket ${ticket.id} created`, 'success');
            // Full page load so detail init runs and fetches the newly created ticket.
            const detailUrl = `${window.location.pathname}?id=${encodeURIComponent(ticket.id)}`;
            window.location.href = detailUrl;
            return;
        }

        if (!currentState.ticket || !titleInput || !descriptionInput || !prioritySelect) {
            return;
        }

        await updateTicket(currentState.ticket.id, {
            title: titleInput.value.trim(),
            description: descriptionInput.value.trim(),
            priority: prioritySelect.value
        });

        const onSuccess = currentState.onSuccess;
        closeTicketForm();
        showToast('Ticket updated', 'success');
        if (onSuccess) {
            onSuccess();
        }
    } catch (error: unknown) {
        console.error('Failed to save ticket', error);
        showApiErrorToast(error, 'Unable to save ticket. Please try again.');
    } finally {
        if (saveButton) {
            saveButton.disabled = false;
            saveButton.textContent = 'Save';
        }
    }
}

export function initTicketForm(): void {
    formRoot = document.getElementById('ticket-form-root');
    if (!formRoot) {
        console.warn('Ticket form root not found; form will not be available');
    }
}

export function isTerminalTicketStatus(status: string): boolean {
    return status === 'Closed' || status === 'Cancelled';
}
