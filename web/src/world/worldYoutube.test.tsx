// Plan M3b3c T2 (4)·(7)·(9)·(12): YouTube cards on the world screen, under
// the day_media headings only after consent, the 1988 synthetic fixture,
// and no player on days without one. MSW only; demo and synthetic values.
import { act, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { ConsentContext } from '../consent/consent'
import { isDisplayable } from '../media/isDisplayable'
import type { Media } from '../media/types'
import {
  world1988WithMedia,
  world1995,
  world2002WithMedia,
} from '../mocks/fixtures/world/responses'
import {
  exampleDaehanNews,
  exampleYoutubeEmbed,
  exampleYoutubeUndated,
  world1988WithYoutube,
} from '../mocks/fixtures/world/youtube'
import { server } from '../mocks/server'
import {
  installYouTubeFake,
  youtubeScripts,
  type YouTubeFake,
} from '../test/youtubeFake'
import type { WorldResponse } from './types'
import { WorldSection } from './WorldSection'

const SAME_DAY = '그날 찍힌 사진'
const OTHER_TIME = '다른 때 찍힌 사진'
const UNDATED = '촬영 시점을 확인하지 못한 사진'

let fake: YouTubeFake | null = null

afterEach(() => {
  fake?.uninstall()
  fake = null
})

const world = () => screen.getByRole('region', { name: '그날의 세계' })

async function showWorld(body: WorldResponse, consented: boolean) {
  fake = installYouTubeFake()
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))
  render(
    <ConsentContext value={{ agreed: consented, setAgreed: () => {} }}>
      <WorldSection date="1988-09-17" />
    </ConsentContext>,
  )
  await waitFor(() => expect(world()).not.toHaveAttribute('aria-busy'))
  // Lets the API player attach.
  await act(async () => {})
}

const headings = () =>
  within(world())
    .queryAllByRole('heading', { level: 3 })
    .map((heading) => heading.textContent)

const block = (title: string) =>
  within(world()).getByRole('heading', { level: 3, name: title }).parentElement!

const dayMedia = (media: Media[]): WorldResponse => ({
  ...world1988WithMedia,
  day_media: media,
})

describe('day_media with YouTube (T2 (7))', () => {
  it('has no heading for a YouTube-only group before consent', async () => {
    await showWorld(dayMedia([exampleYoutubeUndated]), false)
    expect(headings()).toEqual([])
    expect(world().querySelectorAll('iframe')).toHaveLength(0)
  })

  it('shows the card under its heading after consent, titled 기록 영상', async () => {
    await showWorld(
      dayMedia([exampleYoutubeEmbed, exampleYoutubeUndated]),
      true,
    )
    expect(headings()).toEqual([OTHER_TIME, UNDATED])
    for (const title of [OTHER_TIME, UNDATED]) {
      const iframe = block(title).querySelector('iframe')!
      expect(iframe).toHaveAttribute('title', '기록 영상')
    }
  })

  it('takes the heading when its last card cannot play (T2 (4))', async () => {
    await showWorld(dayMedia([exampleYoutubeUndated]), true)
    expect(headings()).toEqual([UNDATED])
    const [player] = fake!.players
    fake!.ready(player)
    fake!.error(player, 101)
    expect(headings()).toEqual([])
    expect(world()).not.toHaveTextContent(/오류|실패|재생할 수 없/)
  })
})

describe('world1988WithYoutube on screen (T2 (9))', () => {
  it('puts Daehan News after event ①’s video, captioned in full', async () => {
    await showWorld(world1988WithYoutube, true)
    const [first, second] = within(
      screen.getByRole('list', { name: '사건 목록' }),
    ).getAllByRole('listitem')
    const order = [...first.children].map((child) =>
      child.tagName === 'FIGURE'
        ? child.firstElementChild!.tagName.toLowerCase()
        : child.tagName.toLowerCase(),
    )
    expect(order).toEqual(['p', 'img', 'img', 'video', 'iframe', 'p'])
    const daehan = first.querySelector('iframe')!.closest('figure')!
    expect(daehan.querySelector('iframe')).toHaveAttribute(
      'title',
      '「1988년 하계 올림픽」 관련 자료',
    )
    expect(daehan.querySelector('figcaption > p')).toHaveTextContent(
      /^같은 해 무렵 1988년 무렵 촬영\(추정\)$/,
    )
    expect(daehan).toHaveTextContent('대한뉴스 제1715호 (1988년 9월 17일 자)')
    expect(daehan).toHaveTextContent('대한뉴스(정부 제작 뉴스영화)')

    const undated = second.querySelector('iframe')!.closest('figure')!
    expect(undated.querySelector('figcaption > p')).toHaveTextContent(
      /^촬영 시점 미상$/,
    )
    expect(undated).not.toHaveTextContent('같은 날')
    expect(world().querySelectorAll('video')).toHaveLength(2)
    expect(world().querySelectorAll('iframe')).toHaveLength(2)
    expect(fake!.players).toHaveLength(2)
  })

  it('shows the Commons videos but no YouTube before consent', async () => {
    await showWorld(world1988WithYoutube, false)
    expect(world().querySelectorAll('video')).toHaveLength(2)
    expect(world().querySelectorAll('iframe')).toHaveLength(0)
    expect(headings()).toEqual([SAME_DAY, UNDATED])
  })
})

describe('days without YouTube (demo table)', () => {
  it.each([
    ['world2002WithMedia', world2002WithMedia],
    ['world1995', world1995],
  ])('%s: no iframe, no script, no error text', async (_name, body) => {
    await showWorld(body, true)
    expect(world().querySelectorAll('iframe')).toHaveLength(0)
    expect(youtubeScripts()).toHaveLength(0)
    expect(fake!.players).toHaveLength(0)
    expect(world()).not.toHaveTextContent(/오류|실패/)
  })
})

describe('youtube.ts fixtures (T2 (12))', () => {
  it.each([exampleYoutubeEmbed, exampleDaehanNews, exampleYoutubeUndated])(
    '$id is displayable',
    (media) => {
      expect(isDisplayable(media)).toBe(true)
    },
  )

  it('keeps every id made up and every embed on the nocookie player', () => {
    for (const media of [
      exampleYoutubeEmbed,
      exampleDaehanNews,
      exampleYoutubeUndated,
    ]) {
      expect(media.id).toMatch(/^youtube-.*xyz/)
      expect(media.embed_url).toMatch(
        /^https:\/\/www\.youtube-nocookie\.com\/embed\/Abc123xyz_0\?/,
      )
    }
  })
})
