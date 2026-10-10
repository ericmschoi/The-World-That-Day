import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { StoryScreen } from './StoryScreen'

describe('StoryScreen', () => {
  it('renders the five story sections in brief order with stable ids', () => {
    render(<StoryScreen onRestart={() => {}} />)
    const regions = screen.getAllByRole('region')
    expect(regions).toHaveLength(5)
    expect(
      regions.map((region) => region.getAttribute('aria-labelledby')),
    ).toEqual(regions.map((region) => `${region.id}-heading`))
    expect(regions.map((region) => region.id)).toEqual([
      'opening',
      'neighborhood',
      'city',
      'world',
      'closing',
    ])
    regions.forEach((region, index) => {
      expect(region).toHaveAccessibleName(
        ['오프닝', '그날의 동네', '그 도시의 풍경', '그날의 세계', '마무리'][
          index
        ],
      )
      expect(region).toHaveTextContent('아직 준비 중')
    })
  })

  it('calls onRestart when the restart button is pressed', async () => {
    const onRestart = vi.fn()
    render(<StoryScreen onRestart={onRestart} />)
    await userEvent.click(screen.getByRole('button', { name: '처음으로' }))
    expect(onRestart).toHaveBeenCalledTimes(1)
  })
})
