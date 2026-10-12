import type { Media } from '../../../media/types'
import { sameDayPhoto } from './photos'

// Synthetic records for the display modes other than `image` (ids start with
// `example-`). The cards for these come in M3b3 (iframe, video) and M4 (sdk).

const noMedia = {
  url: null,
  thumbnail_url: null,
  video_sources: null,
  duration_seconds: null,
  embed_url: null,
  provider: null,
  render: null,
  link_only_reason: null,
} satisfies Partial<Media>

export const imageEmbed: Media = {
  ...sameDayPhoto,
  ...noMedia,
  id: 'example-getty-0001',
  display_mode: 'image_embed',
  embed_url: 'https://embed.example.org/image/0001',
  provider: 'getty',
  source_name: 'Getty Images',
  source_url: 'https://images.example.org/detail/0001',
  license: 'provider-terms',
  license_status: 'other',
  license_url: null,
  attribution: 'Getty Images',
}

export const videoFile: Media = {
  ...sameDayPhoto,
  ...noMedia,
  id: 'example-commons-video-0001',
  type: 'video',
  display_mode: 'video_file',
  thumbnail_url: 'https://media.example.org/commons/thumb/Example_video.jpg',
  video_sources: [
    {
      url: 'https://media.example.org/commons/Example.480p.webm',
      mime: 'video/webm',
      height: 480,
    },
  ],
  duration_seconds: 42,
}

export const videoEmbed: Media = {
  ...sameDayPhoto,
  ...noMedia,
  id: 'example-youtube-0001',
  type: 'video',
  display_mode: 'video_embed',
  embed_url: 'https://player.example.org/embed/0001',
  provider: 'youtube',
  source_name: 'Example Official Channel',
  source_url: 'https://video.example.org/watch/0001',
  license: 'provider-terms',
  license_status: 'other',
  license_url: null,
  attribution: 'Example Official Channel',
}

export const sdkRoadview: Media = {
  ...sameDayPhoto,
  ...noMedia,
  id: 'example-kakao-roadview-0001',
  type: 'streetview',
  display_mode: 'sdk',
  provider: 'kakao',
  render: {
    provider: 'kakao',
    kind: 'roadview',
    lat: 37.58,
    lng: 127.0,
    zoom: 3,
  },
  captured_at: null,
  date_precision: 'current',
  date_relation: { kind: 'current', years: 0 },
}

export const linkOnly: Media = {
  ...sameDayPhoto,
  ...noMedia,
  id: 'example-link-only-0001',
  display_mode: 'link_only',
  link_only_reason: 'hotlink_blocked',
}
