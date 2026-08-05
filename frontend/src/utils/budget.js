// 보증금 (만원 단위)
export const DEPOSIT_OPTIONS = [
  100,
  200,
  300,
  400,
  500,
  1000,
  2000,
  3000,
  4000,
  5000,
  6000,
  7000,
  8000,
  9000,
  10000,
  20000,
  30000,
  40000,
  50000,
  60000,
  70000,
  80000,
  90000,
  100000, // 10억
];

export const DEPOSIT_MAX = 100000; // 10억
export const DEPOSIT_MIN_LABEL = '100만원';
export const DEPOSIT_MAX_LABEL = '10억원';

// 월세
export const RENT_MIN = 0; // 0이면 전세
export const RENT_MAX = 200; // 200만원
export const RENT_STEP = 5; // 5만원 단위
export const RENT_MIN_LABEL = '전세';
export const RENT_MAX_LABEL = '200만원';

export const DEFAULT_DEPOSIT = 5000;
export const DEFAULT_RENT = 100;

export const formatDepositAmount = (amount) => {
  const value = Number(amount || 0);

  if (value >= 10000) {
    const eok = value / 10000;
    const formatted = Number.isInteger(eok) ? String(eok) : eok.toFixed(1).replace(/\.0$/, '');
    return `${formatted}억원`;
  }

  return `${value.toLocaleString()}만원`;
};

export const formatDepositShort = (amount) => {
  const value = Number(amount || 0);

  if (value >= 10000) {
    const eok = value / 10000;
    const formatted = Number.isInteger(eok) ? String(eok) : eok.toFixed(1).replace(/\.0$/, '');
    return `${formatted}억`;
  }

  return value.toLocaleString();
};
