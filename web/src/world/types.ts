import type { Media } from '../media/types'

// Body of `GET /api/story/world` (plan M3b2 "API / 데이터 계약", server
// `WorldStoryResponse`). Names stay snake_case so the JSON maps one to one.

export type EventScope = 'domestic' | 'world' | 'unknown'

export type EventTone = 'normal' | 'tragic'

export type ToneBasis =
  | 'curated'
  | 'rule:keyword'
  | 'rule:class'
  | 'rule:deaths'
  | 'rule:no-signal'
  | 'default'

export type RelatedLink = { title: string; url: string }

export type WorldEvent = {
  id: string
  date: string
  scope: EventScope
  // Title of the main link, null if the line has none.
  title: string | null
  // Wikipedia text unchanged (M3-P16). Render as text only, never as HTML.
  summary: string
  summary_lang: 'ko'
  summary_modified: boolean
  text_license: string
  text_license_url: string
  source_name: string
  source_title: string
  source_url: string
  source_section: '사건' | '문화'
  source_revision: number
  wikidata_qid: string | null
  tone: EventTone
  tone_basis: ToneBasis
  related_links: RelatedLink[]
  media: Media[]
  retrieved_at: string
}

export type WorldNotice = { code: 'NO_EVENTS_FOUND' }

export type WorldResponse = {
  date: string
  // `normal` events first, in source order, then `tragic` ones.
  events: WorldEvent[]
  day_media: Media[]
  // Set when `events` is empty, otherwise null.
  notice: WorldNotice | null
  partial: boolean
}

// The server's problem `code` values (ProblemDetail + `code`, plan M3a2).
export type ApiProblemCode =
  | 'DATE_REQUIRED'
  | 'DATE_FORMAT'
  | 'DATE_NOT_EXIST'
  | 'DATE_IN_FUTURE'
  | 'UPSTREAM_UNAVAILABLE'
