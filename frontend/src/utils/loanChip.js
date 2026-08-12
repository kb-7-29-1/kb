import { LOAN_PRODUCTS } from '@/utils/budget.js';

/**
 * LOAN_PRODUCTS 데이터셋에서 매물 조건(전세/월세) 및 선택된 대출 조건에 따라
 * 동적으로 최적 금융 대출 정보를 추출합니다.
 * @param {Object} prop 매물 객체
 * @param {string} [selectedLoanId] 유저가 선택한 대출 상품 ID (옵션)
 * @returns {Object} { icon, shortName, rateInfo, displayText }
 */
export function getLoanChipInfo(prop, selectedLoanId = null) {
  const rent = Number(prop?.monthlyRent || 0);

  // 1. 유저가 선택한 특정 대출 상품이 있는 경우 LOAN_PRODUCTS 동적 매칭
  if (selectedLoanId && selectedLoanId !== 'NONE') {
    const matched = LOAN_PRODUCTS.find((item) => item.id === selectedLoanId);
    if (matched) {
      const cleanIcon = matched.icon === '⚡' ? '🏦' : matched.icon;
      const rateText = matched.rateInfo.includes('~')
        ? `${matched.rateInfo.split('~')[0]}~`
        : matched.rateInfo;
      return {
        icon: cleanIcon,
        shortName: matched.shortName,
        rateInfo: matched.rateInfo,
        displayText: `${matched.shortName} ${rateText.replace('연 ', '')}`,
      };
    }
  }

  // 2. 미선택 시 매물 조건(월세/전세)에 맞는 LOAN_PRODUCTS 동적 추출
  if (rent === 0) {
    const youthLoan = LOAN_PRODUCTS.find((item) => item.id === 'YOUTH_BUTIMMOK');
    return {
      icon: youthLoan?.icon === '⚡' ? '🏦' : (youthLoan?.icon || '🏦'),
      shortName: youthLoan?.shortName || '청년버팀목',
      rateInfo: youthLoan?.rateInfo || '연 1.5%~',
      displayText: '버팀목 1.5%~',
    };
  }

  const wolseLoan = LOAN_PRODUCTS.find((item) => item.id === 'HOUSING_WOLSE');
  return {
    icon: wolseLoan?.icon === '⚡' ? '🏦' : (wolseLoan?.icon || '🏦'),
    shortName: wolseLoan?.shortName || '주거안정월세',
    rateInfo: wolseLoan?.rateInfo || '연 1.0%~',
    displayText: '월세대출 1.0%~',
  };
}

/**
 * 네이버 지도 핀 옆에 부착할 초심플 뱅킹 BM 칩 HTML 스트링 반환 (은행 이모지 미세 서스펜션 호버 적용)
 * @param {Object} prop 매물 객체
 * @param {string} [selectedLoanId] 선택된 대출 상품 ID
 * @returns {string} HTML 스트링
 */
export function renderLoanChipHTML(prop, selectedLoanId = null) {
  const info = getLoanChipInfo(prop, selectedLoanId);

  return `
    <div class="flex items-center gap-1 rounded-full border border-blue-200/90 bg-blue-50/95 px-1.5 py-0.5 text-[9px] font-bold text-blue-700 shadow-sm transition-all hover:bg-blue-600 hover:text-white hover:border-blue-600 group/chip cursor-pointer">
      <span class="rounded bg-blue-200/80 px-1 py-0.2 text-[7px] font-black text-blue-900 group-hover/chip:bg-white/30 group-hover/chip:text-white shrink-0">AD</span>
      <span class="inline-block transition-transform duration-300 group-hover/chip:-translate-y-0.5 text-[10px] shrink-0">${info.icon}</span>
      <span class="whitespace-nowrap tracking-tighter font-extrabold text-[9px]">${info.displayText}</span>
      <span class="whitespace-nowrap tracking-tighter text-[7px] text-blue-500/80 group-hover/chip:text-blue-100 shrink-0 font-medium">※참고용</span>
    </div>
  `;
}

// ⚡ 대출 칩 글로벌 ON/OFF 스위치 (false로 변경 시 지도상의 모든 대출 칩 1초컷 비활성화)
export const ENABLE_LOAN_CHIP = true;

/**
 * 현재 지도 화면 매물 중 상위 2~3개 추천 매물 ID Set만 선별합니다.
 * @param {Array} properties 매물 목록
 * @param {number} [limit=3] 선별할 매물 개수 (기본 3개)
 * @returns {Set<number|string>} 추천 대상 매물 ID Set
 */
export function getFeaturedLoanPropertyIds(properties, limit = 3) {
  if (!ENABLE_LOAN_CHIP || !Array.isArray(properties) || properties.length === 0) return new Set();

  const sorted = [...properties]
    .map((p) => ({
      id: p.propertyId,
      priceScore: Number(p.deposit || 0) + Number(p.monthlyRent || 0) * 100,
    }))
    .sort((a, b) => b.priceScore - a.priceScore);

  const topIds = sorted.slice(0, limit).map((item) => item.id);
  return new Set(topIds);
}
