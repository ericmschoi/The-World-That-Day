// Plan M3b3 T3: videos on the world screen. `day_media` counts only media
// MediaItem draws, a failed video takes its group heading with it when it
// was the last item, videos in `day_media` read "Archive video", and the sample
// date buttons. MSW only; demo dates, sample dates and synthetic fixtures.
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { agree } from '../test/consent'
import { sdkRoadview, videoEmbed } from '../mocks/fixtures/media/modes'
import { exampleVideoFile } from '../mocks/fixtures/world/media'
import {
  world1988WithMedia,
  world1988WithVideo,
  world1995,
  world2002WithMedia,
  worldWithTragic,
} from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import type { Media } from '../media/types'
import type { WorldResponse } from './types'
import { WorldSection } from './WorldSection'

const SAME_DAY = 'Photos taken that day'
const OTHER_TIME = 'Photos taken at other times'
const UNDATED = 'Photos with an unknown capture time'

const world = () => screen.getByRole('region', { name: 'The world that day' })

async function showWorld(body: WorldResponse) {
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))
  render(<WorldSection date="1988-09-17" />)
  await waitFor(() => expect(world()).not.toHaveAttribute('aria-busy'))
  expect(screen.queryByText("This section couldn't be loaded.")).toBeNull()
}

const headings = () =>
  within(world())
    .queryAllByRole('heading', { level: 3 })
    .map((heading) => heading.textContent)

// The block that a subheading opens.
const block = (title: string) =>
  within(world()).getByRole('heading', { level: 3, name: title }).parentElement!

const failLastSource = (video: Element) => {
  const sources = video.querySelectorAll('source')
  fireEvent.error(sources[sources.length - 1])
}

const dayMedia = (media: Media[]): WorldResponse => ({
  ...world1988WithMedia,
  day_media: media,
})

const undatedVideo = world1988WithVideo.day_media[2]

describe('day_media counts only drawn media (T3 (1))', () => {
  it('has no heading when day_media holds only video_embed and sdk', async () => {
    await showWorld(dayMedia([videoEmbed, sdkRoadview]))
    expect(headings()).toEqual([])
  })

  it('keeps only the heading of the group with a photo', async () => {
    await showWorld(
      dayMedia([videoEmbed, world1988WithMedia.day_media[0], sdkRoadview]),
    )
    // videoEmbed and sdkRoadview are same_day/current: without canRender
    // they would open "Photos taken that day"/"other times" on their own.
    expect(headings()).toEqual([SAME_DAY])
    expect(within(block(SAME_DAY)).getAllByRole('figure')).toHaveLength(1)
  })
})

describe('a video that cannot play (T3 (2))', () => {
  it('takes its group heading when it was the only item', async () => {
    await showWorld(dayMedia([undatedVideo]))
    expect(headings()).toEqual([UNDATED])
    failLastSource(world().querySelector('video')!)
    expect(headings()).toEqual([])
    expect(world().querySelector('video')).toBeNull()
  })

  it('leaves the heading while the undated photo copy remains', async () => {
    await showWorld(world1988WithVideo)
    failLastSource(within(block(UNDATED)).getByLabelText('Archive video'))
    expect(headings()).toEqual([SAME_DAY, UNDATED])
    expect(within(block(UNDATED)).getAllByRole('figure')).toHaveLength(1)
    expect(within(block(UNDATED)).getByRole('img')).toBeInTheDocument()
  })

  it('leaves the event text, its photos and source line', async () => {
    await showWorld(world1988WithVideo)
    const [first] = within(
      screen.getByRole('list', { name: 'Events' }),
    ).getAllByRole('listitem')
    failLastSource(first.querySelector('video')!)
    expect(first.querySelector('video')).toBeNull()
    expect(first).toHaveTextContent('제24회 서울 하계 올림픽 개막.')
    expect(within(first).getAllByRole('img')).toHaveLength(2)
    expect(first.lastElementChild).toHaveTextContent('Source: 한국어 위키백과')
    expect(
      within(first).getByRole('link', { name: /“9월 17일”/ }),
    ).toBeInTheDocument()
  })
})

describe('world1988WithVideo on screen (T3 (3)·(5))', () => {
  it('puts the video after the two photos, between text and source line', async () => {
    await showWorld(world1988WithVideo)
    const [first] = within(
      screen.getByRole('list', { name: 'Events' }),
    ).getAllByRole('listitem')
    const order = [...first.children].map((child) =>
      child.tagName === 'FIGURE'
        ? child.firstElementChild!.tagName.toLowerCase()
        : child.tagName.toLowerCase(),
    )
    expect(order).toEqual(['p', 'img', 'img', 'video', 'p'])
    expect(first.lastElementChild).toHaveTextContent('한국어 위키백과')
  })

  it('captions the event video with 같은 달, its month and links', async () => {
    await showWorld(world1988WithVideo)
    const [first] = within(
      screen.getByRole('list', { name: 'Events' }),
    ).getAllByRole('listitem')
    const figure = first.querySelector('video')!.closest('figure')!
    const caption = figure.querySelector('figcaption')!
    expect(caption.querySelector('p')).toHaveTextContent(
      /^Same month\s+Taken September 1988$/,
    )
    expect(caption).not.toHaveTextContent('Same day')
    // The attribution names CC BY-SA 4.0, so there is no separate line.
    const lines = [...caption.querySelectorAll('p')].map((p) => p.textContent)
    expect(lines).toContain(exampleVideoFile.attribution)
    expect(lines.filter((line) => line === 'CC BY-SA 4.0')).toEqual([])
    expect(
      within(figure).getByRole('link', { name: /^Licence/ }),
    ).toBeInTheDocument()
    expect(
      within(figure).getByRole('link', { name: /^View original/ }),
    ).toBeInTheDocument()
    const sources = [...figure.querySelectorAll('source')]
    expect(sources.map((s) => s.getAttribute('src'))).toEqual(
      exampleVideoFile.video_sources.map((source) => source.url),
    )
    expect(sources.map((s) => s.getAttribute('type'))).toEqual([
      'video/webm',
      'video/webm',
    ])
  })

  it('keeps the three headings and puts the undated video under the last', async () => {
    await showWorld(world1988WithVideo)
    expect(headings()).toEqual([SAME_DAY, UNDATED])
    const undated = block(UNDATED)
    const video = within(undated).getByLabelText('Archive video')
    expect(video.tagName).toBe('VIDEO')
    expect(video.closest('figure')).toHaveTextContent('Capture time unknown')
    expect(video.closest('figure')).not.toHaveTextContent('Same day')
    // Photos in day_media still read "Archive photo".
    for (const img of within(world()).getAllByRole('img')) {
      if (!img.closest('li')) expect(img).toHaveAccessibleName('Archive photo')
    }
    expect(world().querySelectorAll('video')).toHaveLength(2)
  })

  it('puts a dated day_media video under "다른 때 찍힌 사진"', async () => {
    await showWorld(
      dayMedia([
        {
          ...exampleVideoFile,
          date_relation: { kind: 'same_month', years: 0 },
        },
      ]),
    )
    expect(headings()).toEqual([OTHER_TIME])
    expect(
      within(block(OTHER_TIME)).getByLabelText('Archive video'),
    ).toBeVisible()
  })
})

describe('no video where none was sent (T3 (5))', () => {
  it.each([
    ['world2002WithMedia', world2002WithMedia],
    ['world1995', world1995],
  ])('%s has no video or iframe and no error text', async (_name, body) => {
    await showWorld(body)
    expect(world().querySelectorAll('video, iframe')).toHaveLength(0)
    expect(world()).not.toHaveTextContent(
      /영상|오류|실패|video|error|fail|couldn't/i,
    )
  })

  it('draws no video in the tragic block', async () => {
    const [normal, tragic] = worldWithTragic.events
    await showWorld({
      ...worldWithTragic,
      events: [
        normal,
        {
          ...tragic,
          media: [
            ...tragic.media,
            {
              ...exampleVideoFile,
              date_relation: { kind: 'same_month', years: 0 },
            },
          ],
        },
      ],
    })
    const tragicBlock = block('Difficult events on the same day')
    expect(tragicBlock.querySelectorAll('img, iframe, video')).toHaveLength(0)
    expect(world().querySelectorAll('video')).toHaveLength(0)
  })
})

describe('sample date buttons (T3 (6))', () => {
  it.each([
    ['표본: 1969년 7월 20일로 보기', '1969-07-20'],
    ['표본: 1986년 9월 20일로 보기', '1986-09-20'],
  ])('%s asks the world API for %s', async (label, date) => {
    const asked: (string | null)[] = []
    server.use(
      http.get('/api/story/world', ({ request }) => {
        asked.push(new URL(request.url).searchParams.get('date'))
        return HttpResponse.json(world1995)
      }),
    )
    const user = userEvent.setup()
    render(<App />)
    await agree(user)
    await user.click(screen.getByRole('button', { name: label }))
    await screen.findByText('No major events were found for this date.')
    expect(asked).toEqual([date])
    expect(screen.getAllByRole('region')).toHaveLength(5)
  })
})
