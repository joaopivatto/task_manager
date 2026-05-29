document.addEventListener('DOMContentLoaded', function () {
    initializeKanbanDragAndDrop();
    initializeTaskDetailsModal();
});

function initializeKanbanDragAndDrop() {
    if (typeof Sortable === 'undefined') {
        return;
    }

    document.querySelectorAll('.kanban-list').forEach(function (column) {
        Sortable.create(column, {
            group: 'kanban',
            animation: 150,
            onEnd: function (event) {
                const taskId = event.item.dataset.taskId;
                const targetStatus = event.to.dataset.status;
                const sourceStatus = event.from.dataset.status;

                event.item.dataset.lastDragAt = String(Date.now());

                if (!taskId || !targetStatus || sourceStatus === targetStatus) {
                    return;
                }

                persistStatusChange(taskId, targetStatus)
                    .then(function () {
                        window.location.reload();
                    })
                    .catch(function (error) {
                        revertMove(event);
                        window.alert((error && error.message) || 'Unable to move task to this status.');
                    });
            }
        });
    });
}

function persistStatusChange(taskId, status) {
    const body = new URLSearchParams({ status: status });

    return fetch('/tasks/' + taskId + '/status/drag', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
            'HX-Request': 'true'
        },
        body: body
    }).then(async function (response) {
        if (response.ok) {
            return;
        }

        let errorMessage = 'Invalid status transition.';
        try {
            const payload = await response.json();
            if (payload && payload.error) {
                errorMessage = payload.error;
            }
        } catch (ignore) {
            // Keep default error message if the server did not return JSON.
        }

        throw new Error(errorMessage);
    });
}

function revertMove(event) {
    const sourceColumn = event.from;
    const fallbackNode = sourceColumn.children[event.oldIndex] || null;
    sourceColumn.insertBefore(event.item, fallbackNode);
}

function initializeTaskDetailsModal() {
    if (typeof bootstrap === 'undefined') {
        return;
    }

    const modalElement = document.getElementById('taskDetailsModal');
    if (!modalElement) {
        return;
    }

    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);

    document.addEventListener('click', function (event) {
        const trigger = event.target.closest('[data-task-details-trigger="true"]');
        if (!trigger) {
            return;
        }

        if (event.target.closest('[data-no-modal="true"]')) {
            return;
        }

        const lastDragAt = Number(trigger.dataset.lastDragAt || '0');
        if (Date.now() - lastDragAt < 250) {
            return;
        }

        const taskId = trigger.dataset.taskId;
        if (!taskId) {
            return;
        }

        // Fill from card data first so modal never opens empty.
        const fallbackPayload = buildFallbackPayload(trigger);
        fillTaskDetailsModal(fallbackPayload, taskId);
        modal.show();

        loadTaskDetails(taskId)
            .then(function (payload) {
                fillTaskDetailsModal(mergeTaskPayload(fallbackPayload, payload || {}), taskId);
            })
            .catch(function () {
                // Keep fallback values already shown in modal when API fails.
            });
    });
}

function buildFallbackPayload(trigger) {
    const data = trigger.dataset || {};
    const titleAttr = readAttr(trigger, 'data-task-title');
    const descriptionAttr = readAttr(trigger, 'data-task-description');
    const statusValueAttr = readAttr(trigger, 'data-task-status-value');
    const statusLabelAttr = readAttr(trigger, 'data-task-status-label');
    const statusClassAttr = readAttr(trigger, 'data-task-status-class');
    const deadlineAttr = readAttr(trigger, 'data-task-deadline');
    const completedAtAttr = readAttr(trigger, 'data-task-completed-at');
    const categoryIdAttr = readAttr(trigger, 'data-task-category-id');
    const categoryAttr = readAttr(trigger, 'data-task-category');
    const titleFromDom = textOf(trigger, '.card-title');
    const deadlineFromDom = textOf(trigger, 'small.text-body-secondary');
    const categoryFromDom = textOf(trigger, '.badge.text-bg-light.border');
    const statusBadge = trigger.querySelector('.badge.status-badge, .badge.status-todo, .badge.status-doing, .badge.status-done, .badge.status-stuck, .badge:not(.text-bg-light)');
    const statusLabelFromDom = statusBadge ? statusBadge.textContent.trim() : '';
    const statusClassFromDom = statusBadge ? statusBadge.className : '';

    return {
        title: takeFirst(titleAttr, data.taskTitle, titleFromDom, ''),
        description: takeFirst(descriptionAttr, data.taskDescription, ''),
        statusValue: takeFirst(statusValueAttr, data.taskStatusValue, statusValueFromLabel(takeFirst(statusLabelAttr, data.taskStatusLabel, statusLabelFromDom, '')), 'TO_DO'),
        statusLabel: takeFirst(statusLabelAttr, data.taskStatusLabel, statusLabelFromDom, '-'),
        statusClass: takeFirst(statusClassAttr, data.taskStatusClass, statusClassFromDom, ''),
        deadline: takeFirst(deadlineAttr, data.taskDeadline, deadlineFromDom, '-'),
        completedAt: takeFirst(completedAtAttr, data.taskCompletedAt, '-'),
        categoryId: takeFirst(categoryIdAttr, data.taskCategoryId, ''),
        categoryName: takeFirst(categoryAttr, data.taskCategory, categoryFromDom, 'No category'),
        updates: []
    };
}

function mergeTaskPayload(base, remote) {
    return {
        title: takeFirst(remote.title, base.title, ''),
        description: takeFirst(remote.description, base.description, ''),
        statusValue: takeFirst(remote.statusValue, base.statusValue, 'TO_DO'),
        statusLabel: takeFirst(remote.statusLabel, base.statusLabel, '-'),
        statusClass: takeFirst(remote.statusClass, base.statusClass, ''),
        deadline: takeFirst(remote.deadline, base.deadline, '-'),
        completedAt: takeFirst(remote.completedAt, base.completedAt, '-'),
        categoryId: remote.categoryId !== undefined && remote.categoryId !== null && remote.categoryId !== '' ? remote.categoryId : base.categoryId,
        categoryName: takeFirst(remote.categoryName, base.categoryName, 'No category'),
        updates: Array.isArray(remote.updates) ? remote.updates : base.updates
    };
}

function loadTaskDetails(taskId) {
    return fetch('/tasks/' + taskId + '/details', {
        headers: {
            'Accept': 'application/json'
        }
    }).then(function (response) {
        if (!response.ok) {
            throw new Error('Failed to load details');
        }
        return response.json();
    });
}

function fillTaskDetailsModal(data, taskId) {
    setValue('taskDetailsTitle', data.title || '');
    setValue('taskDetailsDescription', data.description || '');
    setValue('taskDetailsStatusSelect', data.statusValue || 'TO_DO');
    setValue('taskDetailsDeadlineInput', toIsoDate(data.deadline));
    setValue('taskDetailsCategorySelect', data.categoryId || '');
    setText('taskDetailsCompletedAt', data.completedAt || '-');

    setFormAction('taskQuickEditForm', '/tasks/' + taskId + '/quick-update');
    setFormAction('taskUpdateForm', '/tasks/' + taskId + '/updates');

    const boardCategoryId = getBoardCategoryId();
    setValue('taskQuickEditBoardCategoryId', boardCategoryId || '');
    setValue('taskUpdateBoardCategoryId', boardCategoryId || '');
    setValue('taskUpdateComment', '');

    renderUpdates(data.updates || []);

    const statusElement = document.getElementById('taskDetailsStatus');
    if (statusElement) {
        statusElement.className = 'badge';
        statusElement.classList.add('status-badge');

        const statusClass = (data.statusClass || '').split(' ');
        statusClass.forEach(function (className) {
            if (className) {
                statusElement.classList.add(className);
            }
        });

        statusElement.textContent = data.statusLabel || '-';
    }
}

function setText(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = value;
    }
}

function setValue(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.value = value;
    }
}

function setFormAction(id, action) {
    const element = document.getElementById(id);
    if (element) {
        element.action = action;
    }
}

function getBoardCategoryId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('categoryId');
}

function toIsoDate(value) {
    if (!value || value === '-') {
        return '';
    }
    const chunks = value.split('/');
    if (chunks.length !== 3) {
        return '';
    }
    return chunks[2] + '-' + chunks[1] + '-' + chunks[0];
}

function renderUpdates(updates) {
    const container = document.getElementById('taskUpdatesList');
    if (!container) {
        return;
    }

    container.innerHTML = '';
    if (!updates.length) {
        const empty = document.createElement('div');
        empty.className = 'list-group-item text-body-secondary';
        empty.textContent = 'No updates yet.';
        container.appendChild(empty);
        return;
    }

    updates.forEach(function (update) {
        const item = document.createElement('div');
        item.className = 'list-group-item';

        const header = document.createElement('div');
        header.className = 'text-body-secondary mb-1';
        header.textContent = update.createdAt || '-';

        const body = document.createElement('div');
        body.textContent = update.comment || '';

        item.appendChild(header);
        item.appendChild(body);
        container.appendChild(item);
    });
}

function takeFirst() {
    for (let index = 0; index < arguments.length; index += 1) {
        const value = arguments[index];
        if (value !== undefined && value !== null && value !== '') {
            return value;
        }
    }
    return '';
}

function textOf(root, selector) {
    const node = root.querySelector(selector);
    return node ? node.textContent.trim() : '';
}

function statusValueFromLabel(label) {
    const normalized = (label || '').trim().toUpperCase();
    if (normalized === 'TO DO') {
        return 'TO_DO';
    }
    if (normalized === 'DOING' || normalized === 'DONE' || normalized === 'STUCK') {
        return normalized;
    }
    return '';
}

function readAttr(element, attrName) {
    const value = element.getAttribute(attrName);
    return value === null ? '' : value;
}

