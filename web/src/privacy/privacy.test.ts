// The privacy policy page (plan M3b3b T1 (1)~(4), (8), C7), in Canadian
// English since plan C1 S1 T3. The page is plain HTML, read here as text and
// parsed; its links are never followed.
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
  it('is an en-CA page titled Privacy policy', () => {
    expect(page.documentElement.lang).toBe('en-CA')
    expect(page.title).toBe('Privacy policy - The World That Day')
    const headings = page.querySelectorAll('h1')
    expect(headings).toHaveLength(1)
    expect(headings[0].textContent).toBe('Privacy policy')
  })

  // Plan C1 S1 T3 (2): the seven sections, in the Korean page's order.
  it('has the seven section headings in order', () => {
    expect([...page.querySelectorAll('h2')].map((h) => h.textContent)).toEqual([
      'What we receive',
      'How we use and share it',
      'Content from elsewhere and ads',
      'YouTube API Services',
      'Cookies and device storage',
      'Contact',
      'Effective date and changes',
    ])
  })

  it('has no Korean left anywhere on the page', () => {
    expect(privacyHtml).not.toMatch(/[ᄀ-ᇿ㄰-㆏가-힯]/)
  })

  it('says what it receives, as the plan words it', () => {
    expect(text).toContain(
      'This service receives the date of birth (a date) and place of birth that you enter. It does not receive accounts, names or contact details. Right now only the date is sent to the server; the place will be received once place input is added.',
    )
  })

  it('keeps the result address free of the date of birth and place', () => {
    expect(text).toContain(
      'The address of the result page never contains your date of birth or place.',
    )
  })

  it.each([
    'YouTube API Services',
    'date of birth',
    'place',
    'cookies',
    'ads',
    'privacy-enhanced mode',
    'By using it, you agree to be bound by the YouTube Terms of Service.',
    'address of the result page',
    'logs',
    'Wikipedia',
    'Wikidata',
    'Wikimedia Commons',
    'YouTube Data API',
    'Effective date',
    'Questions',
  ])('says %s', (phrase) => {
    expect(text).toContain(phrase)
  })

  // T1 (9): the sources match the merged server (application.yml, M3a5a,
  // which includes the YouTube Data API).
  it('lists the outside sources the server calls now', () => {
    expect(text).toContain(
      'Right now these are Korean Wikipedia, Wikidata, Wikimedia Commons (photos and videos) and YouTube Data API (video search on the KTV Archive channel).',
    )
  })

  it('links the YouTube terms and the Google privacy policy', () => {
    expect(linkTo(YOUTUBE_TERMS_URL).map((a) => a.textContent)).toEqual([
      'YouTube Terms of Service',
    ])
    expect(linkTo(GOOGLE_PRIVACY_URL).map((a) => a.textContent)).toEqual([
      'Google Privacy Policy',
    ])
  })
})

describe('facts the PM decided, word for word (T1 (2))', () => {
  it('names the operator', () => {
    expect(text).toContain('Operator: The World That Day operator')
  })

  it('takes questions through GitHub Issues, linked', () => {
    expect(text).toContain(
      'Questions or complaints: open an issue at GitHub Issues.',
    )
    expect(linkTo(CONTACT_ISSUES_URL).map((a) => a.textContent)).toEqual([
      'GitHub Issues',
    ])
  })

  it('states the effective date as the merge day, with no other date', () => {
    expect(text).toContain('Effective date: October 27, 2026')
    expect(text).not.toMatch(/Month D|YYYY/)
    expect(text.match(/\b\d{4}\b|\d{4}-\d{2}-\d{2}/g)).toEqual(['2026'])
  })

  it('has no e-mail address or other contact', () => {
    expect(privacyHtml).not.toMatch(/@|mailto:|tel:/)
  })
})

// Plan C1 S1 T3b: the drafts in docs/research/canada-privacy.md 10.1, one
// key phrase each, and nothing that goes beyond the current design.
describe('additions from the Canada privacy review (C1 S1 T3b)', () => {
  it.each([
    ['S-2 purpose', 'We do not use it for advertising or profiling.'],
    ['S-3 retention', 'Results are cached by date and event, not by person.'],
    ['S-4 outside sources', 'They are not told whose date it is.'],
    [
      'S-5 other countries',
      'Your information may be processed in other countries, where authorities may be able to access it.',
    ],
    ['S-6 access and contact', 'so there is nothing to access or correct.'],
    [
      'S-6 public issues',
      'Do not post personal information in a public issue.',
    ],
    ['S-6 responsible person', 'Responsible person: the operator.'],
  ])('%s', (_rule, sentence) => {
    expect(text).toContain(sentence)
  })

  it('puts the purpose first in "How we use and share it"', () => {
    const [first] = page.querySelectorAll('ul > li')
    expect(first.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'We use the information we receive only to find photos, videos and events for that day and place. We do not use it for advertising or profiling.',
    )
  })

  it('says outside sources get the date as a search term', () => {
    expect(text).toContain('Outside sources receive the date as a search term.')
    expect(text).not.toContain('whose date of birth it is')
  })

  it('names no place where Google processes data, and no log period', () => {
    expect(text).not.toMatch(/Google[^.]*servers in/)
    expect(text).not.toMatch(/\b(days|weeks|months)\b/)
  })

  it('has no age sentence yet (D4 waits for the PM, S2 T8b)', () => {
    expect(text).not.toMatch(/under 13|parent or guardian|general audience/)
  })

  // T3b (4): only the date goes to the server, nothing is stored on the
  // device, and there are no accounts.
  it('claims nothing beyond the current design', () => {
    expect(text).toContain('Right now only the date is sent to the server')
    expect(text).toContain(
      'This service does not use cookies or browser storage.',
    )
    expect(text).toContain('We have no accounts')
    expect(text).not.toMatch(/sign in|log in|your account|newsletter/i)
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
