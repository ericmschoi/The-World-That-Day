import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'

// Demo pairs (Canada research decision 1 (b)); sample dates of public events
// and boundary dates use Calgary.
export type StoryInputValues = { date: string; place: string; time?: string }

export const PLACE_LABEL = 'Place of birth (required)'
export const TIME_LABEL = 'Time of birth (optional)'
export const SUBMIT_LABEL = 'See that day'

export const yearBox = () => screen.getByRole('combobox', { name: 'Year' })
export const placeBox = () => screen.getByRole('textbox', { name: PLACE_LABEL })
export const submitButton = () =>
  screen.getByRole('button', { name: SUBMIT_LABEL })

// Fills the input form; an empty date part or place leaves that box empty.
export async function fillStory(
  user: UserEvent,
  { date, place, time }: StoryInputValues,
) {
  const [year = '', month = '', day = ''] = date
    .split('-')
    .map((part) => (part === '' ? '' : String(Number(part))))
  const parts = { Year: year, Month: month, Day: day }
  for (const [name, value] of Object.entries(parts)) {
    await user.selectOptions(screen.getByRole('combobox', { name }), value)
  }
  await user.clear(placeBox())
  if (place !== '') await user.type(placeBox(), place)
  if (time) await user.type(screen.getByLabelText(TIME_LABEL), time)
}

export const submitStory = (user: UserEvent) => user.click(submitButton())

// From the input form to the story (plan M2b T3 (4)): fills the form and
// presses See that day. Agreeing stays with the caller, as before.
export async function openStory(user: UserEvent, input: StoryInputValues) {
  await fillStory(user, input)
  await submitStory(user)
}
