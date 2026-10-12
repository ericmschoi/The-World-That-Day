# 한국어 위키백과 fixture (M3a1 T2)

## 녹화 fixture (이 폴더의 `*.json`)

- 출처: 한국어 위키백과 날짜 문서·연도 문서, MediaWiki Action API `action=parse` 응답을 그대로 저장했다(바이트 수정 없음).
- 녹화 일자: 2026-10-11 (05:35~05:36 UTC), backend-dev가 `curl` 로 한 번씩 받았다. User-Agent는 공개값(#25 (a), 저장소 URL)을 썼다.
- 라이선스: 위키백과 텍스트, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) (와 GFDL). 각 문서의 기여자는 문서 역사(`https://ko.wikipedia.org/w/index.php?title=<제목>&action=history`)에 있다. 이 fixture는 테스트에만 쓰고 원문을 고치지 않았다.
- 범위: 절 목록과 "사건"·"문화" 절만. 탄생·사망·기념일 절은 요청하지 않았으므로 들어 있지 않다(계획서 C1).
- 절 목록 응답의 `warnings` 에 `"prop=sections" has been deprecated. Please use "prop=tocdata" instead.` 가 있다(녹화 당시 응답 그대로).

| 파일 | 문서 | `revid` | 원 URL |
|---|---|---|---|
| `09-17-sections.json` | 9월 17일 | 42604413 | `https://ko.wikipedia.org/w/api.php?action=parse&page=9%EC%9B%94%2017%EC%9D%BC&prop=sections%7Crevid&redirects=1&format=json&formatversion=2` |
| `09-17-events.json` | 9월 17일 (사건 절) | 42604413 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42604413&prop=wikitext&section=1&format=json&formatversion=2` |
| `09-17-culture.json` | 9월 17일 (문화 절) | 42604413 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42604413&prop=wikitext&section=2&format=json&formatversion=2` |
| `06-22-sections.json` | 6월 22일 | 42614972 | `https://ko.wikipedia.org/w/api.php?action=parse&page=6%EC%9B%94%2022%EC%9D%BC&prop=sections%7Crevid&redirects=1&format=json&formatversion=2` |
| `06-22-events.json` | 6월 22일 (사건 절) | 42614972 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42614972&prop=wikitext&section=1&format=json&formatversion=2` |
| `06-22-culture.json` | 6월 22일 (문화 절) | 42614972 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42614972&prop=wikitext&section=2&format=json&formatversion=2` |
| `03-15-sections.json` | 3월 15일 | 42485257 | `https://ko.wikipedia.org/w/api.php?action=parse&page=3%EC%9B%94%2015%EC%9D%BC&prop=sections%7Crevid&redirects=1&format=json&formatversion=2` |
| `03-15-events.json` | 3월 15일 (사건 절) | 42485257 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42485257&prop=wikitext&section=1&format=json&formatversion=2` |
| `03-15-culture.json` | 3월 15일 (문화 절) | 42485257 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42485257&prop=wikitext&section=2&format=json&formatversion=2` |
| `1988-sections.json` | 1988년 | 42640701 | `https://ko.wikipedia.org/w/api.php?action=parse&page=1988%EB%85%84&prop=sections%7Crevid&redirects=1&format=json&formatversion=2` |
| `1988-events.json` | 1988년 (사건 절) | 42640701 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42640701&prop=wikitext&section=1&format=json&formatversion=2` |
| `1988-culture.json` | 1988년 (문화 절) | 42640701 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42640701&prop=wikitext&section=2&format=json&formatversion=2` |
| `2002-sections.json` | 2002년 | 42498163 | `https://ko.wikipedia.org/w/api.php?action=parse&page=2002%EB%85%84&prop=sections%7Crevid&redirects=1&format=json&formatversion=2` |
| `2002-events.json` | 2002년 (사건 절) | 42498163 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42498163&prop=wikitext&section=1&format=json&formatversion=2` |
| `2002-culture.json` | 2002년 (문화 절) | 42498163 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42498163&prop=wikitext&section=2&format=json&formatversion=2` |
| `1995-sections.json` | 1995년 | 42616845 | `https://ko.wikipedia.org/w/api.php?action=parse&page=1995%EB%85%84&prop=sections%7Crevid&redirects=1&format=json&formatversion=2` |
| `1995-events.json` | 1995년 (사건 절) | 42616845 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42616845&prop=wikitext&section=1&format=json&formatversion=2` |
| `1995-culture.json` | 1995년 (문화 절) | 42616845 | `https://ko.wikipedia.org/w/api.php?action=parse&oldid=42616845&prop=wikitext&section=2&format=json&formatversion=2` |

## 합성 fixture (`synthetic/`)

파일 이름이 모두 `synthetic-` 로 시작한다. 위키백과에서 받은 것이 아니라 손으로 쓴 것이고, 내용은 "시험 사건 가" 같은 중립 문장뿐이다. 실제 사건·인물을 담지 않는다.

| 파일 | 쓰임 |
|---|---|
| `synthetic/synthetic-02-29-*.json` | 윤일 날짜 문서 `2월 29일`: 절 번호가 1·2가 아닐 때(사건 2, 문화 3) 그 번호만 요청하는지 |
| `synthetic/synthetic-02-29-next-*.json` | 같은 문서의 다음 판(`revid` 101, 사건 절만, 문화 절 없음): 캐시 갱신 시 행의 `revid`·본문이 바뀌는지(M3a1b) |
| `synthetic/synthetic-2000-*.json` | 윤년 연도 문서 `2000년`: 문화 절이 없을 때(level 3 "문화"는 고르지 않음) 요청하지 않는지 |
| `synthetic/synthetic-missingtitle.json` | 없는 문서 응답(`error.code` = `missingtitle`) |
| `synthetic/synthetic-ratelimited.json` | HTTP 200에 담긴 속도 제한 응답(`error.code` = `ratelimited`, M3a1b). 필드 구조는 Action API 오류 형식(`code`·`info`·`docref`)을 따랐다 |
| `synthetic/parser/synthetic-*.wikitext` | 파서 경계 사례(별칭 링크, 굵은 글씨, 각주·주석, 파일·분류 링크, 하위 줄, 링크 없는 날짜, 엔 대시, 템플릿, 외부 링크, 날짜 범위, 읽을 수 없는 연도, M3a1b의 HTML 태그·엔티티). 날짜는 2000-02-29 |

503·429 같은 HTTP 오류는 본문이 필요 없어 WireMock 상태 코드로만 만든다.

