# 진행 로그

오케스트레이터가 보고를 받을 때마다 한 줄씩 추가한다.

| 날짜 | 에이전트 | 작업 ID | 상태 | 커밋 해시 |
|---|---|---|---|---|
| 2026-10-08 | planner | M0 계획 (roadmap, m0-source-research) | 완료 · PM 승인 | a015af1 |
| 2026-10-08 | planner | M0 계획 PM 결정 반영 | 완료 | a015af1 |
| 2026-10-09 | data-researcher | T1 | 완료 (1순위 카카오, 부분 확인·PM 호출 대기) | 825002a |
| 2026-10-09 | data-researcher | T2 | 완료 (7절 가정 조건부 성립, 부분 확인) | 4b666f9 |
| 2026-10-09 | data-researcher | T3 | 완료 (1순위 카카오, 미확인·PM 확인 대기) | 3de67cc |
| 2026-10-09 | data-researcher | T4 | 완료 (서울역사아카이브·사진으로 본 서울·Commons 확인됨) | 113c53d |
| 2026-10-09 | data-researcher | T5 | 완료 (1순위 한국어 위키백과, 확인됨) | fc7e26a |
| 2026-10-09 | data-researcher | T6 | 막힘 (summary.md 저장 거부) → PM 승인으로 오케스트레이터가 원문 저장 | - |
| 2026-10-09 | data-researcher | T6 | 완료 (원문 일치 확인, 결정 26개) | 96f2d12 |
| 2026-10-09 | code-reviewer | M0 리뷰 1차 | 수정 요청 (필수 1: T3 2차 출처 근거 열, 권장 6) | 96f2d12 기준 |
| 2026-10-09 | data-researcher | 리뷰 1차 수정 (T1·T2·T3·T4·T6) | 완료 (필수 1·권장 6 반영) | 46a999f 7ed64eb c79ff17 9b455cf d25f2fe 2a764ff |
| 2026-10-09 | code-reviewer | M0 리뷰 2차 | 승인 (권장 1: 정사영상 표현 2곳, 비차단) | 2a764ff 기준 |
| 2026-10-10 | planner | M1 계획 (roadmap 갱신, m1a, m1b) + PM 결정 반영 | 완료 · PM 승인 | cdd5c6e (m1a·roadmap), a310894 (m1b, chore/bootstrap-web) |
| 2026-10-10 | orchestrator | CLAUDE.md 0절·7절 5항 (PM 승인) | 완료 | ef88467 |
| 2026-10-10 | frontend-dev | M1b T1 | 완료 (lint·format·typecheck·build 통과, 117줄) | 336f7ed |
| 2026-10-10 | frontend-dev | M1b T2 | 완료 (테스트 5/0, 누적 제품 160줄) | 6ea91ea |
| 2026-10-10 | backend-dev | M1a T1 | 완료 (check 3/0, Boot 4.1.1·Gradle 9.8.1, 84줄) | 68525b3 720bcfb |
| 2026-10-10 | frontend-dev | M1b T3 | 완료 (테스트 14/0, 누적 제품 345줄) | c1674d1 146ceec |
| 2026-10-10 | backend-dev | M1a T4 | 완료 (server-ci.yml, check 3/0) | 999b76d |
| 2026-10-10 | frontend-dev | M1b T4 | 완료 (web-ci.yml, 테스트 14/0, 누적 384줄) | 76f6c8c |
| 2026-10-10 | backend-dev | M1a T3 | 완료 (compose config 5/0, check 3/0) | 4e4a252 |
| 2026-10-10 | test-engineer | M1b T5 | 완료 (23/0, 5회 반복 안정, 보정 384줄) | 85f3c9d |
| 2026-10-10 | code-reviewer | M1b 리뷰 1차 | 승인 (권장 9) | 85f3c9d 기준 |
| 2026-10-10 | frontend-dev | M1b T2 후속 (리뷰 권장 1·2) | 완료 (25/0, 391줄) | 9793902 5dfa34c |
| 2026-10-10 | code-reviewer | M1b 후속 확인 | 승인 | 5dfa34c 기준 |
| 2026-10-11 | orchestrator | M1a: main 병합 (PR #2 반영) | 완료 | cba251a |
| 2026-10-11 | backend-dev | M1a T2 | 완료 (check 8/0, Testcontainers PG 17.11) · PM 결정 필요(Ryuk) | 7cb5bb5 |
| 2026-10-11 | test-engineer | M1a T5 | 완료 (33/0, 5회 반복, 204줄) | 92eefc3 |
| 2026-10-11 | code-reviewer | M1a 리뷰 1차 | 승인 (권장 5) | 92eefc3 기준 |
| 2026-10-11 | planner | M3 계획 (chore + M3a1~a3, M3b1~b2, roadmap) | 완료 · PM 승인(전부 추천안), 단 사진·영상 인라인 요구로 M3 미디어 계획 수정 대기 | chore 계획만 커밋 |
| 2026-10-11 | orchestrator | CLAUDE.md 0절 실행·테스트·린트 명령 (PM 위임) | 완료 | (이 커밋 묶음) |
| 2026-10-11 | backend-dev | chore T1 | 완료 (check 47/0) | dd7a4b4 ec84378 |
| 2026-10-11 | test-engineer | chore T2 | 완료 (50/0, 3회) | 8f2b66b |
| 2026-10-11 | code-reviewer | chore 리뷰 | 승인 (권장 4) | 8f2b66b 기준 |
| 2026-10-11 | planner | M3 계획 개정 (미디어 바로 재생, 영상 기능 3개 추가) | 완료 · PM 승인 (2026-10-11, 전부 추천안) | 이 브랜치에 커밋 |
| 2026-10-11 | planner | M3 계획 개정 (P9 (b): 임의 날짜 영상 자동 검색, M3a5·M3a6 추가) | 완료 · PM 승인 (2026-10-11, 전부 추천안) | 이 브랜치에 커밋 |
| 2026-10-11 | frontend-dev | M3b1 T1 | 완료 (43/0, S1 +107/-30) | b51a5fb 93b564d |
| 2026-10-11 | backend-dev | M3a1 T1 | 완료 (check 80/0, S1 누적 244줄) | 5b93b14 |
| 2026-10-11 | frontend-dev | M3b1 T2 | 완료 · PM 결정 필요 2건 (촬영 시점 미상 표시, 크기 309줄) | 6ee093a |
| 2026-10-11 | data-researcher | M3 영상 리서치 T1 | 완료 · PM 결정 필요 4건 (Commons: 국내 날짜 0건) | d0aa124 |
| 2026-10-11 | backend-dev | M3a1 T2 | 완료 (check 113/0, S1 누적 577줄 → T3 분리 필요) | e31a542 b440cb1 |
| 2026-10-11 | orchestrator | PM 결정: 촬영 시점 미상 표시(a), M3a1·M3b1 T3 분리, 리서치 결정은 T5에 | 기록 | (CLAUDE.md·정의서 커밋) |
| 2026-10-11 | frontend-dev | M3b1 T2 후속 (촬영 시점 미상 표시, http(s) 검사) | 완료 (106/0, 누적 319줄) | d9e3af7 |
| 2026-10-11 | test-engineer | M3b1 T4 (T1·T2 검증) | 완료 (155/0, 5회 안정) | 653c500 |
| 2026-10-11 | test-engineer | M3a1 T4 (T1·T2 검증) | 완료 (159/0, 3회 안정, S1 577) | 8feaf87 |
| 2026-10-11 | code-reviewer | M3b1 리뷰 | 승인 (권장 6, PR 전 1·2·5 및 무렵 배지 반영) | 653c500 기준 |
| 2026-10-11 | frontend-dev | M3b1 리뷰 후속 (깨진 입력 처리, 날짜 검증, 무렵 거리 표시) | 완료 (184/0) | 9e2b277 557192f |
| 2026-10-11 | code-reviewer | M3a1 리뷰 | 승인 (권장 8, PR 전 1·6 반영) | 8feaf87 기준 |
| 2026-10-11 | backend-dev | M3a1 리뷰 후속 (503 Retry-After 차단, 시작 시 URL 검사) | 완료 (180/0) | 4bc3e3d |
| 2026-10-11 | data-researcher | M3 영상 리서치 T2 (YouTube 검색·공식 채널·임베드 조건) | 완료 (search.list 하루 100회 별도 버킷 발견) | 325530d |
| 2026-10-11 | planner | M3 계획 보정 (M3a1b·M3a1c·M3b1b 분할, PM 결정 반영) | 완료, PM 결정 필요 (재분할, P16, P17) | 미커밋 |
| 2026-10-11 | data-researcher | M3 영상 리서치 T3 (대한뉴스 날짜 검색·임베드) | 완료 (e영상역사관 임베드 없음 → KTV 아카이브 YouTube로 재생) | 725b7f5 |
| 2026-10-11 | planner | M3 계획 PM 결정 기록 (재분할 승인, P16 b, P17 a) | 완료 | 아래 docs(plan) 커밋 |
| 2026-10-11 | data-researcher | M3 영상 리서치 T4 (게티 등 제공사 공식 임베드) | 완료 (게티 임베드 확인, 검색 API는 계약 필요, AP·로이터·연합 미확인) | 1c4100b |
| 2026-10-11 | orchestrator | PR #5 (M3a1)·#6 (M3b1) PM 머지, M3a1b·M3b1b 시작 | 진행 | 6cb3a7a |
| 2026-10-11 | frontend-dev | M3b1b T1 (공용 사진 카드, 캡션) | 완료 (223/0, S1 +151/−9) | 0bcbdc8 c46687b |
| 2026-10-11 | frontend-dev | M3b1b T2 (링크 포커스 링, 대비 테스트) | 완료 (228/0, 브랜치 S1 +158/−9) | 6cb4aaa |
| 2026-10-11 | test-engineer | M3b1b T3 (완료 기준 검증) | 완료 (271/0, 3회 안정) | 7d75802 |
| 2026-10-11 | code-reviewer | M3b1b 리뷰 | 승인 (권장 4, PR 전 1 반영) | 7d75802 기준 |
| 2026-10-11 | test-engineer | M3b1b 리뷰 후속 (중립 대체 문구, alt 가드 테스트) | 완료 (273/0) | 781ed35 |
| 2026-10-11 | orchestrator | M3b1b PR 생성 | PR #7 | 781ed35 |
| 2026-10-11 | data-researcher | M3 영상 리서치 T5 (요약·정확도 규칙·PM 결정 22개) | 완료, PM 결정 필요 | b4a4450 |
| 2026-10-11 | backend-dev | M3a1b T1 (동시 요청 3개, ratelimited 차단, HTML 태그·엔티티) | 완료 (208/0, S1 +120/−19, 반복 1회 미재현 실패 기록) | 11ff3d5 2e07c03 |
| 2026-10-11 | PM | M3 영상 리서치 결정 22개 + M3b1b 라이선스 줄 중복 | 전부 추천안 (M3a6 M3에서 제외, 라이선스 줄 (b)) | - |
| 2026-10-11 | PM | 화면 순서 질문 (결과 화면 먼저 vs 현재 순서) | 현재 순서 유지 (M3a2·M3a3 뒤 M3b2) | - |
| 2026-10-11 | code-reviewer | M3 영상 리서치 리뷰 | 수정 요청 (필수 2: 대한뉴스 제목 공백, YouTube 정책 III.A.2 누락) | b4a4450 기준 |
| 2026-10-11 | backend-dev | M3a1b T2 (위키텍스트 캐시 V2) | 완료 (227/0, 브랜치 S1 +366/−26) | 56e7931 |
| 2026-10-11 | data-researcher | M3 영상 리서치 리뷰 후속 (대한뉴스 제목 공백, III.A.2, 결정 기록) | 완료, PM 결정 필요 (23 동의 단계, 24 YouTube 키) | 72dba38 |
| 2026-10-11 | PM | 리서치 23번 (동의 단계 위치)·24번 (YouTube 키) | 추천안: 23 (b) 입력 화면 안 필수 동의, 24 (a) 발급 | - |
| 2026-10-11 | code-reviewer | M3 영상 리서치 2차 리뷰 | 승인 (권장 2) | 72dba38 기준 |
| 2026-10-11 | data-researcher | M3 영상 리서치 최종 수정 (23·24번 결정 기록, V5-10 표기) | 완료 | c854105 |
| 2026-10-11 | PM | YouTube 키 발급, 확인 명령 Y1~Y4·N3 실행 | 완료 (오류 없음, 결과는 data-researcher에게 전달) | - |
| 2026-10-11 | test-engineer | M3a1b T3 (완료 기준 검증, 불안정 테스트 원인) | 완료 (233/0, 3회 안정, 읽기 타임아웃 2초로 완화) | 486a876 71ac6f9 |
| 2026-10-12 | data-researcher | M3a5 T0 (PM YouTube 확인 결과 기록) | 완료 (YouTube 경로 확인됨, 대한뉴스 색인은 N1·N2·N-B1 대기) | 6bf0267 |
| 2026-10-12 | test-engineer | M3a1b T3 추가 (기반 테스트 읽기 타임아웃 2초) | 완료 (233/0, 20회 반복 통과) | 646fad7 |
| 2026-10-12 | code-reviewer | M3a1b 리뷰 | 수정 요청 (필수 1: DB 다운 시 저장 예외가 새어 나감) | 646fad7 기준 |
| 2026-10-12 | planner | M3 계획 보정 (리서치·결정 반영, 새 계획 5개) | 완료, PM 결정 필요 (D1 분할 18 PR, D2 동의 시점, D3 Playwright CI) | 미커밋 |
| 2026-10-12 | PM | M3 계획 보정 승인, D1 (a) 분할·18 PR, D2 (a) 임시 화면에 동의, D3 (a) Playwright CI | 승인 | - |
| 2026-10-12 | backend-dev | M3a1b 리뷰 후속 (DB 다운 시 값 반환, ABSENT 행 삭제, TTL 경계, INTERRUPTED) | 완료 (241/0, 브랜치 S1 +397/−26) | 8d76b01 04e2144 |
| 2026-10-12 | code-reviewer | M3a1b 2차 리뷰 | 승인 (권장 3, 줄 수 397 경고) | 04e2144 기준 |
| 2026-10-12 | orchestrator | PR #7 (M3b1b) PM 머지, README M3 표 갱신 (18 PR, M3a6 제외) | 완료 | - |
| 2026-10-12 | orchestrator | M3a1b PR 생성 | PR #8 | f77540d |
| 2026-10-12 | planner | M3 계획 보정 PM 결정 기록 (D1·D2·D3) | 완료 | 아래 docs(plan) 커밋 |
| 2026-10-12 | orchestrator | 영상 리서치 + 계획 보정 PR 생성 | PR #9 | 73ac593 |
| 2026-10-12 | orchestrator | PR #8 (M3a1b)·#9 (리서치+계획) PM 머지, M3a1c 시작 | 진행 | 9714f71 |
| 2026-10-12 | backend-dev | M3a1c T1 | 완료 (PM 결정 필요: T2 분리, Hikari timeout) | 3b971a5 |
| 2026-10-12 | orchestrator | M3a1c PM 결정: T2 이번 PR에 포함, Hikari connection-timeout 이번 PR에서 단축 (T1b) | 진행 | 3b971a5 |
| 2026-10-12 | backend-dev | M3a1c T1b | 완료 | e275b0b |
| 2026-10-12 | backend-dev | M3a1c T2 | 완료 | 9b4a324 |
| 2026-10-12 | test-engineer | M3a1c T3 | 완료 (PM 결정 필요: D3 과합침) | afb6f62 |
| 2026-10-12 | orchestrator | M3a1c PM 결정: D3 과합침 (b) 이번 PR에서 수정, DB 읽기 실패 시 쓰기 건너뛰기 이번 PR에 포함 | 진행 | afb6f62 |
| 2026-10-12 | planner | M3a1c 계획 보정 (D3 큰 쪽 분모, 쓰기 건너뛰기, T4·T5) | 완료 (PM 결정 필요: 일반 링크 빈틈) | 미커밋 |
| 2026-10-12 | planner | M3a1c 계획 보정 (b) 반영, PM 승인 | 완료 | 40deef5 |
| 2026-10-12 | backend-dev | M3a1c T4 | 완료 | c4bace7 |
| 2026-10-12 | test-engineer | M3a1c T5 | 완료 | e74e95d |
| 2026-10-12 | orchestrator | PM 결정: 위키 원문이 어색해도 거르지 않고 그대로 전달, 해석·문구 변경은 화면 표시 단계에서 (M3a2·M3b2 입력) | 기록 | e74e95d |
| 2026-10-12 | code-reviewer | M3a1c 전체 리뷰 | 승인 (필수 0, 권장 5) | e74e95d |
| 2026-10-12 | orchestrator | M3a1c PR 생성 | PR | (아래 커밋) |
| 2026-10-12 | orchestrator | PR #10 (M3a1c) PM 머지, M3a2 시작. PM 결정: M3-P16 (b) 구조 필터 제거, 큐레이션 hide는 즉시 숨김 수단으로만 유지 | 진행 | c2f1a5f |
| 2026-10-12 | planner | M3a2 계획 개정 (구조 필터 제거, M3a1c 계약 반영, T2b 추가) | 완료 (PM 결정 필요: 분할) | 미커밋 |
| 2026-10-12 | planner | M3a2 계획 개정 PM 승인 (b) 한 PR, 로드맵 상태 정정 (M3 PR 17개) | 완료 | d8f3bad |
| 2026-10-12 | content-curator | M3a2 T1 | 완료 (worktree, 병합 대기) | 48471a8 |
| 2026-10-12 | backend-dev | M3a2 T2 | 완료 (PM 결정 필요: 확인점 초과 394) | a2a03fe 020f166 |
| 2026-10-12 | orchestrator | M3a2 PM 결정: 확인점 초과에도 (b) 한 PR 유지. T1 worktree 병합 | 진행 | b03e6ff |
| 2026-10-13 | backend-dev | M3a2 T2b | 완료 | e43b848 |
| 2026-10-13 | backend-dev | M3a2 T3 | 완료 (누적 S1 1071) | 9d9b484 04cf822 |
| 2026-10-13 | test-engineer | M3a2 T4 | 완료 | 37bdef7 |
| 2026-10-13 | code-reviewer | M3a2 전체 리뷰 1차 | 수정 요청 (필수 1: hide가 tone 항목에 가려짐) | 37bdef7 |
| 2026-10-13 | backend-dev | M3a2 리뷰 수정 (필수 1, 권장 1·2·3·5) | 완료 | 1f8c703 982d17e |
| 2026-10-13 | code-reviewer | M3a2 2차 리뷰 | 승인 (필수 0, 권장 3) | 982d17e |
| 2026-10-13 | orchestrator | M3a2 PR 생성 | PR | (아래 커밋) |
| 2026-10-13 | orchestrator | PR #11 (M3a2) PM 실제 호출 확인(T5) 후 머지, M3a3 시작 | 진행 | e6abf8b |
| 2026-10-13 | planner | M3a3 계획 개정 (T1~T8, 분할안) | 완료 (PM 결정 필요: A 분할, B 스키마 검증, C env chore) | 미커밋 |
| 2026-10-13 | orchestrator | M3a3 PM 결정: A (a) 두 PR (M3a3 + M3a3b), B (a) 스키마 검증은 테스트에만, C (c) env chore 안 함 | 진행 | e6abf8b |
| 2026-10-13 | orchestrator | PM 결정: 속도 개선 A (서버·웹 동시 진행, M3a3 1부 머지 후부터) | 기록 | e6abf8b |
| 2026-10-13 | planner | M3a3 계획 개정·분할 (M3a3 1부 + M3a3b) PM 승인 | 완료 | 1060e13 |
| 2026-10-13 | backend-dev | M3a3 T2 미디어 공통 형식·R1~R9·date_relation (대조 테스트 (6)은 T1 대기, 확인점 S1 505 > 450 → PM 결정 필요) | 완료 | 4d8929c |
| 2026-10-13 | content-curator | M3a3 T1 공용 미디어 스키마 + valid 10 / invalid 17 예시 | 완료 | d70cb1c |
| 2026-10-13 | orchestrator | PM 결정: 확인점 초과(S1 505) → (a) T3을 별도 PR로 분리 (M3 19개 PR) | 기록 | 4d8929c |
| 2026-10-13 | backend-dev | M3a3 T2 후속: 스키마 대조 테스트 (6) + 정규화를 스키마에 맞춤 6건 (711 tests) | 완료 | 414863c |
| 2026-10-13 | planner | M3a3 T3 분리 → M3a3a 계획 신설, M3a3·M3a3b·로드맵 수정 (19 PR, D1 PM 결정 필요) | 완료(미커밋, PM 승인 대기) | 414863c |
| 2026-10-13 | orchestrator | PM 결정: M3a3a 계획 승인(이름 유지), D1 (b) 지금 문구 그대로 (AP통신 등은 걸리지 않음) | 기록 | 414863c |
| 2026-10-13 | test-engineer | M3a3 T4 완료 기준 검증 + M3a2 2차 권장 2·3 (740/740 × 3) | 완료 | a56f1f5 |
| 2026-10-13 | content-curator | M3a3 T1 후속: README 문구 2곳 (워크트리 격리로 루트 커밋 불가) | 막힘 | a56f1f5 |
| 2026-10-13 | planner | M3a3a 계획 PM 결정 반영(승인, D1 (b)) + 로드맵 정리 | 완료 | f37bdc5 |
| 2026-10-13 | content-curator | M3a3 T1 후속: 예시 README 문구 2곳 | 완료 | 133eede |
| 2026-10-13 | code-reviewer | M3a3 브랜치 리뷰 (권장 6: 윤초·nbsp 빈틈 → M3a3a, Spring 매퍼 단언 → M3a3b, 계획 상태, UA, thumb 호스트) | 승인 | 133eede |
| 2026-10-13 | orchestrator | M3a3 PR 준비: README 진행 표(7 of 19), 계획 상태 정리 | 진행 | 133eede |
| 2026-10-13 | orchestrator | PR #12 M3a3 머지 확인 (main f99a9e7). M3a3a 시작 feat/media-rules, M3b2 웹 병렬 시작 feat/world-events-ui (.worktrees/design) | 진행 | f99a9e7 |
| 2026-10-13 | orchestrator | PM 결정: M3a3 리뷰 권장 1·2(윤초, 유니코드 공백 빈칸)를 M3a3a T2에 더함 | 기록 | f99a9e7 |
| 2026-10-13 | backend-dev | M3a3a T1 촬영 시점 파서 + 라이선스 상태 판정 (840 tests, S1 176) | 완료 | bf5139b 37bd5a6 |
| 2026-10-13 | planner | M3a3a 계획: 리뷰 권장 1·2를 T2 (9)(10) + T2b(content-curator)로 추가 | 완료 | d9f7b63 |
| 2026-10-13 | backend-dev | M3a3a T2 차단 목록·통신사 제외 필터, 윤초·유니코드 공백, Wikidata 주석 (915 tests, 누적 S1 432) | 완료 | e9e8b46 b1a6faa 958a30f |
| 2026-10-13 | planner | M3b2 계획 개정 (실제 계약, MSW 두 층, 웹 타입 3건, T1b/T6, 약 365~510줄) — PM 재승인·결정 2건 필요 | 완료(미커밋) | f99a9e7 |
| 2026-10-13 | orchestrator | PM 결정: M3b2 계획 승인, 1 (a) 한 PR + T1b 뒤 확인점 170, 2 (a) 사진 없이 T5로 머지·사진 확인은 M3a3b 뒤 T6 | 기록 | f99a9e7 |
| 2026-10-13 | frontend-dev | M3b2 T1 API 클라이언트 (시간 초과·취소·problem code, 293 tests, S1 90) | 완료 | 3fdd43b |
| 2026-10-13 | content-curator | M3a3a T2b 스키마 빈 값 패턴 + invalid 예시 r1-nbsp-only-attribution (S3 41) | 완료 | 424242e |
| 2026-10-13 | orchestrator | 세션 한도(21:40 해제) 뒤 재개: planner M3b2 결정 기록 재개, server/build의 ' 2' 사본 정리(gradlew clean) | 진행 | 424242e |
| 2026-10-13 | planner | M3b2 계획 PM 결정 기록 + 로드맵 정리 (feat/world-events-ui) | 완료 | 934e6e8 |
| 2026-10-13 | frontend-dev | M3b2 T1b 타입·fixture·MSW 핸들러 (315 tests, 누적 S1 187 > 확인점 170 → PM 결정 필요) | 완료 | 1593bb0 6c081e8 |
| 2026-10-13 | orchestrator | PM 결정: M3b2 확인점 초과(187) → (a) 한 PR로 계속, T2 시작. 매시 점검 취소 → 90% 사용량 시 해제 시각 일회성 예약 | 기록 | 6c081e8 |
| 2026-10-13 | test-engineer | M3a3a T3 완료 기준 검증 (1178/1178 × 3, BMP 전체 공백 대조 차이 0). 참고: LicenseClassifier·MediaProperties의 isBlank() 기준 불일치 2건 | 완료 | d789025 |
| 2026-10-13 | frontend-dev | M3b2 T2 그날의 세계 섹션 화면 (336 tests, 누적 S1 348) | 완료 | 02d8875 |
| 2026-10-13 | frontend-dev | M3b2 T3 데모 날짜 버튼 + 결과 화면 연결 (340 tests, 누적 S1 404) | 완료 | 893c8b1 |
| 2026-10-13 | test-engineer | M3b2 T4 완료 기준 검증 + 데모 3행 끝까지 시험 (356/356 × 3) | 완료 | 39c566c |
| 2026-10-13 | code-reviewer | M3b2 리뷰 1차: 필수 1(AbortSignal.any가 iOS 16.4/Safari 16.4 빌드 대상에 없음), 권장 3 | 수정 요청 | 39c566c |
| 2026-10-13 | frontend-dev | M3b2 리뷰 필수 1 수정: AbortSignal.any 대체 경로 (361 tests, 누적 S1 434) | 완료 | ae4433b |
| 2026-10-13 | code-reviewer | M3b2 리뷰 2차 | 승인 | ae4433b |
| 2026-10-13 | orchestrator | M3b2 README 표 갱신, push, PR 열림 | 완료 | d95c0dc |
| 2026-10-13 | backend-dev | M3a3a T3 지적 반영: 공백 판정 MediaText.isBlank 하나로 (1190 tests, 누적 S1 448) | 완료 | 90ddb6f |
| 2026-10-13 | code-reviewer | M3a3a 리뷰 1차: 필수 2(CC BY 4.0 nc → free, 공백 시험 값이 보이지 않는 원문자), 권장 4 | 수정 요청 | 90ddb6f |
| 2026-10-13 | backend-dev | M3a3a 리뷰 필수 1: 라이선스 관할 자리에서 NC·ND·SA·BY 제외 (1196 tests, 누적 S1 456) | 완료 | 2c77589 |
| 2026-10-13 | test-engineer | M3a3a 리뷰 필수 2: 공백 시험 값을   이스케이프로 (1196 tests) | 완료 | a1100fd |
| 2026-10-13 | orchestrator | PR #13 M3b2 머지 확인 (main e254ad1). M3b2b 계획 개정 시작 (feat/world-photos-a11y). 5시간 한도 74% → 해제 02:50 뒤 깨우기 예약(02:53, 03:10) | 진행 | e254ad1 |
| 2026-10-13 | code-reviewer | M3a3a 리뷰 2차 | 승인 | a1100fd |
| 2026-10-13 | orchestrator | M3a3a PR 준비: README 진행 표(9 of 19), push, PR | 진행 | 4e738c1 |
| 2026-10-13 | planner | M3b2b 계획 개정 (T0 신설, T6, 약 315~445줄) — PM 재승인·결정 4건 필요 | 완료(미커밋) | e254ad1 |
| 2026-10-13 | planner | M3b2b 계획 결정 1~4 (a) 반영 — 로드맵은 미수정 | 완료(미커밋) | e254ad1 |
| 2026-10-13 | planner | M3b2b 계획·로드맵 (결정 1~4 (a), M3b2 완료 표시) | 완료 | 3eb8d5e |
| 2026-10-14 | frontend-dev | M3b2b T0 응답 모양 검사 (S1 74) | 완료 | 6027121 |
| 2026-10-14 | frontend-dev | M3b2b T1 사진 묶음·비극 블록 (S1 145, 누적 219) | 완료 | 735bb45 |
| 2026-10-14 | frontend-dev | M3b2b T2 캡션 접근성 (S1 34, 누적 249 — 확인점 300 아래, T3 진행) | 완료 | c19777b |
| 2026-10-14 | frontend-dev | M3b2b T3 Playwright·web-ci (S1 70, 누적 319, E1 12/12) | 완료 | c7ef051, 91df908 |
| 2026-10-14 | test-engineer | M3b2b T4 기준 검증·전체 시험 (458/458 ×3, e2e 14/14 ×3) | 완료 | cb6f9b3 |
| 2026-10-14 | code-reviewer | M3b2b 리뷰 — 승인 (필수 0, 권장 5) | 승인 | cb6f9b3 |
| 2026-10-14 | orchestrator | M3b2b PR #15 열림 (README b2e8bfc) | PR 대기 | b2e8bfc |
| 2026-10-14 | orchestrator | PR #14 머지 뒤 main 병합, README 충돌 해결 (웹 검사 458/458, e2e 14/14) | 완료 | 9acba53 |
| 2026-10-14 | planner | M3b3 계획 개정 (약 300, ×1.19 약 355, 확인점 T2 뒤 360) — PM 재승인·결정 3건 필요 | 완료(미커밋) | 9642d08 |
| 2026-10-14 | planner | M3b3 계획 결정 1 (a)·2 (b)·3 (a) 기록 | 완료 | bbdd055 |
| 2026-10-14 | planner | M3a3b 계획 개정 (약 715 → 분할 제안, 이월 권장 1~6) — PM 재승인·결정 2건 필요 | 완료(미커밋) | 9642d08 |
| 2026-10-14 | frontend-dev | M3b3 T1 재생 기초 (S1 275 — mediaFakes 141, 예상 150~180 초과) | 완료 | 08a9424, 4a8d717 |
| 2026-10-14 | frontend-dev | M3b3 T2 영상 카드·캡션 방어 (S1 179, 누적 454 — 확인점 360 초과, test/ 제외 313) — PM 결정 필요 | 완료 | 98a0981, b0a66e3 |
| 2026-10-14 | planner | M3a3b 개정·M3a3c 분할·로드맵 (M3 PR 20개) | 완료 | af88e5d |
| 2026-10-14 | frontend-dev | M3b3 T3 세계 화면 연결·표본 버튼 (S1 27, 누적 340 — test/ 제외 규칙) | 완료 | a21f0ef, 5f6ccf7 |
| 2026-10-14 | backend-dev | M3a3b T0 이월 권장 1~3 (S1 52) — PM 결정 1건 (Commons 첫 글자 규칙) | 완료 | 9560b5f |
| 2026-10-14 | test-engineer | M3b3 T4 기준 통과, 기준 밖 버그 발견 (두 영상 동시 진입 시 둘 다 멈춤, e2e 26/28) — PM 결정 필요 | 실패 발견 | bbe4b8d, d490bbd |
| 2026-10-14 | orchestrator | M3b3 영상 버그 PM (a) — frontend-dev가 이 PR에서 수정 | 진행 | d490bbd |
| 2026-10-14 | backend-dev | M3a3b T1 Commons 클라이언트·매퍼 (S1 333, 누적 385 — 확인점 350 알림) — 막힘: iiextmetadatalanguage=ko 결정 필요 | 막힘 | e8b4a04 |
| 2026-10-15 | frontend-dev | M3b3 두 영상 동시 진입 버그 수정 (e2e 28/28, 누적 S1 343) | 완료 | 0514fe7 |
| 2026-10-15 | backend-dev | M3a3b T1 마무리 — ko 제거, 녹화 fixture·매퍼 시험 (누적 S1 385, 확인점 350 알림) | 완료 | cdfdd37, 72779d8 |
| 2026-10-15 | code-reviewer | M3b3 리뷰 — 승인 (필수 0, 권장 5) | 승인 | 0514fe7 |
| 2026-10-15 | orchestrator | M3b3 PR 열림 (README·진행 기록 f94718e) | PR 대기 | f94718e |
| 2026-10-15 | backend-dev | M3a3b T2 세계 API 그날 사진 연결 (S1 200, 누적 585 — 확인점 450 초과) — PM 결정 필요 | 완료(멈춤) | 505164f |
| 2026-10-15 | planner | M3b3b 계획 개정 (약 316, ×1.14 약 360, 확인점 T1 뒤 210) — PM 재승인·결정 2건 필요 | 완료(미커밋) | 72fd4ae |
| 2026-10-15 | test-engineer | M3a3b T3 완료 기준 테스트 4개 추가, check 3회 연속 통과 (1302/0) | 완료 | d48ffae |
| 2026-10-15 | code-reviewer | M3a3b 브랜치 리뷰 — 승인 (필수 0, 권장 4) | 완료 | d48ffae |
| 2026-10-15 | orchestrator | M3a3b PR #17 열림 (main 병합 081b19a, README·진행 기록 ed2e261) | PR 대기 | ed2e261 |
| 2026-10-15 | planner | M3b3b PM 결정 기록·로드맵 반영 (약 317, ×1.14 약 361) | 완료 | 9671b78 |
| 2026-10-15 | frontend-dev | M3b3b T0 영상 재생 규칙 보강 + T1 방침 페이지·바닥글 (누적 S1 198, 확인점 210 아래, 단위 580/0, e2e 28/28) | 완료 | af443d4, 0755b7f |
| 2026-10-15 | frontend-dev | M3b3b T2 동의 단계 (누적 S1 361, 단위 604/0, e2e 28/28) | 완료 | db74962 |
| 2026-10-15 | planner | M3a3c 계획 개정 (약 445, 400 초과) — PM 재승인·결정 4건 필요 | 완료(미커밋) | ce3760c |
| 2026-10-15 | test-engineer | M3b3b T3 완료 기준 확인, e2e 20개 추가 (단위 604/0 ×3, e2e 48/48 ×3) | 완료 | b4fb5eb |
| 2026-10-15 | code-reviewer | M3b3b 리뷰 1회차 — 수정 요청 (필수 1: 방침의 Commons 문장, 권장 4) | 수정 요청 | b4fb5eb |
| 2026-10-15 | frontend-dev | M3b3b 리뷰 필수 1 수정 — 방침에 Commons를 현재 출처로 (단위 605/0, e2e 5173 사용 중이라 미실행) | 완료 | 33fb047 |
| 2026-10-15 | code-reviewer | M3b3b 리뷰 2회차 — 승인 (필수 0, e2e는 5173 사용 중이라 CI에서 확인) | 승인 | 33fb047 |
| 2026-10-15 | orchestrator | M3b3b PR #18 열림 (main 병합 cd5a256, README·진행 기록 935f540) | PR 대기 | 935f540 |
| 2026-10-15 | orchestrator | M3b3b 최종 브랜치 e2e 재실행 48/48 (PR 본문 갱신) | 완료 | 935f540 |
| 2026-10-16 | PM | M3b3b PR #18 머지 (시행일 커밋 전에 머지 → 후속 fix PR, 시행일 2026-10-16 PM 결정) | 완료 | 300acf7 |
| 2026-10-16 | frontend-dev | M3b3b T4 방침 시행일 2026년 10월 16일 (단위 605/0, e2e 48/48, D1 출력 없음) | 완료 | 3677037 |
| 2026-10-15 | planner | M3a3c PM 결정 1~4 기록·로드맵 8곳 반영 | 완료 | d85a402 |
| 2026-10-16 | code-reviewer | fix/privacy-effective-date 리뷰 — 승인 (필수 0, 권장 0) | 승인 | 8f126bc |
| 2026-10-16 | orchestrator | 시행일 후속 PR #19 열림 | PR 대기 | 8f126bc |
| 2026-10-16 | planner | M3b3c 계획 개정 (약 257, ×1.14 약 293, 확인점 T2 뒤 220) — PM 재승인·결정 2건 필요 | 완료(미커밋) | 728b532 |
| 2026-10-16 | backend-dev | M3a3c T0 공용 Deadline·미디어 1회 호출·Commons error + T1 PageImages·파일 묶음 (누적 S1 433 > 확인점 320, check 1359/0) | 완료(멈춤) | 249f201, 57cc5c2, 6c220bd, 4058edb |
| 2026-10-16 | planner | M3b3c PM 결정 1·2 기록 | 완료 | 5b735fd |
| 2026-10-16 | planner | M3a3c 분할 (T0·T1만, 433) + M3a3d 계획 새로 (약 250), 로드맵 PR 21개 — PM 결정 1건 (M3a3d T1 담당) | 완료 | c56c825 |
| 2026-10-16 | frontend-dev | M3b3c T1 IFrame API·캡션 문구 + T2 YouTube 카드 (누적 S1 229 > 확인점 220, 단위 674/0, e2e 48/48) — PM 결정 필요 | 완료(멈춤) | 615ba01, 20a7fb8, e198d93, 6b40ed7 |
| 2026-10-16 | PM | M3a3d T1 담당 backend-dev, M3b3c T3는 이번 PR (확인점 229 수용) | 결정 | - |
| 2026-10-16 | frontend-dev | M3b3c T3 YouTube 재생 규칙 (누적 S1 344, 단위 693/0, e2e 48/48) | 완료 | 185da59 |
| 2026-10-16 | test-engineer | M3a3c T3 T0·T1 검증, 테스트 3개 추가 (check 1369/0 ×3, 최악 시간 약 11.7초) | 완료 | 85a3df5 |
| 2026-10-16 | code-reviewer | M3a3c 리뷰 — 승인 (필수 0, 권장 5) | 승인 | 85a3df5 |
| 2026-10-16 | orchestrator | M3a3c PR #20 열림 (main 병합 25bf972, README·진행 기록 b99c801) | PR 대기 | b99c801 |
| 2026-10-16 | PM | M3a3c PR #20 머지 | 완료 | 25d5227 |
| 2026-10-16 | test-engineer | M3b3c T4 e2e 32개 추가, 빠른 스크롤 경쟁 버그 발견 (단위 694/1, e2e 78/2) — PM 결정 필요 | 실패 발견 | eda833e |
| 2026-10-16 | PM | M3b3c 경쟁 버그는 이 PR에서 고침 (결정 a) | 결정 | - |
| 2026-10-16 | frontend-dev | M3b3c T3 수정: 취소한 YouTube 시작을 개수로 셈 (단위 695/0, e2e 80/0, S1 353) | 완료 | 22fac92 |
| 2026-10-16 | test-engineer | M3b3c T4 재실행: 22fac92 확인, 보고 순서 시험 추가, 남은 경쟁 버그 1건 (단위 700/1, e2e 90/2) | 실패 발견 | 9d1f16e |
| 2026-10-16 | frontend-dev | M3b3c T3 2차 수정: 바라는 상태 기준으로 YouTube 보고 판단, 멈춤 카운터 제거 (단위 704/0, e2e 92/0, S1 369) | 완료 | 9a7d037 |
| 2026-10-16 | test-engineer | M3b3c T4 재실행 2: 9a7d037 확인, 공격 시험 추가, 버그 2종 (단위 709/1, e2e 110/8) — PM 결정 필요 | 실패 발견 | b9145c5 |
| 2026-10-16 | PM | M3b3c T3 마지막 한 번 더 (화면 밖 PLAYING 멈춤 우선, 실패하면 눌러서 재생으로 축소) | 결정 | - |
| 2026-10-16 | frontend-dev | M3b3c T3 3차 수정: 5/6 통과, e2e 가짜의 멈춤 조건 때문에 1건 불가 (커밋 안 함, 패치 보관) — 막힘, PM 결정 필요 | 막힘 | - |
| 2026-10-16 | PM | M3b3c e2e 가짜 멈춤 규칙을 머리말대로 바로잡고 3차 수정 커밋 (결정 a) | 결정 | - |
| 2026-10-16 | frontend-dev | M3b3c T3 3차 수정 커밋: 시작별 보고 짝짓기, 화면 밖 재생 멈춤 (단위 710/0, S1 375) | 완료 | 2515826 |
| 2026-10-16 | test-engineer | M3b3c T4 재실행 3: e2e 가짜 멈춤 규칙 바로잡음, 2515826 확인 (단위 714/0, e2e 118/0, 3회) | 완료 | a34fc9a |
| 2026-10-16 | code-reviewer | M3b3c 리뷰 — 승인 (필수 0, 권장 5, 단위 714/0, e2e 118/0) | 승인 | a34fc9a |
| 2026-10-16 | orchestrator | M3b3c PR #21 열림 (README·진행 기록 8a97110) | PR 대기 | 8a97110 |
| 2026-10-16 | orchestrator | M3b3c 브랜치에 main 병합 (PR #20 반영) | 완료 | 2084434 |
| 2026-10-17 | PM | M3b3c PR #21 머지 | 완료 | 7d83f3d |
| 2026-10-17 | planner | M3a3d 계획 개정 (리뷰 권장 5개 포함, T0 신설, 약 400 — 경계) + 로드맵 M3a3c·M3b3c 완료 반영 — PM 승인·결정 1건 필요 | 완료(미커밋) | 7d83f3d |
| 2026-10-17 | PM | M3a3d 계획 승인, 결정 1 (a) 한 PR + 확인점 2개 | 결정 | - |
| 2026-10-17 | planner | M3a3d PM 승인·결정 1 (a) 기록, 계획·로드맵 커밋 | 완료 | 2ee1821 |
| 2026-10-17 | backend-dev | M3a3d T0 리뷰 권장 2~5 (Deadline close·Interrupted, no query 실패, 공용 error 검사; check 1416/0, 누적 S1 84/−28 — 확인점 1 통과) | 완료 | 22fc76a, 0d86c85 |
| 2026-10-18 | backend-dev | M3a3d T1 사건 사진 세계 API 연결 (check 1458/0, 누적 S1 312/−72 — 확인점 2 통과) | 완료 | c67e674, 900465d |
| 2026-10-18 | test-engineer | M3a3d T2 기준 검증·인수 테스트 8개, 최악 시간 설정값 상한·미디어 창 하나 (check 1467/0 ×3) | 완료 | 797f7b2 |
| 2026-10-18 | code-reviewer | M3a3d 리뷰 — 승인 (필수 0, 권장 3, check 1467/0) | 승인 | 797f7b2 |
| 2026-10-18 | orchestrator | M3a3d PR #22 열림 (README·진행 기록 c18e9a6) | PR 대기 | c18e9a6 |
| 2026-10-18 | PM | M3a3d PR #22 머지 | 완료 | d566bda |
| 2026-10-18 | planner | M3a4 계획 개정 (약 675 → 두 PR 분할 제안, T0 리뷰 권장 2개) + 로드맵 M3a3d 완료 — PM 승인·결정 2건 필요 | 완료(미커밋) | d566bda |
| 2026-10-18 | PM | M3a4 계획 승인, 결정 1 (b) 한 PR, 결정 2 (b) T2 backend-dev | 결정 | - |
| 2026-10-18 | planner | M3a4 PM 승인·결정 1 (b)·2 (b) 기록, 계획·로드맵 커밋 | 완료 | 6feb319 |
| 2026-10-18 | backend-dev | M3a4 T0 tragic 사건 미디어 이중 방어, within 주석 (check 1468/0, 누적 S1 7) | 완료 | b16ce15, 50c53c1 |
| 2026-10-18 | backend-dev | M3a4 T1 영상 후보 형식·selection·media.video 설정 (check 1494/0, 누적 S1 211) | 완료 | 28c1a16 |
| 2026-10-18 | backend-dev | M3a4 T2 ★ 정확도 판정 VideoAccuracy (check 1586/0, 누적 S1 434 — 확인점 1 400 넘음, 알림) | 완료 | 0b74b15 |
| 2026-10-23 | test-engineer | M3a4 T3 중간 확인 T0–T2 (Sonnet 인계, clean check 1602/0 ×2, 민감 48/0 3회째, S1 434) — PM 결정 필요: 어포스트로피 분리 | 완료 | 65d0331 |
| 2026-10-23 | backend-dev | M3a4 T5 글 속 날짜 TextDates·서수·재료 (1) 연결·어포스트로피 (check 1683/0, 누적 S1 573) — PM 결정 필요: 지정 시험 쌍이 절반 규칙으로 일치 | 완료 | 7a5d526, 42a780e |
| 2026-10-23 | backend-dev | M3a4 T6 검색 틀 연결 VideoSearch·같은 마감·selection (check 1719/0, 누적 S1 953 — 확인점 2 750 넘음, 알림) | 완료 | 1c977d0, ac9aae6 |
| 2026-10-23 | test-engineer | M3a4 T7 완료 기준 검증 (clean check 1737/0 ×2, 민감 79/0 3회째 — iCloud 사본 환경 실패 1회 뒤 clean 후 통과, 최악 11.73s, S1 953) | 완료 | 8c51557 |
| 2026-10-23 | code-reviewer | M3a4 브랜치 리뷰 (승인, 필수 0·권장 3, check 1737/0) — PM 결정 필요: EventTarget 동등성 | 승인 | 8c51557 |
| 2026-10-24 | backend-dev | M3a4 리뷰 권장 1 EventTarget Javadoc 바로잡기 (주석만, PM 결정 5 (a)) | 완료 | c2997ac |
| 2026-10-24 | orchestrator | M3a4 README·진행 기록, PR 열기 | 완료 | - |
| 2026-10-24 | orchestrator | **▶ PM(Eric) 결정** · 저장소 이전: iCloud 사본(" 2" 파일이 Flyway·서식 검사를 깨뜨림, `.git/index 2`) 때문에 작업 사본을 iCloud 동기화 Desktop에서 `~/Projects/The-World-That-Day`(새 clone)로 옮김. 모든 브랜치 origin과 같음 확인, 새 위치 server check 1737/0·web 714 통과, README 안내 추가 | 완료 | - |
| 2026-10-24 | PM | M3a4 PR #23 머지 | 완료 | 298cb8e |
| 2026-10-24 | planner | M3a4b 계획 개정 (머지 이름 대조, 리뷰 권장 2·3을 T0으로, 다시 셈 약 1,100 → 분할안) + 로드맵 M3a4 완료 — PM 승인·결정 4건 필요 | 완료(미커밋) | 298cb8e |
| 2026-10-24 | PM | M3a4b 계획 승인, 결정 1 (a) 두 PR(M3a4c 결과 캐시 분리, M3 22개)·2 (a)·3 (b)·4 (a) | 결정 | 7ce96d4 |
| 2026-10-24 | content-curator | M3a4b T1 확인용 기준표 (8날짜·근거 65, C1 ok, S3 529, 약한 칸 6개 표시) | 완료 | 4b5a143 |
| 2026-10-24 | backend-dev | M3a4b T0 판정 출처별 격리·Candidate/SourceResult 입력 검사·패턴 재사용 (check 1794/0 ×2, 민감 69/0 3회째, 누적 S1 192/−81) | 완료 | 104d4fa, ad2ae3d |
| 2026-10-24 | orchestrator | T1 커밋을 feat/commons-video로 옮김 (cherry-pick) | 완료 | 90f8462 |
| 2026-10-24 | backend-dev | M3a4b T2 Commons 영상 부품·녹화 (check 1826/0 ×2, 민감 117/0 3회째, 누적 S1 763/−222 — 확인점 1 600 넘음, 멈춤) — PM 결정 필요: T3 진행 여부 | 완료 | b38a971, effba72 |
| 2026-10-24 | PM | M3a4b 확인점 1 결정 (a) 이 PR에서 T3까지 | 결정 | 23d3ca5 |
| 2026-10-24 | backend-dev | M3a4b T3 Commons 영상 출처 연결 (check 1850/0 ×2, 민감 127/0 3회째, 누적 S1 954/−222 — 확인점 2 850 넘음, 알림) | 완료 | 25a68e0 |
| 2026-10-24 | test-engineer | M3a4b T4 완료 기준 검증 (clean check 1872/0 ×3, 시험 22개 추가, 미디어 구간 3.03s·최악 11.72s 변화 없음, S1 954) — PM 결정 필요: KBS 행 근거 번호, 녹화 속 제3자 이메일 | 완료 | 360ab8c |
| 2026-10-24 | PM | M3a4b T4 결정 1 (a) KBS 두 행 그대로·2 (a) 녹화 바이트 그대로 | 결정 | 7cb8fc7 |
| 2026-10-24 | code-reviewer | M3a4b 브랜치 리뷰 (승인, 필수 0·권장 5, cleanTest check 1872/0, 근거 직접 확인 일치) | 승인 | 7cb8fc7 |
| 2026-10-24 | orchestrator | M3a4b README·진행 기록, PR 열기 | 완료 | - |
| 2026-10-24 | orchestrator | PR #24 server-ci 실패 1건 (WorldStoryEventPhotoPartsRegressionTest:320 PageImages 요청 0 — 영상·사진 요청 자리 다툼 추정) → backend-dev | 진행 | 690806d |
| 2026-10-24 | backend-dev | PR #24 CI 수정: 사진 경로마다 요청 자리 남김 (영상 Lane), check 1878/0 ×2, 부하 3회 통과, 누적 S1 1155/−236 | 완료 | 943244e |
| 2026-10-24 | code-reviewer | PR #24 수정 커밋 리뷰 (승인, 필수 0·권장 4, cleanTest check 1878/0) | 승인 | 943244e |
| 2026-10-24 | PM | M3a4b PR #24 머지 (T5 확인) | 완료 | 9203562 |
| 2026-10-24 | PM | M3a4b T5·M3b3 T6 확인: 1969-07-20 화면에 Commons 영상 재생 | 완료 | 9203562 |
| 2026-10-24 | planner | M3a4c 계획 작성 (M3a4b B절 분리, Candidate.origin, PR #24 권장 T4, 다시 셈 약 650) + 로드맵 M3a4b 완료 — PM 승인·크기 결정 필요 | 완료(미커밋) | 9203562 |
| 2026-10-24 | PM | M3a4c 계획 승인, 크기 결정 1 (a) 한 PR (확인점 T1 300·T3 700 알림) | 결정 | a3469ba |
| 2026-10-24 | backend-dev | M3a4c T1 결과 캐시 저장소 V3·저장소·TTL 설정 (check 1902/0 ×2, DB 22/0 3회째, 누적 S1 181) — DatabaseSmokeTest 기대값 V3만큼 늘림 | 완료 | aa77bb3 |
| 2026-10-24 | backend-dev | M3a4c T2 Candidate.origin·저장 형식·되살리기 (check 1933/0 ×2, 민감 118/0 3회째, 누적 S1 384/−9) | 완료 | 6f3b93e |
| 2026-10-24 | backend-dev | M3a4c T3 VideoSearch 캐시 흐름·DB 실패 처리 (check 1961/0 ×2, 민감 78/0 3회째, 누적 S1 580/−23 — 확인점 2 700 아래) | 완료 | 5d4be2e |
| 2026-10-24 | backend-dev (Sonnet) | M3a4c T4 PR #24 리뷰 이월 (영상 WARN 단언, 시험 이름 2개, lane 대기 상한, 주석; check 1961/0 ×2) — PM 결정 필요: 영상 WARN 줄 수 단언 방식 | 완료 | f342068, b950b84, 122c78d |
| 2026-10-25 | PM | M3a4c T4 결정 (a) 그대로, T5에서 부하 아래 20회 흔들리면 (b) | 결정 | 2af734c |
| 2026-10-25 | test-engineer (Sonnet) | M3a4c T5 완료 기준 표·빠진 시험 10개 추가 (clean check 1971/0 ×3, clean build 1972/0, 회귀 시험 부하 20/20 → 대체안 (b) 미적용, 시간 3.03 s/11.7 s 동일, S1 583/−23) | 완료 | 4fd00fb |
| 2026-10-25 | code-reviewer (Opus) | M3a4c 리뷰 main...feat/media-search-cache (clean check 1972/0, 필수 0, 권장 5) | 승인 | - |
| 2026-10-25 | orchestrator | M3a4c PR 열기 (README 19 of 22, M3a4b ✅ PR #24, M3a4c 리뷰 중) | 완료 | - |
| 2026-10-25 | PM | M3a4c PR #25 머지 (T6 캐시 확인) | 완료 | 28ffb01 |
| 2026-10-25 | planner (Opus) | M3a5 계획 개정(머지 코드 대조, 작업 11개, 다시 셈 약 830) + 로드맵 — PM 결정 필요: 크기 1, ★ T3 담당 2 | 완료(미커밋) | - |
| 2026-10-25 | PM | M3a5 계획 승인 — 크기 결정 1 (a) 한 PR, 확인점 T3 뒤 500(멈춤)·T5 뒤 1,000(알림), 결정 2 (b) ★ T3 backend-dev | 결정 | 14f1017, 31e2412 |
| 2026-10-25 | data-researcher (Sonnet) | M3a5 T0c YouTube 약관·쿼터 문서 다시 확인 (9개 날짜 바뀜, 뜻 바뀐 조항 없음; `error.errors[].reason` 구조는 문서에 없음) | 완료 | b3f6be6 |
| 2026-10-25 | backend-dev (Sonnet) | M3a5 T1 YouTube 설정·키·웹 origin·TTL 30일 상한 (check 2032/0 ×2, S1 200/−6) — 기존 단언 4개를 새 yml 값에 맞춰 바꿈(약화 없음, T7 허용 목록에 더함) | 완료 | 97ff244 |
| 2026-10-25 | backend-dev (Opus) | M3a5 T2 YouTube Data API 클라이언트 (check 2056/0 ×2, 누적 S1 413/−6 — 확인점 1 근접) | 완료 | ab060b3 |
| 2026-10-26 | backend-dev (Sonnet) | M3a5 T3 쿼터 두 버킷 (check 2077/0 1회, 이후 3회 기존 WorldStoryCommonsVideoTest가 T1 YoutubeKeyCheck WARN에 깨짐 — 순서 의존) — 확인점 1: 누적 S1 525/−6 > 500 멈춤, PM 결정 필요 | 완료(막힘) | 876affb |
| 2026-10-26 | PM | M3a5 확인점 1 결정: 1 (b) 나눔(M3a5 = T0c~T3, T4~T6·T8은 M3a5a `feat/youtube-video-source`, M3 PR 23개), 2 (a) 시험 설정에서 YoutubeKeyCheck 로그 수준을 낮춤, 3 (a) reserve 그대로 | 결정 | - |
| 2026-10-26 | backend-dev (Sonnet) | M3a5 T1 fix 시험 설정에서 YoutubeKeyCheck 로그 수준 ERROR (check --rerun-tasks 2077/0 ×2, S1 그대로 525/−6) | 완료 | 4cce75d |
| 2026-10-26 | planner (Sonnet) | M3a5 분할 반영: M3a5 계획을 T0c~T3·T1b·T7로 좁힘, 새 M3a5a 계획(T0·T4·T5·T6·T7·T8, 약 510 — 시작 때 크기·★ T4 결정), 로드맵 23개 (오케스트레이터가 실수로 고쳐 쓴 지난 결정 기록 줄을 되돌림) | 완료 | f55be36 |
| 2026-10-26 | test-engineer (Sonnet) | M3a5 T7 검증 + M3a4c 리뷰 권장 1 javadoc 이동 (+26 케이스, check --rerun-tasks 2104/0 ×3, 시간 3.03 s/11.7 s 그대로, S1 525/−6) | 완료 | 86a219b |
| 2026-10-26 | code-reviewer (Opus) | M3a5 리뷰 main...feat/official-channel-video (check 2104/0) — 필수 1: YoutubeTestDefaultsTest가 셸 환경변수까지 막는다고 잘못 주장, 권장 7 | 수정 요청 | - |
| 2026-10-26 | PM | M3a5 리뷰 선택: server/build.gradle.kts tasks.test에서 YOUTUBE_API_KEY 환경변수를 지움, 이 PR에 넣음 | 결정 | - |
| 2026-10-26 | backend-dev (Sonnet) | M3a5 리뷰 수정: 필수 1(YoutubeTestDefaultsTest 이름·설명, 공유 컨텍스트), PM 선택 tasks.test에서 YOUTUBE_API_KEY 제거, 권장 2·3 (키를 export한 상태로 check --rerun-tasks 2106/0 ×2, S1 532/−6) | 완료 | 1092495 |
| 2026-10-26 | code-reviewer (Opus) | M3a5 재리뷰 86a219b..HEAD (재현 29/0, check 2106/0) | 승인 | - |
| 2026-10-26 | orchestrator | M3a5 PR 열기 (README 20 of 23, M3a4c ✅ PR #25, M3a5 리뷰 중, M3a5a 행) | 완료 | - |
| 2026-10-26 | PM | M3a5 PR #26 머지 | 완료 | 40f520d |
| 2026-10-26 | PM | M3a5a 계획 승인 — 크기 결정 1 (a) 한 PR, 확인점 T4 뒤 300(멈춤)·T5 뒤 650(알림), 결정 2 (b) ★ T4 backend-dev | 결정 | - |
| 2026-10-26 | backend-dev (Opus) | M3a5a T4 YoutubeVideoMapper 확인·버림 규칙과 레코드 (check --rerun-tasks 2161/0 ×2, 확인점 1: S1 281/−0 ≤ 300) — PM 결정 필요: 계획 T4 (5) 제목 문구, 기존 DEBUG 줄의 영상 ID | 완료(결정 대기) | 478b78d |
| 2026-10-26 | PM | M3a5a T4 결정: 1 (a) 계획 문구 1986년, 2 (a) 기존 DEBUG 줄 예외·공개 배포 전 fix/ | 결정 | - |
| 2026-10-26 | backend-dev | M3a5a T5 | 완료 (PM 결정 필요: T5 (6) 캐시 쓰기 실패 처리), S1 557 | 725a614, b4cf0fd |
| 2026-10-26 | PM | M3a5a T5 결정: (a) 쓰기 한 번 실패하면 down=true, VideoSearchCacheTest 단언 2개 변경 | 결정 | - |
| 2026-10-26 | backend-dev | M3a5a T5 결정 (a) 반영 | 완료, S1 563 | 3b83879 |
| 2026-10-26 | test-engineer | M3a5a T7 | 완료, 2213/0 (check 2회, 해당 범위 3회) | f9c38ce |
| 2026-10-26 | code-reviewer | M3a5a 리뷰 | 승인 (필수 0, 권장 4) | f9c38ce |
| 2026-10-26 | orchestrator | M3a5a PR | PR #27 열림, PM T0 Y5·T8 대기 | 473e3f0 |
| 2026-10-26 | PM | M3a5a | PR #27 머지 | 124cf31 |
| 2026-10-26 | frontend-dev | M3a5a T6 | 완료, web 714/0 | 0016f0f |
| 2026-10-26 | test-engineer | M3a5a T6 | 완료, web 714/0 x2, 빌드 통과 | 0016f0f |
| 2026-10-26 | code-reviewer | M3a5a T6 리뷰 | 승인 (필수 0, 권장 2) | 0016f0f |
| 2026-10-26 | orchestrator | M3a5a T6 PR | PR #28 열림 | 72efe8f |
| 2026-10-26 | PM | M3a5b 결정: (c) planner가 계획을 머지된 이름에 맞춰 고침, T0는 PM이 따로 | 결정 | - |
| 2026-10-26 | planner | M3a5b 계획 개정 | 막힘 (PM T0 대기), PM 결정 필요 1~5 | - |
| 2026-10-27 | PM | M3a5b 개정 재승인: 결정 1 (a), 2 (a), 3 (a), 4 (a) 한 PR·T2 뒤 450 멈춤·T2b 뒤 650 알림, 5 (b) | 결정 | 4bd5787 |
| 2026-10-27 | frontend-dev | M3a5a T6 시행일 2026년 10월 27일 (머지 당일) | 완료, web 714/0 | 2ad2b34 |
| 2026-10-27 | PM | M3a5b T0 대기 중 후속 정리 계획: chore/m3-followups | 결정 | - |
| 2026-10-27 | planner | chore-m3-followups 계획 | 완료, PM 결정 필요 D1~D5 | - |
| 2026-10-27 | PM | chore-m3-followups 계획 승인: D1 (a), D2 (a), D3 (b), D4 (c), D5 (a) | 결정 | - |
| 2026-10-27 | frontend-dev | chore T5·T6 | 완료, web 716/0 | db18c35, 1bad99d |
| 2026-10-27 | backend-dev | chore T1~T4 | 완료, server 2244/0 x2, S1 +44/−17 | 9c86279, 62f7dc0, 695619c, b8468f7 |
| 2026-10-27 | test-engineer | chore T7·T8 | 완료, server 2244/0 x2, web 716/0, T8 60회 재현 안 됨 | - |
| 2026-10-27 | code-reviewer | chore-m3-followups 리뷰 | 승인 (필수 0, 권장 3) | 727ad54 |
| 2026-10-27 | PM | M3a5a T6 | PR #28 머지 | 7bbdc96 |
| 2026-10-27 | orchestrator | chore-m3-followups PR | PR #29 열림 | 526ca50 |
| 2026-10-27 | PM | chore-m3-followups | PR #29 머지 | 49248e9 |
| 2026-10-27 | PM | M3a5b T0 대기 중 M2 계획 시작 | 결정 | - |
| 2026-10-27 | planner | M2 계획 (M2a·M2a2·M2b·M2b2) | 완료, PM 결정 필요 1~16 | - |
| 2026-10-27 | PM | M2 계획 승인: 결정 1~16 모두 추천대로 (#2 a, #3 a, #7 a, #10 a, #11 a, #26 b, M2-P1~P8 a, P9 b, P10 a) | 결정 | - |
| 2026-10-27 | frontend-dev | M2b T2 | 완료, web 755/0 x2, S1 +201 (계획 약 120) | 63560c9 |
| 2026-10-27 | data-researcher | M2a T0a | 완료, 법정동코드 확인됨(#10 a), R1 작성, PM 결정 필요 D6(#27) | 7e0875d |
| 2026-10-27 | PM | M2a D6 (#27): (a) R1만 쓴다 | 결정 | - |
| 2026-10-27 | backend-dev | M2a T2 | 완료, server 2303/0 x2, S1 +278 (계획 약 150), 기준 (4)는 T1 뒤 | 337fc6f |
| 2026-10-27 | frontend-dev | M2b T3 | 완료, web 772/0 x2, e2e 118/0 x2, 누적 S1 +341/−63 > 확인점 330 (알림) | 583fe37 |
| 2026-10-27 | backend-dev | M2a ★ T3 | 완료, server 2353/0 x2 (데모 6개 T1 뒤), 누적 S1 +380 > 확인점 1 300 (알림) | e464db8 |
| 2026-10-27 | frontend-dev | M2b T4 | 완료, web 782/0 x2, e2e 126/0 x2, 누적 S1 +398/−67 | f98cc8d |
| 2026-10-27 | PM | M2 구현 중 질문 A~E 모두 추천대로 | 결정 | - |
| 2026-10-27 | frontend-dev | M2b T5 | 완료, web 784/0 x2, 누적 S1 +403/−70 | 144f80f |
| 2026-10-27 | frontend-dev | M2b T3 후속 (PM 결정 B·C) | 완료, web 785/0 x2, e2e 126/0 x2, 누적 S1 +406/−70 | 5fb7630 |
| 2026-10-27 | backend-dev | M2a T4 + T2 후속 A | 완료, server 2380/0 x2, 로그 시험 x3, 누적 S1 +585 > 확인점 2 450 → 결정 D로 T5는 M2a2로 | afafb38, fd4c171 |
| 2026-10-29 | PM | 방향 전환: 주 대상을 캐나다로, 화면은 영어만, 세계의 사건은 그대로(영어 위키백과로), "그때 vs 지금" 콘텐츠 11가지 모두 범위에, 연·월 단위 값은 "그해" 문구로 표시. 확인된 출처는 바로 시작, 나머지는 리서치 뒤 보고 | 결정 | - |
| 2026-10-29 | orchestrator | M2a·M2a2·M2b·M2b2·M3a5b 멈춤 (브랜치 유지, PM T0·T0b·★T1 보류) | 멈춤 | - |
| 2026-10-29 | planner | 캐나다 전환 리서치 계획 (T1~T9, S1~S5, C0~C8) | 완료, PM 결정 필요 1~10 | - |
| 2026-10-29 | PM | 캐나다 리서치 계획 승인: 결정 1~9 추천대로, 10 (b) backend-dev | 결정 | - |
| 2026-10-29 | data-researcher | Canada T5 돈 | 완료, 카드 12개 (CPI·최저임금·주담대 금리 확인됨, 영화표·우표 빼기 추천) | ca8df1d (병합 5dbb684) |
| 2026-10-29 | data-researcher | Canada T3 영어 위키백과 사건 | 완료, 날짜·연도·YYYY in Canada 확인됨, 결정 후보 D1~D5 (S3 계획 때) | 9018d51 (병합) |
| 2026-10-29 | data-researcher | Canada T1 장소 해석 | 완료, SGC·CGNDB 확인됨(사전), Nominatim 부분 확인(검색), 결정 후보 D1~D5 (T9) | dd16199 (병합) |
| 2026-10-29 | data-researcher | Canada T7 문화 | 완료, 같은 생일·스탠리컵·아기 이름(BC·ON·QC) 확인됨, 노래 부분 확인(Y1 대기), 발명·기기 자동 출처 없음, 결정 후보 D1~D10 (T9) | 11372eb (병합) |
| 2026-10-29 | data-researcher | Canada T2 동네 그때/지금 이미지 | 완료, NAPL·토론토 타일·몽레알 기록보관소 확인됨, 캘거리·몽레알 사진 부분 확인, 지금 위성(Esri·Mapbox) 미확인, 결정 후보 D1~D6 (C6 계획 전) | 79d341d (병합 90e66d5) |
| 2026-10-29 | data-researcher | Canada T8 개인정보 근거 | 완료, PIPEDA 본문·Wikimedia 정책 확인됨, OPC·퀘벡 Law 25·BC/AB PIPA 부분 확인, 방침 바꿀 곳 12개, 결정 후보 D1~D6 (T9). 커밋 트레일러 단락 분리 흠(수정 안 함) | 6963246 (병합) |
| 2026-10-29 | planner | S3 영어 위키백과 사건 계획 (c2-english-events, S3a·S3b·S3c 3 PR 제안) | 완료, PM 결정 필요 D1~D9 + ★ S3b T2 | - |
| 2026-10-29 | planner | S1 영어 화면·S2 M2b 고쳐 쓰기 계획 (c1-english-ui, c1-input-and-opening-ca, PR 2개) | 완료, PM 결정 필요 S1-P1~P4, S2-P1~P2 | - |
| 2026-10-29 | data-researcher | Canada T6 지역 지표 (날씨·집값·인구) | 완료, ECCC 일별 기후·StatCan 인구 추계·NHPI 확인됨, 도시 인구 2001~·도시권 1976~, 집값은 1981~ 지수 비율만, CREA 부적합, 결정 후보 D1~D7 (T9) | 9e1cbcb (병합) |
| 2026-10-29 | data-researcher | Canada T4 기록 사진·영상과 공식 채널 | 완료, 카드 16개 (Commons 기록보관소 사본·Radio-Canada Archives·Olympic 채널 부분 확인, 날짜·장소 검색 API 가진 기록보관소 없음), 결정 후보 D1~D7 (T9), PM 명령 C1~C4 | 90ee455 (병합) |
| 2026-10-29 | data-researcher | Canada T9 요약과 판정 | 완료, 11개 항목 판정(지금 구현 1·2·3·6·11, PM 확인 뒤 4·5·7·9·10, 빼기 8), 결정 47개(막는 것 A1·A2·T1 D1~D5·T8 D4), PM 명령 50개 | 04e3492 |
| 2026-10-29 | PM | S1·S2·S3 계획 승인 ("모두 추천대로"): S1-P1~P4·S2-P1~P2·S3 D1~D9 추천안, ★ S3b T2 (a) 기록 후 S3b 전에 재확인 | 결정 | - |
| 2026-10-29 | code-reviewer | Canada 리서치 브랜치 리뷰 | 수정 요청 (필수 1: summary 2.1 GeoNames CC BY 4.0 누락, 권장 4), 핵심 근거 직접 확인 모두 일치 | - |
| 2026-10-29 | data-researcher | Canada T9 리뷰 수정 | 완료, GeoNames CC BY 4.0 행 분리 + 권장 3건 | 0df7f69 |
| 2026-10-29 | code-reviewer | Canada 리서치 브랜치 재리뷰 | 승인 | - |
