// The privacy policy page (plan M3b3b T1 (1)~(4), (8), C7). The page is plain
// HTML, read here as text and parsed; its links are never followed.
import { describe, expect, it } from 'vitest'
import privacyHtml from '../../privacy.html?raw'
import * as urls from '../external/urls'
import {
  CONTACT_ISSUES_URL,
  GOOGLE_PRIVACY_URL,
  YOUTUBE_EMBED_ORIGIN,
  YOUTUBE_IFRAME_API_URL,
  YOUTUBE_TERMS_URL,
} from '../external/urls'

const page = new DOMParser().parseFromString(privacyHtml, 'text/html')
const text = (page.body.textContent ?? '').replace(/\s+/g, ' ')
const links = [...page.querySelectorAll('a')]
const linkTo = (href: string) => links.filter((a) => a.href === href)

describe('outside URLs (T1 (8))', () => {
  it('keeps the three outside links the policy needs', () => {
    expect(YOUTUBE_TERMS_URL).toBe('https://www.youtube.com/t/terms')
    expect(GOOGLE_PRIVACY_URL).toBe('https://policies.google.com/privacy')
    expect(CONTACT_ISSUES_URL).toBe(
      'https://github.com/ericmschoi/The-World-That-Day/issues',
    )
  })
})

describe('privacy policy page (T1 (1))', () => {
  it('is a Korean page titled 개인정보처리방침', () => {
    expect(page.documentElement.lang).toBe('ko')
    const headings = page.querySelectorAll('h1')
    expect(headings).toHaveLength(1)
    expect(headings[0].textContent).toBe('개인정보처리방침')
  })

  it.each([
    'YouTube API 서비스',
    '생년월일',
    '장소',
    '쿠키',
    '광고',
    '개인정보 보호 강화 모드',
    '이 서비스를 이용하면 YouTube 이용약관에 동의하게 됩니다.',
    '결과 화면 주소',
    '로그',
    '위키백과',
    'Wikidata',
    'Wikimedia Commons',
    'YouTube Data API',
    '시행일',
    '문의',
  ])('says %s', (phrase) => {
    expect(text).toContain(phrase)
  })

  // T1 (9): the sources match the merged server (application.yml, M3a3b).
  it('lists the outside sources the server calls now, and the one to come', () => {
    expect(text).toContain(
      '지금은 한국어 위키백과, Wikidata, Wikimedia Commons(사진·영상), YouTube Data API(KTV 아카이브 채널 영상 검색)입니다.',
    )
  })

  it('links the YouTube terms and the Google privacy policy', () => {
    expect(linkTo(YOUTUBE_TERMS_URL).map((a) => a.textContent)).toEqual([
      'YouTube 이용약관',
    ])
    expect(linkTo(GOOGLE_PRIVACY_URL).map((a) => a.textContent)).toEqual([
      'Google 개인정보처리방침',
    ])
  })
})

describe('facts the PM decided, word for word (T1 (2))', () => {
  it('names the operator', () => {
    expect(text).toContain('The World That Day 운영자')
  })

  it('takes questions through GitHub Issues, linked', () => {
    expect(text).toContain(
      '이 방침이나 개인정보에 관한 문의는 GitHub Issues로 받습니다.',
    )
    expect(linkTo(CONTACT_ISSUES_URL).map((a) => a.textContent)).toEqual([
      'GitHub Issues',
    ])
  })

  it('states the effective date as the merge day, with no other date', () => {
    expect(text).toContain('시행일: 2026년 10월 27일')
    expect(text).not.toContain('머지하는 날 넣음')
    expect(text.match(/\d{4}\s*년|\d{4}-\d{2}-\d{2}/g)).toEqual(['2026년'])
  })

  it('has no e-mail address or other contact', () => {
    expect(privacyHtml).not.toMatch(/@|mailto:|tel:/)
  })
})

describe('what the page loads (T1 (3)·(4))', () => {
  // M3b3c T1 (2): urls.ts also holds the two YouTube player URLs now, which
  // are not on the page. Every value must be one or the other, so a new value
  // added without a group fails here.
  const POLICY_LINKS = [
    YOUTUBE_TERMS_URL,
    GOOGLE_PRIVACY_URL,
    CONTACT_ISSUES_URL,
  ]
  const PLAYER_URLS = [YOUTUBE_IFRAME_API_URL, YOUTUBE_EMBED_ORIGIN]

  it('C7: its absolute URLs are exactly the policy links in urls.ts', () => {
    const inPage = new Set(privacyHtml.match(/https?:\/\/[^"<> ]+/g))
    expect(inPage).toEqual(new Set(POLICY_LINKS))
  })

  it('C7: every urls.ts value is a policy link or a player URL', () => {
    const known = new Set([...POLICY_LINKS, ...PLAYER_URLS])
    expect(Object.values(urls).filter((url) => !known.has(url))).toEqual([])
    expect(PLAYER_URLS).toEqual([
      'https://www.youtube.com/iframe_api',
      'https://www.youtube-nocookie.com',
    ])
    // The check itself: an unsorted value would be caught.
    const added = [...Object.values(urls), 'https://example.org/new']
    expect(added.filter((url) => !known.has(url))).toEqual([
      'https://example.org/new',
    ])
  })

  it('has no script, and one stylesheet: the tokenised privacy.css', () => {
    expect(privacyHtml).not.toMatch(/<script/i)
    const sheets = [...page.querySelectorAll('link[rel="stylesheet"]')]
    expect(sheets.map((link) => link.getAttribute('href'))).toEqual([
      '/src/privacy/privacy.css',
    ])
    expect(page.querySelectorAll('img, iframe, style, [style]')).toHaveLength(0)
    expect(page.querySelectorAll('link')).toHaveLength(1)
  })

  it('opens outside links in the same tab', () => {
    expect(links.length).toBeGreaterThan(0)
    expect(links.filter((link) => link.hasAttribute('target'))).toEqual([])
  })
})
