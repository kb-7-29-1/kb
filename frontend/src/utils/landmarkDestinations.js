import sejongLogo from '@/assets/images/univlogo/세종대.png';
import yonseiLogo from '@/assets/images/univlogo/연세대.png';
import chungangLogo from '@/assets/images/univlogo/중앙대.png';
import seoultechLogo from '@/assets/images/univlogo/과기대.png';

/**
 * 4대 핵심 랜드마크 (티맵 안전데이터 사전 구축 랜드마크)
 */
export const MAJOR_LANDMARK_DESTINATIONS = [
  {
    id: 1,
    name: '세종대학교',
    destName: '세종대학교',
    address: '서울특별시 광진구 능동로 209',
    destAddress: '서울특별시 광진구 능동로 209',
    lat: 37.5502,
    lng: 127.0731,
    latitude: 37.5502,
    longitude: 127.0731,
    logo: sejongLogo,
    shortName: '세종대',
    badgeColor: '#c3002f', // 세종대 크림슨
  },
  {
    id: 5,
    name: '연세대학교',
    destName: '연세대학교',
    address: '서울특별시 서대문구 연세로 50',
    destAddress: '서울특별시 서대문구 연세로 50',
    lat: 37.5658,
    lng: 126.9386,
    latitude: 37.5658,
    longitude: 126.9386,
    logo: yonseiLogo,
    shortName: '연세대',
    badgeColor: '#00205b', // 연세대 로열블루
  },
  {
    id: 126,
    name: '중앙대학교',
    destName: '중앙대학교',
    address: '서울특별시 동작구 흑석로 84',
    destAddress: '서울특별시 동작구 흑석로 84',
    lat: 37.5051,
    lng: 126.9571,
    latitude: 37.5051,
    longitude: 126.9571,
    logo: chungangLogo,
    shortName: '중앙대',
    badgeColor: '#004c97', // 중앙대 블루
  },
  {
    id: 1234,
    name: '공릉역 7호선',
    destName: '공릉역 7호선',
    address: '서울특별시 노원구 동일로 1076',
    destAddress: '서울특별시 노원구 동일로 1076',
    lat: 37.6258,
    lng: 127.0728,
    latitude: 37.6258,
    longitude: 127.0728,
    logo: seoultechLogo,
    shortName: '과기대(공릉역)',
    badgeColor: '#8a1b24', // 과기대 버건디
  },
];
