import { useEffect, useRef } from 'react'
import { openingDate } from '../input/dateRules'
import { PlaybackProvider } from '../media/PlaybackProvider'
import { SECTIONS } from '../story/sections'
import { StorySection } from '../story/StorySection'
import { WorldSection } from '../world/WorldSection'
import styles from './Screen.module.css'

type Props = {
  date: string
  // As typed (trimmed) until M2b2 puts the chosen candidate's name here.
  place: string
  // "HH:MM", for the opening only.
  time?: string
  onRestart: () => void
}

// The story for one date. The opening and "그날의 세계" are filled; the other
// sections stay placeholders until later milestones. Every video on one
// result shares a playback coordinator, so only one plays at a time.
export function StoryScreen({ date, place, time, onRestart }: Props) {
  const opening = useRef<HTMLParagraphElement>(null)
  // The screen changed: start reading at the opening sentence.
  useEffect(() => opening.current?.focus(), [])
  return (
    <PlaybackProvider>
      <h1 className={styles.title}>The World That Day</h1>
      {SECTIONS.map((section) =>
        section.id === 'world' ? (
          <WorldSection key={section.id} date={date} />
        ) : section.id === 'opening' ? (
          <StorySection
            key={section.id}
            id={section.id}
            title={section.title}
            status="ready"
          >
            <p ref={opening} tabIndex={-1} className={styles.opening}>
              {`${openingDate(date, time)}, ${place}`}
            </p>
          </StorySection>
        ) : (
          <StorySection
            key={section.id}
            id={section.id}
            title={section.title}
            status="empty"
          />
        ),
      )}
      <button type="button" className={styles.button} onClick={onRestart}>
        처음으로
      </button>
    </PlaybackProvider>
  )
}
