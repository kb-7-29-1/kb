/**
 * 보안등 공공데이터가 구축된 서울시 14개 자치구 목록
 */
export const SUPPORTED_SAFETY_DISTRICTS = [
  '강서구',
  '관악구',
  '광진구',
  '구로구',
  '도봉구',
  '동대문구',
  '동작구',
  '서대문구',
  '서초구',
  '양천구',
  '은평구',
  '종로구',
  '중구',
  '중랑구',
];

/**
 * 주어진 주소나 이름 텍스트에 14개 보안등 데이터 구축 자치구가 포함되어 있는지 검증합니다.
 * @param {string} text 검증할 주소 또는 이름
 * @returns {boolean} 지원 여부 (true: 지원 자치구, false: 미구축 자치구)
 */
export function isSupportedSafetyDistrict(text) {
  if (!text || !text.trim()) return true;
  for (const district of SUPPORTED_SAFETY_DISTRICTS) {
    if (text.includes(district)) {
      return true;
    }
  }
  return false;
}
