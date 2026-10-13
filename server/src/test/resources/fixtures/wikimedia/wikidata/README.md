# Wikidata Query Service fixture (M3a2 T3)

## 녹화 fixture (이 폴더의 `sparql-*.json`)

- 출처: Wikidata Query Service (`https://query.wikidata.org/sparql`) SPARQL 응답(`format=json`)을 그대로 저장했다(바이트 수정 없음).
- 녹화 일자: 2026-10-13 (18:50 UTC 무렵), backend-dev가 한 번씩 받았다. User-Agent는 공개값 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)`(저장소 URL만, 개인 연락처 없음)를 썼다.
- 라이선스: Wikidata 구조화 데이터, [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) ([Wikidata:Licensing](https://www.wikidata.org/wiki/Wikidata:Licensing)). 출처 표시 의무는 없다.
- 걸린 시간(계획서 리스크 3, `P279*` 질의): `sparql-1988-09-17.json` 0.455초, `sparql-2002-06-22.json` 0.156초(같은 질의를 처음 보낸 시도는 0.370초). 읽기 타임아웃 `wikimedia.wikidata-read-timeout` 5초보다 훨씬 짧다.
- 질의는 서버 코드(`SparqlWikidataSignalSource.query`)가 만드는 문자열과 글자 하나까지 같다(`SparqlWikidataSignalSourceTest.readmeQueryAndUrlAreWhatTheCodeSends` 가 확인). 결과: 세 1988 항목과 Q47735 모두 비극 분류·사망자 수 없음, 국가(P17)에 대한민국(Q884). Q47735는 일본(Q17)도 있다.

| 파일 | 데모 날짜 | 질의한 항목 | 원 URL |
|---|---|---|---|
| `sparql-1988-09-17.json` | 1988-09-17 | Q8470 Q777278 Q624509 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ8470%20wd%3AQ777278%20wd%3AQ624509%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |
| `sparql-2002-06-22.json` | 2002-06-22 | Q47735 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ47735%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |

질의 원문 (1988-09-17):

```sparql
SELECT ?item ?tragicClass ?deaths ?country WHERE { VALUES ?item { wd:Q8470 wd:Q777278 wd:Q624509 } OPTIONAL { ?item wdt:P31/wdt:P279* ?tragicClass . VALUES ?tragicClass { wd:Q3839081 wd:Q171558 wd:Q2223653 wd:Q350604 } } OPTIONAL { ?item wdt:P1120 ?deaths } OPTIONAL { ?item wdt:P17 ?country } }
```

## 합성 fixture (`synthetic/`)

손으로 쓴 응답이다. 항목은 가짜 QID(`Q1`~`Q7`, `Q99`)뿐이고 실제 사건을 담지 않는다. 필드 구조는 위 녹화 응답(SPARQL 1.1 Query Results JSON)과 같다.

| 파일 | 확인하는 것 |
|---|---|
| `synthetic/synthetic-signals.json` | Q1 비극 분류, Q2 사망자 3, Q3 사망자 0(신호 아님), Q4 국가 Q884와 Q17, Q5 국가 Q17만, Q6 신호 없음, Q7 읽을 수 없는 사망자 수(신호 아님), Q99 묻지 않은 항목(무시) |
