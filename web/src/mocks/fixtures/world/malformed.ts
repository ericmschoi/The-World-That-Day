import type { WorldEvent } from '../../../world/types'
import { olympicsOpeningPd } from './media'
import { world1988 } from './responses'

// Synthetic bodies with a wrong shape for the response shape check (plan
// M3b2b T0). Plainly fake text; not used by the default handler. README.md.

export const shapeEventA = {
  ...world1988.events[0],
  id: 'kowiki-1988-09-17-0000000a',
  title: 'Test event',
  summary: 'Test event sentence A',
  media: [],
} satisfies WorldEvent

export const shapePhoto = {
  ...olympicsOpeningPd,
  date_relation: { kind: 'same_day', years: 0 },
}

// A with no `media` key at all.
const shapeEventB = {
  ...Object.fromEntries(
    Object.entries(shapeEventA).filter(([key]) => key !== 'media'),
  ),
  id: 'kowiki-1988-09-17-0000000b',
  summary: 'Test event sentence B',
}

// `summary` is a number: the whole event is dropped.
const shapeEventC = {
  ...shapeEventA,
  id: 'kowiki-1988-09-17-0000000c',
  summary: 42,
}

// One `null` and one photo in `media`: only the photo stays.
const shapeEventD = {
  ...shapeEventA,
  id: 'kowiki-1988-09-17-0000000d',
  summary: 'Test event sentence D',
  media: [null, shapePhoto],
}

// events = [A, null, B, C, D] (T0 (1)).
export const worldMalformedEvents = {
  ...world1988,
  events: [shapeEventA, null, shapeEventB, shapeEventC, shapeEventD],
}

// Every event dropped, nothing else to show (T0 (3)).
export const worldAllDropped = {
  ...world1988,
  events: [null, shapeEventC],
  day_media: [null],
  notice: null,
}
