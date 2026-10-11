import type { DatePrecision, DateRelation, Media } from './types'

export type DateLabel = { badge: string | null; text: string }

const UNKNOWN: DateLabel = { badge: null, text: '촬영 시점 미상' }

// `captured_at` shape for each precision (M3a3 rule R8).
const SHAPES: Partial<Record<DatePrecision, RegExp>> = {
  day: /^(\d{4})-(\d{2})-(\d{2})$/,
  month: /^(\d{4})-(\d{2})$/,
  year: /^(\d{4})$/,
  approx: /^(\d{4})$/,
}

// Precisions each relation needs, so a badge never claims more than the
// capture time supports (product brief 6.1).
const NEEDS: Partial<Record<DateRelation['kind'], DatePrecision[]>> = {
  same_day: ['day'],
  same_month: ['day', 'month'],
  same_year: ['day', 'month', 'year'],
  years_apart: ['day', 'month', 'year'],
  approximate: ['approx'],
}

function formatCapturedAt(media: Media): string | null {
  const shape = media.date_precision && SHAPES[media.date_precision]
  const parts = shape?.exec(media.captured_at ?? '')
  if (!parts) return null
  const [, year, month, day] = parts
  return [`${year}년`, month && `${Number(month)}월`, day && `${Number(day)}일`]
    .filter(Boolean)
    .join(' ')
}

function badgeFor({ kind, years }: DateRelation): string | null {
  switch (kind) {
    case 'same_day':
      return '같은 날'
    case 'same_month':
      return '같은 달'
    case 'same_year':
      return '같은 해'
    case 'approximate':
      return '무렵'
    case 'years_apart':
      if (years < 0) return `${-years}년 전`
      return years > 0 ? `${years}년 뒤` : null
    default:
      return null
  }
}

// Badge and capture-time text for the caption. Anything inconsistent falls
// back to "촬영 시점 미상" rather than a badge that may be wrong.
export function dateLabel(media: Media): DateLabel {
  const relation = media.date_relation
  const precision = media.date_precision
  if (relation?.kind === 'current' && precision === 'current') {
    return { badge: '현재 모습', text: '촬영 시점 미제공' }
  }
  const allowed =
    relation && precision && NEEDS[relation.kind]?.includes(precision)
  const badge = allowed ? badgeFor(relation) : null
  const when = formatCapturedAt(media)
  if (!badge || !when) return UNKNOWN
  const text =
    precision === 'approx' ? `${when} 무렵 촬영(추정)` : `${when} 촬영`
  return { badge, text }
}
