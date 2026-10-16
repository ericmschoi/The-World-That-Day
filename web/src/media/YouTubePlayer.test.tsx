// Plan M3b3c T2: the YouTube card, attaching the API player once, hiding a
// video that cannot play, and drawing nothing before consent. jsdom never
// loads the iframe or the script; a fake `YT` stands in for the API.
import { act, render, screen } from '@testing-library/react'
import { StrictMode, type ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ConsentContext } from '../consent/consent'
import { ConsentProvider } from '../consent/ConsentProvider'
import { sameDayPhoto, undatedPhoto } from '../mocks/fixtures/media/photos'
import { videoFile } from '../mocks/fixtures/media/modes'
import {
  exampleDaehanNews,
  exampleYoutubeEmbed,
  exampleYoutubeUndated,
} from '../mocks/fixtures/world/youtube'
import {
  installYouTubeFake,
  youtubeScripts,
  type YouTubeFake,
} from '../test/youtubeFake'
import cardCss from './MediaCard.module.css?raw'
import playerSource from './YouTubePlayer.tsx?raw'
import { canRender } from './isDisplayable'
import { MediaItem } from './MediaItem'
import type { Media } from './types'

const ALT = '「1988년 하계 올림픽」 관련 자료'
const ERROR_TEXT = /오류|실패|재생할 수 없/

let fake: YouTubeFake | null = null

afterEach(() => {
  fake?.uninstall()
  fake = null
  // A failed script stays in the page by design; take it out between tests.
  for (const script of youtubeScripts()) script.remove()
})

const agreed = { agreed: true, setAgreed: () => {} }
const withConsent = (ui: ReactNode) => (
  <ConsentContext value={agreed}>{ui}</ConsentContext>
)

const item = (media: Media, onUnavailable?: (id: string) => void) => (
  <MediaItem
    key={media.id}
    media={media}
    fallbackAlt={ALT}
    onUnavailable={onUnavailable}
  />
)

// Lets `loadYouTubeApi()` resolve and the player attach.
const settle = () => act(async () => {})

const iframes = () => document.querySelectorAll('iframe')

const patch = (media: Media, fields: Record<string, unknown>) =>
  ({ ...media, ...fields }) as Media

describe('the YouTube card (T2 (1)·(2))', () => {
  it('is a figure with the iframe first, then the caption', async () => {
    fake = installYouTubeFake()
    render(withConsent(item(exampleYoutubeEmbed)))
    await settle()
    const figure = screen.getByRole('figure')
    expect([...figure.children].map((child) => child.tagName)).toEqual([
      'IFRAME',
      'FIGCAPTION',
    ])
    const iframe = figure.querySelector('iframe')!
    expect(iframe.getAttribute('src')).toBe(exampleYoutubeEmbed.embed_url)
    expect(iframe).toHaveAttribute('title', ALT)
    expect(iframe.getAttribute('allow')?.split(/;\s*/)).toEqual(
      expect.arrayContaining([
        'autoplay',
        'encrypted-media',
        'picture-in-picture',
        'fullscreen',
      ]),
    )
    expect(iframe).toHaveAttribute('allowfullscreen')
    expect(iframe).toHaveAttribute('loading', 'lazy')
    expect(iframe).toHaveAttribute(
      'referrerpolicy',
      'strict-origin-when-cross-origin',
    )
    expect(iframe).not.toHaveAttribute('sandbox')
    // Nothing inside or over the player.
    expect(iframe.children).toHaveLength(0)
  })

  // CSS Modules are blank in Vitest, so the rules are read as text.
  const rule = (selector: string) =>
    new RegExp(`(^|\\n)\\${selector} \\{([^}]*)\\}`).exec(cardCss)?.[2] ?? ''

  it('keeps .embed at least 200×200, 16:9, with nothing positioned', () => {
    const embed = rule('.embed')
    expect(embed).toMatch(/display: block;/)
    expect(embed).toMatch(/aspect-ratio: 16 \/ 9;/)
    expect(embed).toMatch(/min-width: 200px;/)
    expect(embed).toMatch(/min-height: 200px;/)
    expect(embed).toMatch(/border: 0;/)
    expect(embed).toMatch(/width: calc\(100% - 2 \* var\(--space-2\)\);/)
    for (const selector of ['.embed', '.card']) {
      expect(rule(selector)).not.toBe('')
      expect(rule(selector)).not.toMatch(/position:\s*(absolute|fixed)/)
    }
  })
})

describe('attaching the API player (T2 (3))', () => {
  it('makes one YT.Player for the iframe, even under StrictMode', async () => {
    fake = installYouTubeFake()
    render(<StrictMode>{withConsent(item(exampleYoutubeEmbed))}</StrictMode>)
    await settle()
    const iframe = iframes()[0]
    expect(fake.players).toHaveLength(1)
    expect(fake.playersFor(iframe)).toHaveLength(1)
    expect(youtubeScripts()).toHaveLength(0)
  })

  it('ignores events after unmount and never calls destroy', async () => {
    fake = installYouTubeFake()
    const onUnavailable = vi.fn()
    const errors = vi.spyOn(console, 'error')
    const { unmount } = render(
      withConsent(item(exampleYoutubeEmbed, onUnavailable)),
    )
    await settle()
    const [player] = fake.players
    unmount()
    fake.ready(player)
    fake.error(player, 150)
    expect(onUnavailable).not.toHaveBeenCalled()
    expect(errors).not.toHaveBeenCalled()
    expect(fake.calls.map((call) => call.method)).not.toContain('destroy')
    errors.mockRestore()
  })
})

describe('a video that cannot play (T2 (4))', () => {
  it.each([2, 5, 100, 101, 150, 153])(
    'error %i hides the card and its caption, once',
    async (code) => {
      fake = installYouTubeFake()
      const onUnavailable = vi.fn()
      render(
        withConsent(
          <>
            {item(sameDayPhoto)}
            {item(exampleYoutubeEmbed, onUnavailable)}
          </>,
        ),
      )
      await settle()
      const [player] = fake.players
      fake.ready(player)
      fake.error(player, code)
      fake.error(player, code)
      expect(iframes()).toHaveLength(0)
      expect(screen.queryByText('KTV 아카이브 · YouTube')).toBeNull()
      expect(onUnavailable).toHaveBeenCalledTimes(1)
      expect(onUnavailable).toHaveBeenCalledWith(exampleYoutubeEmbed.id)
      // The photo card is still there, and no error text anywhere.
      expect(screen.getAllByRole('figure')).toHaveLength(1)
      expect(screen.getByRole('img')).toBeInTheDocument()
      expect(document.body).not.toHaveTextContent(ERROR_TEXT)
    },
  )
})

describe('canRender for video_embed (T2 (5))', () => {
  const url = (value: string) =>
    patch(exampleYoutubeEmbed, { embed_url: value })
  const NOCOOKIE = 'https://www.youtube-nocookie.com/embed/Abc123xyz_0'

  it('is true only with consent and the privacy-enhanced player URL', () => {
    expect(canRender(exampleYoutubeEmbed, true)).toBe(true)
    expect(canRender(exampleDaehanNews, true)).toBe(true)
    expect(canRender(exampleYoutubeUndated, true)).toBe(true)
  })

  it.each([
    ['no consent', exampleYoutubeEmbed, false],
    [
      'provider "example"',
      patch(exampleYoutubeEmbed, { provider: 'example' }),
      true,
    ],
    [
      'origin www.youtube.com',
      url('https://www.youtube.com/embed/Abc123xyz_0?enablejsapi=1'),
      true,
    ],
    [
      'http nocookie',
      url('http://www.youtube-nocookie.com/embed/Abc123xyz_0?enablejsapi=1'),
      true,
    ],
    [
      'not under /embed/',
      url('https://www.youtube-nocookie.com/watch?v=Abc123xyz_0&enablejsapi=1'),
      true,
    ],
    ['no enablejsapi', url(`${NOCOOKIE}?rel=0`), true],
    ['enablejsapi=0', url(`${NOCOOKIE}?enablejsapi=0`), true],
    ['autoplay=1', url(`${NOCOOKIE}?enablejsapi=1&autoplay=1`), true],
    [
      'an unreadable URL',
      url('https://[youtube-nocookie/embed/x?enablejsapi=1'),
      true,
    ],
  ])(
    'is false with %s, and MediaItem draws nothing',
    async (_case, media, consented) => {
      fake = installYouTubeFake()
      expect(canRender(media, consented)).toBe(false)
      const ui = item(media)
      const { container } = render(consented ? withConsent(ui) : ui)
      await settle()
      expect(container).toBeEmptyDOMElement()
      expect(fake.players).toHaveLength(0)
    },
  )

  it('leaves image and video_file as they were, consent or not', () => {
    for (const media of [sameDayPhoto, undatedPhoto, videoFile]) {
      expect(canRender(media, false)).toBe(true)
      expect(canRender(media, true)).toBe(true)
    }
  })
})

describe('before consent (T2 (6))', () => {
  it.each([
    ['outside any provider', (ui: ReactNode) => ui],
    [
      'inside the provider, not ticked',
      (ui: ReactNode) => <ConsentProvider>{ui}</ConsentProvider>,
    ],
  ])('%s: no iframe, no script, no player', async (_case, wrap) => {
    render(wrap(item(exampleYoutubeEmbed)))
    await settle()
    expect(iframes()).toHaveLength(0)
    expect(document.querySelectorAll('script[src*="iframe_api"]')).toHaveLength(
      0,
    )
    // With the API already there, still no player.
    fake = installYouTubeFake()
    render(wrap(item(exampleDaehanNews)))
    await settle()
    expect(iframes()).toHaveLength(0)
    expect(fake.players).toHaveLength(0)
  })
})

describe('when the API script fails (T2 (8), decision 1 (a))', () => {
  it('keeps the official player as is, quietly', async () => {
    render(withConsent(item(exampleYoutubeEmbed)))
    expect(youtubeScripts()).toHaveLength(1)
    act(() => {
      youtubeScripts()[0].dispatchEvent(new Event('error'))
    })
    await settle()
    expect(iframes()).toHaveLength(1)
    expect(screen.getByRole('figure')).toContainElement(iframes()[0])
    expect(window.YT).toBeUndefined()
    expect(document.body).not.toHaveTextContent(ERROR_TEXT)
  })

  it('has no timer that could hide the card later', () => {
    expect(playerSource).not.toMatch(/setTimeout|setInterval/)
  })
})

describe('captions (T2 (9))', () => {
  const caption = async (media: Media) => {
    fake = installYouTubeFake()
    render(withConsent(item(media)))
    await settle()
    return screen.getByRole('figure').querySelector('figcaption')!
  }
  const SOURCE_LINK = /^원문 보기\s*\(새 창에서 열림\)$/
  const TERMS_LINK = /^이용약관\s*\(새 창에서 열림\)$/

  it('1986 sample: 같은 해, the attribution, provider terms and links', async () => {
    const figcaption = await caption(exampleYoutubeEmbed)
    expect(figcaption.firstElementChild).toHaveTextContent(
      /^같은 해 1986년 촬영$/,
    )
    expect(figcaption).toHaveTextContent('시험용 개막식 영상 설명')
    expect(screen.getByText('KTV 아카이브 · YouTube')).toBeInTheDocument()
    expect(screen.getByText('제공사 이용약관에 따름')).toBeInTheDocument()
    expect(figcaption).not.toHaveTextContent('provider-terms')
    expect(screen.getByRole('link', { name: SOURCE_LINK })).toHaveAttribute(
      'href',
      exampleYoutubeEmbed.source_url,
    )
    expect(screen.getByRole('link', { name: TERMS_LINK })).toHaveAttribute(
      'href',
      'https://www.youtube.com/t/terms',
    )
  })

  it('1988 Daehan News: the whole attribution and 같은 해 무렵', async () => {
    const figcaption = await caption(exampleDaehanNews)
    expect(figcaption.firstElementChild).toHaveTextContent(
      /^같은 해 무렵 1988년 무렵 촬영\(추정\)$/,
    )
    expect(screen.getByText(exampleDaehanNews.attribution)).toHaveTextContent(
      /^대한뉴스 제1715호 \(1988년 9월 17일 자\) · 대한뉴스\(정부 제작 뉴스영화\) · KTV 아카이브 · YouTube$/,
    )
  })

  it('unknown capture time: 촬영 시점 미상, never 같은 날', async () => {
    const figcaption = await caption(exampleYoutubeUndated)
    expect(figcaption.firstElementChild).toHaveTextContent(/^촬영 시점 미상$/)
    expect(figcaption).not.toHaveTextContent('같은 날')
  })
})
