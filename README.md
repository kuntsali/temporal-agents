# Temporal Agents (Java)

This project reimagines the [Temporal AI Agent](https://github.com/temporal-community/temporal-ai-agent) demo using the Java ecosystem. It combines Spring Boot, Spring AI, and the Temporal Java SDK to host a goal-oriented AI agent inside a Temporal workflow. The agent keeps durable conversation state, plans tool calls with an LLM, and executes business logic with deterministic activities.

## Features

- **Temporal Workflow Loop** – a long-lived workflow (`AgentGoalWorkflow`) that listens for user prompts, asks an LLM for the next step, and executes tools with confirmation gates.
- **Spring AI Integration** – the planning and validation activities delegate to any OpenAI-compatible model configured through Spring AI.
- **Extensible Tools** – business tools are registered with a Spring managed `ToolRegistry`. The demo includes ecommerce utilities (list orders, check status, track packages) and PandaDoc automation helpers for creating and tracking documents.
- **Goal Catalog** – goals are defined via the `GoalRegistry`, including an agent-selection goal and ecommerce flows.
- **REST API** – `AgentController` exposes endpoints to start conversations, submit prompts, confirm tool runs, switch goals, and inspect state.
- **Test Coverage** – unit and end-to-end tests validate the tool catalog, goal registry, prompt generation, and workflow loop.

## Getting Started

### Prerequisites

- Java 21 (or newer LTS release)
- Docker (to run the bundled Compose file)
- The Gradle wrapper (`./gradlew`) downloads the required build tooling automatically
- Docker Compose can start a local Temporal server for you (see below)
- An OpenAI compatible API key (set `LLM_KEY` and `LLM_MODEL`).

### Configuration

All configuration lives in `src/main/resources/application.yml` and environment variables:

- `TEMPORAL_ADDRESS` – Temporal frontend host (default `localhost:7233`).
- `TEMPORAL_NAMESPACE` – Temporal namespace (default `default`).
- `TEMPORAL_TASK_QUEUE` – task queue used by the workflow and worker (`agent-task-queue`).
- `LLM_KEY`, `LLM_MODEL`, `LLM_BASE_URL` – Spring AI configuration for the chosen model provider.
- `SHOW_CONFIRM` – optional flag to toggle the UI confirmation requirement for tool calls.

### Running the Application

1. Start the Temporal dependencies with Docker Compose:
   ```bash
   docker compose up temporal temporal-ui -d
   ```
   The Temporal gRPC endpoint is exposed on `localhost:7233` and the UI on `http://localhost:8088`.
2. Export your LLM credentials, e.g.:
   ```bash
   export LLM_KEY=sk-...
   export LLM_MODEL=gpt-4o-mini
   ```
3. Launch the Spring Boot worker and API:
   ```bash
   ./gradlew bootRun
   ```

The worker registers the workflow and activities and begins polling the configured task queue. The REST API is available on `http://localhost:8080` and the chat UI is served from the same port.

### API Overview

| Endpoint | Description |
| --- | --- |
| `POST /api/agent/start` | Start a new workflow (accepts optional `workflowId` and `goalId`). |
| `POST /api/agent/{workflowId}/prompt` | Submit a user utterance to the workflow. |
| `POST /api/agent/{workflowId}/confirm` | Confirm the currently proposed tool execution. |
| `POST /api/agent/{workflowId}/goal` | Switch the workflow to a new goal. |
| `POST /api/agent/{workflowId}/end` | Signal the workflow to complete. |
| `GET /api/agent/{workflowId}/history` | Retrieve the deterministic conversation log. |
| `GET /api/agent/{workflowId}/tool` | Retrieve the latest tool planning response. |
| `GET /api/agent/goals` | List available goals with metadata. |
| `GET /api/pandadoc/templates` | List PandaDoc templates (accepts optional `search` query). |

### Browser Chat UI

Navigate to `http://localhost:8080/` once the application is running to launch the built-in chat experience. The interface mirrors familiar chat assistants with:

- **Goal selection** – choose any registered agent goal before starting a workflow or change goals mid-conversation.
- **Template explorer** – browse PandaDoc templates (with optional search) to understand the available document skeletons.
- **Conversation feed** – review every user/agent/tool message captured by the Temporal workflow.
- **Tool confirmation** – inspect pending tool arguments and approve executions directly from the UI.

The client communicates with the existing REST endpoints so it runs alongside the worker without requiring a separate Node.js process.

PandaDoc support is driven through the tool catalog. Provide `PANDADOC_API_KEY` and optionally `PANDADOC_BASE_URL` to execute real API calls. When the key is omitted the application falls back to stub data so the agent can still demonstrate the conversation flow.

## Testing

Run the unit and end-to-end test suite with:

```bash
./gradlew test
```

The tests verify the ecommerce tool implementations, goal registry wiring, prompt generator output, and
end-to-end workflow orchestration using the Temporal test environment.

## MCP Support

The original Python demo integrates with Model Context Protocol (MCP) tools. The Java version wires the same abstractions but currently returns a descriptive error when an MCP tool is requested. The activity and workflow surface area is in place so that native MCP clients can be plugged in later.

## Project Layout

```
src/main/java/io/temporal/agent
├── activities        # Activity implementations using Spring AI & tool registry
├── controller        # REST controller and request DTOs
├── goals             # Goal registry mirroring original demo content
├── prompt            # Prompt generation utilities
├── service           # Facade around Temporal client interactions
├── tools             # Sample native tool definitions
└── workflow          # Temporal workflow interface and implementation
```

## License

This project follows the same permissive approach as the original demo. Refer to the repository license for details.
