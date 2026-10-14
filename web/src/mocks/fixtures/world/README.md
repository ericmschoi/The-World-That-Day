# World section fixtures

MSW bodies for `GET /api/story/world` (plan `docs/plans/m3b2-world-events-ui.md`, "MSW fixture").
TypeScript modules, so `satisfies WorldResponse` type-checks every value, string unions included.
Tests never call the network; media files are never downloaded or copied, only their metadata.

| Module         | Export                                                                       | What it is                                                                                                                                                                                                                                                                                     |
| -------------- | ---------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `responses.ts` | `world1988`                                                                  | 1988-09-17, same shape as the server today: 3 events, every `media` `[]`, `day_media` `[]`                                                                                                                                                                                                     |
|                | `world2002`                                                                  | 2002-06-22, same shape as the server today: 1 event, `media` `[]`                                                                                                                                                                                                                              |
|                | `world1995`                                                                  | 1995-03-15: no events, `notice` `NO_EVENTS_FOUND`                                                                                                                                                                                                                                              |
|                | `world1988WithMedia`                                                         | `world1988` plus media as after M3a3b: event ① has the public domain Commons photo (`same_day`) and the undated example (`unknown`); `day_media` has the unlicensed example (`same_day`) and a copy of the undated example with `-day` added to its `id` (`unknown`)                           |
|                | `world2002WithMedia`                                                         | `world2002` plus the KOGL Type 1 Commons photo (`same_day`) on its event                                                                                                                                                                                                                       |
|                | `worldWithTragic`, `worldTragicOnly`                                         | Synthetic (M3b2b), not used by the default handler. See "Synthetic or hand-copied values"                                                                                                                                                                                                      |
| `media.ts`     | `olympicsOpeningPd`, `worldCupKogl1`, `captureTimeUnknown`, `licenseUnknown` | Stored media records                                                                                                                                                                                                                                                                           |
| `malformed.ts` | `worldMalformedEvents`, `worldAllDropped` (plus `shapeEventA`, `shapePhoto`) | Synthetic bodies with a wrong shape for the response shape check (plan M3b2b T0): events `[A, null, B without media, C with a numeric summary, D with media [null, photo]]`, and a body where every event is dropped. Not typed as `WorldResponse` on purpose; not used by the default handler |

The default handler in `../../handlers.ts` answers `1988-09-17` with `world1988WithMedia`,
`2002-06-22` with `world2002WithMedia`, `1995-03-15` with `world1995`, and every other date with a
500 without a body. Tests swap in `world1988`, `world2002` or errors with `server.use(...)`.

## Where the values come from

Events: Korean Wikipedia text (CC BY-SA 4.0) as the server returns it, from the response example
and demo table in `docs/plans/m3a2-world-events-api.md` and the server's `WorldStoryDemoTest`
(recorded wikitext in `server/src/test/resources/fixtures/wikimedia/kowiki/`, see the README
there for revisions and recording dates). These values match the server exactly:

- every event's `title`, `source_title`, `source_section`, `source_revision`, `wikidata_qid`,
  `tone`, `tone_basis` and `scope`;
- the `id` of the 1988 opening event (`kowiki-1988-09-17-875c69fa`) and the 2002 event
  (`kowiki-2002-06-22-04eaf6a6`);
- the two KBS `summary` lines, character for character;
- `source_url` and `related_links[].url`, built with the server rule (spaces to `_`, UTF-8
  percent-encoding).

Media: copied value for value from `data/schema/examples/media/valid/`, with only the API-only
`date_relation` added. The two real Commons examples (original URLs, checked on 2026-10-13,
licenses) are described in `data/schema/examples/media/README.md`.
`src/world/worldFixtures.test.ts` compares every copied media value with those files.

## Synthetic or hand-copied values

The web must not depend on these matching a live server response.

| Value                                                     | How it was made                                                                                                                                                                                                                                                                                                                                                                                      |
| --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `id` of the two KBS events (`…-f90369fc`, `…-5477d3e7`)   | Computed by hand with the server rule (first 8 hex digits of SHA-256 of page title + `\n` + text). The same computation gives the two known ids above                                                                                                                                                                                                                                                |
| `summary` of the 1988 opening event and of the 2002 event | Copied by hand from the recorded wikitext lines with the links turned into their text and the leading year removed                                                                                                                                                                                                                                                                                   |
| `related_links` of every event                            | The main link only, as in the M3a2 response example                                                                                                                                                                                                                                                                                                                                                  |
| `retrieved_at` of every event                             | Synthetic `2026-10-12T03:00:00Z`, as in the M3a2 response example                                                                                                                                                                                                                                                                                                                                    |
| `id` `example-photo-capture-time-unknown-day`             | The undated example's `id` with `-day` added, so the `day_media` copy has its own key                                                                                                                                                                                                                                                                                                                |
| `date_relation` of every media item                       | Set by hand from the M3a3 rule for the demo date (`same_day` for a `day` capture on that date, `unknown` for a null precision)                                                                                                                                                                                                                                                                       |
| `worldWithTragic`, `worldTragicOnly`                      | Plainly fake text built on `world1988`: a `normal` event "시험용 사건 문장 A" and a `tragic` event (`tone_basis` `rule:keyword`) "시험용 비극 사건 문장" that carries one copy of the public domain photo (`id` `example-tragic-photo`), which the screen must never draw. `worldWithTragic` has both events, `worldTragicOnly` only the tragic one with `notice` `null`. Both have `day_media` `[]` |
