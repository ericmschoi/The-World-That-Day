import type { WorldEvent, WorldResponse } from '../../../world/types'
import {
  captureTimeUnknown,
  licenseUnknown,
  olympicsOpeningPd,
  worldCupKogl1,
} from './media'

// `GET /api/story/world` bodies for the demo dates (product brief 12, plan
// M3b2 "MSW fixture"). Sources and synthetic values: README.md in this folder.

const WIKI = 'https://ko.wikipedia.org/wiki/'

// Fields every Korean Wikipedia event shares.
const kowiki = {
  scope: 'domestic',
  summary_lang: 'ko',
  summary_modified: false,
  text_license: 'CC BY-SA 4.0',
  text_license_url: 'https://creativecommons.org/licenses/by-sa/4.0/deed.ko',
  source_name: '한국어 위키백과',
  tone: 'normal',
  tone_basis: 'curated',
  media: [],
  retrieved_at: '2026-10-12T03:00:00Z',
} satisfies Partial<WorldEvent>

const olympicsOpening = {
  ...kowiki,
  id: 'kowiki-1988-09-17-875c69fa',
  date: '1988-09-17',
  title: '1988년 하계 올림픽',
  summary: '제24회 서울 하계 올림픽 개막.',
  source_title: '9월 17일',
  source_url: `${WIKI}9%EC%9B%94_17%EC%9D%BC`,
  source_section: '사건',
  source_revision: 42604413,
  wikidata_qid: 'Q8470',
  related_links: [
    {
      title: '1988년 하계 올림픽',
      url: `${WIKI}1988%EB%85%84_%ED%95%98%EA%B3%84_%EC%98%AC%EB%A6%BC%ED%94%BD`,
    },
  ],
} satisfies WorldEvent

const kbs1 = {
  ...kowiki,
  id: 'kowiki-1988-09-17-f90369fc',
  date: '1988-09-17',
  title: 'KBS 1TV',
  summary: 'KBS 1TV의 채널에서 광고 24개를 때려갔다.',
  source_title: '1988년',
  source_url: `${WIKI}1988%EB%85%84`,
  source_section: '문화',
  source_revision: 42640701,
  wikidata_qid: 'Q777278',
  related_links: [{ title: 'KBS 1TV', url: `${WIKI}KBS_1TV` }],
} satisfies WorldEvent

const kbs2 = {
  ...kbs1,
  id: 'kowiki-1988-09-17-5477d3e7',
  title: 'KBS 2TV',
  summary: 'KBS 2TV의 채널에서 광고 37개를 때려갔다.',
  wikidata_qid: 'Q624509',
  related_links: [{ title: 'KBS 2TV', url: `${WIKI}KBS_2TV` }],
} satisfies WorldEvent

const quarterFinal = {
  ...kowiki,
  id: 'kowiki-2002-06-22-04eaf6a6',
  date: '2002-06-22',
  title: '2002년 FIFA 월드컵',
  summary:
    '2002 FIFA 월드컵에서 대한민국이 스페인에 승부차기 끝에 PK 결과 5-3으로 승리하여 아시아 최초로 4강에 진출한다. (터키도 일한 만시즈의 결승골로 세네갈을 1:0으로 꺾고 4강에 진출함.)',
  source_title: '6월 22일',
  source_url: `${WIKI}6%EC%9B%94_22%EC%9D%BC`,
  source_section: '문화',
  source_revision: 42614972,
  wikidata_qid: 'Q47735',
  related_links: [
    {
      title: '2002년 FIFA 월드컵',
      url: `${WIKI}2002%EB%85%84_FIFA_%EC%9B%94%EB%93%9C%EC%BB%B5`,
    },
  ],
} satisfies WorldEvent

// Same shape as the server today: no media yet (until M3a3b).
export const world1988 = {
  date: '1988-09-17',
  events: [olympicsOpening, kbs1, kbs2],
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
    kbs1,
    kbs2,
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
