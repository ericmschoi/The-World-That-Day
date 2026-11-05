// Plan M3b2 T1b (4)-(7): the world fixtures have the server's key sets and
// the demo values, and their media are value-for-value copies of the M3a3
// examples in data/schema/examples/media/valid/. Key lists are copied from
// the plan (and the server's WorldStoryDemoTest), not built from types.ts.
// Plan C1 S3a: the event values are synthetic English in the S3a contract
// shape (English Wikipedia, `date_kind`, `range_end`), no longer copies of a
// Korean server response.
import { describe, expect, it } from 'vitest'
import pdExample from '../../../data/schema/examples/media/valid/commons-1988-09-17-seoul-olympics-opening-pd.json'
import koglExample from '../../../data/schema/examples/media/valid/commons-2002-06-22-gwangju-world-cup-kogl1.json'
import unknownTimeExample from '../../../data/schema/examples/media/valid/example-photo-capture-time-unknown.json'
import unknownLicenseExample from '../../../data/schema/examples/media/valid/example-photo-license-unknown.json'
import videoFileExample from '../../../data/schema/examples/media/valid/example-video-file.json'
import { exampleVideoFile } from '../mocks/fixtures/world/media'
import * as responses from '../mocks/fixtures/world/responses'
import * as youtube from '../mocks/fixtures/world/youtube'
import type { Media } from '../media/types'
import type { WorldResponse } from './types'

const TOP_KEYS = ['date', 'events', 'day_media', 'notice', 'partial']

const EVENT_KEYS = [
  'id',
  'date',
  'date_kind',
  'range_end',
  'scope',
  'title',
  'summary',
  'summary_lang',
  'summary_modified',
  'text_license',
  'text_license_url',
  'source_name',
  'source_title',
  'source_url',
  'source_section',
  'source_revision',
  'wikidata_qid',
  'tone',
  'tone_basis',
  'related_links',
  'media',
  'retrieved_at',
]

// The 26 stored fields of media.schema.json plus the API-only date_relation.
const MEDIA_KEYS = [
  'id',
  'type',
  'display_mode',
  'url',
  'thumbnail_url',
  'thumbnail_width',
  'thumbnail_height',
  'video_sources',
  'duration_seconds',
  'embed_url',
  'provider',
  'render',
  'link_only_reason',
  'source_name',
  'source_url',
  'license',
  'license_status',
  'license_url',
  'attribution',
  'description',
  'captured_at',
  'date_precision',
  'lat',
  'lng',
  'place_ref',
  'retrieved_at',
  'date_relation',
]

const sorted = (keys: string[]) => [...keys].sort()

// What the browser sees: the fixture after a JSON round trip.
const asJson = (value: unknown) => JSON.parse(JSON.stringify(value))

const fixtures: [string, WorldResponse][] = Object.entries(responses)
// M3b2b's synthetic tragic fixtures and M3b3's video one are not demo values.
const SYNTHETIC = ['worldWithTragic', 'worldTragicOnly', 'world1988WithVideo']
const demoFixtures = fixtures.filter(([name]) => !SYNTHETIC.includes(name))

describe('world fixtures: key sets (T1b (5))', () => {
  it('has the five demo fixtures and the three synthetic ones', () => {
    expect(fixtures.map(([name]) => name).sort()).toEqual([
      'world1988',
      'world1988WithMedia',
      'world1988WithVideo',
      'world1995',
      'world2002',
      'world2002WithMedia',
      'worldTragicOnly',
      'worldWithTragic',
    ])
  })

  it.each(fixtures)('%s has the server key sets', (_name, fixture) => {
    const json = asJson(fixture)
    expect(sorted(Object.keys(json))).toEqual(sorted(TOP_KEYS))
    const media = [...json.day_media]
    for (const event of json.events) {
      expect(sorted(Object.keys(event))).toEqual(sorted(EVENT_KEYS))
      media.push(...event.media)
    }
    for (const item of media) {
      // JSON keeps keys whose value is null, so this also checks for nulls.
      expect(sorted(Object.keys(item))).toEqual(sorted(MEDIA_KEYS))
    }
  })
})

describe('world fixtures: demo values (T1b (6), C1 S3a T6 (3))', () => {
  it('world1988 has the three synthetic English events, without media', () => {
    const rows = responses.world1988.events.map((event) => [
      event.title,
      event.summary,
      event.source_title,
      event.source_section,
      event.date_kind,
      event.range_end,
      event.media,
    ])

    expect(rows).toEqual([
      [
        '1988 Summer Olympics',
        'The 1988 Summer Olympics open in Seoul, South Korea.',
        'September 17',
        'Events',
        'day',
        null,
        [],
      ],
      [
        'Test event B',
        'Test day event sentence B',
        'September 17',
        'Events',
        'day',
        null,
        [],
      ],
      [
        'Test event C',
        'Test range event sentence C',
        '1988',
        'Events',
        'range_start',
        '1988-10-02',
        [],
      ],
    ])
    expect(responses.world1988.day_media).toEqual([])
    expect(responses.world1988.notice).toBeNull()
  })

  it('world2002 has the one quarter-final event, without media', () => {
    const [event] = responses.world2002.events

    expect(responses.world2002.events).toHaveLength(1)
    expect(event).toMatchObject({
      title: '2002 FIFA World Cup',
      source_title: 'June 22',
      source_url: 'https://en.wikipedia.org/wiki/June_22',
      source_section: 'Events',
      date_kind: 'day',
      range_end: null,
      wikidata_qid: 'Q47735',
      media: [],
    })
    expect(event.summary).toContain('Spain')
  })

  it('world1995 has no events and the NO_EVENTS_FOUND notice', () => {
    expect(responses.world1995).toEqual({
      date: '1995-03-15',
      events: [],
      day_media: [],
      notice: { code: 'NO_EVENTS_FOUND' },
      partial: false,
    })
  })

  it('every demo event is a normal English Wikipedia event', () => {
    for (const [, fixture] of demoFixtures) {
      for (const event of fixture.events) {
        expect([
          event.tone,
          event.tone_basis,
          event.scope,
          event.summary_lang,
          event.source_name,
          event.text_license_url,
        ]).toEqual([
          'normal',
          'rule:no-signal',
          'world',
          'en',
          'English Wikipedia',
          'https://creativecommons.org/licenses/by-sa/4.0/deed.en',
        ])
        expect(event.id).toMatch(/^enwiki-\d{4}-\d{2}-\d{2}-[0-9a-f]{8}$/)
        expect(event.source_url).toMatch(
          /^https:\/\/en\.wikipedia\.org\/wiki\//,
        )
      }
    }
  })

  it('the media fixtures keep the events and only add media', () => {
    const withoutMedia = (fixture: WorldResponse) => ({
      ...fixture,
      events: fixture.events.map((event) => ({ ...event, media: [] })),
      day_media: [],
    })

    expect(withoutMedia(responses.world1988WithMedia)).toEqual(
      responses.world1988,
    )
    expect(withoutMedia(responses.world2002WithMedia)).toEqual(
      responses.world2002,
    )
  })
})

describe('world fixtures: media copied from the M3a3 examples (T1b (7))', () => {
  const withoutRelation = ({
    date_relation,
    ...stored
  }: object & {
    date_relation?: unknown
  }) => {
    void date_relation
    return stored
  }

  it('world1988WithMedia: event ① has the PD photo, then the undated one', () => {
    const [first, second] = responses.world1988WithMedia.events[0].media

    expect(responses.world1988WithMedia.events[0].media).toHaveLength(2)
    expect(first.thumbnail_url).toMatch(/^https:\/\/thumb\.wikimedia\.org\//)
    expect(withoutRelation(first)).toEqual(pdExample)
    expect(first.date_relation).toEqual({ kind: 'same_day', years: 0 })
    expect(withoutRelation(second)).toEqual(unknownTimeExample)
    expect(second.date_relation).toEqual({ kind: 'unknown', years: 0 })
    expect(responses.world1988WithMedia.events[1].media).toEqual([])
    expect(responses.world1988WithMedia.events[2].media).toEqual([])
  })

  it('world1988WithMedia: day_media has the unlicensed photo and an undated copy', () => {
    const [unlicensed, undated] = responses.world1988WithMedia.day_media

    expect(responses.world1988WithMedia.day_media).toHaveLength(2)
    expect(withoutRelation(unlicensed)).toEqual(unknownLicenseExample)
    expect(unlicensed.date_relation).toEqual({ kind: 'same_day', years: 0 })
    expect(withoutRelation(undated)).toEqual({
      ...unknownTimeExample,
      id: 'example-photo-capture-time-unknown-day',
    })
    expect(undated.date_relation).toEqual({ kind: 'unknown', years: 0 })
  })

  // M3b3 T3 (4).
  it('exampleVideoFile is a value-for-value copy of example-video-file.json', () => {
    expect(asJson(exampleVideoFile)).toEqual(videoFileExample)
  })

  it('world1988WithVideo adds the video to event ① and an undated copy to day_media', () => {
    const fixture = responses.world1988WithVideo
    const eventMedia = fixture.events[0].media

    expect(eventMedia.slice(0, 2)).toEqual(
      responses.world1988WithMedia.events[0].media,
    )
    expect(withoutRelation(eventMedia[2])).toEqual(videoFileExample)
    expect(eventMedia[2].date_relation).toEqual({
      kind: 'same_month',
      years: 0,
    })
    expect(fixture.events.slice(1)).toEqual(
      responses.world1988WithMedia.events.slice(1),
    )
    expect(fixture.day_media.slice(0, 2)).toEqual(
      responses.world1988WithMedia.day_media,
    )
    expect(withoutRelation(fixture.day_media[2])).toEqual({
      ...videoFileExample,
      id: 'example-video-file-undated',
      captured_at: null,
      date_precision: null,
    })
    expect(fixture.day_media[2].date_relation).toEqual({
      kind: 'unknown',
      years: 0,
    })
  })

  it('world2002WithMedia: the event has the KOGL Type 1 photo', () => {
    const { media } = responses.world2002WithMedia.events[0]

    expect(media).toHaveLength(1)
    expect(withoutRelation(media[0])).toEqual(koglExample)
    expect(media[0].date_relation).toEqual({ kind: 'same_day', years: 0 })
    expect(responses.world2002WithMedia.day_media).toEqual([])
  })
})

// The server's Selection.score is an int: the sum of the rule table. It is not
// bounded by 100 (the table can add up to 130), so only integer-ness is checked.
describe('world fixtures: YouTube media selection score', () => {
  const isWorld = (value: unknown): value is WorldResponse =>
    typeof value === 'object' && value !== null && 'events' in value
  const exported: unknown[] = Object.values(youtube)
  const media: Media[] = [
    ...exported.filter((value): value is Media => !isWorld(value)),
    ...exported
      .filter(isWorld)
      .flatMap((world) => [
        ...world.day_media,
        ...world.events.flatMap((event) => event.media),
      ]),
  ]

  it('has a scored item to check', () => {
    expect(media.some((item) => item.selection)).toBe(true)
  })

  it('gives every item that has a selection an integer score', () => {
    for (const item of media) {
      if (item.selection) {
        expect(Number.isInteger(item.selection.score), item.id).toBe(true)
      }
    }
  })
})
