import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  installIntersectionObserverFake,
  type IntersectionObserverFake,
} from '../test/mediaFakes'
import { useInView } from './useInView'

function Probe() {
  const [ref, inView] = useInView<HTMLDivElement>()
  return (
    <div ref={ref} data-testid="target">
      {inView ? 'in view' : 'out of view'}
    </div>
  )
}

let fake: IntersectionObserverFake | null = null

afterEach(() => {
  fake?.uninstall()
  fake = null
  vi.restoreAllMocks()
})

describe('useInView', () => {
  it('is in view only while more than half is visible', () => {
    fake = installIntersectionObserverFake()
    render(<Probe />)
    const target = screen.getByTestId('target')
    expect(target).toHaveTextContent('out of view')

    fake.setRatio(target, 0.5)
    expect(target).toHaveTextContent('out of view')
    fake.setRatio(target, 0.51)
    expect(target).toHaveTextContent('in view')
    fake.setRatio(target, 0.5)
    expect(target).toHaveTextContent('out of view')
  })

  it('observes the element with a threshold above 0.5', () => {
    fake = installIntersectionObserverFake()
    render(<Probe />)
    expect(fake.observers).toHaveLength(1)
    const [observer] = fake.observers
    expect([...observer.targets]).toEqual([screen.getByTestId('target')])
    expect(observer.thresholds).toContain(0.5)
    expect(observer.thresholds.some((value) => value > 0.5)).toBe(true)
  })

  it('disconnects the observer on unmount', () => {
    fake = installIntersectionObserverFake()
    const { unmount } = render(<Probe />)
    const [observer] = fake.observers
    expect(observer.disconnected).toBe(false)
    unmount()
    expect(observer.disconnected).toBe(true)
  })

  it('stays out of view, quietly, without IntersectionObserver', () => {
    expect(globalThis.IntersectionObserver).toBeUndefined()
    const spies = [
      vi.spyOn(console, 'error'),
      vi.spyOn(console, 'warn'),
      vi.spyOn(console, 'log'),
    ]
    render(<Probe />)
    expect(screen.getByTestId('target')).toHaveTextContent('out of view')
    for (const spy of spies) expect(spy).not.toHaveBeenCalled()
  })
})
