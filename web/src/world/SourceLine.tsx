import { NewTabLink } from '../components/NewTabLink'
import type { WorldEvent } from './types'
import styles from './WorldSection.module.css'

type Props = { event: WorldEvent }

// "출처: {source_name} 「{source_title}」 · {text_license}" under an event's
// text, shared by the event list and the tragic block.
export function SourceLine({ event }: Props) {
  return (
    <p className={styles.source}>
      출처: {event.source_name}{' '}
      <NewTabLink href={event.source_url}>「{event.source_title}」</NewTabLink>
      <span aria-hidden="true"> · </span>
      <NewTabLink href={event.text_license_url}>
        {event.text_license}
      </NewTabLink>
    </p>
  )
}
