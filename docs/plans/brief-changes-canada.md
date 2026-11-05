# 제품 정의서·CLAUDE.md·에이전트 정의 캐나다 전환 문구 제안 (PM 전용 파일)

> 작성: planner (2026-11-05) · 브랜치: `docs/canada-roadmap` · 상태: **제안**. planner는 `docs/product-brief.md`, `CLAUDE.md`, `.claude/` 를 고치지 않았다(`CLAUDE.md` 5절). PM이 직접 붙여 넣거나, 오케스트레이터에게 명시적으로 맡긴다(`CLAUDE.md` 4절).
> 근거: `docs/plans/canada-research.md` 결정 1·2·3·5 (b)·6·9(2026-10-29), `docs/research/canada-summary.md` 5절(가정 변화)·6절(결정), PM 위임(2026-11-04: A1 (a), A2, T1 D1~D5 (a), T8 D4 (b), T6 D4 (a)), 머지된 S1 화면 글자(`c1-english-ui.md`), S2 계획 T9, S3 계획 D2·D9, 한국판 로드맵의 "제품 정의서·CLAUDE.md 문구 제안" 표(`main` f07e420의 `docs/roadmap.md` 253~271행).

## 쓰는 법

- **1차**: S2·S3a 머지 전. S2 계획 T9의 완료 기준("정의서 1·2·3·8·9·12절과 `CLAUDE.md` 0절·7절 1·5항이 결정 5 (b) 목록대로")을 이 1차가 채운다.
- **2차**: C3(그때 vs 지금 공용) 계획 전이 좋고, 늦어도 배포 직전.
- 블록마다 **지금**은 지금 파일의 글(그대로 옮김), **바꾼 뒤**는 붙여 넣을 글이다. "절 전체"라고 적은 곳은 그 절을 통째로 바꾼다.
- 정의서는 한국어로 둔다(PM 문서). 화면에 나오는 글자만 영어(en-CA)로 적는다. `CLAUDE.md` 와 `.claude/` 는 영어다.
- 결정 5 (b) 목록과 다른 점 두 가지:
  1. 정의서 4절(장소 유형)과 5절 오프닝·6.1절과 6.2절의 **화면 글자**를 1차로 당겼다. T1 결정이 2026-11-04에 정해졌고, S1이 화면 글자를 이미 바꿨고, S2가 오프닝과 장소 안내 예를 쓰기 때문이다.
  2. `CLAUDE.md` 7절 1항의 자유 라이선스 예(OGL-Canada)는 2차로 미뤘다. 정의서 6.2절 라이선스 예와 한 번에 고치는 편이 맞고, T4는 OGL-Canada가 이미지에 적용되는지 문서에 없다고 적었다. 통신사 예(The Canadian Press)는 1차에 그대로 둔다.

---

## 1차

### 1-1. 정의서 1절 한 줄 정의

지금:
```
생년월일과 태어난 장소를 입력하면, 그날 그곳의 대한민국 풍경과 그날 세계에서 일어난 일을 사진과 짧은 영상으로 보여주는 서비스.
```
바꾼 뒤:
```
생년월일과 태어난 장소를 입력하면, 그날 그곳의 캐나다 풍경과 그때와 지금의 생활 비교, 그리고 그날 세계에서 일어난 일을 사진과 짧은 영상, 숫자로 보여주는 서비스.
```
근거: 방향 전환(2026-10-29, "그때 vs 지금" 11개 항목 범위). "그때 vs 지금" 섹션의 내용은 2차 2-1에서 5절에 넣는다.

### 1-2. 정의서 2절 사용자

지금:
```
- 주 사용자: 대한민국에서 태어나 자란 사람. 대부분 모바일로 들어온다.
```
바꾼 뒤:
```
- 주 사용자: 캐나다에서 태어나 자란 사람. 화면은 캐나다 영어(en-CA)다. 대부분 모바일로 들어온다.
```
목표 감정과 성공 장면은 그대로 둔다.

### 1-3. 정의서 3절 입력 (절 전체의 표)

지금:
```
| 항목 | 필수 | 예시 | 규칙 |
|---|---|---|---|
| 생년월일 | 필수 | 1988-09-17 | 미래 날짜, 존재하지 않는 날짜(2월 30일 등)는 거부. 지원 시작 연도는 M0 리서치 결과를 보고 PM이 정한다 (초안: 1950년 이후) |
| 태어난 장소 | 필수 | 서울 / 망원동 / 서울대학교병원 | 자유 입력 |
| 출생 시각 | 선택 | 15:20 | MVP에서는 오프닝 문구에만 쓰고 기능에는 쓰지 않는다 |
```
바꾼 뒤:
```
| 항목 | 필수 | 예시 | 규칙 |
|---|---|---|---|
| 생년월일 | 필수 | 1988-02-13 | 미래 날짜, 존재하지 않는 날짜(2월 30일 등), 1950-01-01 이전 날짜는 거부한다(지원 시작 1950-01-01, 2026-11-04 확정). "오늘"은 캐나다에서 가장 앞선 시간대(`America/St_Johns`)의 오늘이다(2026-10-29). 요일은 날짜만으로 계산한다 |
| 태어난 장소 | 필수 | Calgary / Hochelaga-Maisonneuve, Montréal / Vancouver General Hospital | 자유 입력, 1~50자. 악센트와 아포스트로피는 그대로 받는다 |
| 출생 시각 | 선택 | 15:20 | MVP에서는 오프닝 문구에만 쓰고("3:20 p.m.") 서버로 보내지 않는다 |
```

### 1-4. 정의서 4절 장소 해석 (절 전체)

지금:
```
입력한 장소를 세 유형으로 나눈다. PM 요구사항의 "넓은 지역"과 "구체적 장소" 사이에 "동네"를 둔다. 사용자가 가장 감동하는 지점이 자기 동네이기 때문이다.

| 유형 | 예 | 보여줄 것 |
|---|---|---|
| 광역 `REGION` | 서울, 부산, 경기도, 강남구, 수원시 | 그 지역 사람이면 누구나 아는 대표 명소 5~8곳 |
| 동네 `NEIGHBORHOOD` | 망원동, 해운대구 우동, 광주 서구 풍암동 | 동네 중심의 그때/지금 항공·위성 사진, 동네 곳곳, 그 도시의 대표 명소 일부 |
| 장소 `SPOT` | 서울대학교병원, 병원 이름, 주소 | 그 지점의 그때/지금 항공·위성 사진, 로드뷰, 반경 안의 동네 곳곳, 그 도시의 대표 명소 일부 |

규칙:
- 후보가 여러 개면(예: "중앙병원") 후보 목록을 보여주고 사용자가 고른다. 시스템이 추측해서 하나를 고르지 않는다.
- 문을 닫았거나 이름이 바뀐 병원처럼 찾지 못하는 장소는 동 단위로 다시 입력하도록 안내한다. 지도에서 직접 위치를 고르는 기능을 넣을지는 M2 계획 때 PM이 정한다.
- 대한민국 밖 장소는 MVP에서 지원하지 않고 안내 문구를 보여준다.
```
바꾼 뒤:
```
입력한 장소를 세 유형으로 나눈다. PM 요구사항의 "넓은 지역"과 "구체적 장소" 사이에 "동네"를 둔다. 사용자가 가장 감동하는 지점이 자기 동네이기 때문이다.

| 유형 | 예 | 보여줄 것 |
|---|---|---|
| 광역 `REGION` | Calgary, St. John's, Montréal, 주·준주 이름 | 그 지역 사람이면 누구나 아는 대표 명소 5~8곳 |
| 동네 `NEIGHBORHOOD` | Hochelaga-Maisonneuve, Montréal | 동네 중심의 그때/지금 항공·위성 사진, 동네 곳곳, 그 도시의 대표 명소 일부 |
| 장소 `SPOT` | Vancouver General Hospital, 병원 이름, 주소 | 그 지점의 그때/지금 항공·위성 사진, 거리 사진(출처가 확인된 곳만), 반경 안의 동네 곳곳, 그 도시의 대표 명소 일부 |

규칙:
- 후보가 여러 개면(예: "London", "Victoria", "Saint John") 후보 목록을 보여주고 사용자가 고른다. 시스템이 추측해서 하나를 고르지 않는다.
- 같은 이름의 외국 도시가 있어도 캐나다 안에 같은 이름이 있으면 캐나다 후보만 보여 주고 주 이름을 붙인다(예: "London, Ontario") (2026-11-04).
- 영어·프랑스어 이름과 악센트 유무("Montreal"/"Montréal", "Quebec"/"Québec"), "St."와 "Saint"는 같은 이름으로 찾는다. 세부 규칙은 장소 해석(C2) 계획에서 정한다.
- 동네 이름은 Statistics Canada 지리 분류(SGC), 캐나다 지명 데이터베이스(CGNDB), OpenStreetMap의 이름 하나로 해석한다. 동네 경계는 그 경계가 필요한 기능이 정한다(2026-11-04).
- 병원 같은 지점은 최선의 검색으로 찾는다. 문을 닫았거나 이름이 바뀌어 찾지 못하는 장소는 동네나 도시 이름으로 다시 입력하도록 안내한다(2026-11-04). 지도에서 직접 위치를 고르는 기능을 넣을지는 C2 계획 때 PM이 정한다.
- 캐나다 밖 장소는 MVP에서 지원하지 않고 안내 문구를 보여준다.
```
근거: T1 D3·D4·D5 (a), T9 7절 C2(접기 규칙). "로드뷰"는 카카오 용어라 "거리 사진"으로 바꿨다(Google Street View 사용은 X4 결정 전).

### 1-5. 정의서 5절 오프닝과 그날의 세계 (두 줄만, 섹션 추가는 2차 2-1)

지금:
```
1. **오프닝**: "1988년 9월 17일 토요일, 서울". 요일을 계산해서 보여준다.
```
바꾼 뒤:
```
1. **오프닝**: "Saturday, February 13, 1988, Calgary". 요일을 계산해서 보여준다. 출생 시각을 넣으면 "Saturday, July 17, 1976 at 3:20 p.m., Hochelaga-Maisonneuve, Montréal" 처럼 쓴다.
```
지금:
```
4. **그날의 세계**: 정확히 같은 날짜(연·월·일)에 세계와 국내에서 일어난 사건을 사진이나 짧은 영상과 함께.
```
바꾼 뒤:
```
4. **그날의 세계**: 정확히 같은 날짜(연·월·일)에 세계와 캐나다에서 일어난 사건을 사진이나 짧은 영상과 함께. 사건 문장은 영어 위키백과(날짜 문서, 연도 문서, "YYYY in Canada")의 원문 그대로다.
```
근거: S2 계획 오프닝 표(S2-P2 (a)), S3 계획 D1 (b)·D5 (a).

### 1-6. 정의서 6.1절 정직한 날짜 표기 (화면 글자와 기간 사건)

지금:
```
- 모든 사진과 영상에 실제 촬영 시점과 정밀도를 표시한다: `같은 날` / `같은 달` / `같은 해` / `±N년` / `현재 모습` / `촬영 시점 미상`(촬영 시점을 알 수 없을 때, 배지 없이 표시).
- 그날 찍지 않은 사진을 그날 사진처럼 보이게 하는 문구나 편집을 하지 않는다. 예: "1987년 촬영", "지금의 모습".
```
바꾼 뒤:
```
- 모든 사진과 영상에 실제 촬영 시점과 정밀도를 표시한다. 화면 글자(en-CA): `Same day` / `Same month` / `Same year` / `N years earlier`·`N years later` / `Present day` / `Capture time unknown`(촬영 시점을 알 수 없을 때, 배지 없이 표시).
- 그날 찍지 않은 사진을 그날 사진처럼 보이게 하는 문구나 편집을 하지 않는다. 예: "Taken 1987", "Present day".
- 기간 사건(예: 2월 13~28일 올림픽)은 시작하는 날에만 "Started that day — ran until February 28"로 보이고, 기간 중간 날짜에는 넣지 않는다. 달만 있거나 날짜가 없는 줄은 쓰지 않는다.
```
근거: S1 문구표(머지됨), S3 계획 D2 (a). 고르는 순서와 ±5년 줄은 그대로 둔다(±5년은 C6 계획 때 T2 D3).

### 1-7. 정의서 6.2절 화면 글자와 통신사 예

지금:
```
- 라이선스가 확인되지 않은 자료도 화면에 직접 보여 줄 수 있다. 이때 `license` 는 비워 두고 화면에는 "라이선스 확인 안 됨"으로 표시한다.
```
바꾼 뒤:
```
- 라이선스가 확인되지 않은 자료도 화면에 직접 보여 줄 수 있다. 이때 `license` 는 비워 두고 화면에는 "Licence not verified"로 표시한다.
```
지금:
```
- 통신사·언론사 사진(연합뉴스, AP, 게티이미지 등)은 직접 가져다 쓰지 않는다(핫링크 포함). 제공사가 공식 임베드 기능을 운영하면 그 방식으로만 쓴다.
```
바꾼 뒤:
```
- 통신사·언론사 사진(The Canadian Press, AP, 로이터, 게티이미지, 연합뉴스 등)은 직접 가져다 쓰지 않는다(핫링크 포함). 제공사가 공식 임베드 기능을 운영하면 그 방식으로만 쓴다.
```
근거: S1(`Licence not verified`, 서버 `NO_LICENSE` 와 같은 글자), T4 9절, S3c `excluded-credits`.

### 1-8. 정의서 8절 비기능 요구사항 (첫 줄)

지금:
```
- 모바일 우선, 한국어 UI.
```
바꾼 뒤:
```
- 모바일 우선, 캐나다 영어(en-CA) UI. 철자는 neighbourhood·licence·colour, 날짜는 "Saturday, February 13, 1988", 시각은 "3:20 p.m." 형식이다. 프랑스어 UI는 MVP 뒤에 한다.
```

### 1-9. 정의서 9절 범위 밖

지금:
```
로그인, 결제, 해외 장소, 출생 시각 기반 기능(사주, 별자리 등), AI 생성 이미지, 공유 카드와 SNS 공유, 다국어.
```
바꾼 뒤:
```
로그인, 결제, 캐나다 밖 장소, 출생 시각 기반 기능(사주, 별자리 등), AI 생성 이미지, 공유 카드와 SNS 공유, 프랑스어 UI(MVP 뒤).
```

### 1-10. 정의서 12절 데모 입력과 MVP 완료 조건 (절 전체)

지금:
```
데모와 테스트에는 아래 입력만 쓴다. 실제 사람의 생년월일은 쓰지 않는다.

| 입력 | 확인할 것 |
|---|---|
| 1988-09-17 + "서울" | `REGION`. 대표 명소, 그날의 사건(서울 올림픽 개막) |
| 2002-06-22 + "광주 서구 풍암동" | `NEIGHBORHOOD`. 동네 항공사진 그때/지금, 그날의 사건 |
| 1995-03-15 + "서울대학교병원" | `SPOT`. 지점 항공사진, 로드뷰, 동네 곳곳 |

완료 조건:
- 세 입력 모두 오프닝부터 마무리까지 결과가 나온다.
- 화면의 모든 미디어에 출처, 라이선스, 촬영 시점과 정밀도가 표시된다.
- 외부 출처 하나를 끊어도 화면이 깨지지 않는다.
```
바꾼 뒤:
```
데모와 테스트에는 아래 입력만 쓴다. 실제 사람의 생년월일은 쓰지 않는다. 날짜는 공개된 캐나다 역사 사건의 날짜다(누군가의 생일이 아니다). 공개된 역사 사건의 다른 날짜와 경계값은 `CLAUDE.md` 7절 6항을 따른다.

| 입력 | 확인할 것 |
|---|---|
| 1988-02-13 + "Calgary" | `REGION`, 산악 시간대. 대표 명소, 그날의 사건(캘거리 동계 올림픽 개막) |
| 1976-07-17 + "Hochelaga-Maisonneuve, Montréal" | `NEIGHBORHOOD`, 악센트가 있는 이름. 동네 그때/지금, 그날의 사건(몽레알 하계 올림픽 개막) |
| 2010-02-12 + "Vancouver General Hospital" | `SPOT`, 태평양 시간대. 지점 검색, 찾지 못하면 다시 입력 안내, 그날의 사건(밴쿠버 동계 올림픽 개막) |
| 1980-04-12 + "St. John's" | `REGION`, 뉴펀들랜드 시간대(UTC−3:30), 작은 도시. 그날의 사건(테리 폭스 "희망의 마라톤" 출발) |

장소 이름만 쓰는 예외(같은 이름 후보 목록 확인용, 날짜와 짝짓지 않는다): "London", "Victoria", "Saint John".
표본 날짜: 1969-07-20(영상 표본, 아폴로 11호 달 착륙).

완료 조건:
- 네 입력 모두 오프닝부터 마무리까지 결과가 나온다. 자료가 없는 섹션과 항목은 그 사실을 정직하게 안내한다.
- 화면의 모든 미디어에 출처, 라이선스, 촬영 시점과 정밀도가 표시된다.
- 외부 출처 하나를 끊어도 화면이 깨지지 않는다.
```
근거: 결정 1 (b)(장소 이름 예외 포함), 리서치 계획 X5 (a)·S3 D9 (a)(1969-07-20). 2차 2-11에서 완료 조건에 수치 줄을 더한다.

### 1-11. 정의서 13절 PM 결정 목록 (행 바꾸고 더하기)

지금:
```
| 지도·로드뷰 제공자 (카카오 / 네이버 / 구글) | 대기 — M2 계획 전에 정한다 (추천: 카카오) |
| 지원 시작 연도 | 대기 — M2 계획 전에 정한다 (추천: 1950년 유지) |
```
바꾼 뒤:
```
| 대상 나라와 화면 언어 | **캐나다, 캐나다 영어(en-CA)** (2026-10-29). 프랑스어는 MVP 뒤 |
| 장소 해석 출처 | **SGC·CGNDB 사전 + 지점은 OpenStreetMap Nominatim(확인 뒤)** (2026-11-04, T1 D1~D5). 지도·거리 사진 제공자(Google 사용 여부)는 대기 — C6 계획 전 (추천: 쓰지 않음, X4 (a)) |
| 지원 시작 날짜 | **1950-01-01** (2026-11-04, A2) |
| "오늘"의 기준 | **`America/St_Johns`** (2026-10-29) |
```
`상업적 이용 여부`, `영상 처리 방식`, `배포 여부와 대상` 행은 그대로 둔다.

### 1-12. CLAUDE.md 0절 한 줄 정의

Now:
```
- One-liner: enter a date of birth and a birthplace, and see what that place in South Korea looked like around that day, plus what happened in the world that day, through photos and short videos.
```
After:
```
- One-liner: enter a date of birth and a birthplace, and see what that place in Canada looked like around that day, how life then compares with now, and what happened in the world that day, through photos, short videos and numbers.
```

### 1-13. CLAUDE.md 7절 1항 (통신사 예와 화면 글자)

Now:
```
   - Do not take wire or news agency photos (Yonhap, AP, Getty Images, etc.) directly. Use them only through the provider's official embed feature.
   - Every media item shows its source, license ("unverified" if unknown), and capture time with precision. Media with an empty source field is not shown. Media whose capture time is unknown is shown with the label "촬영 시점 미상" (capture time unknown) and never presented as being from that day (PM decision, 2026-10-11).
```
After:
```
   - Do not take wire or news agency photos (The Canadian Press, AP, Reuters, Getty Images, Yonhap, etc.) directly. Use them only through the provider's official embed feature.
   - Every media item shows its source, licence ("Licence not verified" on screen if unknown), and capture time with precision. Media with an empty source field is not shown. Media whose capture time is unknown is shown with the label "Capture time unknown" and never presented as being from that day (PM decision, 2026-10-11).
```
The selection-order line (free licences) changes in round 2 (2-13).

### 1-14. CLAUDE.md 7절 5항 (fixture)

Now:
```
5. **Tests**: tests never call the external network. Replace external responses with fixtures and mocks (WireMock, MSW). Kakao, Naver and Google responses and map screens use synthetic fixtures built from the field structure in their official docs, plus SDK stubs; real responses are never committed. Sources that allow copying (Wikimedia, etc.) may use recorded fixtures, stored with the recording date, original URL and license.
```
After:
```
5. **Tests**: tests never call the external network. Replace external responses with fixtures and mocks (WireMock, MSW). Responses and map screens from providers whose terms forbid storing or copying responses (for example Google; Kakao and Naver in the paused Korea work) use synthetic fixtures built from the field structure in their official docs, plus SDK stubs; real responses are never committed. Sources whose licence allows copying (for example Wikimedia, Statistics Canada, NRCan CGNDB, ECCC) may use recorded fixtures, stored with the recording date, original URL and licence. If the research document does not mark recording as allowed for a source ("확인됨"), use synthetic fixtures.
```
근거: 결정 5 (b) 1차, T9 8절 8항(밴쿠버·캘거리·Nominatim은 합성).

### 1-15. CLAUDE.md 7절 6항 (장소 이름 예외)

Now:
```
6. **Privacy**: never log a date of birth together with a place. Tests and examples never use a real person's date of birth; use the demo inputs in `docs/product-brief.md` §12, plus dates of publicly known historical events and boundary values (for example a leap day). Those are event dates, not anyone's birthday (PM decision M3-P13, 2026-10-11).
```
After:
```
6. **Privacy**: never log a date of birth together with a place. Tests and examples never use a real person's date of birth; use the demo inputs in `docs/product-brief.md` §12 (including its place-name-only examples), plus dates of publicly known historical events and boundary values (for example a leap day). Those are event dates, not anyone's birthday (PM decision M3-P13, 2026-10-11).
```
2차 2-15에서 외부 출처로 보내는 값에 대한 문장을 더한다.

### 1-16. `.claude/` 에이전트 정의 점검 (1차)

`grep` 로 한국 전용 예시를 찾았다. 1차는 지금 작업에 틀린 지시를 줄 수 있는 것만이다. 그중 **3·5·8은 캐나다와 무관한 기존 모순**이다: 2026-10-11 PM 결정(촬영 시점을 모르는 미디어도 "촬영 시점 미상"으로 보임)과 달리 "촬영 시점이 없으면 보이지 않는다"고 적혀 있다. 지금 코드는 PM 결정대로 만들어져 있다.

| # | 파일:줄 | Now | After |
|---|---|---|---|
| 1 | `planner.md:49` | `` `GET /api/places/resolve?q=서울` 이 type=REGION과 좌표 반환, 단위 테스트 통과 `` | `` `GET /api/places/resolve?q=Calgary` 이 type=REGION과 좌표 반환, 단위 테스트 통과 `` |
| 2 | `backend-dev.md:20` | `(for example `PlaceSearchClient` ↔ `KakaoPlaceSearchClient`)` | `(for example `PlaceSearchClient` ↔ `NominatimPlaceSearchClient`)` |
| 3 | `backend-dev.md:23` | `media with an empty source or capture time is not returned for display` | `media with an empty source is not returned for display; media with an unknown capture time is returned with a null `date_precision` (shown as "Capture time unknown")` |
| 4 | `backend-dev.md:28` | `following `CLAUDE.md` §7.5 (synthetic fixtures for Kakao/Naver/Google; recorded fixtures only for sources that allow copying)` | `following `CLAUDE.md` §7.5 (synthetic fixtures for providers whose terms forbid storing responses, such as Google; recorded fixtures only for sources whose licence allows copying)` |
| 5 | `frontend-dev.md:19`·`:20` | `- Mobile first (360px width), Korean UI.` / `precision label (같은 날 / 같은 달 / 같은 해 / ±N년 / 현재 모습), source and license ("라이선스 확인 안 됨" when unknown). Do not render media missing source or capture time.` | `- Mobile first (360px width), Canadian English (en-CA) UI.` / `precision label (Same day / Same month / Same year / N years earlier / Present day; "Capture time unknown" without a badge), source and licence ("Licence not verified" when unknown). Do not render media missing a source.` |
| 6 | `content-curator.md:13` | `` `data/regions.json` — region name aliases (for example 서울, 서울시, 서울특별시 → the same code) `` | `` place dictionary aliases under `data/` as the C2 plan defines them (for example Montreal, Montréal, Ville de Montréal → the same CSD code) `` |
| 7 | `content-curator.md:20~25`·`:44`·`:45`·`:60` | 예시 레코드 `seoul-namsan-tower`·`N서울타워`·`"region_code": "11"`, `inside South Korea`, `(KOGL type, CC, etc.)`, `feat(data): add Seoul landmarks with existence years` | 예시 레코드를 캐나다 명소로(아래 블록), `inside Canada`, `(OGL-Canada, Crown copyright, CC, etc.)`, `feat(data): add Calgary landmarks with existence years` |
| 8 | `test-engineer.md:19`·`:20` | `the three inputs in product brief §12` / `media missing source or capture time is not displayed` | `the four inputs in product brief §12` / `media missing a source is not displayed, and media with an unknown capture time is shown as "Capture time unknown"` |
| 9 | `data-researcher.md:13`·`:27` | `(developer docs, terms of use, KOGL markings, API specs)` / `Coverage: year range, regions, amount of Korean-language data` | `(developer docs, terms of use, licence markings such as OGL-Canada or Crown copyright, API specs)` / `Coverage: year range, regions, amount of English (and French) data` |

7번의 예시 레코드 (값은 형식을 보이는 예다. content-curator가 실제로 쓸 때는 근거 링크로 다시 확인한다):
```json
{
  "id": "calgary-calgary-tower",
  "region_code": "csd-4806016",
  "name": "Calgary Tower",
  "aliases": ["Husky Tower"],
  "lat": 51.0443, "lng": -114.0631,
  "why": "Seen from almost anywhere downtown",
  "existed_from": 1968,
  "existed_until": null,
  "evidence": ["<URL for the opening/completion year>"]
}
```
`region_code` 의 모양(`csd-<7자리>`)은 C2 계획(`c2-ca-place-resolve.md`)의 `id` 접두어를 따랐다. 값 4806016은 캘거리 CSD 코드로 알려진 값이지만 이 문서에서 확인하지 않았으니, 붙이기 전에 SGC 2021에서 확인하거나 `<CSD code>` 자리 표시로 둔다.

`code-reviewer.md` 에는 한국 전용 예시가 없다.

---

## 2차

### 2-1. 정의서 5절 이야기 순서에 "그때와 지금" 섹션

지금:
```
3. **그 도시의 풍경**: 대표 명소마다 출생 연도에 가장 가까운 기록 사진이나 영상. 그때 아직 없던 명소는 빼고, 지금은 사라진 명소는 넣을 수 있다.
4. **그날의 세계**: ...
5. **마무리**: 이 결과에 쓰인 전체 출처 목록, 다른 날짜나 장소로 다시 해보기.
```
바꾼 뒤 (3 다음에 넣고 뒤 번호를 하나씩 민다):
```
4. **그때와 지금**: 태어난 날과 그해의 캐나다 생활을 지금과 숫자로 비교한다. 1차: 그날 날씨, $100의 그때와 지금 가치와 장바구니(휘발유, 빵, 우유), 최저임금으로 일한 시간, 주별 인기 아기 이름, 도시 인구. 2차(결정·확인 뒤): 그 주 1위 노래, 같은 생일의 유명인, 주택담보대출 금리와 캐나다 달러·미국 달러 환율, 그해 스탠리컵·주노상·영화, 신축 주택 가격 지수. 자료가 없는 항목은 그해 자료가 없다고 보인다(6.4절).
5. **그날의 세계**: (1-5의 문장 그대로)
6. **마무리**: 이 결과에 쓰인 전체 출처 목록, 다른 날짜나 장소로 다시 해보기.
```
근거: A1 (a)(2026-11-04), 항목 8 발명·기기는 뺌. **PM 결정 필요(C3 계획 전)**: 섹션 위치. (a) 도시 풍경 다음, 세계 앞(위 글. 날씨처럼 "그날 그곳" 값이 장소 섹션 다음에 이어진다) / (b) 세계 다음, 마무리 앞. 추천 (a). C3 계획이 이 위치로 섹션 id를 정한다.

### 2-2. 정의서 6.1절 추가 (노래·영상 날짜)

지금 6.1절 끝("AI가 생성하거나 합성한 이미지를 실제 기록처럼 쓰지 않는다.") 다음에 더할 글:
```
- 차트 순위는 발행일 기준으로 "Chart dated <발행일>"로 쓰고 집계 주간처럼 쓰지 않는다.
- 영상의 업로드·게시일과, 사건 날짜보다 늦은 촬영일 필드는 촬영 시점으로 쓰지 않는다. 회고 몽타주는 "Capture time unknown", 공식 기록 영화는 "Same year"로 쓴다.
```
근거: T7 D2 (a), T4 D5 (a), 한국판 영상 2번 (b). T7 D2·T4 D5는 위임으로 추천안을 적용할 결정이라 그 계획(C4, S4)이 정해지면 붙인다.

### 2-3. 정의서 6.2절 라이선스 예와 고르는 순서

지금:
```
| `license` | 예: `KOGL-1`, `CC BY-SA 4.0`, `PD`, `provider-terms`. 확인되지 않으면 비움 |
```
바꾼 뒤:
```
| `license` | 예: `OGL-Canada-2.0`, `OGL-Toronto`, `CC BY-SA 4.0`, `CC BY 4.0`, `CC BY-NC-SA 4.0`, `PD`, `provider-terms`. 확인되지 않으면 비움 |
```
지금:
```
- 고르는 순서: 자유 라이선스(Commons, 공공누리 1유형) → 업로더가 임베드를 허용한 공식 채널 영상(예: YouTube 공식 채널) → 그 밖의 출처 직접 표시.
```
바꾼 뒤:
```
- 고르는 순서: 자유 라이선스(Commons, 그 자료에 적용된다고 제공자가 밝힌 Open Government Licence – Canada 같은 정부 개방 라이선스) → 업로더가 임베드를 허용한 공식 채널 영상(예: YouTube 공식 채널) → 그 밖의 출처 직접 표시.
```
지금:
```
- 지도와 로드뷰 제공자의 약관(저장·캐싱 금지, 로고와 저작권 표시 등)은 M0 리서치 문서를 따른다.
```
바꾼 뒤:
```
- 지도·항공사진·거리 사진 제공자의 약관(저장·캐싱 금지, 로고와 저작권 표시 등)은 `docs/research/canada-*.md` 를 따른다.
```
지금:
```
- 공공누리는 유형마다 조건이 다르다(1유형만 상업 이용과 변경이 자유롭고, 나머지는 상업 이용 금지나 변경 금지 조건이 붙는다). 그래서 서비스를 상업화할지가 쓸 수 있는 자료 범위를 바꾼다. → PM 결정 사항.
```
바꾼 뒤:
```
- 캐나다 기관 자료는 Crown copyright이거나 기관마다 조건이 다르다(예: 몽레알 기록보관소 CC BY-NC-SA 4.0은 비상업·변경 표시 조건, Open Government Licence – Canada는 정보 라이선스라 이미지에 적용되는지 자료마다 본다). 그래서 서비스를 상업화할지가 쓸 수 있는 자료 범위를 바꾼다. → PM 결정 사항.
- 허락을 요구한다고 적힌 자료와 "All rights reserved" 자료를 어떻게 다룰지는 PM이 정한다(X1). [X1 결정 뒤 그 결정을 한 줄로 적는다]
- 타일 지도 레이어처럼 파일 주소가 없는 자료는 `url`·`thumbnail_url` 을 비우고 공식 SDK·타일 방식(`display_mode`)으로만 보인다.
```
근거: canada-summary.md 5절 6.2절 행 4개, T4 9절.

### 2-4. 정의서 6.4절 새로 (수치의 정직한 표기)

6.3절 다음에 넣을 글:
```
### 6.4 수치의 정직한 표기 (그때와 지금)

PM 결정(2026-10-29, 결정 6 (a)):
- 모든 수치에 그 값이 가리키는 기간과 정밀도를 둔다: `day` / `week` / `month` / `year` / `census`. 화면 글자: "That day" / "That week" / "That month" / "That year" / "1986 census (nearest to 1988)".
- 월·연 값은 그달·그해 값으로만 보이고 그날 값처럼 보이게 하지 않는다.
- "지금" 값은 그 값의 최신 기간을 함께 쓴다(예: "2025, latest", "September 2026").
- 계산한 값(물가 환산, 최저임금으로 일한 시간)은 "approx."를 붙이고 계산 방법 링크를 단다.
- 가장 가까운 인구조사 값을 쓰면 연도 차이를 숫자로 보인다. 인구는 경계 기준(예: "2021 boundaries")을 함께 쓴다.
- 규칙으로 정한 경우(가장 가까운 인구조사 등)만 다른 해의 값을 쓰고 그 연도를 밝힌다. 그 밖에는 자료가 없는 해를 다른 날짜·해·지역의 값으로 채우지 않고, 그해 자료가 없다고 보인다.
- 주마다 정의가 다른 값(아기 이름)은 주 사이에 숫자로 비교하지 않는다.
- 출처(`source_name`, `source_url`, `attribution`)와 라이선스는 미디어와 같은 규칙을 따른다. 출처가 비면 보이지 않는다.
- 수치 레코드는 `value`, `unit`, `period`, `period_precision`, `geography`, `series_id`, `source_name`, `source_url`, `license`, `attribution`, `latest_period`, `retrieved_at` 을 가진다. 확정은 C3 공용 수치 형식에서 한다.
```
선택(C3 계획에서 정해지면 더할 줄): 정의가 바뀐 계열(휘발유 풀서비스 → 셀프, 빵·우유 옛 표 → 새 표)의 표기(T5 D2), "그해 자료 없음"의 영어 문구.

### 2-5. 정의서 7절 데이터 출처 (절 전체)

지금: 제목 "## 7. 데이터 출처 후보 (전부 미검증 — M0에서 확인)" 과 한국 출처 표 6행, "현실적인 기대치" 문단.

바꾼 뒤:
```
## 7. 데이터 출처 (캐나다 리서치 결과 — `docs/research/canada-summary.md`)

| 영역 | 1순위 출처 | 확신도 (2026-10-29 리서치) | 쓰는 단계 |
|---|---|---|---|
| 장소 해석·좌표 | Statistics Canada SGC 2021 + NRCan CGNDB 내려받기 파일(사전), 지점은 OpenStreetMap Nominatim | 사전 확인됨 / Nominatim 부분 확인 | C2 |
| 그날의 사건 | 영어 위키백과 날짜·연도·"YYYY in Canada" 문서, Wikidata, Commons | 확인됨 | C1 |
| 사건 사진·영상 | Commons, 영어 위키백과 대표 이미지, YouTube 공식 채널(Radio-Canada Archives, Olympic Games) | Commons 확인됨 / 채널 부분 확인 | C1 |
| 동네 그때/지금 | NRCan 국가 항공사진 시계열(밴쿠버권 등 8곳), 토론토시 타일, 캘거리시 타일, 몽레알 기록보관소(큐레이션). 지금: Esri World Imagery 후보 | NAPL·토론토·몽레알 기록 확인됨 / 캘거리 부분 확인 / Esri 미확인 | C6 |
| 도시의 기록 사진·영상 | Commons 기록보관소 사본, YouTube 공식 채널 | 부분 확인 | C7 |
| 돈 | Statistics Canada(CPI, 휘발유, 식품), ESDC 역대 최저임금, Bank of Canada Valet(금리, 환율) | 확인됨 (환율 부분 확인) | C4·C5 |
| 날씨·인구·집값 | ECCC 일별 기후, Statistics Canada 인구 추계, 신축주택가격지수 | 확인됨 | C5 |
| 문화 | Wikidata(같은 생일, 스탠리컵), 주 정부 아기 이름(BC·온타리오·퀘벡), 영어 위키백과 1위 곡 목록, 주노상 | 같은 생일·스탠리컵·아기 이름 확인됨 / 노래·주노상 부분 확인 | C4·C5 |

현실적인 기대치: 특정 동네를 "그날" 찍은 사진은 대부분 없다. 한국과 달리 연도별 항공 타일이 있지만 전국 균일 서비스가 아니라 도시별이다. 그래서 지원 도시에서만 "그때" 항공사진을 보이고 나머지는 현재 모습과 안내를 보인다. 날짜·장소로 검색하는 캐나다 기록보관소 API는 확인하지 못했다. 기록 사진의 자동 경로는 Commons 사본, 영상은 YouTube 공식 채널뿐이다. 반대로 통계·기후 출처는 키 없이 저장까지 허용하는 개방 라이선스가 많아 "그때와 지금" 숫자는 데모 도시 대부분에서 나온다.
```
"지원 도시에서만 그때"는 T2 D5 (a) 추천이다. C6 계획 전에 위임으로 정해지면 그대로, 다르게 정해지면 그 줄을 고친다.

### 2-6. 정의서 8절 개인정보 줄

지금:
```
- 개인정보: 회원가입 없음. 생년월일과 장소 입력값을 사람과 연결해서 저장하지 않는다. 로그에 생년월일과 장소를 함께 남기지 않는다.
```
바꾼 뒤:
```
- 개인정보: 회원가입 없음. 생년월일과 장소 입력값을 사람과 연결해서 저장하지 않는다. 로그에 생년월일과 장소를 함께 남기지 않는다. 외부 출처에는 그 출처가 필요한 값만 보낸다: 사건은 날짜만, 장소 검색은 장소만, 날씨는 관측소 ID와 날짜만, 연도만 필요한 항목(인구, 아기 이름)은 연도만. 사용자가 쓴 장소 글자와 생년월일을 한 외부 요청에 함께 넣지 않는다.
```
근거: 리서치 계획 리스크 3, T9 7절 C3.

### 2-7. 정의서 9절 범위 밖 (한 항목 더하기)

1-9의 문장 끝에 더한다:
```
, 발명·기기 항목(자동 출처 없음, 2026-11-04)
```

### 2-8. 정의서 10절 아이디어 보관함 (절 전체의 목록)

지금:
```
- 그날의 날씨 (후보: 기상청 기상자료개방포털 과거 관측자료)
- 공유용 카드 이미지와 링크
- 그해의 노래·영화 (저작권 문제가 커서 제목과 링크만 가능할 것)
```
바꾼 뒤:
```
- 공유용 카드 이미지와 링크
- 발명·기기 소규모 큐레이션 목록 (T7 D6 (b))
- 프랑스어 UI와 프랑스어 개인정보처리방침
```
날씨와 노래·영화는 5절 "그때와 지금"으로 옮겼다.

### 2-9. 정의서 11절 마일스톤 초안 (절 전체의 표)

바꾼 뒤:
```
| 마일스톤 | 목표 |
|---|---|
| C0 캐나다 리서치 | 출처 검증, 11개 항목 판정, PM 결정 목록. 코드 없음 (완료) |
| C1 나라 전환 | 화면 en-CA, 입력·오프닝 캐나다판, 사건은 영어 위키백과, 미디어 캐나다로 |
| C2 장소 해석 | 캐나다 장소 → `REGION` / `NEIGHBORHOOD` / `SPOT` 와 좌표, 후보 선택 UI |
| C3 그때 vs 지금 공용 | 수치 레코드 형식, 섹션 뼈대, 표기 규칙 |
| C4 날짜만 필요한 항목 | 물가 환산, 금리·환율, 노래, 같은 생일, 수상 |
| C5 주·도시가 필요한 항목 | 날씨, 최저임금, 장바구니, 아기 이름, 인구, 집값 |
| C6 그날의 동네 | 항공사진 그때/지금(지원 도시) |
| C7 도시의 대표 풍경 | 명소 큐레이션과 기록 미디어 |
| C8 이야기 화면 완성 | 흐름, 성능, 출처 목록, 데모 준비 |

한국판 M0~M6은 2026-10-29에 캐나다로 바꿨다(M0·M1·M3는 완료, M2·M3a5b는 멈춤).
```

### 2-10. 정의서 13절 PM 결정 목록 (행 더하기)

```
| "그때 vs 지금" 범위 | **1차 1·2·3·6·11, 2차 4·5·7·9·10, 8 뺌** (2026-11-04, A1 (a)) |
| 수치 표기 | **6.4절** (2026-10-29, 결정 6 (a)) |
| 허락을 요구하는 자료 | 대기 — C6·C7 계획 전 (X1) |
```

### 2-11. 정의서 12절 완료 조건 (한 줄 더하기)

1-10의 "화면의 모든 미디어에 ..." 줄 다음:
```
- 화면의 모든 수치에 출처, 라이선스, 기간과 정밀도가 표시된다(6.4절).
```

### 2-12. CLAUDE.md 0절 폴더

Now:
```
  - `data/` curated data (landmarks, region aliases, pre-selected media)
```
After:
```
  - `data/` curated data (place dictionary, landmarks, region aliases, fixed then-and-now values, pre-selected media)
```

### 2-13. CLAUDE.md 7절 1항 고르는 순서

Now:
```
   - Selection order: free licenses (Wikimedia Commons, KOGL Type 1) → official channel videos whose uploader allows embedding (for example, official YouTube channels) → other sources shown directly.
```
After:
```
   - Selection order: free licences (Wikimedia Commons, and open government licences such as the Open Government Licence – Canada where the provider states they cover the item) → official channel videos whose uploader allows embedding (for example, official YouTube channels) → other sources shown directly.
```

### 2-14. CLAUDE.md 7절 2항 (해석 메모, 한국판에서 이월)

Now:
```
2. **Verified sources only**: implementation uses only sources whose confidence in `docs/research/` is "확인됨" (verified). Otherwise, do not implement; report it. Follow the terms (caching, storage, attribution, commercial use) exactly as the research documents state.
```
After (한 문장 더함):
```
2. **Verified sources only**: implementation uses only sources whose confidence in `docs/research/` is "확인됨" (verified). Otherwise, do not implement; report it. Follow the terms (caching, storage, attribution, commercial use) exactly as the research documents state. "확인됨" applies to the source (its access method and terms); an individual item whose licence is unverified may still be shown under item 1 (PM decision, 2026-10-11).
```
근거: 한국판 로드맵 문구 제안 표의 `CLAUDE.md` 7절 2항 행("아직").

### 2-15. CLAUDE.md 7절 6항 (외부로 보내는 값)

1-15의 문장 끝에 더한다:
```
 Keep what external sources receive to the minimum they need: never put the place the user typed and the date of birth in the same external request. Where a plan sends a date with a location-derived identifier (for example a weather station ID), the privacy policy says so.
```

### 2-16. CLAUDE.md 7절 8항 새로 (수치 자료)

7항 다음에 넣을 글:
```
8. **Numeric data (then and now)**: every numeric value carries its period and precision (`day` / `week` / `month` / `year` / `census`), geography, source, licence and `retrieved_at`, and follows `docs/product-brief.md` §6.4. A month or year value is never presented as that day's value. Computed values are marked "approx." with a link to the method. Values with an empty source are not shown. When a period has no data, say so; never fill it with another date, year or place unless the plan defines the rule and the label says so (for example the nearest census).
```

### 2-17. `.claude/` 에이전트 정의 (2차)

| # | 파일 | 더하거나 바꿀 것 |
|---|---|---|
| 1 | `frontend-dev.md:18` | 순서를 "(opening → the neighbourhood that day → scenes of the city → then and now → the world that day → closing)"로. 2-1의 위치 결정에 맞춘다 |
| 2 | `frontend-dev.md` Screen principles | 줄 더함: "Render numeric values through the shared then-and-now card, which enforces the period label (That day / That week / That month / That year / census year), the latest period of "now" values, "approx." for computed values, and the source and licence line." |
| 3 | `backend-dev.md` Implementation rules | 줄 더함: "**Numeric records**: follow `CLAUDE.md` §7.8 and the C3 format. Send external sources only what they need (station ID and date, or year only)." |
| 4 | `test-engineer.md` Always check | 줄 더함: "**Numeric values**: the period label matches `period_precision`; no month or year value is labelled as that day; "now" values show their latest period; computed values show "approx."; values with an empty source are not displayed. **External requests**: no request carries both the typed place and the date of birth." |
| 5 | `content-curator.md` Files you own | 줄 더함: 고정된 "그때 vs 지금" 값과 큐레이션 목록(경로는 C3 계획), 지역 코드 대응표(C5 `feat/ca-region-codes`). 설명(`description`)의 "landmark lists per province/city"는 그대로 맞다 |
| 6 | `data-researcher.md` Source card format | 줄 더함: "For numeric sources, add a numeric record field mapping (value, unit, period, period_precision, geography, series_id, licence, attribution, latest_period, retrieved_at) as in `docs/plans/canada-research.md`." |
| 7 | `code-reviewer.md` 확인 목록 | 줄 더함: 수치 레코드의 기간·정밀도·출처 필드와 `CLAUDE.md` 7절 8항 |

---

## 이 문서 밖으로 넘긴 것

- `README.md` 갱신은 PM이 오케스트레이터에게 맡긴 일이다(2026-11-04). 이 문서에 넣지 않았다.
- 한국판 로드맵 문구 제안 표의 다른 "아직" 행(빈 날짜 안내, 동의 단계, 영상 재생 규칙, `tone` 규칙, 위키백과 원문, 자동 영상 기준)은 지금 코드와 S1 방침에 이미 들어 있다. 정의서에 옮기려면 같은 표(`main` f07e420의 `docs/roadmap.md` 257~267행)의 문장을 영어 화면 글자로 바꿔 5·6절에 붙이면 된다. 필요하면 다음 갱신 때 before/after로 만든다.

---

## PM 위임 결정 (2026-11-05, orchestrator)

PM이 2026-11-04 자율 진행을 위임해 아래를 추천안으로 정했다. PM이 다르게 정하면 고친다.

1. C6 동네·C7 도시 풍경: (a) C8은 C6·C7을 기다리지 않고 머지된 만큼으로 마치고, 빈 섹션은 정직한 안내로 둔다. C5가 끝날 때 다시 본다.
2. ★ S3b T2 담당: (b) 자율 진행 중에는 backend-dev가 맡는다.
3. S2 T9(정의서 1차) 시점: (a) PR은 열고, 머지 전까지 PM이 1차 문구를 반영한다.
4. 정의서 1차 반영 방법: (b) PM이 검토하고 오케스트레이터에게 명시적으로 맡긴다. 그 전까지 오케스트레이터는 `docs/product-brief.md`·`CLAUDE.md`·`.claude/` 를 고치지 않는다.
5. "그때와 지금" 섹션 위치: (a) 도시 풍경 다음, 그날의 세계 앞. `c3-then-vs-now.md` C3-P6도 이 위치로 맞췄다.
