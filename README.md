# Enterprise API Discovery - Knowledge Graph

LLM-powered API discovery and catalog using knowledge base. Ingest OpenAPI/Swagger specs and explore them via natural language queries and an interactive visualization.

> **Contributions welcome!** Please read the [Contributing Guide](CONTRIBUTING.md) for development setup, coding standards, workflow, and how to submit issues or pull requests.

## Goal

Turn OpenAPI specifications into a rich, queryable knowledge graph. and graph traversals so users can discover APIs using plain English instead of reading docs.

## Features

- **Ingest OpenAPI**: Upload a JSON/YAML file or provide a public URL. Parses with `swagger-parser`, extracts services, endpoints, parameters, and generates model instances to be generated as graph
- **Natural Language Query**: Ask questions like "show me all payment endpoints that take a userId as parameter". Uses LLM models to generate safe Cypher/OWL, executes against Neo4j/RDF , and returns both structured results - an LLM-generated natural language answer.
- **Interactive Property Graph**: Visualizes API relationships

## Tech Stack

### Backend

- Java 21 + Spring Boot 3.3.4
- Spring AI 1.0.0-M6 (OpenAI chat + embeddings)
- Spring Data Neo4j + Spring AI Neo4j Vector Store
- Swagger Parser 2.1.25

### Frontend

- React 19 + TypeScript + Vite
- Tailwind CSS

### Data & Infrastructure

- Neo4j (property graph)
- OpenAI

## Design Flow

```
OpenAPI (file/URL)
        ↓
Backend (Spring Boot)
  - Parse → map to APIService + Endpoint + Parameter nodes
  - Generate embeddings
  - Persist to Neo4j + vector index
        ↓
Frontend (React)
  - Ingest form (multipart or URL)
  - NL query → /api/discover/semantic
  - Graph view → relationship data
```

Domain model (simplified):

- `ApiService` (name, title, version, description, embedding) --[:HAS_ENDPOINT]→ `Endpoint`
- `Endpoint` (path, method, summary, description, operationId) --[:HAS_PARAMETER]→ `Parameter`

## Prerequisites

- **Java 21** (JDK)
- **Maven** (3.9+ recommended)
- **Node.js** 18+ and npm
- **Neo4j** running locally:
  - Default: `bolt://localhost:7687`
  - (You can start it via Neo4j Desktop or Docker)
- **OpenAI API key**

## Quick Start

### 1. Start Neo4j

Make sure Neo4j is running and accessible at the credentials above. You can open the browser at http://localhost:7474.

### 2. Set OpenAI Key

```bash
export OPENAI_API_KEY=sk-proj-...
```

### 3. Run the Backend

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**.

On first run it will connect to Neo4j and be ready to ingest.

### 4. Run the Frontend

```bash
cd frontend
npm install     # only needed first time or after dependency changes
npm run dev
```

Frontend runs on **http://localhost:5173** (Vite default).

### 5. Use the App

Open http://localhost:5173

- **Ingest OpenAPI** tab: Provide an API name (e.g. `Petstore`), optional URL (pre-filled with the classic Swagger Petstore), or upload a local `.json`/`.yaml` file. Click "Ingest into Knowledge Graph".
- **Natural Language Query** tab: Type a question about your APIs and hit Search. Results include the LLM's natural language summary + structured rows from the graph.
- **Property Graph** tab: Click "Load / Refresh Graph" to visualize the ingested services and their endpoints.

You can also visit Neo4j Browser (link in the header) to run raw Cypher against the graph.

## Backend API Endpoints

All under the `/api` prefix:

| Method | Endpoint                 | Description                                                                                                                                                                                  |
| ------ | ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| POST   | `/api/ingest`            | Ingest an OpenAPI spec. Multipart form: `apiName` (required), `url` (optional), `file` (optional). Returns plain text status message.                                                        |
| POST   | `/api/discover/semantic` | Natural language semantic search. Body: `{ "query": "your question here" }`. Returns `SemanticSearchResponse` with `naturalLanguageAnswer`, `results[]`, `totalResults`, `processingTimeMs`. |
| GET    | `/api/graph/data`        | Returns graph relationship data for visualization: array of `{ sourceId, sourceName?, targetId, targetName?, relationship }`.                                                                |

**CORS** is pre-configured to allow `http://localhost:5173` and `http://localhost:3000`.

## Frontend Notes

- API base URL is hardcoded in `frontend/src/App.tsx` as `const API_BASE = "http://localhost:8080";`
- The frontend currently calls:
  - Ingest: `POST /ui/ingest`
  - Semantic: `POST /api/discover/semantic` (correct)
  - Graph: `GET /ui/graph/data`
- The backend controller exposes ingest and graph under `/api/...` (not `/ui/...`).
- To make ingest + graph work with the current backend, update the two paths in `App.tsx` from `/ui/` to `/api/`.

(Alternatively, a separate UI controller can be added under `/ui` if preferred.)

## Environment Configuration

Backend configuration lives in `backend/src/main/resources/application.yml`:

- Server port: `8080`
- Neo4j connection (uri, username, password)
- OpenAI model selection (chat + embedding)
- CORS allowed origins
- Vector store settings (dimension 1536, cosine similarity)

Override `OPENAI_API_KEY` via environment variable (already wired with `${OPENAI_API_KEY}`).

## Common Issues & Troubleshooting

- **"Connection refused" to Neo4j**: Start Neo4j and verify bolt port + credentials match `application.yml`.
- **401 / OpenAI errors**: Export `OPENAI_API_KEY` in the same terminal before starting the Spring Boot app.
- **Ingest fails with "Either file or url must be provided"**: Provide at least a URL or a file.
- **Port already in use (8080 or 5173)**:

  ```bash
  # Kill frontend
  lsof -ti:5173 | xargs kill -9 2>/dev/null || true

  # Kill backend (if needed)
  lsof -ti:8080 | xargs kill -9 2>/dev/null || true
  ```

- **Graph tab shows "No data found"**: Ingest at least one API first.
- **CORS errors**: Backend WebConfig allows the Vite origin; restart backend after changing `cors.allowed-origins`.

## Development

For full contribution guidelines, development environment setup, coding standards, branch/PR workflow, and testing expectations, please see the **[Contributing Guide](CONTRIBUTING.md)**.

Quick development commands:

- Backend: `mvn spring-boot:run` (hot reload via DevTools can be added).
- Frontend: `npm run dev` (Vite HMR enabled).
- Lint frontend: `npm run lint`
- Build frontend for production: `npm run build`
- The built JAR is also present at `backend/target/api-discovery-backend-0.0.1-SNAPSHOT.jar`.

## Project Structure (Key Paths)

```
.
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/api/discovery/
│       ├── ApiDiscoveryApplication.java
│       ├── controller/ApiDiscoveryController.java
│       ├── service/ (Ingestion + Discovery + Embedding)
│       ├── model/ (APIService, Endpoint, Parameter...)
│       └── resources/application.yml
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/App.tsx          # Main UI (tabs + cytoscape + axios calls)
├── CONTRIBUTING.md
└── README.md
```

## License / Status

This is a proof-of-concept / internal research project demonstrating Spring AI + Neo4j for enterprise API discoverability.

## Contributing

We welcome contributions of all kinds (bug reports, features, tests, documentation, and improvements).

**For any contribution**, please start by reading the **[Contributing Guide](CONTRIBUTING.md)**. It covers:

- Local development setup and prerequisites
- Coding standards and best practices (backend + frontend)
- Git workflow, commit message conventions, and the pull request process
- How to report bugs and suggest enhancements

High-impact areas include adding test coverage, improving OpenAPI ingestion, enhancing the graph data model, and developer experience improvements.

Thank you for helping improve this project!

---

Built with Spring AI, Neo4j, and React.
