import { isRealDate, MONTHS } from '../media/dateLabel'
import type { WorldEvent } from './types'

// The note under an event that starts a range (plan C1 S3a, D2 (a)). The
// sentence alone reads as if it all happened that day, so the note says it
// only started then (product brief 6.1).
export const STARTED = 'Started that day'

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/

// `range_end` as the en-CA month and day ("February 28"), with the year only
// when it is not the event's year ("January 3, 1989"). Null for a value that
// is not a real date after the event's date.
function formatEnd(rangeEnd: unknown, date: unknown): string | null {
  if (typeof rangeEnd !== 'string' || typeof date !== 'string') return null
  const parts = ISO_DATE.exec(rangeEnd)
  if (!parts || !ISO_DATE.test(date) || rangeEnd <= date) return null
  const [, year, month, day] = parts
  if (!isRealDate(year, month, day)) return null
  const text = `${MONTHS[Number(month) - 1]} ${Number(day)}`
  return year === date.slice(0, 4) ? text : `${text}, ${year}`
}

// "Started that day — ran until February 28" for a `range_start` event, only
// "Started that day" when its end cannot be read, null for `day`, a missing
// or an unknown `date_kind`.
export function rangeNote(
  event: Pick<WorldEvent, 'date' | 'date_kind' | 'range_end'>,
): string | null {
  if (event.date_kind !== 'range_start') return null
  const end = formatEnd(event.range_end, event.date)
  return end === null ? STARTED : `${STARTED} — ran until ${end}`
}
