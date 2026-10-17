// Every outside URL in our product code lives here (plan M3b3b, C1).
// web/privacy.html is plain HTML and cannot import this file, so a test checks
// that the page's absolute URLs are exactly the policy links (C7).
export const YOUTUBE_TERMS_URL = 'https://www.youtube.com/t/terms'
export const GOOGLE_PRIVACY_URL = 'https://policies.google.com/privacy'
export const CONTACT_ISSUES_URL =
  'https://github.com/ericmschoi/The-World-That-Day/issues'

// The YouTube player (plan M3b3c), not on the policy page. The IFrame API
// script, and the only origin a YouTube embed URL may have (privacy-enhanced
// mode, as the policy says).
export const YOUTUBE_IFRAME_API_URL = 'https://www.youtube.com/iframe_api'
export const YOUTUBE_EMBED_ORIGIN = 'https://www.youtube-nocookie.com'
