import sejongLogo from '@/assets/images/univlogo/세종대.png';
import yonseiLogo from '@/assets/images/univlogo/연세대.png';
import chungangLogo from '@/assets/images/univlogo/중앙대.png';
import seoultechLogo from '@/assets/images/univlogo/과기대.png';

/**
 * 4대 핵심 랜드마크 UI 메타데이터(DB destination_id에만 의존, 좌표는 DB에서 조회)
 */
export const MAJOR_LANDMARK_METADATA = {
  1: {
    id: 1,
    name: '세종대학교',
    shortName: '세종대',
    logo: sejongLogo,
    badgeColor: '#c3002f', // 세종대 크림슨
  },
  5: {
    id: 5,
    name: '연세대학교',
    shortName: '연세대',
    logo: yonseiLogo,
    badgeColor: '#00205b', // 연세대 로열블루
  },
  126: {
    id: 126,
    name: '중앙대학교',
    shortName: '중앙대',
    logo: chungangLogo,
    badgeColor: '#004c97', // 중앙대 블루
  },
  1234: {
    id: 1234,
    name: '공릉역 7호선',
    shortName: '과기대(공릉역)',
    logo: seoultechLogo,
    badgeColor: '#8a1b24', // 과기대 버건디
  },
};

export const LANDMARK_IDS = [1, 5, 126, 1234];

/**
 * 주어진 목적지 ID 또는 이름이 4대 핵심 랜드마크인지 판별합니다.
 * @param {number|string|null} id 목적지 ID
 * @param {string|null} name 목적지 이름
 * @returns {boolean}
 */
export function isLandmarkDestination(id, name) {
  if (id != null && LANDMARK_IDS.includes(Number(id))) {
    return true;
  }
  if (name) {
    return Object.values(MAJOR_LANDMARK_METADATA).some(
      (lm) => name.includes(lm.shortName) || name.includes(lm.name),
    );
  }
  return false;
}
