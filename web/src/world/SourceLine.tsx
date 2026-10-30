import { NewTabLink } from '../components/NewTabLink'
import type { WorldEvent } from './types'
import styles from './WorldSection.module.css'

type Props = { event: WorldEvent }

// "Source: {source_name}, “{source_title}” · {text_license}" under an event's
// text, shared by the event list and the tragic block. The page title is in
// the text's language.
export function SourceLine({ event }: Props) {
  return (
    <p className={styles.source}>
      Source: {event.source_name},{' '}
      <NewTabLink href={event.source_url} lang={event.summary_lang}>
        “{event.source_title}”
      </NewTabLink>
      <span aria-hidden="true"> · </span>
      <NewTabLink href={event.text_license_url}>
        {event.text_license}
      </NewTabLink>
    </p>
  )
}
