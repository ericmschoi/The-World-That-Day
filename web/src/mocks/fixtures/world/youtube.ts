import type { Media } from '../../../media/types'
import type { WorldResponse } from '../../../world/types'
import { world1988WithVideo } from './responses'

// Synthetic YouTube records (plan M3b3c "fixture"): the M3a5 / M3a5b contract
// examples with their empty fields filled in. Every video id is made up; no
// real response or video is in here (CLAUDE.md 7.5). Not used by the default
// handler; tests swap them in with `server.use(...)`. As in the plan, the
// other two change only the fields listed there (same embed URL).

// The 1986-09-20 sample date: a `year` capture in the same year.
export const exampleYoutubeEmbed = {
  id: 'youtube-Abc123xyz_0',
  type: 'video',
  display_mode: 'video_embed',
  url: null,
  thumbnail_url: null,
  thumbnail_width: null,
  thumbnail_height: null,
  video_sources: null,
  duration_seconds: null,
  embed_url:
    'https://www.youtube-nocookie.com/embed/Abc123xyz_0?playsinline=1&rel=0&enablejsapi=1&origin=http%3A%2F%2Flocalhost%3A5173',
  provider: 'youtube',
  render: null,
  link_only_reason: null,
  source_name: 'KTV 아카이브',
  source_url: 'https://www.youtube.com/watch?v=Abc123xyz_0',
  license: 'provider-terms',
  license_status: 'other',
  license_url: 'https://www.youtube.com/t/terms',
  attribution: 'KTV 아카이브 · YouTube',
  description: '시험용 개막식 영상 설명',
  captured_at: '1986',
  date_precision: 'year',
  lat: null,
  lng: null,
  place_ref: 'country:KR',
  retrieved_at: '2026-10-16T03:00:00Z',
  date_relation: { kind: 'same_year', years: 0 },
} satisfies Media

// Daehan News issue 1715 for the 1988-09-17 demo date (M3a5b attribution).
export const exampleDaehanNews = {
  ...exampleYoutubeEmbed,
  id: 'youtube-Dhn1715xyz0',
  attribution:
    '대한뉴스 제1715호 (1988년 9월 17일 자) · 대한뉴스(정부 제작 뉴스영화) · KTV 아카이브 · YouTube',
  description: '서울 올림픽 개막',
  captured_at: '1988',
  date_precision: 'approx',
  date_relation: { kind: 'approximate', years: 0 },
  selection: { basis: 'daehan_news', score: 90 },
} satisfies Media

export const exampleYoutubeUndated = {
  ...exampleYoutubeEmbed,
  id: 'youtube-Und000xyz_0',
  captured_at: null,
  date_precision: null,
  date_relation: { kind: 'unknown', years: 0 },
} satisfies Media

// `world1988WithVideo` plus Daehan News after event ①'s video and the
// undated record on event ② (KBS 1TV): two Commons videos and two YouTube
// players on one screen.
const [opening, kbs1, ...rest] = world1988WithVideo.events
export const world1988WithYoutube = {
  ...world1988WithVideo,
  events: [
    { ...opening, media: [...opening.media, exampleDaehanNews] },
    { ...kbs1, media: [...kbs1.media, exampleYoutubeUndated] },
    ...rest,
  ],
} satisfies WorldResponse
