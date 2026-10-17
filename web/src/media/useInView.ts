import { useCallback, useEffect, useEffectEvent, useState } from 'react'

// "In view" means more than half of the element is visible.
const VISIBLE_RATIO = 0.5
// The observer only calls back when a threshold is crossed. 0.51 makes the
// call arrive as soon as the ratio goes past 0.5, and 0.5 as soon as it drops
// back; 0 covers leaving the screen entirely.
export const IN_VIEW_THRESHOLDS = [0, VISIBLE_RATIO, 0.51]

type Seen = { target: Element; inView: boolean }

// Returns a callback ref for the element and whether it is in view. Without
// `IntersectionObserver` the element is never in view (so no autoplay).
// `onReport` hears every report, even one that changes nothing (two
// "out of view" in a row), which the returned boolean cannot show.
// `onScreen` hears, before it, whether any of the element is visible.
export function useInView<T extends Element>(
  onReport?: (inView: boolean) => void,
  onScreen?: (visible: boolean) => void,
): [(node: T | null) => void, boolean] {
  const [node, setNode] = useState<T | null>(null)
  const [seen, setSeen] = useState<Seen | null>(null)
  const ref = useCallback((next: T | null) => setNode(next), [])
  const report = useEffectEvent((inView: boolean, ratio: number) => {
    onScreen?.(ratio > 0)
    onReport?.(inView)
  })

  useEffect(() => {
    if (node === null || typeof IntersectionObserver === 'undefined') return
    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries.findLast((e) => e.target === node)
        if (entry) {
          const inView = entry.intersectionRatio > VISIBLE_RATIO
          setSeen({ target: node, inView })
          report(inView, entry.intersectionRatio)
        }
      },
      { threshold: IN_VIEW_THRESHOLDS },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [node])

  // A result for an earlier element does not count for the current one.
  return [ref, seen !== null && seen.target === node && seen.inView]
}
