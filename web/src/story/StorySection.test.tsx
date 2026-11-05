import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StorySection } from './StorySection'

describe('StorySection', () => {
  it('shows a loading message and marks the section busy', () => {
    render(
      <StorySection id="world" title="The world that day" status="loading" />,
    )
    const region = screen.getByRole('region', { name: 'The world that day' })
    expect(region).toHaveAttribute('aria-busy', 'true')
    expect(region).toHaveTextContent('Loading')
  })

  it('shows the empty message', () => {
    render(
      <StorySection id="world" title="The world that day" status="empty" />,
    )
    expect(screen.getByText(/coming soon/)).toBeInTheDocument()
    expect(screen.getByRole('region')).not.toHaveAttribute('aria-busy')
  })

  it('shows the error message as a status', () => {
    render(
      <StorySection id="world" title="The world that day" status="error" />,
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      "This section couldn't be loaded",
    )
  })

  it('renders children only when ready', () => {
    const { rerender } = render(
      <StorySection id="city" title="Scenes of the city" status="empty">
        <p>준비된 내용</p>
      </StorySection>,
    )
    expect(screen.queryByText('준비된 내용')).not.toBeInTheDocument()
    rerender(
      <StorySection id="city" title="Scenes of the city" status="ready">
        <p>준비된 내용</p>
      </StorySection>,
    )
    expect(screen.getByText('준비된 내용')).toBeInTheDocument()
  })

  it('accepts only story section ids (checked by typecheck)', () => {
    render(
      // @ts-expect-error 'weather' is not a SectionId
      <StorySection id="weather" title="날씨" status="empty" />,
    )
    expect(screen.getByRole('region')).toHaveAttribute('id', 'weather')
  })

  it('keeps a ready section visible next to a failed one', () => {
    render(
      <>
        <StorySection id="world" title="The world that day" status="error" />
        <StorySection id="city" title="Scenes of the city" status="ready">
          <p>도시 섹션 내용</p>
        </StorySection>
      </>,
    )
    expect(
      screen.getByRole('region', { name: 'The world that day' }),
    ).toHaveTextContent("This section couldn't be loaded")
    expect(
      screen.getByRole('region', { name: 'Scenes of the city' }),
    ).toHaveTextContent('도시 섹션 내용')
  })
})
