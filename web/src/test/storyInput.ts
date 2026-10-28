import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'

// Demo pairs (product brief 12); sample dates of public events use 서울.
export type StoryInputValues = { date: string; place: string; time?: string }

export const PLACE_LABEL = '태어난 장소 (필수)'
export const TIME_LABEL = '출생 시각 (선택)'
export const SUBMIT_LABEL = '그날 보기'

export const yearBox = () => screen.getByRole('combobox', { name: '년' })
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
  const parts = { 년: year, 월: month, 일: day }
  for (const [name, value] of Object.entries(parts)) {
    await user.selectOptions(screen.getByRole('combobox', { name }), value)
  }
  await user.clear(placeBox())
  if (place !== '') await user.type(placeBox(), place)
  if (time) await user.type(screen.getByLabelText(TIME_LABEL), time)
}

export const submitStory = (user: UserEvent) => user.click(submitButton())

// From the input form to the story (plan M2b T3 (4)): fills the form and
// presses 그날 보기. Agreeing stays with the caller, as before.
export async function openStory(user: UserEvent, input: StoryInputValues) {
  await fillStory(user, input)
  await submitStory(user)
}
