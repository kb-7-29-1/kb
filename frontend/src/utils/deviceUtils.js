/**
 * 터치 및 마우스 포인터 관련 디바이스 감지 유틸리티
 */

/**
 * 화면 너비 500px 이하(모바일 화면 뷰포트) 여부를 확인합니다.
 * @returns {boolean}
 */
export const isMobileScreen = () => {
  return typeof window !== 'undefined' && window.innerWidth <= 500;
};

/**
 * 이벤트가 모바일 터치 환경인지 확인 (기기 감지 제외, 화면 너비 <= 500px AND 포인터 타입)
 * @param {Object} e - 네이버 지도 이벤트 또는 DOM 이벤트
 * @returns {boolean}
 */
export const isTouchEvent = (e) => {
  // 1. 화면 너비가 500px 초과인 경우 모바일 롱프레스 대상 아님
  if (!isMobileScreen()) return false;

  if (!e) return true;
  const domEvent = e.pointerEvent || e.domEvent || e.rawEvent;

  // 2. 마우스 클릭 포인터인 경우 제외
  if (domEvent && domEvent.pointerType === 'mouse') return false;

  return true;
};

/**
 * 이벤트가 PC 마우스 포인터 환경인지 확인 (기기 감지 제외, 화면 너비 > 500px AND 포인터 타입)
 * @param {Object} e - 네이버 지도 이벤트 또는 DOM 이벤트
 * @returns {boolean}
 */
export const isMousePointer = (e) => {
  // 1. 화면 너비가 500px 이하인 경우 PC 우클릭 대상 아님
  if (isMobileScreen()) return false;

  if (!e) return true;
  const domEvent = e.pointerEvent || e.domEvent || e.rawEvent;

  // 2. 터치 포인터인 경우 제외
  if (domEvent && domEvent.pointerType === 'touch') return false;

  return true;
};
