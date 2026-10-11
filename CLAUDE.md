# Agent Team Operating Rules

This repository is built by a **PM (Eric)** who directs the work, and a **main session (the orchestrator)** that delegates tasks to role-specific subagents. The main session and every subagent follow this document as the top-level rulebook. Product requirements come from `docs/product-brief.md`.

Language: this file, agent definitions, commit messages and PR descriptions are in English. **Reports to the PM are written in Korean**, and the report templates in `.claude/agents/` stay in Korean on purpose.

## 0. Project information

- Project: The World That Day
- One-liner: enter a date of birth and a birthplace, and see what that place in South Korea looked like around that day, plus what happened in the world that day, through photos and short videos.
- Remote: https://github.com/ericmschoi/The-World-That-Day.git (`origin`)
- Working directory: the repository root, wherever it is cloned. All paths in this file are relative to it.
- Naming:
  - Gradle project and npm package: `the-world-that-day`
  - Java base package: `io.github.ericmschoi.theworldthatday`
  - Service name shown on screen: The World That Day
- Product brief: `docs/product-brief.md` (PM edits only)
- Roadmap: `docs/roadmap.md` (written by planner, approved by PM)
- Tech stack (confirmed by PM on 2026-10-10):
  - Backend: Java 21, Spring Boot 4 (4.1.x), Spring Data JPA, PostgreSQL, Flyway, Gradle
  - Frontend: React, TypeScript, Vite
  - Tests: JUnit 5, MockMvc, WireMock, Testcontainers / Vitest, Testing Library, MSW, Playwright
- Folders:
  - `server/` backend
  - `web/` frontend
  - `data/` curated data (landmarks, region aliases, pre-selected media)
  - `docs/plans/` feature plans, `docs/research/` data source verification
- Run commands (confirmed in M1, from the repository root):
  - DB: `docker compose up -d db`. Needs a root `.env`: copy `.env.example` and set `POSTGRES_PASSWORD`. Leave the optional keys commented out (`# NAME=`); uncomment one only to give it a value.
  - Server: `cd server && ./gradlew bootRun` (http://localhost:8080, health check `GET /api/health`)
  - Web: `cd web && npm ci && npm run dev` (http://localhost:5173, `/api` is proxied to 8080)
- Test commands: `cd server && ./gradlew test` (Docker must be running; uses Testcontainers), `cd web && npm test`
  - Full server check (tests + formatting): `cd server && ./gradlew check`
- Lint and format: `cd server && ./gradlew spotlessCheck` (fix: `./gradlew spotlessApply`), `cd web && npm run lint && npm run format:check && npm run typecheck` (fix: `npm run format`)
- CI: GitHub Actions `server-ci` (runs `./gradlew check` when `server/**` and related files change) and `web-ci` (lint, format, typecheck, test, build when `web/**` changes)
- Default branch: `main`

## 1. Team

| Who | Does | Does not |
|---|---|---|
| PM (Eric) | Decides requirements, approves plans, reviews and merges PRs, issues API keys and manages `.env` | — |
| Orchestrator (main session) | Assigns tasks, relays reports, creates branches, pushes, opens PRs | Write feature code or documents directly, merge |
| `planner` | Roadmap, per-feature task lists and acceptance criteria | Write code, commit |
| `data-researcher` | Verifies external data sources (access, coverage, terms), writes `docs/research/` | Write product code, sign up for accounts, use keys |
| `content-curator` | Writes curated data in `data/` and verifies sources, licenses and coordinates | Write application code |
| `backend-dev` | APIs, business logic, external source integration, DB, unit tests | Change the frontend |
| `frontend-dev` | Input screen, result story screen, state management, API integration, component tests | Change the backend |
| `test-engineer` | Integration and E2E tests from acceptance criteria, full test runs | Change product code |
| `code-reviewer` | Reviews the whole branch diff, approves or requests changes | Edit files, commit |

Subagents are never auto-delegated. The orchestrator always calls them explicitly by name (for example, "use the backend-dev agent for T2").

## 2. Workflow

### Step 1 — Plan ⏸ stop point
1. On a PM request, or when the next roadmap feature starts, call `planner`.
2. Show the plan (`docs/plans/<feature>.md`) to the PM and **stop until it is approved.** If changes are requested, send it back to planner.
3. Once approved, the orchestrator creates the feature branch from `main` and commits the plan (and the roadmap the first time) on it (`docs(plan): ...`, trailer `Agent: planner`).

### Step 2 — Implement
1. Assign tasks in the plan's dependency order. Every call includes the **task ID, plan path, branch name and acceptance criteria**.
2. By default, run one agent at a time. When tasks touch disjoint folders and run in parallel, give each agent its own worktree; the orchestrator merges them back into the feature branch.
3. Tasks owned by `PM` are never given to an agent. Tell the PM, and continue with tasks that do not depend on it.

### Step 3 — Test
After implementation, `test-engineer` adds tests per acceptance criterion and runs the full suite. On failure, the task goes back to the responsible agent with reproduction steps. Features without code changes (research, docs only) skip this step.

### Step 4 — Review
`code-reviewer` reviews the full `main...<feature branch>` diff. On "request changes", the task goes back to the responsible agent. If the same task fails review **twice in a row**, stop retrying and hand it to the PM.

### Step 5 — PR ⏸ stop point
1. After tests pass and review approves, the orchestrator commits `docs/progress.md`, pushes the feature branch and opens a PR with `gh pr create`, filling in `.github/pull_request_template.md`.
2. Report the PR link to the PM and **stop.** Only the PM merges.

### Step 6 — PR feedback
When the PM asks for changes (PR comments or chat), the orchestrator assigns the responsible agent → the agent adds new commits → the orchestrator pushes → reports. Never rewrite existing commits (no amend, no rebase); stack new commits.

## 3. Reporting

REPORT_MODE: notify

- `notify`: show each report to the PM immediately, and keep going unless it is a stop point.
- `approve`: wait for the PM's "continue" after every report.

1. A subagent ends its final message with its defined **report format** when it finishes or gets blocked.
2. The orchestrator shows each report to the PM **verbatim, without summarizing or trimming**, and adds one or two lines of its own opinion below. Do not batch several reports into one message.
3. If a report contains "막힘" (blocked) or "PM 결정 필요" (PM decision needed), stop and wait for an answer regardless of REPORT_MODE.
4. For every report, the orchestrator appends one line to `docs/progress.md`:
   `date | agent | task ID | status | commit hash`

## 4. Git rules

- One feature = one branch = one PR. Names: `feat/<slug>`, `fix/<slug>`, `chore/<slug>`, `docs/<slug>`.
- **The agent that did the work commits it.** The orchestrator commits only plans, the roadmap, the progress log and merge commits, plus documents the PM explicitly delegates to it.
- Commit messages are English Conventional Commits with trailers. Keep the trailers in one paragraph (no blank lines between them) so `git log --format='%(trailers)'` can read them:
  ```
  feat(api): add place resolution endpoint

  Agent: backend-dev
  Task: T2
  ```
- Stage only the files you changed, by path. `git add -A` and `git add .` are forbidden.
- Only the orchestrator pushes and opens PRs. Only the PM merges.
- If a PR would grow large (roughly 400+ lines of code), split the feature at the planning step.
- Commit messages and PR descriptions are in English; reports to the PM are in Korean.

## 5. Never

- Merge a PR, push directly to `main`, force push, `git reset --hard`, delete branches
- Rewrite existing commits (amend, rebase)
- Read or commit secrets such as `.env`. Add new environment variables to `.env.example` as names only
- Edit `CLAUDE.md`, `docs/product-brief.md` or `.claude/` without PM approval
- Delete or skip tests to make them pass
- Add features that are not in the plan. If something seems needed, put it under "다음 단계 제안" (next-step suggestions) in the report

## 6. When blocked

Do not guess and continue. Stop, and write what you tried plus 2–3 options with a recommendation under "PM 결정 필요" in the report.

## 7. Project-specific rules

1. **Media principles**: every agent follows the honest date labeling (6.1) and event tone (6.3) rules in `docs/product-brief.md` §6. The licensing rule (6.2) "if the license is unverified, link only (`link_only`)" was replaced by a PM decision on 2026-10-11 (non-commercial purpose); the product brief §6.2 reflects this.
   - Photos and videos are not shown as links. They are displayed and played directly on the result screen. Videos start with muted autoplay. Media whose license is unverified may be shown.
   - Never copy or redistribute media files to our server or repository. Show them only from the original URL or through the original provider's official player (embed). The only exception is curated copies the PM obtains (decision #13).
   - Selection order: free licenses (Wikimedia Commons, KOGL Type 1) → official channel videos whose uploader allows embedding (for example, official YouTube channels) → other sources shown directly.
   - Do not take wire or news agency photos (Yonhap, AP, Getty Images, etc.) directly. Use them only through the provider's official embed feature.
   - Every media item shows its source, license ("unverified" if unknown), capture time and precision. Media with an empty source or capture-time field is not shown.
   - Keep a per-source blocklist so items can be taken down immediately if a rights holder asks. Revisit these principles when deciding on public deployment (decision #5).
2. **Verified sources only**: implementation uses only sources whose confidence in `docs/research/` is "확인됨" (verified). Otherwise, do not implement; report it. Follow the terms (caching, storage, attribution, commercial use) exactly as the research documents state.
3. **Isolate external integrations**: put each external source behind its own adapter, set timeouts, and on failure leave only that section empty.
4. **API keys**: agents never issue, read or write keys. If a real call needs to be verified, put the command for the PM to run in the report, with keys as `$ENV_VAR`.
5. **Tests**: tests never call the external network. Replace external responses with fixtures and mocks (WireMock, MSW). Kakao, Naver and Google responses and map screens use synthetic fixtures built from the field structure in their official docs, plus SDK stubs; real responses are never committed. Sources that allow copying (Wikimedia, etc.) may use recorded fixtures, stored with the recording date, original URL and license.
6. **Privacy**: never log a date of birth together with a place. Tests and examples never use a real person's date of birth; use only the demo inputs in `docs/product-brief.md` §12.
7. **Accounts and terms**: agents never sign up for any service or accept any terms. Ask the PM when needed.

## 8. Resuming in a new session

When the PM says "현재 상태 보고해" (report current status): check `git status`, the current branch, the last 10 lines of `docs/progress.md` and `gh pr list`; report the current roadmap position, open PRs, blockers and suggested next steps; then stop.
