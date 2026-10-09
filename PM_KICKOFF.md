# PM 킥오프 가이드 — The World That Day

- 원격 저장소: https://github.com/ericmschoi/The-World-That-Day.git
- 로컬 경로: `/Users/choi/Desktop/Eric/Canada/Canada_job_search/Project/The-World-That-Day`

## 0. 준비 (한 번만, 약 10분)

1. 로컬 폴더를 준비한다.
   ```bash
   cd /Users/choi/Desktop/Eric/Canada/Canada_job_search/Project

   # 아직 clone 하지 않았다면
   git clone https://github.com/ericmschoi/The-World-That-Day.git

   cd The-World-That-Day
   git remote -v   # origin 이 위 주소인지 확인
   ```
   이미 이 경로에 폴더가 있는데 git 저장소가 아니라면, 대신 `git init` 후 `git remote add origin https://github.com/ericmschoi/The-World-That-Day.git` 을 실행한다.

2. 키트를 저장소 루트에 푼다. (zip 경로는 내려받은 위치에 맞게)
   ```bash
   unzip ~/Downloads/the-world-that-day-kit.zip -d .
   ls -a   # .claude, .github, .gitignore, CLAUDE.md, PM_KICKOFF.md, docs, data 확인
   ```
   GitHub에서 README나 .gitignore를 만들어 둔 상태라 덮어쓸지 물으면: README는 아니오(n), .gitignore는 예(y). 기존 .gitignore 내용이 필요했다면 나중에 합친다.

3. 첫 커밋은 PM이 직접 한다. (에이전트는 `main` 에 push할 수 없게 막혀 있다.)
   ```bash
   git add .
   git commit -m "chore: add agent team kit and product brief"
   git branch -M main
   git push -u origin main
   ```
   GitHub 쪽에 이미 다른 커밋이 있어서 push가 거절되면, `git pull origin main --no-rebase` 로 합친 뒤 다시 push 한다.

4. `gh auth status` 로 GitHub CLI 로그인을 확인한다. 안 되어 있으면 `gh auth login`. `ericmschoi` 계정이어야 한다.
5. M1(부트스트랩) 전까지 Java 21, Node 20 이상, Docker를 준비한다. M0에는 필요 없다.
6. API 키는 아직 필요 없다. M0가 끝나면 어떤 키가 필요한지 보고서로 온다. 키는 PM이 발급받아 `.env` 에 직접 넣는다. 에이전트는 `.env` 를 읽을 수 없게 막혀 있다.
7. 저장소 폴더(`.../Project/The-World-That-Day`)에서 Claude Code를 실행한다. 권한 모드는 기본값이나 `acceptEdits` 를 권장한다. `bypassPermissions` 는 쓰지 않는다.
8. Claude Code에서 `/agents` 로 에이전트 7개가 보이는지 확인한다.

## 1. 킥오프 프롬프트 (그대로 붙여넣기)

```text
너는 The World That Day 저장소의 오케스트레이터다. 나는 PM Eric이다.
CLAUDE.md를 최상위 규칙으로 따르고, 제품 요구사항은 docs/product-brief.md가 원천이다. 두 문서를 먼저 끝까지 읽어.

## 지금 할 일

1. 사전 점검
   아래를 확인해서 표(항목 | 결과 | 비고)로 보고해. 하나라도 실패하면 고치려 하지 말고 멈춰.
   - 작업 디렉터리가 /Users/choi/Desktop/Eric/Canada/Canada_job_search/Project/The-World-That-Day
   - git 저장소이고 현재 브랜치가 main, 작업 트리가 깨끗함
   - origin 원격 주소가 https://github.com/ericmschoi/The-World-That-Day.git 이고, origin/main 과 로컬 main 이 같음
   - gh auth status 통과, gh repo view ericmschoi/The-World-That-Day 로 저장소에 접근 가능
   - .claude/agents/ 에 planner, data-researcher, content-curator, backend-dev, frontend-dev, test-engineer, code-reviewer 7개 파일이 있음
   - .gitignore 에 .env 가 있음

2. 로드맵과 첫 계획
   점검이 통과하면 planner 에이전트를 호출해. 넘길 내용:
   - docs/product-brief.md 를 바탕으로 docs/roadmap.md 작성
   - 첫 기능 "M0 데이터 출처 리서치"의 계획서 docs/plans/m0-source-research.md 작성
     - 브랜치: docs/m0-source-research
     - 작업은 data-researcher가 영역별로 나눠 맡는다: 장소 검색·좌표, 과거 항공사진, 현재 위성·로드뷰, 기록 사진·영상, 그날의 사건
     - 마지막 작업은 data-researcher의 docs/research/summary.md (영역별 1순위, 필요한 API 키 목록, 제품 정의서 가정 중 뒤집힌 것, PM 결정 목록)

3. planner의 보고를 CLAUDE.md 3절대로 그대로 보여주고, 1단계 정지점에서 내 승인을 기다려.

내가 승인하면 CLAUDE.md 2절 흐름대로 M0를 끝까지 진행해.
- data-researcher 작업은 한 번에 하나씩 순차로 돌려.
- M0는 코드가 없으니 3단계(테스트)는 건너뛰고, code-reviewer가 리서치 문서의 근거 링크와 확신도를 리뷰해.
- PR을 열면 링크를 보고하고 멈춰.
```

## 2. 자주 쓰는 PM 명령

| 상황 | 이렇게 말한다 |
|---|---|
| 계획 승인 | `승인. 진행해.` |
| 계획 수정 | `계획 수정: <바꿀 내용>. planner에게 다시 맡겨.` |
| 결정 답변 | `결정: <질문>은 <선택>으로. 이유: <한 줄>` |
| ★ 작업 직접 하기 | `T3는 내가 직접 할게. T3에 의존하지 않는 작업부터 진행해.` |
| ★ 작업 끝냈을 때 | `T3 끝냈어. 커밋 <해시>. 다음 진행해.` |
| PR 수정 지시 | `PR #<번호> 코멘트 반영해줘. gh pr view <번호> --comments 읽고 담당 에이전트에게 배정해.` |
| 머지 후 다음 | `PR #<번호> 머지했어. main 최신화하고 로드맵 다음 기능으로 planner 호출해.` |
| 새 세션에서 재개 | `CLAUDE.md 8절대로 현재 상태 보고하고 멈춰.` |
| 급정지 | `지금 하던 거 멈추고 현재 상태만 보고해.` |
| 보고마다 멈추기 | CLAUDE.md의 `REPORT_MODE` 를 `approve` 로 직접 바꾼다 |

## 3. M0가 끝나면 PM이 정할 것

`docs/research/summary.md` 를 보고 정한 뒤, 결정은 `docs/product-brief.md` 13절에 직접 기록한다.

- [ ] 상업적 이용 여부 (쓸 수 있는 공공누리 유형이 달라진다)
- [ ] 지도·로드뷰 제공자 (카카오 / 네이버 / 구글)
- [ ] 지원 시작 연도
- [ ] 영상 처리 방식 (원 제공자 임베드 / 링크만)
- [ ] 기술 스택 확정 (CLAUDE.md 0절)
- [ ] 필요한 API 키 발급 → `.env` 에 넣기
- [ ] 제품 정의서 가정 중 뒤집힌 것 반영

## 4. 알아둘 것

- `.claude/settings.json` 이 force push, main push, rebase, amend, 머지, `git add -A`, `.env` 읽기를 막는다. 흔한 형태의 명령을 막는 것이지 완벽한 보안 장치는 아니다. 최종 방어선은 PR 검수다.
- `CLAUDE.md`, `docs/product-brief.md`, `.claude/` 를 에이전트가 고치려 하면 승인을 묻는다.
- 리서치 중 처음 보는 사이트를 열 때 WebFetch 승인을 물을 수 있다. 공식 문서 사이트면 허용해도 된다.
