// Plan M2b T3 (1)·(2): the input form. Dates and places are the demo pairs
// (product brief 12) and boundary values only.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { CONSENT_HINT } from '../consent/consent'
import { ConsentProvider } from '../consent/ConsentProvider'
import { DATE_MESSAGES } from '../input/dateRules'
import { PLACE_MESSAGES } from '../input/placeRules'
import { agree, consentBox } from '../test/consent'
import {
  fillStory,
  placeBox,
  submitButton,
  submitStory,
  TIME_LABEL,
  yearBox,
} from '../test/storyInput'
import { InputScreen } from './InputScreen'

function setup() {
  const onSubmit = vi.fn()
  const user = userEvent.setup()
  render(
    <ConsentProvider>
      <InputScreen onSubmit={onSubmit} />
    </ConsentProvider>,
  )
  return { onSubmit, user }
}

describe('InputScreen', () => {
  it('lays out the form as the plan table says', () => {
    setup()
    const heading = screen.getByRole('heading', { level: 1 })
    expect(heading).toHaveTextContent('The World That Day')
    expect(heading).toHaveAttribute('tabindex', '-1')
    const form = submitButton().closest('form')!
    expect(form).toHaveAttribute('novalidate')
    expect(form).toHaveAttribute('autocomplete', 'off')
    expect(screen.getByRole('group', { name: '생년월일 (필수)' })).toBeVisible()
    expect(placeBox()).toHaveAccessibleDescription(
      '예: 서울, 광주 서구 풍암동, 서울대학교병원',
    )
    const time = screen.getByLabelText(TIME_LABEL)
    expect(time).toHaveAttribute('type', 'time')
    expect(time).toHaveAccessibleDescription(
      '오프닝 문구에만 씁니다. 서버로 보내지 않습니다. 시와 분을 모두 넣어야 반영됩니다.',
    )
    expect(submitButton()).toHaveAttribute('type', 'submit')
    expect(submitButton()).not.toHaveAttribute('disabled')
    // The consent sits right before the submit button.
    expect(consentBox().compareDocumentPosition(submitButton())).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    )
  })

  it('an empty submit focuses the year box and shows three messages', async () => {
    const { onSubmit, user } = setup()
    await submitStory(user)
    expect(yearBox()).toHaveFocus()
    expect(yearBox()).toHaveAccessibleDescription(DATE_MESSAGES.DATE_REQUIRED)
    expect(placeBox()).toHaveAttribute('aria-invalid', 'true')
    expect(placeBox()).toHaveAccessibleDescription(
      `${PLACE_MESSAGES.PLACE_REQUIRED} 예: 서울, 광주 서구 풍암동, 서울대학교병원`,
    )
    expect(screen.getByText(CONSENT_HINT)).toBeVisible()
    expect(consentBox()).toHaveAccessibleDescription(CONSENT_HINT)
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('refuses February 30 with DATE_NOT_EXIST', async () => {
    const { onSubmit, user } = setup()
    await agree(user)
    await fillStory(user, { date: '1988-02-30', place: '서울' })
    await submitStory(user)
    expect(yearBox()).toHaveFocus()
    expect(screen.getByText(DATE_MESSAGES.DATE_NOT_EXIST)).toBeVisible()
    expect(placeBox()).not.toHaveAttribute('aria-invalid')
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('refuses a 51-character place with PLACE_TOO_LONG', async () => {
    const { onSubmit, user } = setup()
    await agree(user)
    await fillStory(user, { date: '1988-09-17', place: '가'.repeat(51) })
    await submitStory(user)
    expect(placeBox()).toHaveFocus()
    expect(placeBox()).toHaveAccessibleDescription(
      new RegExp(`^${PLACE_MESSAGES.PLACE_TOO_LONG}`),
    )
    expect(yearBox()).not.toHaveAttribute('aria-invalid')
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('without consent shows the hint and focuses the consent box', async () => {
    const { onSubmit, user } = setup()
    await fillStory(user, { date: '1988-09-17', place: '서울' })
    await submitStory(user)
    expect(consentBox()).toHaveFocus()
    expect(consentBox()).toHaveAccessibleDescription(CONSENT_HINT)
    expect(screen.queryByText(DATE_MESSAGES.DATE_REQUIRED)).toBeNull()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it.each([
    [{ date: '1988-09-17', place: '서울' }, undefined],
    [
      { date: '2002-06-22', place: ' 광주 서구 풍암동 ', time: '15:20' },
      '15:20',
    ],
    [{ date: '1995-03-15', place: '서울대학교병원', time: '00:05' }, '00:05'],
  ])('hands over %o when all is right', async (input, time) => {
    const { onSubmit, user } = setup()
    await fillStory(user, input)
    await agree(user)
    await submitStory(user)
    expect(onSubmit).toHaveBeenCalledTimes(1)
    expect(onSubmit).toHaveBeenCalledWith({
      date: input.date,
      place: input.place.trim(),
      time,
    })
  })

  it('submits with Enter in the place box too', async () => {
    const { onSubmit, user } = setup()
    await agree(user)
    await fillStory(user, { date: '1988-09-17', place: '서울' })
    await user.type(placeBox(), '{Enter}')
    expect(onSubmit).toHaveBeenCalledWith({
      date: '1988-09-17',
      place: '서울',
      time: undefined,
    })
  })

  // PM decision (C): a half-filled time is "no time", not an error.
  it('submits a partial time as no time, with the hint on the field', async () => {
    const { onSubmit, user } = setup()
    const time = screen.getByLabelText(TIME_LABEL)
    expect(time).toHaveAccessibleDescription(
      /시와 분을 모두 넣어야 반영됩니다\.$/,
    )
    await agree(user)
    await fillStory(user, { date: '1988-09-17', place: '서울' })
    // Hour only: the browser reports an empty value, as here.
    await user.type(time, '15')
    expect(time).toHaveValue('')
    await submitStory(user)
    expect(time).not.toHaveAttribute('aria-invalid')
    expect(onSubmit).toHaveBeenCalledWith({
      date: '1988-09-17',
      place: '서울',
      time: undefined,
    })
  })
})
