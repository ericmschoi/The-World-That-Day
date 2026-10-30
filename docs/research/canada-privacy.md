# 캐나다 개인정보 근거 (PIPEDA 수준) — 출처 검증

> 작업: Canada T8 · 계획서: docs/plans/canada-research.md (브랜치 `docs/canada-research`, PM 승인 2026-10-29) · 조사 일자: 2026-10-29 · 작성: data-researcher
> **이 문서의 해석은 법률 자문이 아니다.** 법령과 규제기관 안내를 읽고 이 서비스의 설계에 대 본 것이며, 법적 판단이 아니다. 확신도 기준은 `docs/plans/m0-source-research.md` 의 "확신도 기준"을 따른다.
> 이 문서는 데이터 출처가 아니라 **근거 문서**를 다룬다. 그래서 템플릿 4~6절은 `해당 없음` 으로 쓰고, 3절은 "근거 문서 카드"(문서, 기관, 확인 일자, 요지)다. 확인 일자는 따로 적지 않은 한 모두 2026-10-29다. 셸 요청은 하지 않았고 WebFetch·WebSearch만 썼다(User-Agent는 도구 기본값). 문서에 이메일 주소와 개인 정보는 없다.

## 1. 요약

- **이 서비스는 지금 PIPEDA의 적용을 받을 가능성이 낮다. 다만 "법적으로 안전하다"는 결론은 아니다.** PIPEDA는 "상업 활동(commercial activity) 중에" 모으는 개인정보에만 적용된다(4조 1항 [1]). 이 서비스는 비상업이고 공개 배포 없는 로컬 데모(결정 #5)다. 그러나 OPC는 무료 서비스도 사업 모델 전체를 보고 상업 활동일 수 있다고 한다 [5]. 공개 배포, 광고, 유료 기능, 수익이 생기면 다시 봐야 한다. 이 판단은 확인된 조문에 사실을 대 본 것이라 "부분 확인"이다.
- **주 법이 가장 큰 불확실성이다.** BC PIPA는 비영리를 포함한 모든 민간 "organization"에 적용되고 개인의 사적 목적만 뺀다 [25][26]. 앨버타 PIPA는 일부 비영리를 상업 활동에만 적용한다 [27]. **퀘벡 Law 25는 "상업적이든 아니든 조직된 경제 활동"이면 적용된다** [17]. 취미 사이트가 이에 드는지는 CAI도 "경우별 분석"이라고 한다. 법 조문 원문은 사이트가 403으로 막아 못 읽었다(6절 B1).
- **방침은 번역만으로는 부족하다.** 현재 `web/privacy.html` 은 한국어이고 담당자의 직함·연락처, 목적의 입력 시점 고지, 국외 이전 안내, 접근·이의 제기 문장, 나이 문장이 없다. 고칠 목록은 10.1절이다. 로컬 데모 단계에서는 OPC가 말하는 "모범 사례로서 공정 정보 원칙 따르기" [6] 수준이면 충분하다고 본다(추천안, 7절).
- **외부 임베드(YouTube, Wikimedia)에 대한 캐나다 규제기관의 직접 안내는 찾지 못했다(미확인).** OPC의 국경 간 안내(2009)는 "처리 위탁"을 다루고, 브라우저가 제3자 서버에서 직접 받는 경우는 다루지 않는다 [11]. 그래서 안내는 "어느 당사자가 무엇을 받는지"를 쉬운 말로 알리는 쪽으로 쓰자(동의 안내 [10]의 핵심 요소 근거).
- **최근 변화(제품 정의서에 없음):** 연방 개정안 Bill C-36이 2026-06-15에 1차 독회를 마쳤고 [29], OPC가 2026-10-20에 어린이 개인정보 규약을 냈다 [12][13]. 둘 다 아직 이 서비스에 직접 적용되지 않는다. 규약은 PIPEDA 적용 조직의 상업 활동을 대상으로 한다.

## 2. 확인할 질문과 답

| # | 질문 (계획서 T8 + 지시문) | 출처 | 답 | 확신도 | 근거 |
|---|---|---|---|---|---|
| Q1 | 비상업 개인 프로젝트이자 로컬 데모에 PIPEDA가 적용되나 | PIPEDA, OPC | 적용 범위는 "상업 활동 중에 모으고 쓰고 공개하는 개인정보"다(4조 1항). 개인이 "개인적 또는 가정 목적만"으로 다루면 제외된다(4조 2항). 사이트로 공개해 다른 사람이 쓰게 하면 "개인적 목적만"에 들기 어렵다는 것은 우리의 읽기이고 확인한 안내는 없다. 비영리·무료여도 자동 면제가 아니고 사업 모델 전체를 본다. **이 프로젝트: 비상업, 광고·수익 없음, 로컬 데모라 지금은 상업 활동의 근거가 보이지 않는다. 공개 배포 때 다시 본다** | 조문·OPC 해석은 확인됨 / 이 프로젝트에 대 본 결론은 부분 확인 | [1] 2조·4조, [4], [5], [6] |
| Q1-a | 캐나다 밖 운영자에게도 적용되나 | OPC 판정 | OPC는 "캐나다와 실질적 연관(real and substantial connection)"이 있으면 외국 운영자에도 PIPEDA를 적용했다. 근거는 캐나다 판례를 겨냥한 내용, 캐나다인의 결제·삭제 요청 등이다. 단 그 사건도 "상업 활동"(광고 수익, 유료 삭제)이 있어서 적용됐다. 이 서비스는 캐나다 날짜·지역을 겨냥하므로 "연관" 요소는 갖출 수 있지만, 상업 활동이 없으면 적용되지 않는다 | 확인됨 (판정 1건) / 이 서비스에 적용은 부분 확인 | [8] |
| Q1-b | 날짜·장소가 "개인정보"인가 | OPC 해석 | 정의는 "식별 가능한 개인에 관한 정보"다. 식별 가능성은 "심각한 가능성(serious possibility)"으로 보며 다른 정보와 합쳐서도 본다. OPC 해석서는 IP 주소가 개인과 연결될 수 있으면 개인정보라 하고, **생년월일은 다루지 않는다.** 이 서비스는 이름·계정이 없고 날짜만 서버로 가므로 날짜 자체는 약하지만, 같은 요청에 오는 IP 주소와 합쳐지면 달라질 수 있다 | 정의·IP 해석은 확인됨 / 생년월일에 대한 OPC 입장은 미확인 | [1] 2조, [7] |
| Q1-c | 민감 정보인가 | OPC 동의 안내, 조문 | 민감성은 맥락에 따른다(Sch.1 4.3.4). 건강·금융은 통상 민감하다고 본다. 생년월일을 민감 정보로 분류한 문서는 찾지 못했다 | 부분 확인 (분류 문서 미확인) | [2] 4.3.4, [10] |
| Q2 | 공정 정보 원칙 10가지를 지금 설계에 대 보면 | OPC 원칙, Sch.1 | 아래 "원칙별 점검표" | 원칙은 확인됨 / 점검은 우리 읽기 | [3], [2] |
| Q3 | 주 법은 어디까지 적용되나 | CAI, BC·AB 기관, 조문 | 아래 "주 법 적용 범위 표". 요지: 퀘벡은 "경제 활동"이면 비상업도 적용될 수 있어 취미 사이트에 대한 결론은 불확실하고, BC는 비영리도 적용되며, 앨버타는 일부 비영리를 상업 활동에만 적용한다. 온타리오 등은 건강정보법만 있다 | 부분 확인 (퀘벡 조문 원문 미확인, 운영자 소재지 미확인) | [17]~[27], [4] |
| Q4 | 영어 방침에 더하거나 바꿀 문장 | 위 근거 전체 | 10.1절 목록 | 근거는 확인됨 / 문장은 초안 | 10.1절 |
| Q5 | 외부 임베드로 정보가 국외로 가는 부분을 어떻게 알리나 | OPC 국경 간 안내, 동의 안내, Wikimedia 정책 | OPC는 처리 위탁에서 "정보가 외국에서 처리될 수 있고 그 나라 당국이 접근할 수 있다"를 쉬운 말로 알리라고 한다 [11]. 임베드는 위탁이 아니라 브라우저가 제3자에게 직접 IP 등을 보내는 경우라, 같은 안내가 그대로 적용된다는 근거는 없다(미확인). 동의 안내는 "어떤 당사자가 받는지"를 핵심 요소로 앞에 두라고 한다 [10]. Wikimedia 서버는 미국에 있고 IP·브라우저·참조 사이트를 받는다 [30]. Google의 처리 위치는 이 조사에서 확인하지 않았다 | 안내 내용은 확인됨 / 임베드에 적용은 미확인 | [10], [11], [30] |
| Q5-a | 임베드에 대한 동의를 받아야 하나 | OPC | 임베드에 대한 직접 안내는 찾지 못했다(검색: OPC 사이트의 OBA·쿠키·동의·아동 규약). 어린이 규약 페이지도 임베드를 따로 다루지 않는다고 확인했다. 현재 설계(필수 체크 상자 + YouTube는 동의 뒤에만 그림)는 YouTube API 정책 III.A.2 때문에 이미 있다(`video-sources.md`). 캐나다 법상 그 자체로 필요한지는 미확인 | 미확인 | [12], [16], `video-sources.md` V2-10 |
| Q6 | 나이 제한 안내는 어떻게 하나 | OPC, CAI | OPC는 13세 미만의 의미 있는 동의는 보호자가 해야 한다고 본다 [10][12]. 퀘벡은 14세 미만은 보호자 동의, 14세 이상은 본인 가능이다 [21]. 연방 어린이 규약은 18세 미만이 쓸 수 있는 온라인 서비스에서 "상업 활동 중" 개인정보를 다루는 PIPEDA 적용 조직을 대상으로 한다 [12]. OPC 연령 확인 안내(2026-05)의 상세는 읽지 못했다 [14]. 나이 확인(연령 게이트)을 두라는 법적 요구는 확인하지 못했다 | 숫자는 확인됨 / 이 서비스에 대한 의무는 부분 확인 | [10], [12], [14], [21] |
| Q7 | 개인정보 침해(유출) 신고 의무가 있나 | PIPEDA, OPC | PIPEDA 적용 조직이 "중대한 피해의 실제 위험"이 있는 침해를 신고하고 기록을 2년 보관한다. 이 서비스는 개인정보를 저장하지 않으므로 지금 실질 영향이 적다. 공개 배포 후 접속 로그(IP)를 두면 달라진다 | 확인됨 (규칙) / 적용은 부분 확인 | [2] 10.1조, [15] |
| Q8 | 방침은 프랑스어도 필요한가 | OPC 어린이 규약, CAI | 어린이 규약은 개인정보 안내를 두 공용어로 두라고 한다 [12]. PIPEDA 본문과 CAI 페이지에서 방침의 언어 의무는 찾지 못했다. 퀘벡의 프랑스어 헌장(Charter of the French Language)은 이 조사 범위 밖이라 읽지 않았다 | 규약 문구는 확인됨 / 나머지 미확인 | [12] |
| Q9 | 쿠키·추적 안내는 | OPC | OBA 입장은 사이트 간 추적으로 광고를 보여 주는 경우의 동의를 다룬다(opt-out 허용 조건 등). 이 서비스는 광고·추적이 없다. YouTube 플레이어가 쿠키·저장소를 쓸 수 있다는 현재 방침 문장은 그대로 쓸 수 있고, 퀘벡 방침 요건에도 "쿠키 사용"이 들어 있다 [19] | 확인됨 | [16], [19] |

### 원칙별 점검표 (Q2)

설계 사실의 출처는 `web/privacy.html`, `web/src/consent/ConsentNotice.tsx`, `docs/product-brief.md` 110행(회원가입 없음, 생년월일과 장소를 함께 로그에 남기지 않음), 그리고 이 작업 지시문(날짜만 서버로, 장소·시각은 화면에만, 장소 해석 제외)이다.

| # | 원칙 [3] | 현재 설계 | 평가 (우리 읽기) | 보강 |
|---|---|---|---|---|
| 1 | 책임 | 운영자 "The World That Day 운영자"만 있고 직함·주소 없음 | 보강 필요. 조문(Sch.1 4.8.1)은 책임자의 이름 또는 직함과 주소를 공개하라고 한다 [2] | 책임자 직함과 연락 방법을 방침에 쓴다 |
| 2 | 목적 식별 | 방침에 "그날의 자료를 찾는 데만" | 대체로 충족. Sch.1 4.2.3은 수집 "때나 그 전"에 알리라고 한다 [2]. 방침 링크가 체크 상자 옆에 있으므로 충족에 가깝다 | 입력 화면에 한두 줄 요약을 둔다(원칙 3과 함께) |
| 3 | 동의 | 필수 체크 상자 + 방침 링크 + YouTube 약관 문장 | 비민감 정보에는 암묵 동의로 족하다는 것이 OPC·조문의 일반 입장 [2] 4.3.6. 그러나 동의 안내는 방침에 묻지 말고 핵심 4요소(무엇, 누구에게, 왜, 위험)를 앞에 두라고 한다 [10] | 체크 상자 위에 4요소 요약 |
| 4 | 수집 제한 | 날짜(서버), 장소(해석 때만), 시각은 브라우저 안에서만 | 충족. 가장 강한 부분이다 | 현재 문장 유지 + 시각이 서버로 가지 않음을 명시 |
| 5 | 사용·공개·보관 제한 | 요청한 사람 정보를 저장하지 않음, 결과는 날짜·사건 기준 캐시, 로그에 생년월일+장소 안 남김 | 충족. 단 서버 접속 로그의 보관 기간은 배포 환경이 정해져야 쓸 수 있다(미확인, 배포 미정) | 공개 배포 때 로그 보관 기간을 방침에 쓴다 |
| 6 | 정확성 | 사용자가 입력한 값을 쓰고 저장하지 않음 | 해당 거의 없음 | 없음 |
| 7 | 안전 보호 | 개인정보를 저장하지 않음. 로컬 데모 | 저장하는 정보가 없어 위험이 낮다. 공개 배포 때 HTTPS가 필요하다 | 배포 때 확인 |
| 8 | 공개성 | `/privacy.html` 을 바닥글·입력 화면에서 늘 열 수 있음 | 충족. 책임자 연락처만 빠짐 | 원칙 1과 같음 |
| 9 | 개인 접근 | 문장 없음 | 보강 필요(문장). 이름이나 계정으로 찾을 수 있는 정보가 없다는 사실을 쓰면 된다 | 10.1절 S-6 |
| 10 | 이의 제기 | 문의처가 GitHub Issues | 경로는 있으나 공개 게시판이다. 이의 제기를 "책임자에게"로 연결하는 문장과 "공개 이슈에 개인정보를 쓰지 말 것" 문구가 필요하다 | 10.1절 S-6 |

### 주 법 적용 범위 표 (Q3)

| 법 | 이 서비스 같은 비상업 소규모 서비스에 | 확신도 | 근거 |
|---|---|---|---|
| 연방 PIPEDA | 상업 활동 중에만. 알버타·BC·퀘벡 안에서 일어나는 활동은 주 법이 "실질적으로 비슷"해 주로 주 법이 맡고, 주 경계·국경을 넘는 상업 거래는 PIPEDA가 맡는다 | 확인됨 | [1] 4조, [4], [9] |
| 퀘벡 개인정보 보호법(민간 부문, Law 25) | **"기업(enterprise)" = 상업적이든 아니든 조직된 경제 활동**(민법 1525조 기준). 비영리·협회도 "경우별 분석"으로 적용될 수 있다. 퀘벡 밖 조직도 퀘벡에서의 활동 중 개인정보를 다루면 적용된다. 규모 기준(소규모 면제)은 CAI 페이지에 없다. 취미 사이트가 "경제 활동"에 드는지는 CAI도 정하지 않는다 → **미확정** | 부분 확인 (조문 원문 못 읽음) | [17], [22] |
| BC PIPA | **모든 민간 organization**. 비영리 포함. "organization"에서 **개인의 사적·가정 목적, 직원 신분, 공공기관**은 뺀다(1조, 3조 2항 (a)). PIPEDA가 적용되는 정보는 PIPA에서 뺀다(3조 2항 (c)). BC 소재 조직이거나 BC 거주자의 정보를 다루는 조직에 적용 | 확인됨 (조문, 현행 2026-10-27) | [25], [26] |
| 앨버타 PIPA | 민간 조직과 "상업적 신분의 개인". 일부 비영리(56조 유형)는 **상업 활동에만**, 그 밖의 비영리(비등록 협회 등)는 전부. 기부 수령은 상업 활동이 아니다 | 확인됨 (OIPC 안내) / 개인의 사적 목적 제외 문구는 페이지에서 확인 못 함 | [27], [28] |
| 온타리오·뉴브런즈윅·노바스코샤·뉴펀들랜드 | 개인 건강정보법만 있다고 OPC가 적는다. 이 서비스는 건강정보를 받지 않는다 | 확인됨 | [4] |

## 3. 출처 카드

카드의 "항목별 확신도"는 데이터 출처용 항목(접근·쿼터 등)이 아니라 이 영역에 맞춰 `적용 범위`, `요구 사항`, `최신성`으로 바꿨다. 종합 확신도는 가장 낮은 항목과 같다.

### 출처 — PIPEDA 본문 (Justice Laws)
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: 캐나다 법무부(Justice Laws) — https://laws-lois.justice.gc.ca/eng/acts/p-8.6/FullText.html [1][2]
- 요지: 2조 1항 정의("상업 활동", "개인정보"=식별 가능한 개인에 관한 정보). 4조 1항: 상업 활동 중 개인정보에 적용. 4조 2항: 개인·가정 목적, 언론·예술·문학 목적, 정부 기관은 제외. 5조 3항: "합리적인 사람이 적절하다고 볼" 목적에만 수집·사용·공개. 6조 1항: 개인이 성질·목적·결과를 이해할 것이라 기대할 수 있어야 유효한 동의. 10조 1항: 중대한 피해의 실제 위험이 있는 침해의 신고. 26조 2항 (b): 주 법이 실질적으로 비슷하면 면제 명령 가능. 별표 1(CSA 규약)의 원칙 4.1~4.10.
- 이 서비스에 맞는가: 적용 여부의 첫 기준이다. 상업 활동이 없으면 적용되지 않는다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 적용 범위 (4조) | 확인됨 | [1], 2026-10-29. 법은 "current to 2026-09-30", 마지막 개정 2025-03-04 |
  | 요구 사항 (별표 1, 6.1조, 10.1조) | 확인됨 | [2], 2026-10-29 |
  | 최신성 | 확인됨 | 같은 페이지의 "current to" 표시 |
- 확신도: 확인됨
- 샘플 응답 확인: 해당 없음 — 법 조문
- 근거 링크: [1][2]

### 출처 — OPC의 PIPEDA 안내 (원칙, 상업 활동, 개인정보, 비영리)
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: 캐나다 개인정보보호위원회(OPC) — [3] 공정 정보 원칙(수정 2025-05-29), [4] 요건 요약(2024-05-01), [5] 해석서 "상업 활동"(2022-03-24), [6] 비영리에의 적용(2019-06-25), [7] 해석서 "개인정보"(2013-10-11), [8] 판정 2015-002
- 요지: 위 2절 Q1, Q1-a, Q1-b. [5]는 무료 서비스도 사업 모델 전체로 보고, 광고를 싣거나 유료 삭제를 받는 사이트를 상업적이라고 봤다 [8]. [6]은 비영리는 보통 해당 없지만 상업 활동이 있으면 해당되고, 비영리라도 공정 정보 원칙을 모범 사례로 따르라고 권한다. OPC는 해석서가 법적 구속력이 없다고 밝힌다.
- 이 서비스에 맞는가: 적용 여부 판단의 근거. 일부 문서는 오래됐다(2013, 2019).
- 항목별 확신도:
  | 항목 | 확신도 | 근거 |
  |---|---|---|
  | 적용 범위 | 확인됨 | [4][5][6], 2026-10-29 |
  | 요구 사항 (10원칙) | 확인됨 | [3], 2026-10-29 |
  | 최신성 | 부분 확인 | [7]은 2013년 문서, [6]은 2019년 문서. 이후 개정(6.1조 등)이 반영됐는지는 문서에 없음 |
- 확신도: 부분 확인 (최신성)
- 근거 링크: [3]~[8]

### 출처 — OPC의 동의·국경 간 이전·어린이·침해 안내
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: OPC — [10] 의미 있는 동의 지침(2025-08-11), [11] 국경 간 처리 지침(2009-01-27), [12] 어린이 개인정보 규약(2026-10-20), [13] 보도자료(2026-10-20), [14] 연령 확인 안내 보도자료(2026-05-04), [15] 침해 신고 안내(2025-08-11), [16] 온라인 행동 광고 입장(2025-08-11)
- 요지:
  - [10] 7원칙. 핵심 4요소(무엇을, 누가 받는지, 왜, 위험)를 앞에 두라. 13세 미만은 보호자가 동의한다.
  - [11] 처리를 맡긴 조직도 책임이 있다. 국외에서 처리될 수 있고 그 나라 당국이 접근할 수 있음을 쉬운 말로 알려라. **처리 위탁 이전은 "사용"이며 추가 동의가 필요 없다.** 이 지침은 "위탁"을 다룬다.
  - [12] 18세 미만. 13세 미만은 보호자 동의, 비필수 사용은 옵트인, 제3자와 공유는 기본 꺼짐. **PIPEDA 적용 조직이 상업 활동 중 개인정보를 다룰 때** 적용된다. 임베드는 따로 다루지 않는다. 의견 접수는 2027-01-20까지이며 이후 바뀔 수 있다 [13]. 법적 구속력 여부는 보도자료에 없다.
  - [15] 중대한 피해의 실제 위험이 있는 침해를 신고하고, 모든 침해의 기록을 2년 보관한다.
  - [16] 사이트 간 추적 광고를 다룬다. 이 서비스는 해당 기능이 없다.
- 이 서비스에 맞는가: 조건부. 모두 PIPEDA 적용이 전제다. 로컬 데모에서는 모범 사례의 참고다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 |
  |---|---|---|
  | 적용 범위 | 확인됨 | [12] "PIPEDA 적용 조직... 상업 활동", 2026-10-29 |
  | 요구 사항 | 확인됨 | [10][11][12][15], 2026-10-29. [14]는 보도자료만 읽음(본 지침 미확인) |
  | 최신성 | 부분 확인 | [11]은 2009년 문서이고 2019년 협의 언급이 페이지에 없다. [12]는 의견 수렴 중 |
- 확신도: 부분 확인
- 근거 링크: [10]~[16]

### 출처 — 퀘벡: 민간 부문 개인정보 보호법 (Law 25) 와 CAI 안내
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: 퀘벡 정보접근위원회(CAI) — [17] 적용 범위, [18] 주요 변경, [19] 수집, [20] 국외 이전, [21] 동의, [22] 책임자, [23] 제재. 조문: https://www.legisquebec.gouv.qc.ca/en/document/cs/p-39.1 [24] (이 조사에서는 403이라 못 읽음)
- 요지:
  - 적용: "기업" = 민법 1525조의 조직된 경제 활동, 상업 여부 무관. 비영리는 경우별. 퀘벡 밖 조직도 퀘벡에서의 활동이면 적용 [17].
  - 수집 시 알림: 목적, 수단, 접근·정정·동의 철회 권리. 해당 시 제3자 범주, **퀘벡 밖으로 전달될 가능성**, 식별·위치·프로파일링 기술. 기술로 수집하면 **쉬운 말의 방침**을 사이트에 올리고(수집 정보, 목적, 받는 제3자, 보호 조치, 쿠키, 권리, 변경) 변경을 알린다 [19].
  - 책임자의 직함과 연락처를 사이트에 공개 [22]. 정책·관행을 쉬운 말로 공개 [22].
  - 동의: 14세 미만은 보호자 동의 [21].
  - 퀘벡 밖 전달: 전달 전 프라이버시 영향 평가와 서면 계약 [20].
  - 제재: 행정 과징금 최대 1천만 캐나다 달러 또는 전 세계 매출 2%, 형사 벌금 기업 최대 2천5백만 달러 또는 매출 4% 중 큰 쪽 [23] (CAI 페이지 요약이며 법적 효력이 없다고 CAI가 밝힌다).
- 이 서비스에 맞는가: 조건부. 퀘벡에서 취미 사이트가 "기업"인지가 열린 질문이고, 데모 입력 2에 몽레알이 있어 퀘벡 사용자를 배제하기도 어렵다(5절).
- 항목별 확신도:
  | 항목 | 확신도 | 근거 |
  |---|---|---|
  | 적용 범위 | 부분 확인 | [17] (경우별 분석이라고만 함). 조문 원문 [24] 403 |
  | 요구 사항 | 부분 확인 | [18]~[23] CAI 요약. 조문 번호(8조 1항·8조 2항·17조 등)는 검색 요약에서만 봤고 원문 미확인 |
  | 최신성 | 부분 확인 | CAI 페이지에 작성일 없음(저작권 2026만 표시) |
- 확신도: 부분 확인
- 근거 링크: [17]~[24]

### 출처 — BC PIPA와 앨버타 PIPA
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: BC 법령 — https://www.bclaws.gov.bc.ca/civix/document/id/complete/statreg/03063_01 [25]; BC OIPC [26]; 앨버타 OIPC [27]; 앨버타 정부 [28]
- 요지: Q3 표. BC는 "organization"에 비영리가 들고 개인의 사적·가정 목적은 뺀다. 통지(10조 1항)와 책임자 직함·연락처 공개(4조)가 있다. 앨버타는 일부 비영리를 상업 활동에만 적용한다.
- 이 서비스에 맞는가: 운영자가 BC나 앨버타에 있거나 그곳 거주자의 정보를 다루면 영향. 운영자 소재지를 이 조사는 모른다(PM 확인).
- 항목별 확신도:
  | 항목 | 확신도 | 근거 |
  |---|---|---|
  | 적용 범위 | 확인됨 (BC) / 확인됨 (AB, 비영리 부분) | [25] 현행 2026-10-27, [26], [27][28] |
  | 요구 사항 | 확인됨 (BC 통지·동의 조문) / 미확인 (AB 조문 원문) | [25]; AB 조문은 읽지 않음 |
  | 최신성 | 확인됨 (BC) / 부분 확인 (AB) | [25] 날짜. AB 페이지에 작성일 없음 |
- 확신도: 부분 확인
- 근거 링크: [25]~[28]

### 출처 — 연방 개정안 Bill C-36 (Protecting Privacy and Consumer Data Act)
- 구분: 추가 후보
- 제공 기관 / 공식 문서: 의회 LEGISinfo — https://www.parl.ca/legisinfo/en/bill/45-1/c-36 [29]
- 요지: 2026-06-15 1차 독회. PIPEDA 개정을 담은 법안이다. 페이지의 요약 칸이 비어 있어 내용은 확인하지 못했다. 2차 독회 활동은 "없음"으로 표시되어 있고, 같은 페이지에 2026-08-19 "헌장 성명 제출" 표시도 있어 상태 표시가 일관되지 않는다. 검색 요약(2차 출처, 근거로 쓰지 않음)은 어린이 정보 보호 강화와 새 규제기관을 말하지만 원문으로 확인하지 못했다.
- 이 서비스에 맞는가: 아직 법이 아니다. 방침 설계에 쓰지 않고 지켜본다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 |
  |---|---|---|
  | 적용 범위 | 미확인 | 법안 내용 미확인 |
  | 요구 사항 | 미확인 | [29] 요약 칸 비어 있음 |
  | 최신성 | 부분 확인 | [29] 2026-10-29, 상태 표시 불일치 |
- 확신도: 부분 확인 (존재와 1차 독회 일자만)
- 근거 링크: [29]

### 출처 — Wikimedia 개인정보 정책 (임베드 대상)
- 구분: 추가 후보
- 제공 기관 / 공식 문서: Wikimedia Foundation — https://foundation.wikimedia.org/wiki/Policy:Privacy_policy [30]
- 요지: 서버가 방문자의 IP 주소, 브라우저·기기 정보, 참조 사이트, 요청 페이지·시각을 받는다. 서버와 데이터센터는 미국에 있고 다른 나라로 옮겨질 수 있다. 개인정보는 보통 90일 뒤 삭제·집계·비식별화한다. 제3자 사이트가 Wikimedia 콘텐츠를 불러올 때 생기는 정보 수집도 정책이 다룬다고 한다. 직접 링크(hotlinking)는 정책이 따로 다루지 않는다. 현재판은 2025-10-06 시행, 페이지 마지막 편집 2026-02-21.
- 이 서비스에 맞는가: 방침 문구("Wikimedia 서버가 IP를 받는다")의 근거. YouTube/Google은 `video-sources.md` 의 [98][104]를 쓴다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 |
  |---|---|---|
  | 받는 정보 | 확인됨 | [30], 2026-10-29 |
  | 처리 위치 | 확인됨 (Wikimedia) / 미확인 (Google) | [30]; Google 위치는 읽지 않음 |
  | 최신성 | 확인됨 | [30] 시행일 |
- 확신도: 확인됨 (Wikimedia 범위에서)
- 근거 링크: [30]

### 찾아봤지만 읽지 못한 것
- 퀘벡 법 조문 원문(legisquebec): 403. 6절 B1.
- 반스팸법(CASL)의 컴퓨터 프로그램 설치 규정(YouTube 스크립트에 해당하는지): 조사하지 않음. 미확인. 이 서비스는 메시지를 보내지 않고 자체 프로그램을 사용자 기기에 설치하지 않는다.
- 퀘벡 프랑스어 헌장: 조사하지 않음.
- OPC의 연령 확인 안내 본문: 보도자료만 읽음.
- 한국 개인정보 보호법(운영자가 한국에 있다면): 이 조사 범위 밖. 현재 방침의 법적 근거 표시는 없다.

## 4. 미디어 레코드 필드 매핑

해당 없음 — 데이터 출처가 아님(개인정보 근거 문서).

## 5. 데모 입력 판단

결정 1 (b)(PM 승인 2026-10-29)의 데모 입력 4개. 이 영역은 미디어를 얻는 출처가 아니므로 "얻을 수 있는 것"은 개인정보 관점에서 해당 입력이 무엇을 뜻하는지를 쓴다.

| 데모 입력 | 출처 | 얻을 수 있는 것 | 얻을 수 없는 것 | 근거 | 확신도 |
|---|---|---|---|---|---|
| 1988-02-13 + Calgary | 해당 없음 — 데이터 출처가 아님 | 개인정보 관점: 공개된 역사 사건의 날짜이며 누구의 생일도 아니다(CLAUDE.md 7절 6항, M3-P13). 앨버타 장소라 앨버타 PIPA 적용 여부는 운영자·사용자 소재지에 달림 | 해당 없음 | 지시문, [27] | 부분 확인 |
| 1976-07-17 + Hochelaga-Maisonneuve, Montréal | 해당 없음 — 데이터 출처가 아님 | 위와 같음. **퀘벡 장소**라 퀘벡 사용자를 상정한 입력이다. 퀘벡법 적용 질문(Q3)이 가장 직접 걸리는 데모다 | 해당 없음 | [17] | 부분 확인 |
| 2010-02-12 + Vancouver General Hospital | 해당 없음 — 데이터 출처가 아님 | 위와 같음. BC 장소. 병원 이름이 들어가지만 건강정보를 받지 않는다(탄생 장소 이름만) | 해당 없음 | [25] | 부분 확인 |
| 1980-04-12 + St. John's | 해당 없음 — 데이터 출처가 아님 | 위와 같음. 뉴펀들랜드·래브라도에는 민간 부문 일반 법이 없고 건강정보법만 있다 | 해당 없음 | [4] | 확인됨 |

## 6. PM이 실행할 확인 명령

해당 없음 — 모든 확인을 공개 문서로 마쳤고 키가 필요한 출처가 없다.

PM 브라우저 확인(로그인 불필요, 에이전트는 403으로 못 읽음):

- **B1**: https://www.legisquebec.gouv.qc.ca/en/document/cs/p-39.1 를 브라우저로 열어 1조(적용 범위·"기업"), 3.1·3.2조(책임자·정책 공개), 4.1조(미성년), 8조·8.1조·8.2조(수집 시 알림·방침), 17조(퀘벡 밖 전달)의 문구가 3절 퀘벡 카드의 요지와 같은지 본다. 같으면 data-researcher가 새 커밋으로 퀘벡 카드의 "요구 사항"을 확인됨으로 올린다. 이 조문 번호들은 검색 요약에서만 본 것이다.

## 7. 이 영역 추천안

- **1순위: "PIPEDA 수준을 모범 사례로 따르는 방침"** (종합 확신도: 부분 확인). 법이 지금 적용된다는 근거는 없지만, 적용될 가능성(공개 배포, 퀘벡·BC)을 대비해 공정 정보 원칙 10가지에 맞춘 영어 방침을 쓴다. OPC도 비영리에 이를 모범 사례로 권한다 [6]. 현재 설계는 수집 제한과 보관 제한 면에서 이미 강하다.
- 대안:
  - (b) 방침을 번역만 하고(S1) 보강은 공개 배포 결정 때 한꺼번에 한다. 로컬 데모에는 충분하지만 S2 이후 퀘벡 데모(몽레알)를 보여 줄 때 근거가 약하다.
  - (c) 변호사 검토를 받은 뒤 쓴다. 공개 배포 때 권한다. 지금은 비용 대비 효과가 낮다.
- 이유:
  - 확정할 수 없는 부분(퀘벡 취미 사이트, 임베드 동의)을 법 해석으로 풀지 않고, 알릴 것을 쉬운 말로 알리는 것으로 위험을 줄인다.
  - 이미 있는 필수 체크 상자와 방침 페이지는 YouTube API 정책 때문에 유지해야 하므로 바꾸는 비용이 문구 수준이다.
  - 이 서비스가 저장하는 개인정보가 없다는 사실이 가장 큰 방어다. 이것을 잃는 변경(서버 저장, 계정, 로그 보관, 분석 도구)은 개인정보 검토를 다시 하게 한다.

## 8. 위험

1. **퀘벡 적용 여부가 열려 있다.** CAI는 비영리·소규모 면제를 말하지 않고 "경우별 분석"이라 한다 [17]. 벌칙이 크다 [23]. 공개 배포하면 몽레알 데모는 퀘벡 사용자를 상정한다.
2. **상업 활동의 경계.** 무료여도 사업 모델 전체를 본다 [5]. 광고, 제휴 링크, 유료 기능, 후원 안내를 더하면 달라질 수 있다(후원이 상업 활동인지는 확인하지 못했다. 앨버타 정부 페이지는 자선 기부를 상업 활동이 아니라고 한다 [28]).
3. **운영자 소재지·법인 여부를 이 조사는 모른다.** 한국에서 운영되면 캐나다 법의 영향이 줄지만 한국 법이 영향한다(범위 밖). BC·앨버타·퀘벡에서 운영되면 해당 주 법이 직접 적용될 수 있다.
4. **국외 이전 고지의 근거가 약하다.** 임베드에 대한 규제기관 안내를 찾지 못했다. 방침 문구는 OPC의 일반 원칙(투명성)에 기대는 것이지, 임베드 전용 안내가 아니다.
5. **어린이 규약과 개정안이 움직인다.** 규약은 의견 수렴 중(2027-01-20까지), C-36은 1차 독회 [13][29]. 공개 배포 때 다시 확인해야 한다.
6. **OPC 문서 일부가 오래됐다.** 해석서 [7](2013), [6](2019), 국경 간 지침 [11](2009)은 이후 개정을 반영했는지 문서에 없다.
7. **IP 주소.** 서버에 닿는 요청은 IP를 가진다. 지금은 저장하지 않지만 공개 배포의 호스팅·CDN·접속 로그는 IP를 남길 수 있다. IP는 개인과 연결되면 개인정보다 [7]. 이는 배포 결정 때 정해야 한다.
8. **GitHub Issues가 문의처.** 공개 게시판이라 사용자가 개인정보를 올릴 수 있다. 방침에 쓰지 말라고 안내해야 한다.

## 9. 제품 정의서 가정과 다른 점

제품 정의서 자체에는 캐나다 개인정보 가정이 없다(한국 기준 서술). 계획서의 가정과 다른 점은 둘이다.

- **계획서 S1 (`feat/english-ui`)**: "`privacy.html`(내용은 같고 시행일은 머지하는 날로)". 번역만 하면 책임자 직함·연락처, 국외 이전 안내, 접근·이의 문장, 나이 문장이 빠진다. 10.1절을 S1에 넣을지 S2로 미룰지는 PM 결정 D3이다. 다른 작업을 막지는 않는다.
- **현재 방침의 출처 목록**("한국어 위키백과, Wikidata, Commons, KTV 아카이브")은 S3·S4(영어 위키백과, 캐나다 채널)와 장소 해석 출처(T1 결과)가 정해질 때마다 고쳐야 한다. 방침은 사실대로 써야 한다는 M3b3b 원칙 그대로다.

## 10. PM 결정 필요

이 절의 결정은 어느 것도 다른 T 작업을 막지 않는다.

### 10.1 방침 변경 목록

제안 문장은 초안이며 법률 검토를 거친 것이 아니다. 담당 위치: `web/privacy.html`, `web/src/consent/ConsentNotice.tsx`, `web/src/privacy/`.

| # | 바꿀 곳 | 변경 | 근거 | 우선순위 |
|---|---|---|---|---|
| S-0 | 언어·표기 | 영어(en-CA)로 쓴다. `lang="en-CA"`, 제목 "Privacy policy". 결정 9와 같은 기준 | 계획서 결정 9 | S1 |
| S-1 | 받는 정보 | "You enter a date of birth, a place of birth and, optionally, a time of birth. Only the date is sent to our server. The time stays in your browser. The place is sent only to look it up on the map (provider named once T1 is decided)." 지금 한국어 방침의 "장소는 입력이 생긴 뒤부터"를 현재 사실로 고친다 | 원칙 2·4 [3] | S2 (장소 입력이 생길 때) |
| S-2 | 목적 | "We use these only to find photos, videos and events for that day and place. We do not use them for advertising or profiling." | 원칙 2 [3], Sch.1 4.2.3 [2] | S1 |
| S-3 | 보관 | "We have no accounts and keep no record that links a date or place to you. Results are cached by date and event, not by person." 서버 로그의 보관 기간은 배포 환경이 정해지면 쓴다 | 원칙 5 [3], Sch.1 4.5.3 [2] | S1 (로그 문장은 배포 때) |
| S-4 | 외부 출처에 보내는 것 | 지금 "외부 출처에는 누구의 생년월일인지 보내지 않습니다"는 사실이다. 영어로는 "Outside sources receive the date as a search term. They are not told whose date it is." 로 정확히 쓴다(날짜가 검색어로 가는 점을 숨기지 않는다) | 원칙 3 [10] 핵심 요소 "누가 받는지" | S1 |
| S-5 | 국외 처리 안내 | "Photos and videos load straight from the source into your browser. Wikimedia (servers in the United States) and Google/YouTube receive your IP address and device details under their own privacy policies. Your information may be processed in other countries, where authorities may be able to access it." Google의 처리 위치는 확인하지 못했으므로 "other countries"로만 쓴다 | [11] 투명성, [30] | S1 |
| S-6 | 접근·이의·문의 | "We hold nothing that can be looked up by name or account, so there is nothing to access or correct. Questions or complaints: open an issue at <GitHub Issues>. Do not post personal information in a public issue. Responsible person: the operator." 직함과 연락 방법(공개 배포 때는 우편·이메일 같은 비공개 경로)은 PM이 정한다 | 원칙 1·8·9·10, Sch.1 4.8.1 [2][3]; 퀘벡 [22] | S1 (문장) / 공개 배포 (연락처) |
| S-7 | 나이 | 선택지는 D4 | [10][12][21] | D4 |
| S-8 | 쿠키·기기 저장 | 현재 문장 유지("no cookies or storage, except the YouTube player and IFrame API script"). 퀘벡 방침 요건에도 쿠키가 들어 있다 | [19] | S1 |
| S-9 | 입력 화면 요약 | 체크 상자 위에 4요소 요약(무엇을, 누가 받는지, 왜, 위험)을 두 줄로. 방침 링크는 유지 | [10] | S2 (입력 폼 이동 때) |
| S-10 | YouTube 의무 문장 | 그대로 유지(영문화만). 이 문장들은 캐나다 법이 아니라 YouTube API 정책 때문이다 | `video-sources.md` V2-10 | S1 |
| S-11 | 출처 목록 | S3·S4·T1 결과가 들어올 때마다 갱신. 영어 위키백과, Wikidata, Commons, YouTube(허용 캐나다 채널), 장소 제공자 | 사실대로 쓰기 | 각 기능 PR |
| S-12 | 시행일·변경 | 변경 때 페이지를 고치고 날짜를 새로 쓴다(그대로). 퀘벡은 변경 알림을 요구한다 [19] | [19] | S1 |

### 10.2 결정 후보

| # | 질문 | 선택지 | 추천 | 막는 것 |
|---|---|---|---|---|
| D1 | 방침의 기준 | (a) PIPEDA 수준을 모범 사례로 따른다 (b) 법이 적용되지 않는다고 보고 최소 문구만 쓴다 | **(a)**. OPC가 비영리에 권하는 방식이고 설계가 이미 가깝다 [6] | 없음 |
| D2 | 운영자 소재지와 법인 여부 | PM이 알려 준다. 개인 운영 / 법인·단체 운영, 소재지(한국, 캐나다 어느 주) | 정보 제공. 주 법 적용과 담당자 문구가 여기에 달렸다 | 없음 |
| D3 | 방침 보강을 언제 넣나 | (a) S1(영어 전환)에 S-0·S-2~S-6·S-8·S-10·S-12를 함께 넣는다 (b) S1은 번역만, 보강은 S2에 넣는다 (c) 공개 배포 결정 때까지 미룬다 | **(a)**. 문장 추가는 작고 S1이 어차피 파일 전체를 영어로 바꾼다. 장소·입력 화면(S-1·S-9)은 S2에서 | 없음 |
| D4 | 나이 문장 | (a) 없음 (b) "This service is for a general audience. If you are under 13 (14 in Quebec), please ask a parent or guardian first." (c) 연령 확인 게이트 | **(b)**. (c)는 오히려 정보를 더 모으게 되어 추천하지 않는다. OPC도 위험에 비례하고 개인정보를 덜 쓰는 방식을 권한다 [12][14] | 없음 |
| D5 | 프랑스어 방침 | (a) 프랑스어 UI와 함께 나중에 (b) 지금 번역해 둔다 | **(a)**. 어린이 규약은 두 공용어를 요구하지만 PIPEDA 적용 조직 대상이고, 계획서 범위 밖(프랑스어 UI는 나중)이다 | 없음 |
| D6 | 공개 배포 때 다시 볼 목록 | 아래 트리거 목록을 계획서·로드맵에 남긴다 | **남긴다** | 없음 |

공개 배포 때 다시 볼 것(D6): 호스팅·CDN의 접속 로그와 IP 보관 / HTTPS / 광고·후원·유료 기능 등 수익 / 접속 분석 도구(분석 쿠키) / 서버 저장·계정 도입 / 퀘벡 적용 판단과 변호사 검토 / 책임자 연락처(공개 게시판이 아닌 경로) / 프랑스어 방침 / Bill C-36과 어린이 규약의 확정 내용 / 퀘벡 밖 전달 평가(임베드를 "전달"로 보는지) / 반스팸법과 프랑스어 헌장.

## 11. 근거 링크

확인 일자는 모두 2026-10-29. "최종 수정"은 페이지에 표시된 날짜다.

| # | URL | 문서 제목 | 확인 일자 |
|---|---|---|---|
| 1 | https://laws-lois.justice.gc.ca/eng/acts/p-8.6/page-1.html | Personal Information Protection and Electronic Documents Act, 1~7조 (current to 2026-09-30, 개정 2025-03-04) | 2026-10-29 |
| 2 | https://laws-lois.justice.gc.ca/eng/acts/p-8.6/FullText.html | PIPEDA 전문, 별표 1 (6.1조, 10.1조, 26조) | 2026-10-29 |
| 3 | https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/p_principle/ | PIPEDA fair information principles (2025-05-29) | 2026-10-29 |
| 4 | https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/pipeda_brief/ | PIPEDA requirements in brief (2024-05-01) | 2026-10-29 |
| 5 | https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/pipeda-compliance-help/pipeda-interpretation-bulletins/interpretations_03_ca/ | Interpretation Bulletin: Commercial Activity (2022-03-24) | 2026-10-29 |
| 6 | https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/r_o_p/02_05_d_19/ | How PIPEDA applies to charitable and non-profit organizations (2019-06-25) | 2026-10-29 |
| 7 | https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/pipeda-compliance-help/pipeda-interpretation-bulletins/interpretations_02/ | Interpretation Bulletin: Personal Information (2013-10-11) | 2026-10-29 |
| 8 | https://www.priv.gc.ca/en/opc-actions-and-decisions/investigations/investigations-into-businesses/2015/pipeda-2015-002/ | PIPEDA Report of Findings #2015-002 (외국 웹사이트, 실질적 연관, 광고 수익) | 2026-10-29 |
| 9 | https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/r_o_p/02_05_d_26/ | Q&A: PIPEDA, Alberta and BC PIPA (2004-11-05) | 2026-10-29 |
| 10 | https://www.priv.gc.ca/en/privacy-topics/collecting-personal-information/consent/gl_omc_201805/ | Guidelines for obtaining meaningful consent (2025-08-11) | 2026-10-29 |
| 11 | https://www.priv.gc.ca/en/privacy-topics/personal-information-transferred-across-borders/gl_dab_090127/ | Guidelines for processing personal data across borders (2009-01-27) | 2026-10-29 |
| 12 | https://www.priv.gc.ca/en/privacy-topics/privacy-for-businesses/protection-of-children-for-businesses/childrens-privacy-code/children-code-final-report-2026/ | Children's privacy code (2026-10-20) | 2026-10-29 |
| 13 | https://www.priv.gc.ca/en/opc-news/news-and-announcements/2026/nr-c_261020/ | 보도자료: Children's Privacy Code (2026-10-20) | 2026-10-29 |
| 14 | https://www.priv.gc.ca/en/opc-news/news-and-announcements/2026/nr-c_260504/ | 보도자료: age assurance guidance (2026-05-04), 본 지침은 읽지 않음 | 2026-10-29 |
| 15 | https://www.priv.gc.ca/en/privacy-topics/privacy-for-businesses/privacy-breaches-at-your-business/gd_pb_201810/ | What you need to know about mandatory reporting of breaches of security safeguards (2025-08-11) | 2026-10-29 |
| 16 | https://www.priv.gc.ca/en/privacy-topics/technology/online-privacy-tracking-cookies/tracking-and-ads/bg_ba_1206/ | Policy position on online behavioural advertising (2025-08-11) | 2026-10-29 |
| 17 | https://www.cai.gouv.qc.ca/protection-renseignements-personnels/information-entreprises-privees/champ-application-loi_entreprises | CAI — Champ d'application de la Loi (entreprises privées) | 2026-10-29 |
| 18 | https://www.cai.gouv.qc.ca/protection-renseignements-personnels/sujets-et-domaines-dinteret/principaux-changements-loi-25 | CAI — Principaux changements (Loi 25) | 2026-10-29 |
| 19 | https://www.cai.gouv.qc.ca/protection-renseignements-personnels/information-entreprises-privees/collecte-renseignements-personnels_entreprises | CAI — Collecte de renseignements personnels (entreprises) | 2026-10-29 |
| 20 | https://www.cai.gouv.qc.ca/protection-renseignements-personnels/information-entreprises-privees/utilisation-communication-renseignements-personnels | CAI — Utilisation et communication (hors Québec) | 2026-10-29 |
| 21 | https://www.cai.gouv.qc.ca/protection-renseignements-personnels/information-entreprises-privees/consentement-personnes-entreprises | CAI — Consentement (mineurs de moins de 14 ans) | 2026-10-29 |
| 22 | https://www.cai.gouv.qc.ca/protection-renseignements-personnels/information-entreprises-privees/responsable-protection-renseignements-personnels-entreprise | CAI — Responsable de la protection des renseignements personnels | 2026-10-29 |
| 23 | https://www.cai.gouv.qc.ca/protection-renseignements-personnels/information-entreprises-privees/sanctions-entreprises-poursuites | CAI — Sanctions et poursuites | 2026-10-29 |
| 24 | https://www.legisquebec.gouv.qc.ca/en/document/cs/p-39.1 | Act respecting the protection of personal information in the private sector (P-39.1) — 403, 원문 읽지 못함 | 2026-10-29 (실패) |
| 25 | https://www.bclaws.gov.bc.ca/civix/document/id/complete/statreg/03063_01 | BC Personal Information Protection Act (current to 2026-10-27) | 2026-10-29 |
| 26 | https://www.oipc.bc.ca/about/legislation/ | BC OIPC — Legislation | 2026-10-29 |
| 27 | https://oipc.ab.ca/legislation/pipa/ | Alberta OIPC — PIPA Overview | 2026-10-29 |
| 28 | https://www.alberta.ca/personal-information-for-non-profits-and-other-organizations | Alberta — Personal information for non-profits and other organizations | 2026-10-29 |
| 29 | https://www.parl.ca/legisinfo/en/bill/45-1/c-36 | LEGISinfo — Bill C-36, Protecting Privacy and Consumer Data Act (1차 독회 2026-06-15) | 2026-10-29 |
| 30 | https://foundation.wikimedia.org/wiki/Policy:Privacy_policy | Wikimedia Foundation Privacy Policy (시행 2025-10-06, 마지막 편집 2026-02-21) | 2026-10-29 |
| 31 | `docs/research/video-sources.md` 의 [98] YouTube API 개발자 정책, [103] IFrame API, [104] 개인정보 보호 강화 모드 | YouTube 의무와 쿠키 문장의 근거(이미 확인됨, 2026-10-11) | — |
