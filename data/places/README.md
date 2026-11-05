# Canadian place dictionary (`data/places/`)

The server resolves a Canadian place input with these files only, with no outside calls (plan `docs/plans/c2-ca-place-resolve.md`, C2a). `regions.csv` and `neighbourhoods.csv` are built by `PlaceDictionaryBuilder` from three official download files. `provinces.csv`, `overrides.csv` and this README are written by hand. The source files are not in the repository. Only these derived files are.

## Sources

All three files were downloaded once on **2026-11-05** with the User-Agent `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)`. No account, key or terms acceptance was needed.

| File | URL | Size, SHA-256 | Notes |
|---|---|---|---|
| SGC 2021 classification structure | https://www.statcan.gc.ca/en/statistical-programs/document/sgc-cgt-2021-structure-eng.csv | 239,897 bytes, `55f6eb915415b4aa911c6fce3cbaced641c4ed9af023bebc414b2929721a62d2` | ISO-8859-1, `\r\n`. No `Last-Modified` header. Dataset record https://open.canada.ca/data/en/dataset/1d815243-28ce-45e6-ad6b-58643327a253 (published 2022-02-09, record modified 2023-04-21) |
| SGC 2021 elements | https://www.statcan.gc.ca/en/statistical-programs/document/sgc-cgt-2021-element-eng.csv | 1,829,851 bytes, `bf15e1655a06fdd03fba0da89fa3793def1915ebca89913694f81b1427f830b6` | Same encoding and record. Only `Place name(s)` rows of CSDs are used |
| CGNDB English CSV, national file | https://ftp.maps.canada.ca/pub/nrcan_rncan/vector/geobase_cgn_toponyme/prov_csv_eng/cgn_canada_csv_eng.zip | zip 16,265,828 bytes, `11cfb15659964c6b66cde57d2067e6e9c965e85f4420de67d20dc43b8ea80a6f`. Contains `cgn_canada_csv_eng.csv` (77,274,370 bytes, 366,235 data rows) | Server `Last-Modified` 2026-11-04 04:53:58 GMT. UTF-8 with byte order mark. Dataset record https://open.canada.ca/data/en/dataset/e27c6eba-3c5d-4051-9db2-082dc6411c2c (weekly extract) |

The CGNDB download has **no status column**. It has `CGNDB ID, Geographical Name, ISO Language Code, Language, Syllabic Form, Generic Term, Generic Category, Concise Code, Toponymic Feature ID, Latitude, Longitude, Location, Province - Territory, Relevance at Scale, Decision Date, Source`. Every row was treated as a current official name. The builder skipped the 28 rows whose `Province - Territory` is `Undersea Feature`. All 13 province and territory names in the CGNDB file match the SGC English spelling, so no province was skipped.

The CGNDB record warns that the database "contains historical terminology that is considered racist, offensive and derogatory". Names are kept exactly as published and are not edited here.

City populations were used only to choose which CSDs get an override. No population is stored in these files. Source: Statistics Canada, Table 98-10-0002-01 "Population and dwelling counts: Canada and census subdivisions (municipalities)", 2021 Census, https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=9810000201. The full-table CSV was downloaded on 2026-11-05 from https://www150.statcan.gc.ca/n1/tbl/csv/98100002-eng.zip (551,875 bytes, SHA-256 `36ab6c8f3f6b82d70d9dea927cb5cf04f6fbf9b543a7ad543695e6ea70d22876`, `Last-Modified` 2022-02-09) with the same User-Agent, and kept outside the repository. Column used: "Population, 2021". The table's geographic attributes also give the CSD type, used for Langley (5915001 is `DM`, district municipality; 5915002 is `CY`, city).

Province abbreviations (`provinces.csv`) come from Statistics Canada, *Standard Geographical Classification (SGC) 2021 – Introduction*, Table B "List of provinces and territories with codes and abbreviations, 2021", https://www.statcan.gc.ca/en/subjects/standard/sgc/2021/introduction (checked 2026-11-05, page modified 2024-03-12). `abbr` is the Canada Post alpha code. `aliases` is the Statistics Canada abbreviation. Some aliases fold to the same key as `abbr` (for example `N.L.` and `NL`). No other aliases are added because no official source was found for them.

## Licences and attribution

- SGC 2021: Statistics Canada Open Licence, https://www.statcan.gc.ca/en/terms-conditions/open-licence. These files are adapted, so section 5(b) applies:
  "Adapted from Statistics Canada, Standard Geographical Classification (SGC) 2021, 2021. This does not constitute an endorsement by Statistics Canada of this product."
  The date is the reference date that the licence asks for (SGC 2021). The downloaded record was last modified on 2023-04-21 (see Sources).
- CGNDB: Open Government Licence – Canada, https://open.canada.ca/en/open-government-licence-canada.
  "Contains information licensed under the Open Government Licence – Canada."
  Do not suggest that Natural Resources Canada endorses this product.

## Build

From the repository root, with the three downloads outside the repository (the zip unzipped):

```
cd server && ./gradlew buildPlaceDictionary \
  -PsgcStructure=<folder>/sgc-cgt-2021-structure-eng.csv \
  -PsgcElements=<folder>/sgc-cgt-2021-element-eng.csv \
  -Pcgndb=<folder>/cgn_canada_csv_eng.csv \
  -PplacesOut=../data/places
```

The builder reads `overrides.csv` from the output folder and rewrites `regions.csv` and `neighbourhoods.csv`. It only reads local files. A second build on the same inputs gave byte-identical files (checked 2026-11-05). Printed result for this build:

```
Place dictionary written: {neighbourhoods=12464, none=1942, override=14, shared=35, skipped: Undersea Feature=28, unique=3205}
```

Rebuild when SGC 2026 is released, or when the CGNDB extract needs to be refreshed. Record the new download date and counts here.

## Files

All files are UTF-8 without a byte order mark, with `\n` line ends and a header row.

| File | Made by | Columns | Data rows | Bytes |
|---|---|---|---|---|
| `provinces.csv` | hand | `code,name,abbr,aliases,lat,lng,cgndb_id`. `aliases` split by `\|`. Coordinates are the CGNDB `PROV`/`TERR` row with the English SGC name | 13 | 742 |
| `regions.csv` | builder | `csd_code,name,province_code,cd_name,lat,lng,cgndb_id,coord_rule`. `coord_rule` is `unique`, `override` or `none`; for `none` the last coordinates and id are empty | 5,161 | 335,012 |
| `neighbourhoods.csv` | builder | `cgndb_id,name,csd_code,province_code,lat,lng` | 12,464 | 653,365 |
| `overrides.csv` | hand | `csd_code,cgndb_id,reason,evidence_url,reviewed_at` | 14 | 4,181 |
| `README.md` | hand | — | — | this file |

Total size of the folder is under 1 MB (limit 4 MB, plan C2a T2).

## Matching rules and results

`fold` is `NameFold` in the server: accents dropped, lower case, curly apostrophes made straight, hyphens and dashes made spaces, `st`/`st.`/`saint` become `saint`, `ste`/`ste.`/`sainte` become `sainte`, other full stops removed, and spaces collapsed.

- **B1, CSD point.** A CSD gets the CGNDB point when exactly one CGNDB name in its province has the same folded name and a `Concise Code` of `CITY`, `TOWN`, `VILG`, `HAM`, `UTM`, `LTM`, `STM`, `MUN1` or `MUN2`, and no other CSD in that province has the same folded name. Otherwise it gets the point from `overrides.csv`, or no point (`none`). Points are never guessed.
- **B2, neighbourhood.** For each SGC `Place name(s)` row of a CSD with a point, take the CGNDB names in the same province with the same folded name, a `Concise Code` of `UNP`, `HAM` or `VILG`, and within **25 km** of the CSD point. If there is exactly one, and it is not the CSD's own point, it is a neighbourhood of that CSD. A name that would belong to two CSDs is left out (`shared`). The 25 km radius has no official basis. It is the constant `PlaceDictionaryBuilder.NEIGHBOURHOOD_RADIUS_KM`.
- **B3.** Rows are sorted by `csd_code`, then `cgndb_id`.

Results for the 5,161 CSDs:

| `coord_rule` | CSDs | Why |
|---|---|---|
| `unique` | 3,205 | one matching CGNDB name |
| `override` | 14 | `overrides.csv` |
| `none` | 1,942 | 1,608 with no matching CGNDB name, 63 with two or more, 271 that share their folded name with another CSD of the province |

Neighbourhoods: 12,464 rows, plus 35 names left out because they matched two CSDs.

All 56 CSDs with a 2021 Census population of 100,000 or more (Table 98-10-0002-01, above) have coordinates: 43 `unique` and 13 `override` (Montréal and the 12 added in T2b). Smaller CSDs with `none` resolve without coordinates. Moncton (1307022, 2021 population under 100,000) is one of them, and so is its same-name CSD 1307019.

## Overrides

An override is only for a CSD that B1 cannot give a point to. An override on a CSD that already has a `unique` point stops the build. Each row names one CGNDB record and links to it in the Geoname service. Every chosen record was opened there on 2026-11-05: status Official, with the same name, type and coordinates as the download.

Two groups of CSDs have overrides: the plan's demo and same-name CSDs, and every CSD with a 2021 population of 100,000 or more that B1 left without a point. In each case exactly one CGNDB record matched the CSD by type and location. No such city was left empty.

| CSD | CGNDB | Why |
|---|---|---|
| 1102032 Victoria (Queens) | BACOS (`VILG`) | Two PEI CSDs are named Victoria. BACOS is the only current populated-place Victoria in PEI, and its CGNDB Location is Queens, the census division of this CSD. The other PEI Victoria, 1103068 (Prince), has no CGNDB name to match and stays `none` |
| 2423027 Québec | EHTWR (`CITY`) | one `CITY` and two `MUN1` (agglomeration, territory equivalent to an MRC) |
| 2425213 Lévis | EGZSK (`CITY`) | one `CITY` and one `MUN1` (territory equivalent to an MRC) |
| 2437067 Trois-Rivières | EIITR (`CITY`) | one `CITY` and one `MUN1` (territory equivalent to an MRC) |
| 2443027 Sherbrooke | EIDHN (`CITY`) | one `CITY` and one `MUN1` (territory equivalent to an MRC) |
| 2458227 Longueuil | EHAYB (`CITY`) | one `CITY` and two `MUN1` (agglomeration, territory equivalent to an MRC) |
| 2465005 Laval | EGYIC (`CITY`) | one `CITY` and two `MUN1` (administrative region, territory equivalent to an MRC) |
| 2466023 Montréal | EHHUN (`CITY`) | one `CITY` and three `MUN1` (territory equivalent to an MRC, administrative region, agglomeration) |
| 2481017 Gatineau | EGLTT (`CITY`) | one `CITY` and one `MUN1` (territory equivalent to an MRC) |
| 2494068 Saguenay | ERBYN (`CITY`) | one `CITY` and one `MUN1` (territory equivalent to an MRC) |
| 3521005 Mississauga | FEAKO (`CITY`) | the `CITY` with Location Peel (this CSD's division) and a `MUN2` First Nation administrative location in Algoma |
| 3525005 Hamilton | FEVNS (`CITY`) | two Ontario CSDs are named Hamilton. FEVNS is the only `CITY` Hamilton in Ontario. The other CSD (3514019, Northumberland) matches the `MUN2` township FELSA, Location Northumberland, and stays `none` (small, not a demo place) |
| 3530016 Waterloo | FDBJZ (`CITY`) | the `CITY` and the `MUN1` regional municipality FDBKD |
| 5915001 Langley | JAPSK (`MUN1`) | two BC CSDs are named Langley. 5915001 is the district municipality (`DM`) and 5915002 the city (`CY`). JAPSK is the CGNDB District Municipality of Langley, and JAPSJ is the `CITY`. 5915002 stays `none` (under 100,000) |

Where the Why column says "one `CITY` and ...", it lists the current names in the province that fold to the CSD name with a B1 code. The `CITY` row is the city itself.

Placing these cities changed the neighbourhoods. 12 CSDs gained neighbourhoods. Laval's SGC place names include "Lachenaie" and "Montréal", so two names now fall in two CSDs and were left out: ERBYL Lachenaie (also Terrebonne) and EMBOX "Montréal" (a post office, also Montréal).

## Demo and same-name checks (plan C2, "demo expectations")

| Input | Row |
|---|---|
| Calgary | `regions.csv` 4806016, 51.0458333 / -114.0574999, IAKID, `unique` |
| Hochelaga-Maisonneuve, Montréal | `neighbourhoods.csv` EQVRD, CSD 2466023, 45.5647222 / -73.5388889 |
| Vancouver | `regions.csv` 5915022, 49.2611110 / -123.1138890, JBRIK, `unique` |
| St. John's | `regions.csv` 1001519, 47.4821970 / -52.7970690, ABEFS, `unique` |
| London | `regions.csv` 3539036 (Middlesex), FCAEN, `unique`. The only London CSD |
| Victoria | `regions.csv` 1001365 (NL), 1102032 (PEI, Queens), 1103068 (PEI, Prince, `none`), 4608042 (MB), 5917034 (BC) |
| Saint John | `regions.csv` 1301006, DAEGW, `unique`. Fort St. John is 5955034, a separate name |
