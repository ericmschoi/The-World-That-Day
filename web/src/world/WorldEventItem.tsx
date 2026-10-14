import type { ReactNode } from 'react'
import { isHttpUrl } from '../media/isDisplayable'
import { MediaItem } from '../media/MediaItem'
import type { WorldEvent } from './types'
import styles from './WorldSection.module.css'

type Props = { event: WorldEvent }

// Alt text for event media without a description. No date words: the event
// is that day's, but its photos may not be (CLAUDE.md 7.1).
const fallbackAlt = (event: WorldEvent) =>
  event.title ? `「${event.title}」 관련 자료` : '사건 관련 자료'

// Opens in a new tab: the result screen is not in the address, so leaving it
// in the same tab would lose the result. Non-http(s) values stay plain text.
function NewTabLink({ href, children }: { href: string; children: ReactNode }) {
  if (!isHttpUrl(href)) return <>{children}</>
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
      <span className="visually-hidden"> (새 창에서 열림)</span>
    </a>
  )
}

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
      <p className={styles.source}>
        출처: {event.source_name}{' '}
        <NewTabLink href={event.source_url}>
          「{event.source_title}」
        </NewTabLink>
        <span aria-hidden="true"> · </span>
        <NewTabLink href={event.text_license_url}>
          {event.text_license}
        </NewTabLink>
      </p>
    </li>
  )
}
