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
    render(<StoryScreen date="1995-03-15" onRestart={() => {}} />)
    // The world section loads its own content (MSW default handler).
    await screen.findByText('No major events were found for this date.')
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
        [
          'Opening',
          'The neighbourhood that day',
          'Scenes of the city',
          'The world that day',
          'Closing',
        ][index],
      )
      if (region.id === 'world') {
        expect(region).not.toHaveTextContent('coming soon')
      } else {
        expect(region).toHaveTextContent('coming soon')
      }
    })
  })

  it('calls onRestart when the restart button is pressed', async () => {
    const onRestart = vi.fn()
    render(<StoryScreen date="1995-03-15" onRestart={onRestart} />)
    await screen.findByText('No major events were found for this date.')
    await userEvent.click(screen.getByRole('button', { name: 'Start over' }))
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
    render(<StoryScreen date="1988-09-17" onRestart={() => {}} />)

    const list = await screen.findByRole('list', { name: 'Events' })

    expect(within(list).getAllByRole('listitem')).toHaveLength(3)
    const regions = screen.getAllByRole('region')
    expect(regions.map((region) => region.id)).toEqual([
      'opening',
      'neighborhood',
      'city',
      'world',
      'closing',
    ])
    for (const region of regions) {
      if (region.id !== 'world') {
        expect(region).toHaveTextContent('coming soon')
      }
    }
    expect(screen.getByRole('button', { name: 'Start over' })).toBeEnabled()
    for (const spy of spies) expect(spy).not.toHaveBeenCalled()
  })
})
