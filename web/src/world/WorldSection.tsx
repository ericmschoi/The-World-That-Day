import { useEffect, useState } from 'react'
import { SECTIONS } from '../story/sections'
import { StorySection } from '../story/StorySection'
import { getWorld } from './api'
import type { WorldResponse } from './types'
import { WorldEventItem } from './WorldEventItem'
import styles from './WorldSection.module.css'
import { toWorldResponse } from './worldShape'

type Props = {
  date: string
  // Only tests pass this; the client default is 10 seconds.
  timeoutMs?: number
}

// The last answer, tagged with its date: an answer for another date counts as
// "still loading", so a date change needs no state reset.
type Result = { date: string; data: WorldResponse | null }

const TITLE = SECTIONS.find((section) => section.id === 'world')?.title ?? ''

// "그날의 세계": the day's `normal` events in server order. `tragic` events
// and `day_media` are drawn by M3b2b, not here. Never shows the error
// details or the requested date.
export function WorldSection({ date, timeoutMs }: Props) {
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    // Answers after an abort (unmount, new date) are dropped.
    getWorld(date, controller.signal, timeoutMs).then(
      (data) => {
        if (controller.signal.aborted) return
        // A 200 whose body is not the world shape is an error, not a crash.
        setResult({ date, data: toWorldResponse(data) })
      },
      () => {
        if (controller.signal.aborted) return
        setResult({ date, data: null })
      },
    )
    return () => controller.abort()
  }, [date, timeoutMs])

  if (result?.date !== date) {
    return <StorySection id="world" title={TITLE} status="loading" />
  }
  if (result.data === null) {
    return <StorySection id="world" title={TITLE} status="error" />
  }
  const { events, notice } = result.data
  const normal = events.filter((event) => event.tone === 'normal')
  return (
    <StorySection id="world" title={TITLE} status="ready">
      {notice?.code === 'NO_EVENTS_FOUND' && (
        <p className={styles.notice}>
          이 날짜에 기록된 큰 사건을 찾지 못했습니다.
        </p>
      )}
      {normal.length > 0 && (
        <ul aria-label="사건 목록" className={styles.events}>
          {normal.map((event) => (
            <WorldEventItem key={event.id} event={event} />
          ))}
        </ul>
      )}
    </StorySection>
  )
}
