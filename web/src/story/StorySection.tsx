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
      {status === 'loading' && <p className={styles.note}>불러오는 중…</p>}
      {status === 'empty' && (
        <p className={styles.note}>아직 준비 중인 섹션입니다.</p>
      )}
      {status === 'error' && (
        <p role="status" className={styles.note}>
          이 섹션을 불러오지 못했습니다.
        </p>
      )}
      {status === 'ready' && children}
    </section>
  )
}
