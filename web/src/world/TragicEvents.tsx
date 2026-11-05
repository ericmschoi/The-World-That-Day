import { SourceLine } from './SourceLine'
import type { WorldEvent } from './types'
import styles from './WorldSection.module.css'

type Props = { events: WorldEvent[] }

const TITLE = 'Difficult events on the same day'

// `tragic` events after the normal ones, in a calm grey block (product brief
// 6.3, #22 (c)): text and source line only, never their media, no accent.
export function TragicEvents({ events }: Props) {
  if (events.length === 0) return null
  return (
    <div className={styles.tragic}>
      <h3 className={styles.subheading}>{TITLE}</h3>
      <ul className={styles.events}>
        {events.map((event) => (
          <li key={event.id} className={styles.event}>
            <p className={styles.summary} lang={event.summary_lang}>
              {event.summary}
            </p>
            <SourceLine event={event} />
          </li>
        ))}
      </ul>
    </div>
  )
}
