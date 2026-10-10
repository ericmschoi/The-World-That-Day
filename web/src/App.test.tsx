import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('switches between the input placeholder and the empty story', async () => {
    const user = userEvent.setup()
    render(<App />)

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { level: 1, name: 'The World That Day' }),
    ).toBeInTheDocument()
    expect(screen.queryAllByRole('region')).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: '빈 결과 화면 보기' }))
    expect(screen.getAllByRole('region')).toHaveLength(5)

    await user.click(screen.getByRole('button', { name: '처음으로' }))
    expect(screen.queryAllByRole('region')).toHaveLength(0)
    expect(
      screen.getByRole('button', { name: '빈 결과 화면 보기' }),
    ).toBeInTheDocument()
  })

  it('works with the keyboard alone', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.tab()
    expect(
      screen.getByRole('button', { name: '빈 결과 화면 보기' }),
    ).toHaveFocus()
    await user.keyboard('{Enter}')
    expect(screen.getAllByRole('region')).toHaveLength(5)
  })
})
