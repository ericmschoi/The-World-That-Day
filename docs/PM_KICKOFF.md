# PM Kickoff Guide — The World That Day

How this project was started: the PM sets up the repository, then directs an AI agent team (an orchestrator session plus seven role-specific subagents defined in `.claude/agents/`) under the rules in `CLAUDE.md`. This guide is kept as a record of that setup.

- Remote: https://github.com/ericmschoi/The-World-That-Day.git
- Local path: `<your-workspace>/The-World-That-Day` (anywhere you like; all project paths are relative to the repository root)

## 0. Setup (once, about 10 minutes)

1. Prepare the local folder.
   ```bash
   cd <your-workspace>

   # if not cloned yet
   git clone https://github.com/ericmschoi/The-World-That-Day.git

   cd The-World-That-Day
   git remote -v   # check that origin is the URL above
   ```
   If the folder already exists but is not a git repository, run `git init` and `git remote add origin https://github.com/ericmschoi/The-World-That-Day.git` instead.

2. Unpack the starter kit at the repository root (adjust the zip path to where you downloaded it).
   ```bash
   unzip ~/Downloads/the-world-that-day-kit.zip -d .
   ls -a   # expect .claude, .github, .gitignore, CLAUDE.md, docs, data
   ```
   If GitHub already created a README or .gitignore and you are asked whether to overwrite: README no (n), .gitignore yes (y). Merge the old .gitignore content later if you needed it.

3. The PM makes the first commit. (Agents are blocked from pushing to `main`.)
   ```bash
   git add .
   git commit -m "chore: add agent team kit and product brief"
   git branch -M main
   git push -u origin main
   ```
   If the push is rejected because GitHub already has other commits, merge with `git pull origin main --no-rebase` and push again.

4. Check the GitHub CLI login with `gh auth status`. If needed, run `gh auth login`. It must be the `ericmschoi` account.
5. Before M1 (bootstrap), install Java 21, Node 24 and Docker. M0 does not need them.
6. No API keys are needed yet. After M0, a report lists which keys are required. The PM issues keys and puts them in `.env`; agents are blocked from reading `.env`.
7. Start Claude Code in the repository folder. Use the default permission mode or `acceptEdits`. Do not use `bypassPermissions`.
8. In Claude Code, run `/agents` and check that all seven agents are listed.

## 1. Kickoff prompt (paste as is)

The PM works in Korean, so the original prompt was Korean. This is the English version.

```text
You are the orchestrator of The World That Day repository. I am the PM, Eric.
Follow CLAUDE.md as the top-level rulebook; docs/product-brief.md is the source of product requirements. Read both documents to the end first.

## What to do now

1. Pre-flight check
   Check the items below and report them as a table (item | result | notes). If any item fails, do not try to fix it; stop.
   - The working directory is the repository root
   - It is a git repository, the current branch is main, and the working tree is clean
   - The origin remote is https://github.com/ericmschoi/The-World-That-Day.git, and origin/main equals local main
   - gh auth status passes, and gh repo view ericmschoi/The-World-That-Day can access the repository
   - .claude/agents/ contains seven files: planner, data-researcher, content-curator, backend-dev, frontend-dev, test-engineer, code-reviewer
   - .gitignore contains .env

2. Roadmap and first plan
   If the check passes, call the planner agent with:
   - Write docs/roadmap.md based on docs/product-brief.md
   - Write the plan for the first feature, "M0 data source research", at docs/plans/m0-source-research.md
     - Branch: docs/m0-source-research
     - data-researcher splits the work by area: place search and coordinates, historical aerial photos, current satellite and street view, archive photos and videos, day-of events
     - The last task is data-researcher's docs/research/summary.md (first choice per area, required API keys, product brief assumptions that turned out wrong, PM decisions)

3. Show the planner's report verbatim as CLAUDE.md §3 requires, and wait for my approval at the Step 1 stop point.

Once I approve, run M0 to the end following the CLAUDE.md §2 workflow.
- Run data-researcher tasks one at a time.
- M0 has no code, so skip Step 3 (tests); code-reviewer reviews the evidence links and confidence levels in the research documents.
- When you open the PR, report the link and stop.
```

## 2. Common PM commands

| Situation | Say |
|---|---|
| Approve a plan | `승인. 진행해.` (Approved. Go ahead.) |
| Change a plan | `계획 수정: <change>. planner에게 다시 맡겨.` (Revise the plan; send it back to planner.) |
| Answer a decision | `결정: <question>은 <choice>으로. 이유: <one line>` |
| Do a ★ task yourself | `T3는 내가 직접 할게. T3에 의존하지 않는 작업부터 진행해.` (I'll do T3; continue with tasks that don't depend on it.) |
| Finished a ★ task | `T3 끝냈어. 커밋 <hash>. 다음 진행해.` |
| Address PR feedback | `PR #<n> 코멘트 반영해줘.` (Have the owning agent address PR #n's comments.) |
| After merging | `PR #<n> 머지했어. 다음 진행해.` (Merged; update main and continue with the next roadmap feature.) |
| Resume in a new session | `CLAUDE.md 8절대로 현재 상태 보고하고 멈춰.` (Report status per CLAUDE.md §8 and stop.) |
| Emergency stop | `지금 하던 거 멈추고 현재 상태만 보고해.` (Stop and report status only.) |
| Stop after every report | Change `REPORT_MODE` in CLAUDE.md to `approve` |

## 3. Decisions for the PM after M0

Decide after reading `docs/research/summary.md`, and record the decisions in `docs/product-brief.md` §13.

- [ ] Commercial use or not (changes which KOGL types are usable)
- [ ] Map and street view provider (Kakao / Naver / Google)
- [ ] Earliest supported birth year
- [ ] How to handle video
- [ ] Confirm the tech stack (CLAUDE.md §0)
- [ ] Issue the required API keys → put them in `.env`
- [ ] Apply product brief assumptions that turned out wrong

## 4. Good to know

- `.claude/settings.json` blocks force push, pushing to main, rebase, amend, merge, `git add -A` and reading `.env`. It blocks the common forms of these commands; it is not a complete security boundary. The final safeguard is PR review.
- Agents are asked for approval before editing `CLAUDE.md`, `docs/product-brief.md` or `.claude/`.
- Research may trigger a WebFetch approval when opening a site for the first time. Allow it for official documentation sites.
