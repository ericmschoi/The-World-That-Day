import type { Media } from '../media/types'
import type { WorldEvent, WorldNotice, WorldResponse } from './types'

// Response shape check (plan M3b2b T0). Only the fields the screen reads are
// checked: a malformed event or media item is dropped, the rest still render.
// Dropped items are never logged (no request date in logs, M3b2 C5).

type Loose = Record<string, unknown>

const isObject = (value: unknown): value is Loose =>
  typeof value === 'object' && value !== null

const EVENT_STRINGS = [
  'id',
  'summary',
  'source_name',
  'source_title',
  'source_url',
  'text_license',
  'text_license_url',
] as const

const TONES: readonly unknown[] = ['normal', 'tragic']

// Objects with a non-empty `id` (the React key). `isDisplayable` checks the rest.
const toMediaList = (value: unknown): Media[] =>
  Array.isArray(value)
    ? value.filter(
        (item): item is Media =>
          isObject(item) && typeof item.id === 'string' && item.id !== '',
      )
    : []

function toEvent(value: unknown): WorldEvent | null {
  if (!isObject(value)) return null
  if (!EVENT_STRINGS.every((key) => typeof value[key] === 'string')) {
    return null
  }
  if (!TONES.includes(value.tone)) return null
  return {
    ...(value as WorldEvent),
    title: typeof value.title === 'string' ? value.title : null,
    media: toMediaList(value.media),
  }
}

const toNotice = (value: unknown): WorldNotice | null =>
  isObject(value) && value.code === 'NO_EVENTS_FOUND'
    ? { code: 'NO_EVENTS_FOUND' }
    : null

// The body as a `WorldResponse` the screen can use, or null for the section's
// error state: not an object, `events` not an array, or every event dropped
// with nothing else left to show.
export function toWorldResponse(value: unknown): WorldResponse | null {
  if (!isObject(value) || !Array.isArray(value.events)) return null
  const events = value.events
    .map(toEvent)
    .filter((event): event is WorldEvent => event !== null)
  const dayMedia = toMediaList(value.day_media)
  const notice = toNotice(value.notice)
  if (
    value.events.length > 0 &&
    events.length === 0 &&
    dayMedia.length === 0 &&
    notice === null
  ) {
    return null
  }
  return { ...(value as WorldResponse), events, day_media: dayMedia, notice }
}
