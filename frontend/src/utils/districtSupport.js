/**
 * 보안등 공공데이터가 구축된 서울시 17개 자치구 목록
 */
export const SUPPORTED_SAFETY_DISTRICTS = [
  '강동구',
  '강서구',
  '관악구',
  '광진구',
  '구로구',
  '노원구',
  '도봉구',
  '동대문구',
  '동작구',
  '서대문구',
  '서초구',
  '송파구',
  '양천구',
  '은평구',
  '종로구',
  '중구',
  '중랑구',
];

/**
 * 보안등 공공데이터 미구축 확정 8개 자치구 목록 (17 + 8 = 서울 25개 자치구 100% 매핑)
 */
export const UNSUPPORTED_SAFETY_DISTRICTS = [
  '강남구',
  '강북구',
  '금천구',
  '마포구',
  '성동구',
  '성북구',
  '영등포구',
  '용산구',
];

/**
 * 주어진 주소나 이름 텍스트가 미구축 자치구인지 검증합니다.
 * @param {string} text 검증할 주소 또는 이름
 * @returns {boolean} 지원 여부 (true: 지원 자치구 또는 랜드마크, false: 미구축 자치구)
 */
export function isSupportedSafetyDistrict(text) {
  if (!text || !text.trim()) return true;

  // 1. 14개 지원 자치구 명시 포함 시 통과
  for (const district of SUPPORTED_SAFETY_DISTRICTS) {
    if (text.includes(district)) {
      return true;
    }
  }

  // 2. 미구축 11개 자치구 텍스트 명시 시 미구축(false) 리턴
  for (const unsupported of UNSUPPORTED_SAFETY_DISTRICTS) {
    if (text.includes(unsupported)) {
      return false;
    }
  }

  // 3. "세종대학교", "홍대입구"처럼 '구' 이름이 들어있지 않은 랜드마크 명칭은 오판하지 않고 통과
  return true;
}
