import type { ReactNode } from 'react'
import type { SectionId } from './sections'
import styles from './StorySection.module.css'

export type SectionStatus = 'loading' | 'empty' | 'error' | 'ready'

type Props = {
  id: SectionId
  title: string
  status: SectionStatus
  children?: ReactNode
}

// Shared frame for every story section. Each section owns its own status,
// so one failing section never hides the others.
export function StorySection({ id, title, status, children }: Props) {
  const headingId = `${id}-heading`
  return (
    <section
      id={id}
      aria-labelledby={headingId}
      aria-busy={status === 'loading' ? true : undefined}
      className={styles.section}
    >
      <h2 id={headingId} className={styles.title}>
        {title}
      </h2>
      {status === 'loading' && <p className={styles.note}>Loading…</p>}
      {status === 'empty' && (
        <p className={styles.note}>This section is coming soon.</p>
      )}
      {status === 'error' && (
        <p role="status" className={styles.note}>
          {"This section couldn't be loaded."}
        </p>
      )}
      {status === 'ready' && children}
    </section>
  )
}
