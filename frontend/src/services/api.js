const resolveRuntimeBaseUrl = () => {
    if (typeof window !== 'undefined' && window.location?.origin) {
        return window.location.origin;
    }
    return '';
};

const DEFAULT_BASE_URL = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL)
    ? import.meta.env.VITE_API_BASE_URL
    : resolveRuntimeBaseUrl() || 'http://localhost:8080';

const trimTrailingSlash = (url) => url.replace(/\/$/, '');
const API_BASE_URL = trimTrailingSlash(DEFAULT_BASE_URL);
const buildApiPath = (baseUrl, path) => (baseUrl ? `${baseUrl}${path}` : path);
const AGENT_API = buildApiPath(API_BASE_URL, '/api/agent');
const PANDADOC_API = buildApiPath(API_BASE_URL, '/api/pandadoc');

export const WORKFLOW_STORAGE_KEY = 'temporal-agent-workflow-id';

export const storageService = {
    getWorkflowId() {
        try {
            return window.localStorage.getItem(WORKFLOW_STORAGE_KEY);
        } catch (error) {
            console.error('Unable to read workflow id from storage', error);
            return null;
        }
    },
    setWorkflowId(workflowId) {
        try {
            if (workflowId) {
                window.localStorage.setItem(WORKFLOW_STORAGE_KEY, workflowId);
            } else {
                window.localStorage.removeItem(WORKFLOW_STORAGE_KEY);
            }
        } catch (error) {
            console.error('Unable to persist workflow id', error);
        }
    },
};

export class ApiError extends Error {
    constructor(message, status) {
        super(message);
        this.status = status;
        this.name = 'ApiError';
    }
}

async function handleResponse(response) {
    const text = await response.text();
    let data = {};

    if (text) {
        try {
            data = JSON.parse(text);
        } catch (error) {
            data = { message: text };
        }
    }

    if (!response.ok) {
        throw new ApiError(data.message || response.statusText || 'An error occurred', response.status);
    }

    return data;
}

function mapConversation(data) {
    const messages = Array.isArray(data?.messages) ? data.messages.map((message) => ({
        ...message,
        actor: message.actor || message.type || message.kind || 'agent',
    })) : [];
    return { ...data, messages };
}

export const apiService = {
    async startWorkflow(goalId) {
        const body = goalId ? JSON.stringify({ goalId }) : '{}';
        const response = await fetch(`${AGENT_API}/start`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body,
        });
        const data = await handleResponse(response);
        if (!data.workflowId) {
            throw new ApiError('Failed to start workflow', response.status);
        }
        return data;
    },

    async getConversationHistory(workflowId) {
        if (!workflowId) {
            throw new ApiError('Missing workflow id', 400);
        }
        const response = await fetch(`${AGENT_API}/${workflowId}/history`);
        const data = await handleResponse(response);
        return mapConversation(data);
    },

    async sendMessage(workflowId, message) {
        if (!workflowId) {
            throw new ApiError('Missing workflow id', 400);
        }
        if (!message?.trim()) {
            throw new ApiError('Message cannot be empty', 400);
        }

        const response = await fetch(`${AGENT_API}/${workflowId}/prompt`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ prompt: message }),
        });
        await handleResponse(response);
    },

    async confirm(workflowId) {
        if (!workflowId) {
            throw new ApiError('Missing workflow id', 400);
        }
        const response = await fetch(`${AGENT_API}/${workflowId}/confirm`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        });
        await handleResponse(response);
    },

    async endConversation(workflowId) {
        if (!workflowId) {
            return;
        }
        try {
            const response = await fetch(`${AGENT_API}/${workflowId}/end`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            });
            await handleResponse(response);
        } catch (error) {
            // Ending an already-closed workflow should not disrupt the UI flow.
            console.warn('Unable to end workflow', error);
        }
    },

    async listTemplates(searchTerm) {
        const params = new URLSearchParams();
        if (searchTerm) {
            params.set('search', searchTerm);
        }
        const url = `${PANDADOC_API}/templates${params.toString() ? `?${params.toString()}` : ''}`;
        const response = await fetch(url);
        return handleResponse(response);
    },
};
