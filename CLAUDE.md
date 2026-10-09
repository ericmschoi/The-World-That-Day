# 에이전트 팀 운영 규칙

이 저장소는 **PM(Eric)** 이 지휘하고, **메인 세션(오케스트레이터)** 이 역할별 서브에이전트에게 일을 맡기는 방식으로 개발한다. 메인 세션과 모든 서브에이전트는 이 문서를 최상위 규칙으로 따른다. 제품 요구사항은 `docs/product-brief.md` 가 원천이다.

## 0. 프로젝트 정보

- 프로젝트: The World That Day
- 한 줄 설명: 생년월일과 태어난 장소를 입력하면, 그날 그곳의 대한민국 풍경과 그날 세계에서 일어난 일을 사진과 짧은 영상으로 보여주는 서비스
- 원격 저장소: https://github.com/ericmschoi/The-World-That-Day.git (`origin`)
- PM 로컬 경로: `/Users/choi/Desktop/Eric/Canada/Canada_job_search/Project/The-World-That-Day`
- 이름 규칙:
  - Gradle 프로젝트·npm 패키지: `the-world-that-day`
  - Java 기본 패키지: `io.github.ericmschoi.theworldthatday`
  - 화면에 보이는 서비스 이름: The World That Day
- 제품 정의서: `docs/product-brief.md` (PM만 수정)
- 로드맵: `docs/roadmap.md` (planner 작성, PM 승인)
- 기술 스택 (PM 초안, M0 끝에 확정):
  - 백엔드: Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Flyway, Gradle
  - 프론트엔드: React, TypeScript, Vite
  - 테스트: JUnit 5, MockMvc, WireMock, Testcontainers / Vitest, Testing Library, MSW, Playwright
- 폴더 구조:
  - `server/` 백엔드
  - `web/` 프론트엔드
  - `data/` 큐레이션 데이터 (대표 명소, 지역 별칭, 미리 골라둔 미디어)
  - `docs/plans/` 기능별 계획서, `docs/research/` 데이터 출처 검증 문서
- 실행 명령: (M1에서 확정 후 PM이 갱신) 예정: `docker compose up -d db`, `cd server && ./gradlew bootRun`, `cd web && npm run dev`
- 테스트 명령: (M1에서 확정) 예정: `cd server && ./gradlew test`, `cd web && npm test`
- 린트·포맷 명령: (M1에서 확정)
- 기본 브랜치: `main`

## 1. 팀 구성

| 누구 | 하는 일 | 하지 않는 일 |
|---|---|---|
| PM (Eric) | 요구사항 결정, 계획 승인, PR 검수와 머지, API 키 발급과 `.env` 관리 | — |
| 오케스트레이터 (메인 세션) | 작업 배정, 보고 전달, 브랜치 생성, push, PR 생성 | 기능 코드·문서 직접 작성, 머지 |
| `planner` | 로드맵, 기능별 작업 목록·완료 기준 | 코드 작성, 커밋 |
| `data-researcher` | 외부 데이터 출처의 접근 방법·커버리지·약관 검증, `docs/research/` 작성 | 제품 코드 작성, 계정 가입·키 사용 |
| `content-curator` | `data/` 큐레이션 데이터 작성과 출처·라이선스·좌표 검증 | 애플리케이션 코드 작성 |
| `backend-dev` | API, 비즈니스 로직, 외부 출처 연동, DB, 단위 테스트 | 프론트엔드 수정 |
| `frontend-dev` | 입력 화면, 결과 이야기 화면, 상태 관리, API 연동, 컴포넌트 테스트 | 백엔드 수정 |
| `test-engineer` | 완료 기준 기반 통합·E2E 테스트, 전체 테스트 실행 | 제품 코드 수정 |
| `code-reviewer` | 브랜치 전체 diff 리뷰, 승인·수정 요청 판정 | 파일 수정, 커밋 |

서브에이전트는 자동 위임에 맡기지 않는다. 오케스트레이터가 항상 이름으로 명시해서 호출한다. (예: "backend-dev 에이전트로 T2 진행")

## 2. 작업 흐름

### 1단계 — 계획 ⏸ 정지점
1. PM의 요청이나 로드맵의 다음 기능을 받으면 `planner` 를 호출한다.
2. 계획서(`docs/plans/<feature>.md`)를 PM에게 보여주고 **승인을 받을 때까지 멈춘다.** 수정 요청이 오면 planner에게 다시 맡긴다.
3. 승인되면 오케스트레이터가 `main` 에서 기능 브랜치를 만들고, 그 위에 계획서(처음이면 로드맵도)를 커밋한다. (`docs(plan): ...`, 트레일러 `Agent: planner`)

### 2단계 — 구현
1. 계획서의 의존 순서대로 작업을 배정한다. 호출할 때 **작업 ID, 계획서 경로, 브랜치 이름, 완료 기준**을 함께 넘긴다.
2. 기본은 한 번에 한 에이전트씩 순차 실행. 서로 다른 폴더만 건드리는 작업을 병렬로 돌릴 때는 에이전트마다 worktree 격리를 쓰고, 끝나면 오케스트레이터가 기능 브랜치로 합친다.
3. 담당이 `PM` 인 작업은 에이전트에게 맡기지 않는다. PM에게 알리고, 그 작업에 의존하지 않는 작업을 먼저 진행한다.

### 3단계 — 테스트
구현이 끝나면 `test-engineer` 가 완료 기준별 테스트를 추가하고 전체 테스트를 돌린다. 실패하면 원인 담당 에이전트에게 재현 방법과 함께 돌려보낸다. 코드 변경이 없는 기능(리서치, 문서만)은 이 단계를 건너뛴다.

### 4단계 — 리뷰
`code-reviewer` 가 `main...<기능 브랜치>` 전체 diff를 리뷰한다. "수정 요청"이면 해당 에이전트에게 돌려보낸다. 같은 작업이 **2번 연속** 리뷰를 통과하지 못하면 더 시도하지 않고 PM에게 넘긴다.

### 5단계 — PR ⏸ 정지점
1. 테스트 통과 + 리뷰 승인 후, 오케스트레이터가 `docs/progress.md` 를 커밋하고 기능 브랜치를 push 한 뒤 `gh pr create` 로 PR을 연다. 본문은 `.github/pull_request_template.md` 를 채운다.
2. PR 링크를 PM에게 보고하고 **멈춘다.** 머지는 PM만 한다.

### 6단계 — PR 피드백
PM이 PR 코멘트나 채팅으로 수정을 지시하면, 오케스트레이터가 해당 에이전트에게 배정 → 에이전트가 새 커밋 → 오케스트레이터가 push → 보고한다. 기존 커밋은 고치지 않고(amend, rebase 금지) 새 커밋을 쌓는다.

## 3. 보고 규칙

REPORT_MODE: notify

- `notify`: 각 보고를 PM에게 즉시 보여주고, 정지점이 아니면 다음 작업을 계속한다.
- `approve`: 매 보고마다 PM의 "계속"을 기다린다.

1. 서브에이전트는 작업을 마치거나 막히면 정해진 **보고 형식**으로 마지막 메시지를 끝낸다.
2. 오케스트레이터는 보고를 받는 즉시 **요약하거나 빼지 말고 그대로** PM에게 보여주고, 그 아래에 자기 의견을 한두 줄 덧붙인다. 여러 보고를 모아서 한 번에 내지 않는다.
3. 보고에 "막힘" 이나 "PM 결정 필요" 가 있으면 REPORT_MODE와 관계없이 멈추고 답을 기다린다.
4. 오케스트레이터는 보고마다 `docs/progress.md` 에 한 줄을 추가한다:
   `날짜 | 에이전트 | 작업 ID | 상태 | 커밋 해시`

## 4. Git 규칙

- 기능 하나 = 브랜치 하나 = PR 하나. 이름은 `feat/<slug>`, `fix/<slug>`, `chore/<slug>`, `docs/<slug>`.
- 커밋은 **작업한 에이전트가 직접** 한다. 오케스트레이터는 계획서, 로드맵, 진행 로그, 병합 커밋만 한다.
- 커밋 메시지는 영어 Conventional Commits에 트레일러를 붙인다:
  ```
  feat(api): add place resolution endpoint

  Agent: backend-dev
  Task: T2
  ```
- stage는 자기가 바꾼 파일만 경로를 지정해서 한다. `git add -A`, `git add .` 금지.
- push와 PR 생성은 오케스트레이터만, 머지는 PM만 한다.
- PR 하나가 커질 것 같으면(코드 기준 대략 400줄 이상) 계획 단계에서 기능을 나눈다.
- 커밋 메시지와 PR 본문은 영어, PM에게 하는 보고는 한국어.

## 5. 절대 하지 않는 것

- PR 머지, `main` 직접 push, force push, `git reset --hard`, 브랜치 삭제
- 기존 커밋 수정(amend, rebase)
- `.env` 등 비밀값 읽기·커밋. 새 환경변수는 `.env.example` 에 키 이름만 추가
- PM 승인 없이 `CLAUDE.md`, `docs/product-brief.md`, `.claude/` 수정
- 테스트를 지우거나 skip 처리해서 통과시키기
- 계획서에 없는 기능 추가. 필요해 보이면 보고서의 "다음 단계 제안"에 적는다.

## 6. 막혔을 때

추측으로 진행하지 말고 멈춘다. 시도한 것, 선택지 2~3개와 추천안을 보고서의 "PM 결정 필요"에 적는다.

## 7. 이 프로젝트의 추가 규칙

1. **미디어 원칙**: `docs/product-brief.md` 6절(정직한 날짜 표기, 라이선스, 사건의 톤)은 모든 에이전트가 지킨다. 미디어 레코드에 필수 필드가 비어 있으면 화면에 직접 보여주지 않는다.
2. **검증된 출처만**: 구현에는 `docs/research/` 에서 확신도가 "확인됨"인 출처만 쓴다. 아니라면 구현하지 말고 보고한다. 약관 조항(캐싱, 저장, 출처 표시, 상업 이용)은 리서치 문서에 적힌 대로 지킨다.
3. **외부 연동 격리**: 외부 출처는 하나씩 어댑터 뒤에 두고, 타임아웃을 걸고, 실패하면 그 섹션만 비운다.
4. **API 키**: 에이전트는 키를 발급받거나 읽거나 쓰지 않는다. 실제 호출 확인이 필요하면 PM이 실행할 명령(키는 `$ENV_VAR` 형태)을 보고서에 적는다.
5. **테스트**: 테스트는 외부 네트워크를 호출하지 않는다. 외부 응답은 녹화한 fixture와 목(WireMock, MSW)으로 대신한다.
6. **개인정보**: 로그에 생년월일과 장소를 함께 남기지 않는다. 테스트와 예시에는 실제 사람의 생년월일을 쓰지 않고 `docs/product-brief.md` 12절의 데모 입력만 쓴다.
7. **계정·약관**: 에이전트는 어떤 서비스에도 가입하거나 약관에 동의하지 않는다. 필요하면 PM에게 요청한다.

## 8. 새 세션에서 이어서 할 때

PM이 "현재 상태 보고해"라고 하면: `git status`, 현재 브랜치, `docs/progress.md` 마지막 10줄, `gh pr list` 를 확인하고 로드맵에서 지금 위치, 열린 PR, 막힌 것, 다음 단계 제안을 보고한 뒤 멈춘다.
