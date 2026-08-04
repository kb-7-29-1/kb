/**
 * 부동산 보증금 및 월세 단위 일관 포맷팅 유틸리티
 */

/**
 * 만원 단위 보증금/전세금을 한국어 금액 표현으로 정밀 변환
 * @param {number} depositInTenThousand 만원 단위 금액 (예: 100 = 100만원, 3500 = 3,500만원, 10000 = 1억원, 125000 = 12.5억원)
 * @returns {string} 예: "100만", "3,500만", "1억", "1.5억", "12.5억", "100억"
 */
export const formatDeposit = (depositInTenThousand) => {
  if (!depositInTenThousand || depositInTenThousand <= 0) return '0만';

  const deposit = Number(depositInTenThousand);

  if (deposit < 10000) {
    // 1억 미만 (예: 100만, 500만, 3,500만)
    return `${deposit.toLocaleString()}만`;
  }

  // 1억 이상 (10,000만원 이상)
  const uk = Math.floor(deposit / 10000);
  const remainder = deposit % 10000;

  if (remainder === 0) {
    return `${uk.toLocaleString()}억`;
  } else if (remainder % 1000 === 0) {
    // 1,000만원 단위로 소수점 깔끔 표기 (예: 1.5억, 12.5억)
    const decimal = remainder / 10000;
    return `${(uk + decimal).toFixed(1)}억`;
  } else {
    // 예: 1억 3,500만
    return `${uk.toLocaleString()}억 ${remainder.toLocaleString()}만`;
  }
};

/**
 * 지도 마커 핀 및 매물 목록 카드용 요약 가격 포맷터
 * @param {number} deposit 만원 단위 보증금
 * @param {number} monthlyRent 만원 단위 월세
 * @returns {string} 예: "100만/65", "3,500만/50", "1.5억/70", "전세 3,500만", "전세 1.5억"
 */
export const formatPropertyPrice = (deposit, monthlyRent) => {
  const formattedDeposit = formatDeposit(deposit);

  if (!monthlyRent || monthlyRent === 0) {
    return `전세 ${formattedDeposit}`;
  }
  return `${formattedDeposit}/${monthlyRent}`;
};

/**
 * 우측 슬라이딩 도어 상세 패널용 정밀 서술형 가격 포맷터
 * @param {number} deposit 만원 단위 보증금
 * @param {number} monthlyRent 만원 단위 월세
 * @returns {string} 예: "보증금 100만원 / 월세 65만원", "보증금 3,500만원 / 월세 50만원", "전세 1억 5,000만원"
 */
export const formatPropertyPriceDetail = (deposit, monthlyRent) => {
  const dep = Number(deposit || 0);
  let depText = '';

  if (dep < 10000) {
    depText = `${dep.toLocaleString()}만원`;
  } else {
    const uk = Math.floor(dep / 10000);
    const remainder = dep % 10000;
    if (remainder === 0) {
      depText = `${uk.toLocaleString()}억원`;
    } else {
      depText = `${uk.toLocaleString()}억 ${remainder.toLocaleString()}만원`;
    }
  }

  if (!monthlyRent || monthlyRent === 0) {
    return `전세 ${depText}`;
  }
  return `보증금 ${depText} / 월세 ${monthlyRent}만원`;
};
