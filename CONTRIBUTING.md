# Contributing to Enterprise API Discovery - Knowledge Graph

Thank you for your interest in contributing to the Enterprise API Discovery Knowledge Graph project!

This is an LLM-powered API discovery and cataloging tool built with **Spring AI**, **Neo4j**, and **React**. We welcome contributions that improve functionality, reliability, developer experience, documentation, or the overall architecture.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Enhancements](#suggesting-enhancements)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards & Guidelines](#coding-standards--guidelines)
- [Git Workflow & Branching](#git-workflow--branching)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)
- [Documentation](#documentation)
- [Questions?](#questions)

## Code of Conduct

We are committed to providing a welcoming and inclusive environment. By participating in this project, you agree to uphold respectful and professional behavior.

Please report unacceptable behavior by opening a private issue or contacting the maintainers.

## Getting Started

1. Read the main [README.md](README.md) for project goals, architecture, and quick start instructions.
2. Fork the repository and clone your fork locally.
3. Set up the required environment (see [Development Setup](#development-setup)).
4. Create a branch for your changes (see [Git Workflow](#git-workflow--branching)).
5. Make your changes, test them, and submit a Pull Request.

## How Can I Contribute?

- **Bug fixes** — Fix issues in ingestion, semantic search, graph visualization, or configuration.
- **Features** — New query capabilities, better OpenAPI parsing, improved graph modeling, UI enhancements.
- **Tests** — The project currently has limited automated tests. Adding unit, integration, or E2E tests is highly valued.
- **Documentation** — Improve README, add examples, clarify architecture, or expand API docs.
- **DX / Tooling** — Better dev scripts, Docker Compose support, CI configuration, linting/formatting improvements.
- **Refactoring** — Clean up technical debt while preserving behavior (especially around frontend/backend endpoint alignment).
- **Deployment to Public Cloud** - How this can be scaled to a publicly available product

If you're unsure where to start, look for issues labeled `good first issue` or `help wanted` (we will add these over time).

## Reporting Bugs

Before creating a bug report, please:

- Search existing issues to avoid duplicates.
- Reproduce the issue with the latest `main` branch.
- Collect relevant details (error messages, logs, steps, environment).

When you create an issue, use the **Bug Report** template and include:

- Clear title and description
- Steps to reproduce
- Expected vs actual behavior
- Screenshots or logs if applicable
- Version of Java, Node, Neo4j, and any relevant environment variables (never paste real API keys)

## Suggesting Enhancements

We love ideas that make API discovery more powerful or easier to use.

Use the **Feature Request** template and describe:

- The problem you're trying to solve
- Your proposed solution
- Alternative solutions considered
- Any relevant OpenAPI or graph modeling considerations

Major feature ideas are best discussed in an issue before significant implementation work begins.

## Development Setup

Follow the setup instructions in the [README](README.md#quick-start). Key requirements:

### Required

- **Java 21** (JDK)
- **Maven** 3.9+
- **Node.js** 18+
- **Neo4j** (Community or Enterprise) running locally on `bolt://localhost:7687` with user `neo4j` / `password`
- **OpenAI API key** exported as `OPENAI_API_KEY`

### Quick Verification

```bash
# Backend
cd backend
mvn clean compile

# Frontend
cd frontend
npm install
npm run lint
```

See the full [Quick Start](README.md#quick-start) and [Troubleshooting](README.md#common-issues--troubleshooting) sections in the README.

## Project Structure

```
.
├── backend/                  # Spring Boot 3.3 + Spring AI
│   ├── src/main/java/com/api/discovery/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── model/            # Neo4j @Node entities (APIService, Endpoint, etc.)
│   │   ├── dto/
│   │   ├── repository/
│   │   ├── config/
│   │   └── ApiDiscoveryApplication.java
│   └── src/main/resources/application.yml
├── frontend/                 # React 19 + TypeScript + Vite + Tailwind + Cytoscape
│   ├── src/
│   │   ├── App.tsx           # Main tabs + ingest/query/graph UI
│   │   └── ...
│   └── package.json
└── README.md
```

## Coding Standards & Guidelines

### General

- Keep changes focused and small when possible. Large refactors should be broken into logical PRs.
- Never commit secrets, API keys, or local configuration overrides.
- Update documentation (README, code comments, or this file) when behavior or setup changes.

### Backend (Java / Spring Boot)

- Follow standard Spring Boot and Spring Data Neo4j patterns.
- Prefer constructor injection (current style in the codebase).
- Use Lombok (`@Data`, `@Slf4j`, `@RequiredArgsConstructor`) where it improves readability without hiding important logic.
- Use `log` for logging instead of `System.out`.
- Validate inputs on controllers (see existing use of `@Valid` and `jakarta.validation`).
- Keep the ingestion and discovery logic clearly separated (see `ApiIngestionService` and `ApiDiscoveryService`).
- Run `mvn clean compile` (or `mvn spring-boot:run`) before submitting a PR that touches the backend.

### Frontend (React + TypeScript)

- Use TypeScript strictly. Avoid `any` unless absolutely necessary; prefer proper interfaces/types.
- Follow existing patterns: functional components + hooks, clear state management in components.
- Run `npm run lint` and fix all warnings/errors before committing.
- Keep the `API_BASE` configuration obvious (currently in `App.tsx`).
- Prefer Lucide icons for new UI elements to maintain visual consistency.

### Styling

- Tailwind CSS is used. Keep class names organized and avoid unnecessary custom CSS when Tailwind utilities suffice.
- The graph visualization (Cytoscape) styles live in `App.tsx` — keep them readable.

## Git Workflow & Branching

We follow a simple GitHub flow:

1. Fork the repo (if you don't have write access).
2. Create a feature branch from `main`:
   - `feature/add-graphql-support`
   - `fix/ingestion-idempotency`
   - `docs/improve-contributing-guide`
   - `refactor/split-ingestion-logic`
3. Make commits with clear messages.
4. Push your branch and open a Pull Request against `main`.
5. Keep your branch up to date with `main` (rebase or merge as needed).
6. Respond to review feedback promptly.

**Do not push directly to `main`**.

## Commit Message Guidelines

We strongly encourage **Conventional Commits** for clarity and to enable future automation (changelogs, releases).

Format:

```
<type>(<scope>): <short summary>

[optional body]

[optional footer]
```

Common types:

- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Formatting, missing semicolons, etc. (no code change)
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding or updating tests
- `chore`: Maintenance tasks, dependency bumps, etc.
- `ci`: CI configuration changes

Examples:

```
feat(ingest): support parsing response schemas from OpenAPI
fix(frontend): correct API endpoint paths for ingest and graph
docs: add Neo4j Desktop instructions to README
chore(deps): bump spring-ai to latest milestone
```

## Pull Request Process

1. Ensure your branch is up to date with `main` and all conflicts are resolved.
2. Run relevant checks:
   - Backend: `mvn clean compile`
   - Frontend: `npm run lint`
3. Update the README or other docs if your change affects usage or setup.
4. Fill out the Pull Request template completely.
5. Link related issues using keywords (`Closes #123`, `Fixes #45`).
6. Request review. At least one maintainer approval is required before merge.
7. Be prepared to make additional changes based on feedback.

### PR Checklist (use as reference)

- [ ] I have read this CONTRIBUTING guide
- [ ] My branch follows the naming convention
- [ ] I have run lint / compile locally
- [ ] I have added or updated tests where feasible
- [ ] I have updated documentation (README, comments, or architecture notes)
- [ ] My commit messages follow the guidelines
- [ ] The PR description clearly explains the change and why it is needed
- [ ] No secrets or credentials are included in the diff

## Testing

The project currently has **limited automated test coverage**. This is an area where contributions have high impact.

- Backend tests would use standard Spring Boot test support (`@SpringBootTest`, `@DataNeo4jTest`, Mockito, etc.).
- Frontend tests could be added using Vitest + React Testing Library (not yet configured).
- When adding tests, place them following standard Maven (`src/test/java`) and frontend conventions.
- For changes that are hard to unit test (LLM-dependent flows), consider adding clear integration notes or manual test steps in the PR description.

If you add a testing framework or significant test coverage, please document the new commands in the README.

## Documentation

- Keep the main README accurate and up to date.
- Add Javadoc or inline comments for complex logic (especially around OpenAPI mapping and graph modeling).
- When adding new environment configuration, document it in `application.yml` comments and the README "Environment Configuration" section.

## Questions?

- Open a GitHub Discussion (if enabled) or a new issue with the `question` label.
- For quick questions, a well-described issue is preferred over direct messages so the whole community can benefit from the answers.

---

We appreciate every contribution — whether it's a one-line fix, a major feature, or simply reporting a confusing part of the documentation. Thank you for helping make enterprise API discovery more accessible!

**Happy contributing!**
