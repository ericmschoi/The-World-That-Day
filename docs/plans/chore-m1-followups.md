# M1 후속 정리 (chore)

> 브랜치: `chore/m1-followups` · 작성: planner · 상태: PM 승인 (2026-10-11)
> 기준: M1 리뷰에서 미룬 권장 항목(오케스트레이터 전달, 2026-10-10), `docs/plans/m1a-bootstrap-server.md`, `docs/plans/m1b-bootstrap-web.md`, `CLAUDE.md` 4절·7절
> 위치: M3 앞의 작은 PR (로드맵 M3 절, 결정 M3-P2). M3a1(`feat/wiki-events-source`)과 바꾸는 파일이 겹치지 않으므로, 이 PR을 연 뒤에는 머지를 기다리지 않고 M3a1을 `main` 에서 시작해도 된다.

## 목표

1. 루트 `.env.example` 이 바뀌면 `server-ci` 가 돈다.
2. PM이 `.env.example` 을 그대로 복사해 `.env` 를 만들어도, 비워 둔 선택 항목이 빈 문자열로 기본값을 덮어쓰지 않는다. (Spring은 `POSTGRES_DB=` 를 "빈 값으로 설정됨"으로 읽어 `application.yml` 의 기본값을 쓰지 않는다)
3. `docker-compose.yml` 과 `application.yml` 의 기본값(DB 이름, 사용자, 포트)이 어긋나면 테스트가 실패한다.
4. M1 계획서의 줄 수 명령과 CI 경로 필터 설명의 보정 내용을 기록한다(아래 "M1 계획서 보정 기록").

## 범위 밖

- 웹 쪽 미룬 항목: API 클라이언트 타임아웃·`AbortSignal`, effect 기반 요청 테스트 규칙 → M3b2. `StorySection` 의 `id` 를 `SectionId` 타입으로 → M3b1. 화면 전환 포커스 이동 → M2b (로드맵 "M1 리뷰에서 미룬 항목")
- 새 환경변수, 도메인 코드, `application.yml` 값 변경
- GitHub 브랜치 보호·필수 상태 검사 (M1a 리스크 4 — 켤 때 따로)
- 완료된 M1 계획서 본문 수정 (아래 보정 기록으로 대신한다)

## 사용할 외부 출처

없음.

## 바꾸는 것

### 1. server-ci 경로 필터

`pull_request` 와 `push` 두 곳 모두 아래 4개가 되게 한다(순서 유지, `.env.example` 만 추가).

```yaml
paths:
  - "server/**"
  - "docker-compose.yml"
  - ".env.example"
  - ".github/workflows/server-ci.yml"
```

이유: `application.yml` 이 루트 `.env` 를 읽고(`spring.config.import`), `LocalDatabaseComposeTest` 가 `.env.example` 을 검사한다. 지금은 `.env.example` 만 고친 PR에서 그 테스트가 돌지 않는다.

### 2. `.env.example` 선택 항목 주석 처리

선택 항목 3개(`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PORT`)는 `# NAME=` 로 둔다. 필수 항목 2개(`POSTGRES_PASSWORD`, `WIKIMEDIA_USER_AGENT`)는 지금처럼 `NAME=` 로 둔다. "If you leave it unset, delete this line" 문장은 지우고 "Uncomment to override" 로 바꾼다. 예:

```
# Database name. Optional, default the_world_that_day. Uncomment to override.
# POSTGRES_DB=
```

- `WIKIMEDIA_USER_AGENT` 는 M3a1부터 서버가 읽는다. 비어 있으면 Wikimedia 출처를 끄고 외부 호출을 하지 않는다(M3a1 계획서). 그래서 주석으로 숨기지 않는다. 주석의 "not read by the server yet" 은 "read from M3a1; when empty, Wikimedia sources are disabled" 로 바꾼다.
- C1 검사(아래)는 주석 줄을 허용하므로 그대로 통과해야 한다.

### 3. Compose ↔ `application.yml` 기본값 일치 테스트

`docker-compose.yml` 의 `${NAME:-default}` 와 `application.yml` 의 `${NAME:default}` 에서 `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PORT` 의 기본값을 읽어 비교한다. 어느 한쪽에 기본값이 없거나 값이 다르면 실패한다. 파일은 기존 `RepositoryFiles` 도우미로 읽는다. 문자열에서 기본값을 꺼내는 도우미에는 단위 테스트를 붙인다(일치, 불일치, 기본값 없음).

### 4. M1 계획서 보정 기록

M1 계획서는 완료된 기록이라 본문을 고치지 않는다. 아래 보정이 그 계획서의 해당 문장보다 우선하고, M3 이후 계획서는 보정된 형식을 쓴다.

| 계획서 | 문장 | 보정 |
|---|---|---|
| `docs/plans/m1b-bootstrap-web.md` "크기" 절 확인 명령 | `':(exclude)web/src/**/*.test.ts'`, `':(exclude)web/src/**/*.test.tsx'`, `':(exclude)web/src/test/**'`, `':(exclude)web/src/mocks/**'` | glob 매직이 없으면 `**/` 가 "0개 이상의 폴더"로 해석되지 않아 `web/src/` 바로 아래 테스트 파일(`App.test.tsx`, `bootstrap.test.ts`, `network.test.ts`)을 빼지 못한다. `':(exclude,glob)web/src/**/*.test.ts'` 처럼 `glob` 을 붙인다 |
| `docs/plans/m1a-bootstrap-server.md` "M1a·M1b 경계"의 CI 경로 필터, T4 완료 기준 (1) / `docs/plans/m1b-bootstrap-web.md` 같은 절 | `server-ci` 는 `server/**`, `docker-compose.yml`, `.github/workflows/server-ci.yml` 이 바뀔 때만 | 이 기능 머지 뒤에는 `.env.example` 도 포함한다 |

## 파일 소유 예외

backend-dev가 `server/` 밖의 루트 파일 2개(`.env.example`, `.github/workflows/server-ci.yml`)를 고친다. M1a P4와 같은 파일 단위 예외를 이 기능에도 적용하는 것을 PM이 승인한다(결정 M3-P2에 포함). 웹(`web/`)은 건드리지 않는다.

## 크기 (400줄 규칙)

세는 법은 M1a와 같다(제품 코드·설정은 세고, 테스트는 따로 보고). 예상: 설정 약 10줄 바뀜, 테스트 약 40~60줄. 확인 명령은 아래 S1·S2.

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | CI 경로 필터, `.env.example` 주석, 기본값 일치 테스트 | backend-dev | `.github/workflows/server-ci.yml`, `.env.example`, `server/src/test/java/io/github/ericmschoi/theworldthatday/ServerCiWorkflowTest.java`, `LocalDatabaseComposeTest.java`, 기본값 일치 테스트(새 파일 또는 `LocalDatabaseComposeTest` 안) | - | (1) C2 출력이 `2`. (2) `ServerCiWorkflowTest` 의 기대 경로가 위 4개(순서 포함)이고 통과한다. (3) C3 출력 없음, C4 출력이 `3`, C5 출력이 `2`. (4) C1 출력 없음, C6 출력이 `0`. (5) `LocalDatabaseComposeTest` 의 이름 검사가 필수 2개는 `NAME=`, 선택 3개는 `# NAME=` 로 확인하도록 바뀌어 통과한다. (6) 기본값 일치 테스트가 세 이름(`POSTGRES_DB`→`the_world_that_day`, `POSTGRES_USER`→`postgres`, `POSTGRES_PORT`→`5432`)에 대해 통과하고, 도우미 단위 테스트(일치·불일치·기본값 없음)가 있다. (7) Docker 실행 중 `cd server && ./gradlew check` 성공(통과·실패 개수 보고) |
| T2 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트(테스트 파일만) | T1 | (1) T1 기준을 표로 확인. (2) `cd server && ./gradlew check` 통과(개수 보고). (3) S1·S2 결과 보고. (4) PR을 연 뒤 오케스트레이터가 `gh pr checks` 로 `server` 잡이 이 PR에서 실제로 돌고 통과했는지 확인한다(경로 필터 동작 확인) |

완료 기준에 쓰는 검사 명령 (저장소 루트):

```
# C1 — .env.example 에 값이 든 줄이 없는지. 출력이 없어야 한다
grep -vE '^(#.*)?$' .env.example | grep -vE '^[A-Z0-9_]+=$'

# C2 — server-ci 두 트리거에 .env.example 경로가 있는지. 2
grep -c '".env.example"' .github/workflows/server-ci.yml

# C3 — 선택 항목이 주석 없이 남아 있지 않은지. 출력이 없어야 한다
grep -nE '^(POSTGRES_DB|POSTGRES_USER|POSTGRES_PORT)=' .env.example

# C4 — 선택 항목 3개가 주석으로 있는지. 3
grep -cE '^# (POSTGRES_DB|POSTGRES_USER|POSTGRES_PORT)=$' .env.example

# C5 — 필수 항목 2개가 주석 없이 있는지. 2
grep -cE '^(POSTGRES_PASSWORD|WIKIMEDIA_USER_AGENT)=$' .env.example

# C6 — 브라우저 키가 없는지. 0
grep -c '^VITE_' .env.example

# S1 — 제품 코드·설정 줄 수 (테스트 제외)
git diff --numstat main...HEAD -- server .env.example .github/workflows/server-ci.yml \
  ':(exclude,glob)server/src/test/**' | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'

# S2 — 테스트 줄 수 (따로 보고)
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
```

담당별: backend-dev 1, test-engineer 1.

## 리스크와 확인할 것

1. **PM의 기존 `.env`** — 이미 `POSTGRES_DB=` 같은 빈 줄이 든 `.env` 를 쓰고 있다면 이 PR과 무관하게 지금도 기본값이 무시된다. 머지 뒤 PM이 빈 줄을 지우거나 값을 채운다("끝나면 PM이 할 것").
2. **경로 필터 확인은 PR에서만** — 워크플로 테스트는 YAML 내용만 본다. 실제로 잡이 도는지는 T2 (4)에서 `gh pr checks` 로 본다.

## PM이 정할 것

| # | 질문 | 선택지 | 추천 |
|---|---|---|---|
| M3-P2 | 이 정리를 어디에 둘지 | (a) M3 앞 별도 `chore` PR + 루트 파일 2개 파일 단위 예외 승인 / (b) M3a1에 섞기 / (c) 나중으로 미룸 | (a). 근거: M3a1은 이미 400줄에 가깝고 주제가 다르다. 빈 문자열 문제는 PM의 로컬 실행에 바로 영향을 준다. 리뷰가 몇 분이면 끝나는 크기다 |

## 끝나면 PM이 할 것

- 루트 `.env` 에 `POSTGRES_DB=`, `POSTGRES_USER=`, `POSTGRES_PORT=` 처럼 값 없는 줄이 있으면 지우거나 값을 채운다.
- M3a1 전에 `.env` 의 `WIKIMEDIA_USER_AGENT` 에 값을 넣는다. 형식 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` (#25 (a), 따옴표 없이).
