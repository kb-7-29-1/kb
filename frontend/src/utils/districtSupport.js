/**
 * 보안등 공공데이터가 충분히 구축된 서울시 14개 자치구 목록
 */
export const SUPPORTED_SAFETY_DISTRICTS = [
  '강동구',
  '광진구',
  '구로구',
  '금천구',
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
];

/**
 * 보안등 공공데이터 미구축/데이터 미제공 11개 자치구 목록 (14 + 11 = 서울 25개 자치구 100% 매핑)
 */
export const UNSUPPORTED_SAFETY_DISTRICTS = [
  '강남구',
  '강북구',
  '강서구',
  '관악구',
  '마포구',
  '성동구',
  '성북구',
  '영등포구',
  '용산구',
  '중구',
  '중랑구',
];

/**
 * 주어진 주소나 이름 텍스트가 미구축 자치구인지 검증합니다.
 * @param {string} text 검증할 주소 또는 이름
 * @returns {boolean} 지원 여부 (true: 지원 자치구 또는 랜드마크, false: 미구축 자치구)
 */
export function isSupportedSafetyDistrict(text) {
  if (!text || !text.trim()) return true;

  // 1. 미구축 11개 자치구 텍스트 명시 시 미구축(false) 즉시 리턴 (성동구 예외 없음)
  for (const unsupported of UNSUPPORTED_SAFETY_DISTRICTS) {
    if (text.includes(unsupported)) {
      return false;
    }
  }

  // 2. 미지원 자치구가 아니면 지원 통과
  return true;
}
