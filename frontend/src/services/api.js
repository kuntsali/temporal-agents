const resolveRuntimeBaseUrl = () => {
    if (typeof window === 'undefined' || !window.location?.origin) {
        return '';
    }

    const { protocol, hostname, port } = window.location;
    const isLocalhost = ['localhost', '127.0.0.1', '0.0.0.0'].includes(hostname);

    if (isLocalhost && port && port !== '8080') {
        return `${protocol}//${hostname}:8080`;
    }

    return window.location.origin;
};

const DEFAULT_BASE_URL = (() => {
    if (typeof import.meta !== 'undefined') {
        const explicitBaseUrl = import.meta.env?.VITE_API_BASE_URL;
        if (explicitBaseUrl) {
            return explicitBaseUrl;
        }

        if (import.meta.env?.DEV) {
            return 'http://localhost:8080';
        }
    }

    return resolveRuntimeBaseUrl() || 'http://localhost:8080';
})();

const trimTrailingSlash = (url) => url.replace(/\/$/, '');
const API_BASE_URL = trimTrailingSlash(DEFAULT_BASE_URL);
const buildApiPath = (baseUrl, path) => (baseUrl ? `${baseUrl}${path}` : path);
const AGENT_API = buildApiPath(API_BASE_URL, '/api/agent');
const PANDADOC_API = buildApiPath(API_BASE_URL, '/api/pandadoc');

const DEFAULT_FETCH_OPTIONS = Object.freeze({
    mode: 'cors',
    referrerPolicy: 'no-referrer',
});

const withDefaultOptions = (options = {}) => {
    const init = {
        ...DEFAULT_FETCH_OPTIONS,
        ...options,
    };

    if (options.headers) {
        init.headers = options.headers;
    }

    return init;
};

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
        const response = await fetch(`${AGENT_API}/start`, withDefaultOptions({
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body,
        }));
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
        const response = await fetch(`${AGENT_API}/${workflowId}/history`, withDefaultOptions());
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

        const response = await fetch(`${AGENT_API}/${workflowId}/prompt`, withDefaultOptions({
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ prompt: message }),
        }));
        await handleResponse(response);
    },

    async confirm(workflowId) {
        if (!workflowId) {
            throw new ApiError('Missing workflow id', 400);
        }
        const response = await fetch(`${AGENT_API}/${workflowId}/confirm`, withDefaultOptions({
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        }));
        await handleResponse(response);
    },

    async endConversation(workflowId) {
        if (!workflowId) {
            return;
        }
        try {
            const response = await fetch(`${AGENT_API}/${workflowId}/end`, withDefaultOptions({
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            }));
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
        const response = await fetch(url, withDefaultOptions());
        return handleResponse(response);
    },
};
