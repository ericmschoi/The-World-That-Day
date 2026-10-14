import { MediaItem } from '../media/MediaItem'
import { SourceLine } from './SourceLine'
import type { WorldEvent } from './types'
import styles from './WorldSection.module.css'

type Props = { event: WorldEvent }

// Alt text for event media without a description. No date words: the event
// is that day's, but its photos may not be (CLAUDE.md 7.1).
const fallbackAlt = (event: WorldEvent) =>
  event.title ? `「${event.title}」 관련 자료` : '사건 관련 자료'

// One event: the Wikipedia text exactly as written (text node only, never
// HTML, M3-P16), its media shown inline, then the text's source line.
export function WorldEventItem({ event }: Props) {
  const alt = fallbackAlt(event)
  return (
    <li className={styles.event}>
      <p className={styles.summary}>{event.summary}</p>
      {event.media.map((media) => (
        <MediaItem key={media.id} media={media} fallbackAlt={alt} />
      ))}
      <SourceLine event={event} />
    </li>
  )
}
