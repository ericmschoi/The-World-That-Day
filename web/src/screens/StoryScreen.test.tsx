import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { worldMalformedEvents } from '../mocks/fixtures/world/malformed'
import { server } from '../mocks/server'
import { StoryScreen } from './StoryScreen'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('StoryScreen', () => {
  it('renders the five story sections in brief order with stable ids', async () => {
    render(
      <StoryScreen
        date="1995-03-15"
        place="서울대학교병원"
        onRestart={() => {}}
      />,
    )
    // The world section loads its own content (MSW default handler).
    await screen.findByText('이 날짜에 기록된 큰 사건을 찾지 못했습니다.')
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
      // M2b T4: the opening is filled too.
      if (region.id === 'world' || region.id === 'opening') {
        expect(region).not.toHaveTextContent('아직 준비 중')
      } else {
        expect(region).toHaveTextContent('아직 준비 중')
      }
    })
  })

  it('calls onRestart when the restart button is pressed', async () => {
    const onRestart = vi.fn()
    render(
      <StoryScreen
        date="1995-03-15"
        place="서울대학교병원"
        onRestart={onRestart}
      />,
    )
    await screen.findByText('이 날짜에 기록된 큰 사건을 찾지 못했습니다.')
    await userEvent.click(screen.getByRole('button', { name: '처음으로' }))
    expect(onRestart).toHaveBeenCalledTimes(1)
  })
})

describe('StoryScreen with a malformed world answer (M3b2b T0 (4))', () => {
  it('keeps the other four sections and the restart button', async () => {
    const spies = [vi.spyOn(console, 'error'), vi.spyOn(console, 'warn')]
    server.use(
      http.get('/api/story/world', () =>
        HttpResponse.json(worldMalformedEvents),
      ),
    )
    render(<StoryScreen date="1988-09-17" place="서울" onRestart={() => {}} />)

    const list = await screen.findByRole('list', { name: '사건 목록' })

    expect(within(list).getAllByRole('listitem')).toHaveLength(3)
    const regions = screen.getAllByRole('region')
    expect(regions.map((region) => region.id)).toEqual([
      'opening',
      'neighborhood',
      'city',
      'world',
      'closing',
    ])
    // M2b T4: the opening is filled too.
    for (const region of regions) {
      if (region.id !== 'world' && region.id !== 'opening') {
        expect(region).toHaveTextContent('아직 준비 중')
      }
    }
    expect(screen.getByRole('button', { name: '처음으로' })).toBeEnabled()
    for (const spy of spies) expect(spy).not.toHaveBeenCalled()
  })
})
