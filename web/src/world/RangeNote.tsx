import { rangeNote } from './rangeText'
import type { WorldEvent } from './types'
import styles from './WorldSection.module.css'

type Props = { event: WorldEvent }

// Its own paragraph after the event's sentence, in the page language, so the
// Wikipedia text stays exactly as written. Shared by the event list and the
// tragic block; nothing for an event of that day.
export function RangeNote({ event }: Props) {
  const note = rangeNote(event)
  if (note === null) return null
  return <p className={styles.range}>{note}</p>
}
