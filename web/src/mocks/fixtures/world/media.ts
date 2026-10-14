import type { Media } from '../../../media/types'

// Stored media records copied value for value from
// data/schema/examples/media/valid/ (M3a3). `src/world/worldFixtures.test.ts` compares
// them with those files. Responses add the API-only `date_relation`.
export type StoredMedia = Omit<Media, 'date_relation' | 'selection'>

// commons-1988-09-17-seoul-olympics-opening-pd.json
// Real Commons photo (public domain), captured 1988-09-17.
export const olympicsOpeningPd = {
  id: 'commons-4930021',
  type: 'photo',
  display_mode: 'image',
  url: 'https://upload.wikimedia.org/wikipedia/commons/1/18/1988_Summer_Olympics_opening_ceremony.JPEG',
  thumbnail_url:
    'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/18/1988_Summer_Olympics_opening_ceremony.JPEG/960px-1988_Summer_Olympics_opening_ceremony.JPEG',
  thumbnail_width: 640,
  thumbnail_height: 955,
  video_sources: null,
  duration_seconds: null,
  embed_url: null,
  provider: null,
  render: null,
  link_only_reason: null,
  source_name: 'Wikimedia Commons',
  source_url:
    'https://commons.wikimedia.org/wiki/File:1988_Summer_Olympics_opening_ceremony.JPEG',
  license: 'Public domain',
  license_status: 'free',
  license_url: null,
  attribution:
    'Ken Hackman, U.S. Air Force · Public domain · Wikimedia Commons',
  description:
    '제24회 하계 올림픽(1988년 서울) 개막식에서 화려한 의상을 입은 한국인이 깃발을 들고 있다',
  captured_at: '1988-09-17',
  date_precision: 'day',
  lat: null,
  lng: null,
  place_ref: 'country:KR',
  retrieved_at: '2026-10-13T23:51:32Z',
} satisfies StoredMedia

// commons-2002-06-22-gwangju-world-cup-kogl1.json
// Real Commons photo (KOGL Type 1), captured 2002-06-22.
export const worldCupKogl1 = {
  id: 'commons-129150318',
  type: 'photo',
  display_mode: 'image',
  url: 'https://upload.wikimedia.org/wikipedia/commons/c/cf/2002%EB%85%84_%ED%95%9C%C2%B7%EC%9D%BC_%EC%9B%94%EB%93%9C%EC%BB%B5-%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%C2%B7%EC%8A%A4%ED%8E%98%EC%9D%B8_8%EA%B0%95_%EA%B2%BD%EA%B8%B0-%EA%B2%BD%EA%B8%B0%EC%9E%A5_%EC%A0%84%EA%B2%BD.jpg',
  thumbnail_url:
    'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cf/2002%EB%85%84_%ED%95%9C%C2%B7%EC%9D%BC_%EC%9B%94%EB%93%9C%EC%BB%B5-%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%C2%B7%EC%8A%A4%ED%8E%98%EC%9D%B8_8%EA%B0%95_%EA%B2%BD%EA%B8%B0-%EA%B2%BD%EA%B8%B0%EC%9E%A5_%EC%A0%84%EA%B2%BD.jpg/960px-2002%EB%85%84_%ED%95%9C%C2%B7%EC%9D%BC_%EC%9B%94%EB%93%9C%EC%BB%B5-%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%C2%B7%EC%8A%A4%ED%8E%98%EC%9D%B8_8%EA%B0%95_%EA%B2%BD%EA%B8%B0-%EA%B2%BD%EA%B8%B0%EC%9E%A5_%EC%A0%84%EA%B2%BD.jpg',
  thumbnail_width: 640,
  thumbnail_height: 422,
  video_sources: null,
  duration_seconds: null,
  embed_url: null,
  provider: null,
  render: null,
  link_only_reason: null,
  source_name: 'Wikimedia Commons',
  source_url:
    'https://commons.wikimedia.org/wiki/File:2002%EB%85%84_%ED%95%9C%C2%B7%EC%9D%BC_%EC%9B%94%EB%93%9C%EC%BB%B5-%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%C2%B7%EC%8A%A4%ED%8E%98%EC%9D%B8_8%EA%B0%95_%EA%B2%BD%EA%B8%B0-%EA%B2%BD%EA%B8%B0%EC%9E%A5_%EC%A0%84%EA%B2%BD.jpg',
  license: 'KOGL Type 1',
  license_status: 'free',
  license_url: 'http://www.kogl.or.kr/info/licenseType1.do',
  attribution:
    '문덕관 기증 · 국립민속박물관 민속아카이브 · 공공누리 제1유형 출처 표시 (KOGL Type 1) · Wikimedia Commons',
  description:
    '2002년 한·일 월드컵 중 대한민국과 스페인의 8강 경기 중 촬영한 광주 월드컵 경기장 내부의 전경 및 관중들의 모습',
  captured_at: '2002-06-22',
  date_precision: 'day',
  lat: null,
  lng: null,
  place_ref: 'country:KR',
  retrieved_at: '2026-10-13T23:51:32Z',
} satisfies StoredMedia

// example-photo-capture-time-unknown.json
// Synthetic: capture time unknown (captured_at and date_precision null).
export const captureTimeUnknown = {
  id: 'example-photo-capture-time-unknown',
  type: 'photo',
  display_mode: 'image',
  url: 'https://media.example.org/files/example-photo-undated.jpg',
  thumbnail_url:
    'https://media.example.org/thumbs/640px-example-photo-undated.jpg',
  thumbnail_width: 640,
  thumbnail_height: 480,
  video_sources: null,
  duration_seconds: null,
  embed_url: null,
  provider: null,
  render: null,
  link_only_reason: null,
  source_name: 'Example Archive',
  source_url: 'https://archive.example.org/items/example-photo-undated',
  license: 'CC BY 4.0',
  license_status: 'free',
  license_url: 'https://creativecommons.org/licenses/by/4.0/',
  attribution: 'Example Photographer · CC BY 4.0 · Example Archive',
  description: '시험용 사진 설명 (촬영 시점 미상)',
  captured_at: null,
  date_precision: null,
  lat: null,
  lng: null,
  place_ref: 'world',
  retrieved_at: '2026-10-13T03:00:00Z',
} satisfies StoredMedia

// example-photo-license-unknown.json
// Synthetic: license unknown, captured 1988-09-17.
export const licenseUnknown = {
  id: 'example-photo-license-unknown',
  type: 'photo',
  display_mode: 'image',
  url: 'https://media.example.org/files/example-photo-1988.jpg',
  thumbnail_url:
    'https://media.example.org/thumbs/640px-example-photo-1988.jpg',
  thumbnail_width: 640,
  thumbnail_height: 427,
  video_sources: null,
  duration_seconds: null,
  embed_url: null,
  provider: null,
  render: null,
  link_only_reason: null,
  source_name: 'Example Archive',
  source_url: 'https://archive.example.org/items/example-photo-1988',
  license: null,
  license_status: 'unknown',
  license_url: null,
  attribution: '작가 정보 없음 · 라이선스 확인 안 됨 · Example Archive',
  description: '시험용 사진 설명 (라이선스 미확인)',
  captured_at: '1988-09-17',
  date_precision: 'day',
  lat: null,
  lng: null,
  place_ref: 'country:KR',
  retrieved_at: '2026-10-13T03:00:00Z',
} satisfies StoredMedia
