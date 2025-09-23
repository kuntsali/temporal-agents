const state = {
    workflowId: null,
    goals: [],
    selectedGoalId: null,
    currentGoal: null,
    templates: [],
    messages: [],
    toolDecision: null,
    polls: {
        history: null,
        tool: null,
        goal: null,
    },
};

const elements = {
    goalList: document.getElementById('goal-list'),
    startButton: document.getElementById('start-conversation'),
    endButton: document.getElementById('end-conversation'),
    confirmButton: document.getElementById('confirm-tool'),
    sendButton: document.getElementById('send-prompt'),
    promptForm: document.getElementById('prompt-form'),
    promptInput: document.getElementById('prompt'),
    chatMessages: document.getElementById('chat-messages'),
    templateList: document.getElementById('template-list'),
    templateSearch: document.getElementById('template-search'),
    templateRefresh: document.getElementById('template-refresh'),
    statusWorkflow: document.getElementById('status-workflow'),
    statusGoal: document.getElementById('status-goal'),
    statusNext: document.getElementById('status-next'),
    pendingTool: document.getElementById('pending-tool'),
    notification: document.getElementById('notification'),
    refreshHistory: document.getElementById('refresh-history'),
};

const messageTemplate = document.getElementById('message-template');
let notificationTimeout;
let templateSearchTimeout;
let lastErrorMessage = null;

const STORAGE_KEY = 'temporal-agent-workflow';
const HISTORY_POLL_INTERVAL = 3000;
const TOOL_POLL_INTERVAL = 4000;
const GOAL_POLL_INTERVAL = 10000;
const WORKFLOW_NOT_FOUND = 'WORKFLOW_NOT_FOUND';

document.addEventListener('DOMContentLoaded', async () => {
    wireEventListeners();
    await Promise.all([loadGoals(), loadTemplates()]);
    const savedWorkflowId = window.localStorage.getItem(STORAGE_KEY);
    if (savedWorkflowId) {
        await resumeWorkflow(savedWorkflowId);
    } else {
        updateStatusGoal();
        renderMessages();
    }
});

function wireEventListeners() {
    elements.startButton.addEventListener('click', startConversation);
    elements.endButton.addEventListener('click', endConversation);
    elements.confirmButton.addEventListener('click', confirmTool);
    elements.promptForm.addEventListener('submit', sendPrompt);
    elements.promptInput.addEventListener('input', updateControls);
    elements.templateRefresh.addEventListener('click', () => loadTemplates(elements.templateSearch.value));
    elements.templateSearch.addEventListener('input', () => {
        clearTimeout(templateSearchTimeout);
        templateSearchTimeout = setTimeout(() => loadTemplates(elements.templateSearch.value), 350);
    });
    elements.refreshHistory.addEventListener('click', () => {
        loadHistory();
        loadToolDecision();
        loadGoalStatus();
    });
}

async function loadGoals() {
    try {
        const response = await fetch('/api/agent/goals');
        if (!response.ok) {
            throw new Error(`Failed to load goals (${response.status})`);
        }
        const goals = await response.json();
        state.goals = Array.isArray(goals) ? goals : [];
        if (!state.selectedGoalId && state.goals.length > 0) {
            state.selectedGoalId = state.goals[0].id;
        }
        renderGoalList();
    } catch (error) {
        console.error(error);
        setNotification('Unable to load goals. Verify the backend is running.', 'error');
    }
}

function renderGoalList() {
    elements.goalList.innerHTML = '';
    if (state.goals.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'empty';
        empty.textContent = 'No goals registered.';
        elements.goalList.appendChild(empty);
        updateControls();
        return;
    }
    for (const goal of state.goals) {
        const label = document.createElement('label');
        label.className = 'goal-item';
        if (goal.id === state.selectedGoalId) {
            label.classList.add('active');
        }
        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'goal';
        radio.value = goal.id;
        radio.checked = goal.id === state.selectedGoalId;
        radio.addEventListener('change', () => handleGoalSelection(goal.id));
        const info = document.createElement('div');
        info.className = 'goal-info';
        const title = document.createElement('h3');
        title.textContent = goal.agentName || goal.id;
        const description = document.createElement('p');
        description.textContent = goal.agentFriendlyDescription || goal.description || 'No description available.';
        info.append(title, description);
        label.append(radio, info);
        elements.goalList.appendChild(label);
    }
    updateControls();
}

async function handleGoalSelection(goalId) {
    state.selectedGoalId = goalId;
    for (const item of elements.goalList.querySelectorAll('.goal-item')) {
        item.classList.toggle('active', item.querySelector('input')?.value === goalId);
    }
    if (state.workflowId) {
        try {
            const response = await fetch(`/api/agent/${encodeURIComponent(state.workflowId)}/goal`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ goalId }),
            });
            if (!response.ok) {
                throw new Error(`Failed to update goal (${response.status})`);
            }
            setNotification('Goal updated for the active workflow.', 'info');
            await loadGoalStatus();
        } catch (error) {
            console.error(error);
            setNotification('Unable to switch goal. Try again or start a new conversation.', 'error');
        }
    } else {
        updateStatusGoal();
    }
    updateControls();
}

async function loadTemplates(searchTerm = '') {
    const query = searchTerm && searchTerm.trim() ? `?search=${encodeURIComponent(searchTerm.trim())}` : '';
    try {
        const response = await fetch(`/api/pandadoc/templates${query}`);
        if (!response.ok) {
            throw new Error(`Failed to load templates (${response.status})`);
        }
        const data = await response.json();
        state.templates = Array.isArray(data?.templates) ? data.templates : [];
        renderTemplates();
    } catch (error) {
        console.error(error);
        setNotification('Unable to fetch PandaDoc templates. Check your API key.', 'error');
    }
}

function renderTemplates() {
    elements.templateList.innerHTML = '';
    if (state.templates.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'empty';
        empty.textContent = 'No templates found.';
        elements.templateList.appendChild(empty);
        return;
    }
    for (const template of state.templates) {
        const card = document.createElement('article');
        card.className = 'template-card';
        const name = document.createElement('h3');
        name.textContent = template.name || 'Untitled Template';
        const id = document.createElement('p');
        id.className = 'template-id';
        id.textContent = template.id || 'Unknown template id';
        card.append(name, id);
        if (template.updatedAt) {
            const time = document.createElement('time');
            time.dateTime = template.updatedAt;
            time.textContent = `Updated ${formatTimestamp(template.updatedAt)}`;
            card.appendChild(time);
        }
        elements.templateList.appendChild(card);
    }
}

async function startConversation() {
    if (!state.selectedGoalId) {
        setNotification('Select a goal before starting a conversation.', 'error');
        return;
    }
    try {
        const response = await fetch('/api/agent/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ goalId: state.selectedGoalId }),
        });
        if (!response.ok) {
            throw new Error(`Failed to start workflow (${response.status})`);
        }
        const data = await response.json();
        if (!data?.workflowId) {
            throw new Error('Workflow id missing in response');
        }
        state.messages = [];
        renderMessages();
        state.toolDecision = null;
        renderToolDecision();
        setWorkflowId(data.workflowId);
        setNotification('Conversation started. You can now send prompts.', 'info');
        await loadGoalStatus();
        startPolling();
    } catch (error) {
        console.error(error);
        setNotification('Unable to start a conversation. Check the Temporal worker logs.', 'error');
    }
}

async function resumeWorkflow(workflowId) {
    try {
        setWorkflowId(workflowId);
        await Promise.all([loadGoalStatus(), loadHistory(), loadToolDecision()]);
        if (!state.workflowId) {
            return;
        }
        setNotification('Restored your previous conversation.', 'info');
        startPolling();
    } catch (error) {
        console.error(error);
        setWorkflowId(null);
        setNotification('Previous conversation could not be restored. Start a new one.', 'error');
    }
}

async function sendPrompt(event) {
    event.preventDefault();
    const prompt = elements.promptInput.value.trim();
    if (!prompt || !state.workflowId) {
        return;
    }
    try {
        const response = await fetch(`/api/agent/${encodeURIComponent(state.workflowId)}/prompt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt }),
        });
        if (!response.ok) {
            throw new Error(`Failed to send prompt (${response.status})`);
        }
        elements.promptInput.value = '';
        await Promise.all([loadHistory(), loadToolDecision()]);
        updateControls();
    } catch (error) {
        console.error(error);
        setNotification('Unable to submit prompt. Please retry.', 'error');
    }
}

async function confirmTool() {
    if (!state.workflowId) {
        return;
    }
    try {
        const response = await fetch(`/api/agent/${encodeURIComponent(state.workflowId)}/confirm`, {
            method: 'POST',
        });
        if (!response.ok) {
            throw new Error(`Failed to confirm tool (${response.status})`);
        }
        setNotification('Tool execution confirmed.', 'info');
        await Promise.all([loadHistory(), loadToolDecision()]);
    } catch (error) {
        console.error(error);
        setNotification('Unable to confirm the tool run.', 'error');
    }
}

async function endConversation() {
    if (!state.workflowId) {
        return;
    }
    try {
        const response = await fetch(`/api/agent/${encodeURIComponent(state.workflowId)}/end`, {
            method: 'POST',
        });
        if (!response.ok) {
            throw new Error(`Failed to end conversation (${response.status})`);
        }
        setNotification('End signal sent to the workflow.', 'info');
        stopPolling();
        setWorkflowId(null);
        updateControls();
    } catch (error) {
        console.error(error);
        setNotification('Unable to end the conversation.', 'error');
    }
}

async function loadHistory() {
    if (!state.workflowId) {
        return;
    }
    try {
        const response = await fetch(`/api/agent/${encodeURIComponent(state.workflowId)}/history`);
        if (response.status === 404) {
            handleWorkflowNotFound();
            throw new Error(WORKFLOW_NOT_FOUND);
        }
        if (!response.ok) {
            throw new Error(`Failed to fetch history (${response.status})`);
        }
        const history = await response.json();
        state.messages = Array.isArray(history?.messages) ? history.messages : [];
        renderMessages();
    } catch (error) {
        if (error.message === WORKFLOW_NOT_FOUND) {
            return;
        }
        console.error(error);
        setNotification('Unable to refresh conversation history.', 'error');
    }
}

async function loadToolDecision() {
    if (!state.workflowId) {
        return;
    }
    try {
        const response = await fetch(`/api/agent/${encodeURIComponent(state.workflowId)}/tool`);
        if (response.status === 404) {
            handleWorkflowNotFound();
            throw new Error(WORKFLOW_NOT_FOUND);
        }
        if (!response.ok) {
            throw new Error(`Failed to fetch tool decision (${response.status})`);
        }
        const decision = await response.json();
        state.toolDecision = decision;
        renderToolDecision();
    } catch (error) {
        if (error.message === WORKFLOW_NOT_FOUND) {
            return;
        }
        console.error(error);
        setNotification('Unable to load the tool decision.', 'error');
    }
}

async function loadGoalStatus() {
    if (!state.workflowId) {
        updateStatusGoal();
        return;
    }
    try {
        const response = await fetch(`/api/agent/${encodeURIComponent(state.workflowId)}/goal`);
        if (response.status === 404) {
            handleWorkflowNotFound();
            throw new Error(WORKFLOW_NOT_FOUND);
        }
        if (!response.ok) {
            throw new Error(`Failed to fetch goal (${response.status})`);
        }
        state.currentGoal = await response.json();
        updateStatusGoal();
    } catch (error) {
        if (error.message === WORKFLOW_NOT_FOUND) {
            return;
        }
        console.error(error);
        setNotification('Unable to load the workflow goal.', 'error');
    }
}

function startPolling() {
    stopPolling();
    state.polls.history = setInterval(loadHistory, HISTORY_POLL_INTERVAL);
    state.polls.tool = setInterval(loadToolDecision, TOOL_POLL_INTERVAL);
    state.polls.goal = setInterval(loadGoalStatus, GOAL_POLL_INTERVAL);
    loadHistory();
    loadToolDecision();
    loadGoalStatus();
}

function stopPolling() {
    clearInterval(state.polls.history);
    clearInterval(state.polls.tool);
    clearInterval(state.polls.goal);
    state.polls.history = state.polls.tool = state.polls.goal = null;
}

function setWorkflowId(workflowId) {
    if (state.workflowId === workflowId) {
        updateControls();
        return;
    }
    state.workflowId = workflowId;
    if (workflowId) {
        window.localStorage.setItem(STORAGE_KEY, workflowId);
    } else {
        window.localStorage.removeItem(STORAGE_KEY);
        state.currentGoal = null;
    }
    elements.statusWorkflow.textContent = workflowId || '—';
    updateControls();
    updateStatusGoal();
    if (!workflowId) {
        renderToolDecision();
    }
}

function renderMessages() {
    elements.chatMessages.innerHTML = '';
    if (!state.messages || state.messages.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'empty';
        empty.textContent = 'No conversation yet. Start by sending a prompt.';
        elements.chatMessages.appendChild(empty);
        return;
    }
    for (const message of state.messages) {
        const clone = messageTemplate.content.firstElementChild.cloneNode(true);
        const role = clone.querySelector('.message-role');
        role.textContent = roleLabel(message.type);
        clone.classList.add(slugifyType(message.type));
        const time = clone.querySelector('.message-time');
        time.textContent = '';
        const content = clone.querySelector('.message-content');
        renderMessageContent(content, message.response);
        elements.chatMessages.appendChild(clone);
    }
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
}

function renderToolDecision() {
    const card = elements.pendingTool;
    const decision = state.toolDecision;
    if (!decision || !state.workflowId) {
        card.classList.add('hidden');
        updateStatusNext();
        elements.confirmButton.disabled = true;
        return;
    }
    const next = decision.next || 'question';
    updateStatusNext(next);
    if (next === 'confirm' && decision.tool) {
        card.classList.remove('hidden');
        card.innerHTML = '';
        const heading = document.createElement('h3');
        heading.textContent = `Confirm ${decision.tool}`;
        const summary = document.createElement('p');
        summary.textContent = decision.response || 'The agent wants to run a tool.';
        card.append(heading, summary);
        const args = decision.args && typeof decision.args === 'object' ? decision.args : {};
        if (Object.keys(args).length > 0) {
            const list = document.createElement('dl');
            list.className = 'tool-args';
            Object.entries(args).forEach(([key, value]) => {
                const dt = document.createElement('dt');
                dt.textContent = key;
                const dd = document.createElement('dd');
                if (value && typeof value === 'object') {
                    dd.textContent = JSON.stringify(value, null, 2);
                } else {
                    dd.textContent = value === null || value === undefined ? '' : String(value);
                }
                list.append(dt, dd);
            });
            card.appendChild(list);
        }
        elements.confirmButton.disabled = false;
    } else if (next === 'done') {
        card.classList.remove('hidden');
        card.innerHTML = '<h3>Workflow completed</h3><p>The agent marked this conversation as complete.</p>';
        elements.confirmButton.disabled = true;
        stopPolling();
    } else if (next === 'pick-new-goal') {
        card.classList.remove('hidden');
        card.innerHTML = '<h3>Pick a new goal</h3><p>Select another goal from the list to continue.</p>';
        elements.confirmButton.disabled = true;
    } else {
        card.classList.add('hidden');
        elements.confirmButton.disabled = true;
    }
}

function updateStatusGoal() {
    let label = '—';
    const goal = state.workflowId ? state.currentGoal : findGoal(state.selectedGoalId);
    if (goal) {
        label = goal.agentFriendlyDescription || goal.description || goal.agentName || goal.id || 'Unknown goal';
    }
    elements.statusGoal.textContent = label;
}

function updateStatusNext(next = 'question') {
    const labels = {
        confirm: 'Awaiting your confirmation',
        question: 'Waiting for your next prompt',
        'pick-new-goal': 'Choose a different goal to continue',
        done: 'Workflow completed',
    };
    elements.statusNext.textContent = labels[next] || 'Waiting for updates';
}

function updateControls() {
    const hasWorkflow = Boolean(state.workflowId);
    const hasPrompt = Boolean(elements.promptInput.value && elements.promptInput.value.trim());
    elements.startButton.disabled = !state.selectedGoalId || hasWorkflow;
    elements.endButton.disabled = !hasWorkflow;
    elements.sendButton.disabled = !hasWorkflow || !hasPrompt;
    elements.promptInput.disabled = !hasWorkflow;
    if (!hasWorkflow) {
        elements.confirmButton.disabled = true;
    }
}

function renderMessageContent(container, content) {
    container.innerHTML = '';
    if (content === null || content === undefined) {
        const p = document.createElement('p');
        p.textContent = '—';
        container.appendChild(p);
        return;
    }
    if (typeof content === 'string') {
        const p = document.createElement('p');
        p.textContent = content;
        container.appendChild(p);
        return;
    }
    if (typeof content === 'object') {
        const pre = document.createElement('pre');
        pre.textContent = JSON.stringify(content, null, 2);
        container.appendChild(pre);
        return;
    }
    const p = document.createElement('p');
    p.textContent = String(content);
    container.appendChild(p);
}

function roleLabel(type) {
    switch (type) {
        case 'user':
            return 'You';
        case 'agent':
            return 'Agent';
        case 'tool_result':
            return 'Tool Result';
        case 'conversation_summary':
            return 'Summary';
        case 'user_confirmed_tool_run':
            return 'Confirmation';
        default:
            return type ? type.replace(/_/g, ' ') : 'System';
    }
}

function slugifyType(type) {
    if (!type) {
        return 'message-system';
    }
    return type.toLowerCase().replace(/[^a-z0-9]+/g, '_');
}

function formatTimestamp(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return date.toLocaleString();
}

function findGoal(goalId) {
    if (!goalId) {
        return null;
    }
    return state.goals.find((goal) => goal.id === goalId) || null;
}

function setNotification(message, type = 'info') {
    const card = elements.notification;
    clearTimeout(notificationTimeout);
    if (!message) {
        card.classList.add('hidden');
        card.classList.remove('info', 'error');
        card.innerHTML = '';
        lastErrorMessage = null;
        return;
    }
    if (type === 'error' && message === lastErrorMessage) {
        return;
    }
    if (type === 'error') {
        lastErrorMessage = message;
    } else {
        lastErrorMessage = null;
    }
    card.classList.remove('hidden', 'info', 'error');
    card.classList.add(type === 'error' ? 'error' : 'info');
    card.innerHTML = '';
    const heading = document.createElement('h3');
    heading.textContent = type === 'error' ? 'Something went wrong' : 'Update';
    const body = document.createElement('p');
    body.textContent = message;
    card.append(heading, body);
    notificationTimeout = setTimeout(() => {
        card.classList.add('hidden');
        lastErrorMessage = null;
    }, 5000);
}

function handleWorkflowNotFound() {
    stopPolling();
    setWorkflowId(null);
    setNotification('The workflow is no longer available. Start a new conversation to continue.', 'info');
}
