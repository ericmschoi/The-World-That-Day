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
