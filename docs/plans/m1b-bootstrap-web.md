# 웹 부트스트랩 (M1b)

> 브랜치: `chore/bootstrap-web` · 작성: planner · 상태: PM 승인 (2026-10-10)
> 기준: `CLAUDE.md` 0절·7절, `docs/product-brief.md` 5절·8절·12절, `docs/roadmap.md` M1, `docs/research/summary.md` 6절·8절
> 짝 계획서: `docs/plans/m1a-bootstrap-server.md` (`chore/bootstrap-server`). 두 계획서의 "M1a·M1b 경계" 절은 같은 내용이다. 테스트 방침, summary 8절 처리, PM 결정 P1~P4는 M1a 계획서에 있다.

## 목표

`web/` 에 이후 모든 화면이 붙을 골격을 만든다. 끝나면 다음이 된다.

1. `cd web && npm test` 와 린트·포맷·타입 검사·빌드가 통과한다.
2. `npm run dev` 로 띄운 화면(`http://localhost:5173`)에 입력 화면 자리가 있고, 버튼으로 빈 결과 화면을 볼 수 있다. 빈 결과 화면에는 제품 정의서 5절 순서대로 섹션 자리 5개(오프닝, 그날의 동네, 그 도시의 풍경, 그날의 세계, 마무리)가 있다.
3. 섹션마다 로딩·빈 상태·에러 상태를 그리는 공용 섹션 컴포넌트가 있다(이후 섹션이 모두 재사용).
4. 우리 백엔드만 부르는 API 클라이언트와 MSW 테스트 기반이 있고, 목이 없는 요청은 테스트를 실패시킨다.
5. 개발 서버가 `/api` 요청을 `http://localhost:8080` 으로 넘긴다.
6. PR마다 GitHub Actions `web-ci` 가 린트·포맷·타입 검사·테스트·빌드를 돌린다. 배포 잡은 없다(#5).

## 범위 밖

- 실제 입력 폼(생년월일·장소), 날짜 검증, 후보 선택, 오프닝 문구 → M2b
- 라우팅 라이브러리와 결과 화면 URL 설계 → M2b (생년월일과 장소를 함께 URL에 넣으면 접속 로그·브라우저 기록에 남는 개인정보 문제를 그때 함께 정한다)
- 데이터 패칭·상태 관리 라이브러리 → M2b 또는 M3b
- 공용 미디어 컴포넌트(`MediaFigure`), 캡션 강제, `link_only` 카드 → M3b
- 지도 SDK 로더와 SDK 스텁, 브라우저 키(`VITE_KAKAO_JS_KEY`)와 `web/.env.example` → M4a·M4b (#8)
- 브라우저용 MSW 워커(`public/mockServiceWorker.js`, 개발 화면에서 목 쓰기) → 필요해지는 기능에서
- Playwright E2E → M6c (또는 test-engineer가 처음 E2E가 필요할 때 그 계획서에서)
- 디자인 시스템, UI 라이브러리, 웹 폰트, 다크 모드 → M6a에서 필요하면
- `server/`, 루트 `docker-compose.yml`·`.env.example`·`.gitignore`·`README.md`, `CLAUDE.md`, `.claude/` 수정

## 사용할 외부 출처

없음. 프론트엔드는 우리 백엔드(`/api`)만 부르고, M1b에서는 그마저 테스트의 MSW 목으로만 부른다. 개발 도구 내려받기(npm 레지스트리)는 외부 데이터 출처가 아니다.

## 전제 (PM 결정과 가정)

| 항목 | 내용 | 상태 |
|---|---|---|
| 기술 스택 | `CLAUDE.md` 0절 초안 확정 (#6 (b)): React, TypeScript, Vite / Vitest, Testing Library, MSW, Playwright | 결정 2026-10-10 |
| 배포 | 로컬 데모만 (#5 (a)). CI는 린트·테스트·빌드만 | 결정 |
| 테스트 fixture | #12 (a). M1b는 MSW로 우리 API만 목으로 만든다. 지도 SDK 스텁은 M4a | 결정 |
| 브라우저 키 | #8 (a). **M1b는 환경변수를 하나도 쓰지 않는다** (`import.meta.env.VITE_*` 없음) | 결정 |
| 지도 제공자 | **가정**: 카카오(#2 추천 (a)). M1b는 이 가정에 기대는 코드가 없다. 개발 서버 주소 `http://localhost:5173` 을 고정해 두면 M4에서 카카오 JavaScript SDK 도메인에 이 주소를 등록한다 | 미정 (M2 계획 전에 확정) |

## 기술 선택

| 항목 | 정한 것 | 이유 |
|---|---|---|
| Node | 24 LTS, `web/.nvmrc` 에 `24`, `package.json` `engines.node` `>=24` | 현재 LTS. CI가 `.nvmrc` 를 읽는다 |
| 패키지 관리 | npm, `web/package-lock.json` 커밋, CI는 `npm ci` | 추가 도구 없이 재현 가능한 설치 |
| 시작 방법 | create-vite `react-ts` 템플릿의 현재 메이저(React 19 예상)로 만들고 데모 코드(카운터, 로고, `App.css`, `src/assets/`, 템플릿 README)를 지운다 | 표준 구성. 고른 Vite·React·Vitest 메이저 버전은 보고서에 적는다 |
| 패키지 이름 | `"name": "the-world-that-day"`, `"private": true` | `CLAUDE.md` 0절 이름 규칙 |
| TypeScript | 템플릿 기본(strict) 유지 | |
| 스타일 | CSS Modules(`*.module.css`) + `src/styles/global.css`(CSS 변수, 기본 리셋). 시스템 한글 글꼴만 쓰고 웹 폰트를 내려받지 않는다 | Vite 내장, 추가 의존성 없음 |
| 린트 | ESLint (템플릿 flat config: typescript-eslint, react-hooks, react-refresh) + `eslint-config-prettier` | |
| 포맷 | Prettier (`web/.prettierrc`, `web/.prettierignore`) | |
| 테스트 | Vitest(jsdom) + `@testing-library/react` + `@testing-library/jest-dom` + `@testing-library/user-event` + `msw` | `CLAUDE.md` 0절 |
| 라우팅 | 없음. `App` 의 상태 하나로 입력 화면 ↔ 빈 결과 화면을 바꾼다 | URL 설계는 M2b |

### 스크립트 (`web/package.json`)

| 스크립트 | 명령 | 용도 |
|---|---|---|
| `dev` | `vite` | 개발 서버 `http://localhost:5173` |
| `build` | `tsc -b && vite build` | 타입 검사 + 빌드 |
| `preview` | `vite preview` | 빌드 결과 보기 |
| `lint` | `eslint .` | 린트 |
| `format` | `prettier --write .` | 포맷 자동 수정 |
| `format:check` | `prettier --check .` | 포맷 검사 (CI) |
| `typecheck` | `tsc -b` | 타입 검사만 (템플릿 tsconfig는 `noEmit`) |
| `test` | `vitest run` | 테스트 한 번 실행 (CI, 에이전트) |
| `test:watch` | `vitest` | 개발 중 감시 모드 |

### 개발 서버 (`web/vite.config.ts`)

- `server.port: 5173`, `server.strictPort: true` (포트가 바뀌면 M4의 SDK 도메인 등록이 깨지므로 다른 포트로 넘어가지 않게)
- `server.proxy: { '/api': 'http://localhost:8080' }` (M1a 계약)
- Vitest 설정은 같은 파일의 `test` 블록: `environment: 'jsdom'`, `setupFiles: ['./src/test/setup.ts']`

## API / 데이터 변경

### 쓰는 백엔드 API (M1a와의 계약)

```
GET /api/health
200 OK  {"status":"UP"}
503     {"status":"DOWN"}
```

M1b에서는 화면에 쓰지 않는다. API 클라이언트와 MSW 테스트 기반을 검증하는 데만 쓴다.

### API 클라이언트 (`web/src/api/client.ts`)

- `apiGet<T>(path: string): Promise<T>` — `path` 는 `/api/` 로 시작해야 한다. 절대 URL이나 다른 경로는 요청을 보내지 않고 바로 오류를 낸다(프론트엔드는 우리 백엔드만 부른다, 제품 정의서 8절).
- 2xx가 아니면 `ApiError`(`status` 포함)를 던진다.
- `getHealth(): Promise<{ status: string }>` — `apiGet('/api/health')`.
- 테스트 환경(jsdom + Node `fetch`)에서 상대 경로가 해석되지 않으면 `new URL(path, window.location.origin)` 으로 만든다.

### 결과 화면 섹션 (이후 마일스톤과의 계약)

| 순서 | `id` | 제목 (화면, 접근성 이름) | 제품 정의서 | 채울 마일스톤 |
|---|---|---|---|---|
| 1 | `opening` | 오프닝 | 5절 1번 | M2b |
| 2 | `neighborhood` | 그날의 동네 | 5절 2번 | M4 (`REGION` 이면 숨김, M2b 이후) |
| 3 | `city` | 그 도시의 풍경 | 5절 3번 | M5c |
| 4 | `world` | 그날의 세계 | 5절 4번 | M3b |
| 5 | `closing` | 마무리 | 5절 5번 | M6a·M6b |

- `id` 는 이후 섹션 API 이름과 테스트가 기대는 값이라 바꾸지 않는다. 화면에 보이는 제목은 M6a에서 바뀔 수 있다(그때도 접근성 이름은 유지).
- 각 섹션은 `<section id=… aria-labelledby=…>` + `<h2>` 로 그린다. 전체는 `<main>` 안에 둔다.

### 공용 섹션 컴포넌트 (`web/src/story/StorySection.tsx`)

| `status` | 화면 | 접근성 |
|---|---|---|
| `loading` | "불러오는 중…" | 섹션에 `aria-busy="true"` |
| `empty` | "아직 준비 중인 섹션입니다." | |
| `error` | "이 섹션을 불러오지 못했습니다." | `role="status"` |
| `ready` | `children` | |

M1b의 빈 결과 화면은 5개 섹션 모두 `empty` 로 그린다. 문구는 M6a에서 다듬는다. 한 섹션이 `error` 여도 다른 섹션은 그대로 그려진다(제품 정의서 8절 — 컴포넌트가 섹션마다 독립이다).

### 화면

- 입력 화면 자리(`src/screens/InputScreen.tsx`): `<h1>The World That Day</h1>`, 한 줄 안내("생년월일과 태어난 장소를 입력하는 화면이 곧 이곳에 생깁니다."), 버튼 "빈 결과 화면 보기".
- 빈 결과 화면(`src/screens/StoryScreen.tsx`): 섹션 5개 + 버튼 "처음으로".
- 버튼으로 오가는 것은 M2b가 실제 입력 흐름으로 바꾸기 전까지의 임시 장치다.
- 모바일 우선: 360px 폭 기준, 가로 스크롤 없음, 본문 최대 폭을 두고 가운데 정렬. `index.html` 은 `<html lang="ko">`, `<title>The World That Day</title>`, viewport 메타.

## 확정 명령 (M1 끝나면 PM이 `CLAUDE.md` 0절에 반영)

모두 `web/` 에서 실행한다.

| 목적 | 명령 |
|---|---|
| 설치 | `npm ci` |
| 개발 서버 | `npm run dev` (http://localhost:5173, `/api` 는 8080으로 프록시) |
| 테스트 | `npm test` |
| 린트 | `npm run lint` |
| 포맷 검사 / 자동 수정 | `npm run format:check` / `npm run format` |
| 타입 검사 | `npm run typecheck` |
| 빌드 | `npm run build` |
| 전체 검사 (CI와 같음) | `npm run lint && npm run format:check && npm run typecheck && npm test && npm run build` |

서버 명령과 `CLAUDE.md` 0절 문구 제안은 `docs/plans/m1a-bootstrap-server.md` 에 있다.

## 테스트 방침 (#12 반영, M1 이후 모든 웹 작업에 적용)

1. 테스트는 네트워크를 호출하지 않는다. MSW 서버를 `onUnhandledRequest: 'error'` 로 띄워 **목이 없는 요청은 테스트를 실패**시킨다.
2. 목은 우리 API(`/api/...`)만 만든다. 외부 제공자 응답을 프론트엔드에서 목으로 만들 일은 없어야 한다(프론트엔드는 우리 백엔드만 부른다). 예외는 지도 SDK이고, M4a에서 SDK 스텁으로 처리한다.
3. 목 데이터는 제품 정의서 12절 데모 입력 기준으로 만든다. 실제 사람의 생년월일을 쓰지 않는다.
4. 핸들러는 `src/mocks/handlers.ts` 에 모으고, 테스트마다 `server.use(...)` 로 덮어쓴다. `afterEach` 에서 `resetHandlers`.

## M1a·M1b 경계 (병렬 진행)

**판단: 병렬 가능 (M1a 계획서 P4 (a) 결정).** 두 브랜치는 서로 다른 파일만 만든다. 각자 `main` 에서 갈라지고 각자 PR을 연다. 머지 순서는 상관없다. 오케스트레이터는 에이전트마다 worktree를 따로 둔다. M1b의 자동 테스트는 서버 없이(MSW) 돌기 때문에 M1a를 기다리지 않는다. PM의 프록시 확인(T6 (4))만 서버가 필요하다.

| 항목 | 값 | 정하는 쪽 | 쓰는 쪽 |
|---|---|---|---|
| 서버 포트 | `8080` | M1a (`server.port`) | M1b Vite 프록시 대상 `http://localhost:8080` |
| API 접두사 | `/api` | M1a | M1b 프록시 규칙 `/api` |
| 헬스 체크 | `GET /api/health` → `200 {"status":"UP"}` | M1a | M1b MSW 핸들러·API 클라이언트 테스트, PM의 프록시 확인 |
| 웹 개발 서버 | `http://localhost:5173` (`strictPort`) | M1b | M4 카카오 JavaScript SDK 도메인 등록 |
| CI 워크플로 | **파일 두 개**: `.github/workflows/server-ci.yml`(M1a 소유), `.github/workflows/web-ci.yml`(M1b 소유) | 각자 | 한 파일이면 두 브랜치가 같은 파일을 만들어 충돌하므로 나눈다 |

파일 소유 (에이전트 정의의 "자기 폴더 밖 수정 금지"에 대한 예외를 파일 단위로 정한다 — M1a 계획서 P4 (a)로 승인됨):

| 파일·폴더 | M1a (backend-dev) | M1b (frontend-dev) |
|---|---|---|
| `server/**` | 만든다 | 건드리지 않음 |
| `web/**` | 건드리지 않음 | 만든다 |
| `docker-compose.yml` (루트) | 만든다 (예외) | 건드리지 않음 |
| `.env.example` (루트) | 만든다 (예외) | 건드리지 않음 |
| `.github/workflows/server-ci.yml` | 만든다 (예외) | 건드리지 않음 |
| `.github/workflows/web-ci.yml` | 건드리지 않음 | 만든다 (예외) |
| 루트 `.gitignore`, `README.md`, `CLAUDE.md`, `.claude/`, `docs/` | 건드리지 않음 | 건드리지 않음 |

- 빌드 산출물 무시 규칙은 루트 `.gitignore` 에 이미 있다(`server/build/`, `server/.gradle/`, `web/node_modules/`, `web/dist/`). 더 필요하면 각자 `server/.gitignore`, `web/.gitignore` 에 둔다(템플릿이 만드는 `web/.gitignore` 는 유지해도 된다).
- CI 경로 필터: `server-ci` 는 `server/**`, `docker-compose.yml`, `.github/workflows/server-ci.yml` 이 바뀔 때만, `web-ci` 는 `web/**`, `.github/workflows/web-ci.yml` 이 바뀔 때만 돈다. 문서만 바뀐 PR에서는 둘 다 돌지 않는다(M1a 계획서 리스크 4).
- 계획서 커밋 위치 제안: `docs/plans/m1a-bootstrap-server.md` 와 `docs/roadmap.md` 는 `chore/bootstrap-server` 에, 이 계획서는 `chore/bootstrap-web` 에 커밋한다.

## 크기 (400줄 규칙)

**세는 법** (M1a와 같은 기준):
- 센다: 직접 쓴 제품 코드 + 직접 쓴 설정(`package.json`, `vite.config.ts`, ESLint·Prettier 설정, `index.html`, CI 워크플로).
- 따로 세어 보고만 한다(한도에 넣지 않음): 테스트 코드(`*.test.ts(x)`), 테스트 지원 코드(`src/test/**`, `src/mocks/**`).
- 뺀다: lockfile(`package-lock.json`), 템플릿이 만들고 손대지 않은 파일(`tsconfig*.json`, `src/vite-env.d.ts`, `public/**`, `web/.gitignore`), 문서.

확인 명령 (test-engineer가 T5에서 실행해 보고):

```
git diff --numstat main...HEAD -- web .github/workflows/web-ci.yml \
  ':(exclude)web/package-lock.json' ':(exclude)web/tsconfig*.json' ':(exclude)web/src/vite-env.d.ts' \
  ':(exclude)web/public/**' ':(exclude)web/.gitignore' \
  ':(exclude)web/src/**/*.test.ts' ':(exclude)web/src/**/*.test.tsx' \
  ':(exclude)web/src/test/**' ':(exclude)web/src/mocks/**'
```

**예상**: 제품 코드·설정 약 300~380줄(화면·섹션·클라이언트 약 170, CSS 약 50, `vite.config.ts` 약 25, `package.json` 약 45, ESLint 설정 약 30, `index.html` 약 15, CI 약 40), 테스트 약 120~160줄. 한도에 가깝다. **T3이 끝났을 때 센 값이 450줄을 넘으면** frontend-dev가 보고서에 적고, 오케스트레이터는 T3(화면)를 별도 기능으로 나눌지 PM에게 묻는다. 줄이는 1순위는 CSS와 섹션 상태 문구다.

## 작업 목록

순서: T1 → T2 → T3 → T5 → T6. T4는 T1 이후 아무 때나.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | Vite + React + TypeScript 골격, ESLint·Prettier, 스크립트, 개발 서버 프록시 | frontend-dev | `web/package.json`, `web/package-lock.json`, `web/.nvmrc`, `web/index.html`, `web/vite.config.ts`, `web/eslint.config.js`, `web/.prettierrc`, `web/.prettierignore`, `web/src/main.tsx`, 최소 `web/src/App.tsx` | - | (1) `cd web && npm ci && npm run lint && npm run format:check && npm run typecheck && npm run build` 이 모두 종료 코드 0. (2) `cd web && node -p "require('./package.json').name"` → `the-world-that-day`, `private` 가 `true`. (3) `package.json` 스크립트가 위 "스크립트" 표의 이름 9개와 같다. (4) `grep -c '<html lang="ko">' web/index.html` = 1, `grep -c '<title>The World That Day</title>' web/index.html` = 1, viewport 메타 있음. (5) `vite.config.ts` 에 `port: 5173`, `strictPort: true`, `'/api'` → `'http://localhost:8080'` 프록시가 있다. (6) `cat web/.nvmrc` → `24`. (7) 템플릿 데모 코드가 없다: `web/src/assets/`, `web/src/App.css` 가 없고 `grep -rn 'count is' web/src` 결과 없음 |
| T2 | 테스트 기반(Vitest·Testing Library·MSW)과 API 클라이언트 | frontend-dev | `web/src/test/setup.ts`, `web/src/mocks/handlers.ts`, `web/src/mocks/server.ts`, `web/src/api/client.ts`, `web/src/api/client.test.ts`, `vite.config.ts` 의 `test` 블록 | T1 | (1) `cd web && npm test` 종료 코드 0, 감시 모드로 멈추지 않는다. (2) `setup.ts` 가 `server.listen({ onUnhandledRequest: 'error' })`, `afterEach(resetHandlers)`, `afterAll(close)` 를 하고 jest-dom 매처를 등록한다. (3) `client.test.ts` 에 테스트 4개 이상: (a) `getHealth()` 가 MSW 응답 `{"status":"UP"}` 을 돌려준다, (b) 핸들러가 503을 주면 `ApiError` 이고 `status === 503`, (c) `apiGet('https://example.com/x')` 는 요청 없이 거부된다, (d) 핸들러가 없는 `/api/__unhandled__` 요청은 거부된다(`onUnhandledRequest: 'error'` 동작 증명). (4) `npm run lint && npm run format:check && npm run typecheck` 통과 |
| T3 | 입력 화면 자리, 빈 결과 화면, 공용 섹션 컴포넌트, 모바일 기본 스타일 | frontend-dev | `web/src/App.tsx`, `web/src/screens/InputScreen.tsx`, `web/src/screens/StoryScreen.tsx`, `web/src/story/StorySection.tsx`, `web/src/story/sections.ts`(섹션 `id`·제목 목록), `*.module.css`, `web/src/styles/global.css`, 컴포넌트 테스트 | T2 | (1) `StoryScreen` 테스트: `getAllByRole('region')` 이 5개이고 접근성 이름이 순서대로 오프닝, 그날의 동네, 그 도시의 풍경, 그날의 세계, 마무리. 각 섹션 `id` 가 `opening`, `neighborhood`, `city`, `world`, `closing`. (2) `StorySection` 테스트: `loading` → "불러오는 중" 문구와 `aria-busy="true"`, `empty` → "아직 준비 중" 문구, `error` → "이 섹션을 불러오지 못했습니다" 문구, `ready` → `children` 표시. 같은 화면에 `error` 섹션과 `ready` 섹션을 함께 그려 `ready` 쪽 내용이 보이는 테스트 1개. (3) `App` 테스트(user-event): 처음에 `heading` 이름 "The World That Day"(level 1)가 보이고, "빈 결과 화면 보기" 클릭 → region 5개, "처음으로" 클릭 → 입력 화면으로 돌아온다. (4) 위 "확정 명령"의 전체 검사가 통과. (5) 보고서에 "크기" 절 명령으로 센 줄 수를 적는다 |
| T4 | 웹 CI 워크플로 | frontend-dev | `.github/workflows/web-ci.yml` | T1 | (1) 트리거: `pull_request` 와 `main` 대상 `push`, 경로 필터 `web/**`, `.github/workflows/web-ci.yml`. (2) `permissions: contents: read`. (3) setup-node가 `node-version-file: web/.nvmrc`, npm 캐시(`cache-dependency-path: web/package-lock.json`)를 쓴다. (4) `working-directory: web` 에서 `npm ci` → `npm run lint` → `npm run format:check` → `npm run typecheck` → `npm test` → `npm run build`. (5) `timeout-minutes` 설정. (6) `grep -c 'secrets\.' .github/workflows/web-ci.yml` = 0, 배포·업로드 단계 없음. (7) PR을 연 뒤 오케스트레이터가 `gh pr checks` 로 `web` 잡 통과를 확인한다 |
| T5 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 추가(테스트 파일만) | T3, T4 | (1) T1~T4 완료 기준을 표로 확인하고 빠진 테스트를 추가. (2) 위 "확정 명령"의 전체 검사 통과(테스트 통과·실패 개수 보고). (3) 아래 검사 명령 C1, C2의 출력이 없다. (4) "크기" 절의 `git diff --numstat` 결과(제품·설정 줄 수, 테스트 줄 수)를 보고 |
| T6 | 로컬 화면 확인 (PR 검수 때) | PM | - | T5 | (1) `cd web && npm ci && npm run dev` 후 `http://localhost:5173` 에 "The World That Day" 가 보인다. (2) 브라우저 개발자 도구에서 폭 360px로 보고 가로 스크롤이 없다. (3) "빈 결과 화면 보기"로 섹션 5개가 순서대로 보이고 "처음으로"로 돌아온다. (4) M1a 서버를 띄운 상태(M1a가 머지됐으면 `main`, 아니면 `chore/bootstrap-server` 브랜치)에서 `curl -s http://localhost:5173/api/health` → `{"status":"UP"}` (프록시 확인) |

완료 기준에 쓰는 검사 명령 (저장소 루트에서 실행):

```
# C1 — M1b는 환경변수를 쓰지 않는다. 출력이 없어야 한다
grep -rn 'import.meta.env' web/src

# C2 — 제품 코드에 외부 절대 URL이 없다(테스트 파일 제외). 출력이 없어야 한다
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx'
```

담당별: frontend-dev 4 (T1~T4), test-engineer 1 (T5), PM 1 (T6).

## ★ PM 직접 구현 후보

없음 — 이 기능에는 백엔드 작업이 없다(★는 백엔드 작업에서 고른다). M1a T2도 P2 (b) 결정으로 backend-dev가 하므로, M1에는 PM 직접 구현 작업이 없다.

## 리스크와 확인할 것

1. **도구 메이저 버전 조합** — create-vite가 만드는 Vite 메이저에 맞는 Vitest 메이저를 써야 한다(맞지 않으면 설정 타입 오류). frontend-dev가 고른 Vite·React·Vitest·MSW 메이저 버전을 보고서에 적는다.
2. **Node 버전** — PM 컴퓨터의 Node가 24가 아니면 `engines` 경고나 설치 차이가 생길 수 있다. `.nvmrc` 로 맞춘다(`nvm use`).
3. **jsdom에서 상대 경로 `fetch`** — Node의 `fetch` 는 `/api/health` 같은 상대 경로를 해석하지 못할 수 있다. 클라이언트가 `window.location.origin` 으로 절대 URL을 만들면 브라우저와 테스트에서 같게 동작한다.
4. **ESLint와 Prettier 규칙 충돌** — `eslint-config-prettier` 로 포맷 관련 린트 규칙을 끈다.
5. **임시 버튼이 남는다** — "빈 결과 화면 보기"는 M2b가 실제 입력 흐름으로 바꿀 때 지운다. M2b 계획서 범위에 넣는다.
6. **크기 한도에 가깝다** — "크기" 절의 450줄 기준을 넘으면 T3를 나눈다.
7. **포트 5173 고정** — 다른 프로그램이 5173을 쓰면 `strictPort` 때문에 개발 서버가 뜨지 않는다. 의도된 동작이다(M4의 SDK 도메인 등록과 맞추기 위해). 오류 메시지를 보고 그 프로그램을 끈다.

## PM이 정할 것

| # | 질문 | 결정 (2026-10-10) |
|---|---|---|
| - | M1b만의 결정 | 결정: 없음 |
| P1~P4 | M1 공통 결정 (M1a 계획서) | 결정: P1 Spring Boot 4.1.x, P2 M1a T2는 backend-dev, P3 `CLAUDE.md` 7절 5항 반영함, P4 M1a·M1b 병렬(worktree)과 파일 단위 예외 승인 |
