// Plan M3b2b T0: the response shape check keeps what the screen can draw and
// drops only the malformed event or media item. Synthetic values only.
import { describe, expect, it } from 'vitest'
import {
  shapeEventA,
  shapePhoto,
  worldAllDropped,
  worldMalformedEvents,
} from '../mocks/fixtures/world/malformed'
import {
  world1988,
  world1988WithMedia,
  world1995,
} from '../mocks/fixtures/world/responses'
import { toWorldResponse } from './worldShape'

const summaries = (value: unknown) =>
  toWorldResponse(value)?.events.map((event) => event.summary)

describe('toWorldResponse top level', () => {
  it.each([null, 'text', 42, [], { unexpected: true }, { events: {} }])(
    'is null for %j',
    (value) => {
      expect(toWorldResponse(value)).toBeNull()
    },
  )

  it('keeps the demo fixtures as they are', () => {
    for (const body of [world1988, world1988WithMedia, world1995]) {
      expect(toWorldResponse(body)).toEqual(body)
    }
  })
})

describe('toWorldResponse events', () => {
  it('drops null and malformed events, keeps the rest in order (T0 (1))', () => {
    const result = toWorldResponse(worldMalformedEvents)

    expect(summaries(worldMalformedEvents)).toEqual([
      '시험용 사건 문장 A',
      '시험용 사건 문장 B',
      '시험용 사건 문장 D',
    ])
    expect(result?.events.map((event) => event.media)).toEqual([
      [],
      [],
      [shapePhoto],
    ])
  })

  it.each([
    'id',
    'summary',
    'source_name',
    'source_title',
    'source_url',
    'text_license',
    'text_license_url',
  ])('drops an event whose %s is not a string', (key) => {
    const bad = { ...shapeEventA, id: 'bad', [key]: null }
    expect(summaries({ ...world1988, events: [shapeEventA, bad] })).toEqual([
      '시험용 사건 문장 A',
    ])
  })

  it.each([undefined, 'happy', null])(
    'drops an event whose tone is %j',
    (tone) => {
      const bad = { ...shapeEventA, id: 'bad', tone }
      expect(summaries({ ...world1988, events: [shapeEventA, bad] })).toEqual([
        '시험용 사건 문장 A',
      ])
    },
  )

  it('keeps tragic events', () => {
    const tragic = { ...shapeEventA, id: 'tragic', tone: 'tragic' }
    expect(toWorldResponse({ ...world1988, events: [tragic] })?.events).toEqual(
      [tragic],
    )
  })

  it.each([undefined, 42, {}])('turns a title of %j into null', (title) => {
    const result = toWorldResponse({
      ...world1988,
      events: [{ ...shapeEventA, title }],
    })
    expect(result?.events[0].title).toBeNull()
  })

  it.each([undefined, null, 'photo', {}])(
    'turns media of %j into []',
    (media) => {
      const result = toWorldResponse({
        ...world1988,
        events: [{ ...shapeEventA, media }],
      })
      expect(result?.events[0].media).toEqual([])
    },
  )

  it('keeps only media objects with a non-empty string id', () => {
    const result = toWorldResponse({
      ...world1988,
      events: [
        {
          ...shapeEventA,
          media: [
            null,
            'photo',
            42,
            { ...shapePhoto, id: '' },
            { ...shapePhoto, id: 7 },
            shapePhoto,
          ],
        },
      ],
    })
    expect(result?.events[0].media).toEqual([shapePhoto])
  })
})

describe('toWorldResponse day_media and notice', () => {
  it.each([undefined, null, 'photo', {}])(
    'turns day_media of %j into [] (T0 (2))',
    (dayMedia) => {
      const result = toWorldResponse({ ...world1988, day_media: dayMedia })
      expect(result?.day_media).toEqual([])
      expect(result?.events).toHaveLength(3)
    },
  )

  it('ignores null in day_media (T0 (2))', () => {
    const result = toWorldResponse({
      ...world1988,
      day_media: [null, shapePhoto, { id: '' }],
    })
    expect(result?.day_media).toEqual([shapePhoto])
  })

  it.each([undefined, 'NO_EVENTS_FOUND', { code: 'OTHER' }, {}])(
    'turns notice %j into null',
    (notice) => {
      expect(toWorldResponse({ ...world1988, notice })?.notice).toBeNull()
    },
  )
})

describe('toWorldResponse when everything is dropped (T0 (3))', () => {
  it('is null when nothing is left to show', () => {
    expect(toWorldResponse(worldAllDropped)).toBeNull()
  })

  it('is not null while day_media or a notice is left', () => {
    expect(
      toWorldResponse({ ...worldAllDropped, day_media: [shapePhoto] }),
    ).not.toBeNull()
    expect(
      toWorldResponse({
        ...worldAllDropped,
        notice: { code: 'NO_EVENTS_FOUND' },
      }),
    ).not.toBeNull()
  })

  it('is not null when the server sent no events at all', () => {
    expect(
      toWorldResponse({ ...world1988, events: [], notice: null }),
    ).not.toBeNull()
  })
})
