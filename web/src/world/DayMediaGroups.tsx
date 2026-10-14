import { useCallback, useState } from 'react'
import { dateLabel } from '../media/dateLabel'
import { canRender } from '../media/isDisplayable'
import { MediaItem } from '../media/MediaItem'
import type { Media } from '../media/types'
import styles from './WorldSection.module.css'

type Props = { media: Media[] }

const TITLES = [
  '그날 찍힌 사진',
  '다른 때 찍힌 사진',
  '촬영 시점을 확인하지 못한 사진',
]

// M3-P17 (a) and decision 4 (a), read from the caption's own badge so a
// heading never says more than its captions: "같은 날" → 0, no badge
// ("촬영 시점 미상") → 2, any other badge (같은 달, ±N년, …) → 1.
function groupOf(media: Media): number {
  const { badge } = dateLabel(media)
  if (badge === '같은 날') return 0
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
  const shown = media.filter((item) => canRender(item) && !failed.has(item.id))
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
              item.display_mode === 'video_file' ? '기록 영상' : '기록 사진'
            }
            onUnavailable={hide}
          />
        ))}
      </div>
    )
  })
}
