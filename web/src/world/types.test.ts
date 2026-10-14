// Plan M3b2 T1b (1): `WorldResponse`, `WorldEvent` and `ApiProblemCode` have
// the names and types of the plan's contract ("응답 200", "에러"). `Expected*`
// are written from the plan, not from types.ts. The assertions run under
// `npm run typecheck` (tsc -b).
import { describe, expect, it } from 'vitest'
import type { Media } from '../media/types'
import type { ApiProblemCode, WorldEvent, WorldResponse } from './types'

type Equal<A, B> =
  (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2
    ? true
    : false

type ExpectedEvent = {
  id: string
  date: string
  scope: 'domestic' | 'world' | 'unknown'
  title: string | null
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
  tone: 'normal' | 'tragic'
  tone_basis:
    | 'curated'
    | 'rule:keyword'
    | 'rule:class'
    | 'rule:deaths'
    | 'rule:no-signal'
    | 'default'
  related_links: { title: string; url: string }[]
  media: Media[]
  retrieved_at: string
}

type ExpectedResponse = {
  date: string
  events: ExpectedEvent[]
  day_media: Media[]
  notice: { code: 'NO_EVENTS_FOUND' } | null
  partial: boolean
}

type ExpectedProblemCode =
  | 'DATE_REQUIRED'
  | 'DATE_FORMAT'
  | 'DATE_NOT_EXIST'
  | 'DATE_IN_FUTURE'
  | 'UPSTREAM_UNAVAILABLE'

// Each fails `tsc -b` if a name, a type or null-ability differs.
const sameEvent: Equal<WorldEvent, ExpectedEvent> = true
const sameResponse: Equal<WorldResponse, ExpectedResponse> = true
const sameProblemCode: Equal<ApiProblemCode, ExpectedProblemCode> = true

describe('world types (T1b (1))', () => {
  it('match the plan contract', () => {
    expect([sameEvent, sameResponse, sameProblemCode]).toEqual([
      true,
      true,
      true,
    ])
  })
})
