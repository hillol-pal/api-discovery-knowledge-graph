# Enterprise API Discovery - React Frontend

Professional React + TypeScript + Vite + Tailwind + Cytoscape.js frontend for the Enterprise API Discovery POC (LLM + Neo4j Knowledge Graph).

## Features

- **Ingest OpenAPI**: Upload Swagger/JSON/YAML or provide URL + API name. Sends to backend `/ui/ingest`.
- **Natural Language Query**: Semantic search over APIs using vector + LLM. Results displayed as formatted JSON.
- **Property Graph**: Interactive visualization of ApiService → Endpoint relationships using Cytoscape.js (blue = services, green = endpoints).

## Tech Stack

- React 19 + TypeScript + Vite
- Tailwind CSS 3
- Axios
- Cytoscape.js for graph
- Lucide icons

## Setup & Run

```bash
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173

**Backend**: Expects Spring Boot on `http://localhost:8080` with endpoints:

- `POST /ui/ingest` (multipart/form-data: apiName, url?, file?)
- `POST /api/discover/semantic` { "query": "..." }
- `GET /ui/graph/data` → array of {sourceId, sourceName?, targetId, targetName?, relationship}

Make sure CORS is enabled on backend (see previous POC instructions).

## Customization

- Change `API_BASE` in `src/App.tsx` if backend runs elsewhere.
- Graph styles/colors easily editable in the `cytoscape` style array.
- Extend query results rendering from raw JSON to rich cards as needed.

Built as part of the LLM + Knowledge Graph Enterprise API Discovery POC.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(["dist"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ["./tsconfig.node.json", "./tsconfig.app.json"],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
]);
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from "eslint-plugin-react-x";
import reactDom from "eslint-plugin-react-dom";

export default defineConfig([
  globalIgnores(["dist"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs["recommended-typescript"],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ["./tsconfig.node.json", "./tsconfig.app.json"],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
]);
```

# Kill port and run

lsof -ti:5173 | xargs kill -9 2>/dev/null;

run command dev
