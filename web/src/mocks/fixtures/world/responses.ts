import type { WorldEvent, WorldResponse } from '../../../world/types'
import {
  captureTimeUnknown,
  exampleVideoFile,
  licenseUnknown,
  olympicsOpeningPd,
  worldCupKogl1,
} from './media'

// `GET /api/story/world` bodies for the demo dates (product brief 12, plan
// M3b2 "MSW fixture"). Sources and synthetic values: README.md in this folder.

const WIKI = 'https://en.wikipedia.org/wiki/'

// Fields every English Wikipedia event shares (plan C1 S3a API contract).
// Synthetic English: written for these fixtures, not copied from a response.
const enwiki = {
  scope: 'world',
  summary_lang: 'en',
  summary_modified: false,
  text_license: 'CC BY-SA 4.0',
  text_license_url: 'https://creativecommons.org/licenses/by-sa/4.0/deed.en',
  source_name: 'English Wikipedia',
  source_section: 'Events',
  tone: 'normal',
  tone_basis: 'rule:no-signal',
  media: [],
  retrieved_at: '2026-10-30T03:00:00Z',
} satisfies Partial<WorldEvent>

// A day page line.
const olympicsOpening = {
  ...enwiki,
  id: 'enwiki-1988-09-17-0000a001',
  date: '1988-09-17',
  date_kind: 'day',
  range_end: null,
  title: '1988 Summer Olympics',
  summary: 'The 1988 Summer Olympics open in Seoul, South Korea.',
  source_title: 'September 17',
  source_url: `${WIKI}September_17`,
  source_revision: 1000000001,
  wikidata_qid: 'Q8470',
  related_links: [
    {
      title: '1988 Summer Olympics',
      url: `${WIKI}1988_Summer_Olympics`,
    },
  ],
} satisfies WorldEvent

const dayEventB = {
  ...olympicsOpening,
  id: 'enwiki-1988-09-17-0000a002',
  title: 'Test event B',
  summary: 'Test day event sentence B',
  source_revision: 1000000001,
  wikidata_qid: null,
  related_links: [],
} satisfies WorldEvent

// A year page range line that starts on the requested date (D2 (a)): the
// screen adds "Started that day — ran until October 2".
const rangeEventC = {
  ...dayEventB,
  id: 'enwiki-1988-09-17-0000a003',
  date_kind: 'range_start',
  range_end: '1988-10-02',
  title: 'Test event C',
  summary: 'Test range event sentence C',
  source_title: '1988',
  source_url: `${WIKI}1988`,
  source_revision: 1000000002,
} satisfies WorldEvent

const quarterFinal = {
  ...enwiki,
  id: 'enwiki-2002-06-22-0000b001',
  date: '2002-06-22',
  date_kind: 'day',
  range_end: null,
  title: '2002 FIFA World Cup',
  summary:
    'South Korea beat Spain on penalties in a 2002 FIFA World Cup quarter-final.',
  source_title: 'June 22',
  source_url: `${WIKI}June_22`,
  source_revision: 1000000003,
  wikidata_qid: 'Q47735',
  related_links: [
    {
      title: '2002 FIFA World Cup',
      url: `${WIKI}2002_FIFA_World_Cup`,
    },
  ],
} satisfies WorldEvent

// Without media.
export const world1988 = {
  date: '1988-09-17',
  events: [olympicsOpening, dayEventB, rangeEventC],
  day_media: [],
  notice: null,
  partial: false,
} satisfies WorldResponse

export const world2002 = {
  date: '2002-06-22',
  events: [quarterFinal],
  day_media: [],
  notice: null,
  partial: false,
} satisfies WorldResponse

export const world1995 = {
  date: '1995-03-15',
  events: [],
  day_media: [],
  notice: { code: 'NO_EVENTS_FOUND' },
  partial: false,
} satisfies WorldResponse

// With media, as after M3a3b. `day_media` is drawn by M3b2b, not M3b2.
export const world1988WithMedia = {
  ...world1988,
  events: [
    {
      ...olympicsOpening,
      media: [
        { ...olympicsOpeningPd, date_relation: { kind: 'same_day', years: 0 } },
        { ...captureTimeUnknown, date_relation: { kind: 'unknown', years: 0 } },
      ],
    },
    dayEventB,
    rangeEventC,
  ],
  day_media: [
    { ...licenseUnknown, date_relation: { kind: 'same_day', years: 0 } },
    {
      ...captureTimeUnknown,
      id: `${captureTimeUnknown.id}-day`,
      date_relation: { kind: 'unknown', years: 0 },
    },
  ],
} satisfies WorldResponse

export const world2002WithMedia = {
  ...world2002,
  events: [
    {
      ...quarterFinal,
      media: [
        { ...worldCupKogl1, date_relation: { kind: 'same_day', years: 0 } },
      ],
    },
  ],
} satisfies WorldResponse

// Synthetic, plainly fake text (plan M3b2b "fixture 추가"). Not used by the
// default handler; tests swap them in with `server.use(...)`.
const testEventA = {
  ...dayEventB,
  id: 'enwiki-1988-09-17-0000000a',
  title: 'Test event',
  summary: 'Test event sentence A',
  wikidata_qid: null,
  related_links: [],
} satisfies WorldEvent

// Its photo must never be drawn (product brief 6.3, #22 (c)).
const testTragic = {
  ...testEventA,
  id: 'enwiki-1988-09-17-0000000e',
  title: null,
  summary: 'Test tragic event sentence',
  tone: 'tragic',
  tone_basis: 'rule:keyword',
  media: [
    {
      ...olympicsOpeningPd,
      id: 'example-tragic-photo',
      date_relation: { kind: 'same_day', years: 0 },
    },
  ],
} satisfies WorldEvent

export const worldWithTragic = {
  ...world1988,
  events: [testEventA, testTragic],
  day_media: [],
} satisfies WorldResponse

export const worldTragicOnly = {
  ...world1988,
  events: [testTragic],
  day_media: [],
  notice: null,
} satisfies WorldResponse

// Synthetic (plan M3b3 "fixture 추가"): `world1988WithMedia` plus the example
// video after event ①'s photos, and an undated copy of it at the end of
// `day_media`. Two videos on one screen, for the coordinator and e2e. Not
// used by the default handler.
export const world1988WithVideo = {
  ...world1988WithMedia,
  events: [
    {
      ...world1988WithMedia.events[0],
      media: [
        ...world1988WithMedia.events[0].media,
        {
          ...exampleVideoFile,
          date_relation: { kind: 'same_month', years: 0 },
        },
      ],
    },
    ...world1988WithMedia.events.slice(1),
  ],
  day_media: [
    ...world1988WithMedia.day_media,
    {
      ...exampleVideoFile,
      id: 'example-video-file-undated',
      captured_at: null,
      date_precision: null,
      date_relation: { kind: 'unknown', years: 0 },
    },
  ],
} satisfies WorldResponse
