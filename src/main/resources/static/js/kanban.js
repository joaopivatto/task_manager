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

        fillTaskDetailsModal(trigger.dataset);
        modal.show();
    });
}

function fillTaskDetailsModal(data) {
    setText('taskDetailsTitle', data.taskTitle || '-');
    setText('taskDetailsDescription', data.taskDescription || 'No description');
    setText('taskDetailsCategory', data.taskCategory || '-');
    setText('taskDetailsDeadline', data.taskDeadline || '-');
    setText('taskDetailsCompletedAt', data.taskCompletedAt || '-');

    const statusElement = document.getElementById('taskDetailsStatus');
    if (statusElement) {
        statusElement.className = 'badge';
        statusElement.classList.add('status-badge');

        const statusClass = (data.taskStatusClass || '').split(' ');
        statusClass.forEach(function (className) {
            if (className) {
                statusElement.classList.add(className);
            }
        });

        statusElement.textContent = data.taskStatusLabel || '-';
    }
}

function setText(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = value;
    }
}
