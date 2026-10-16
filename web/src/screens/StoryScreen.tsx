import { PlaybackProvider } from '../media/PlaybackProvider'
import { SECTIONS } from '../story/sections'
import { StorySection } from '../story/StorySection'
import { WorldSection } from '../world/WorldSection'
import styles from './Screen.module.css'

type Props = { date: string; onRestart: () => void }

// The story for one date. Only "그날의 세계" is filled so far; the other
// sections stay placeholders until later milestones. Every video on one
// result shares a playback coordinator, so only one plays at a time.
export function StoryScreen({ date, onRestart }: Props) {
  return (
    <PlaybackProvider>
      <h1 className={styles.title}>The World That Day</h1>
      {SECTIONS.map((section) =>
        section.id === 'world' ? (
          <WorldSection key={section.id} date={date} />
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
