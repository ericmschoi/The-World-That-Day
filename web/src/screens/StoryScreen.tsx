import { SECTIONS } from '../story/sections'
import { StorySection } from '../story/StorySection'
import styles from './Screen.module.css'

type Props = { onRestart: () => void }

// Empty story: every section is a placeholder until later milestones fill it.
export function StoryScreen({ onRestart }: Props) {
  return (
    <>
      <h1 className={styles.title}>The World That Day</h1>
      {SECTIONS.map((section) => (
        <StorySection
          key={section.id}
          id={section.id}
          title={section.title}
          status="empty"
        />
      ))}
      <button type="button" className={styles.button} onClick={onRestart}>
        처음으로
      </button>
    </>
  )
}
