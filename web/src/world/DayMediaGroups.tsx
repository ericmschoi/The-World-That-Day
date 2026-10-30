import { useCallback, useState } from 'react'
import { useConsent } from '../consent/consent'
import { dateLabel, SAME_DAY } from '../media/dateLabel'
import { canRender } from '../media/isDisplayable'
import { MediaItem } from '../media/MediaItem'
import type { Media } from '../media/types'
import styles from './WorldSection.module.css'

type Props = { media: Media[] }

const TITLES = [
  'Photos taken that day',
  'Photos taken at other times',
  'Photos with an unknown capture time',
]

// M3-P17 (a) and decision 4 (a), read from the caption's own badge so a
// heading never says more than its captions: "Same day" → 0, no badge
// ("Capture time unknown") → 2, any other badge (Same month, N years
// earlier, …) → 1.
function groupOf(media: Media): number {
  const { badge } = dateLabel(media)
  if (badge === SAME_DAY) return 0
  return badge === null ? 2 : 1
}

// `day_media` under its three subheadings, server order inside each. Only
// media `MediaItem` draws counts, and a video that failed to play drops out,
// so a group never keeps its heading over nothing. Videos sit under the same
// headings (M3b3 decision 3 (a)). Alt text names no date (CLAUDE.md 7.1).
export function DayMediaGroups({ media }: Props) {
  const [failed, setFailed] = useState<ReadonlySet<string>>(new Set())
  const hide = useCallback(
    (id: string) => setFailed((ids) => new Set(ids).add(id)),
    [],
  )
  const consented = useConsent()
  const shown = media.filter(
    (item) => canRender(item, consented) && !failed.has(item.id),
  )
  return TITLES.map((title, index) => {
    const items = shown.filter((item) => groupOf(item) === index)
    if (items.length === 0) return null
    return (
      <div key={title} className={styles.group}>
        <h3 className={styles.subheading}>{title}</h3>
        {items.map((item) => (
          <MediaItem
            key={item.id}
            media={item}
            fallbackAlt={
              item.display_mode === 'image' ? 'Archive photo' : 'Archive video'
            }
            onUnavailable={hide}
          />
        ))}
      </div>
    )
  })
}
