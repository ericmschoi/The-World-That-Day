import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StorySection } from './StorySection'

describe('StorySection', () => {
  it('shows a loading message and marks the section busy', () => {
    render(<StorySection id="world" title="그날의 세계" status="loading" />)
    const region = screen.getByRole('region', { name: '그날의 세계' })
    expect(region).toHaveAttribute('aria-busy', 'true')
    expect(region).toHaveTextContent('불러오는 중')
  })

  it('shows the empty message', () => {
    render(<StorySection id="world" title="그날의 세계" status="empty" />)
    expect(screen.getByText(/아직 준비 중/)).toBeInTheDocument()
    expect(screen.getByRole('region')).not.toHaveAttribute('aria-busy')
  })

  it('shows the error message as a status', () => {
    render(<StorySection id="world" title="그날의 세계" status="error" />)
    expect(screen.getByRole('status')).toHaveTextContent(
      '이 섹션을 불러오지 못했습니다',
    )
  })

  it('renders children only when ready', () => {
    const { rerender } = render(
      <StorySection id="city" title="그 도시의 풍경" status="empty">
        <p>준비된 내용</p>
      </StorySection>,
    )
    expect(screen.queryByText('준비된 내용')).not.toBeInTheDocument()
    rerender(
      <StorySection id="city" title="그 도시의 풍경" status="ready">
        <p>준비된 내용</p>
      </StorySection>,
    )
    expect(screen.getByText('준비된 내용')).toBeInTheDocument()
  })

  it('keeps a ready section visible next to a failed one', () => {
    render(
      <>
        <StorySection id="world" title="그날의 세계" status="error" />
        <StorySection id="city" title="그 도시의 풍경" status="ready">
          <p>도시 섹션 내용</p>
        </StorySection>
      </>,
    )
    expect(
      screen.getByRole('region', { name: '그날의 세계' }),
    ).toHaveTextContent('이 섹션을 불러오지 못했습니다')
    expect(
      screen.getByRole('region', { name: '그 도시의 풍경' }),
    ).toHaveTextContent('도시 섹션 내용')
  })
})
