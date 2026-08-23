import { calculateDistanceKm } from '@/utils/geo.js';

/**
 * 로그인 유저아이디 결합 최근 목적지 저장 및 로드 유틸리티
 */

export const getRecentDestinationsStorageKey = (userId) => {
  const keyUser = userId || 'guest';
  return `salgosipo_recent_destinations_${keyUser}`;
};

export const getRecentDestinations = (userId) => {
  try {
    const key = getRecentDestinationsStorageKey(userId);
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : [];
  } catch (e) {
    return [];
  }
};

export const saveRecentDestinationGlobal = (destObj, userId) => {
  if (!destObj || !destObj.destName) return;
  const key = getRecentDestinationsStorageKey(userId);
  let list = [];
  try {
    const data = localStorage.getItem(key);
    list = data ? JSON.parse(data) : [];
  } catch (e) {
    list = [];
  }

  const filtered = list.filter((item) => item.destName !== destObj.destName);
  const updated = [
    {
      destName: destObj.destName,
      destAddress: destObj.destAddress || '',
      destLatitude: destObj.destLatitude || destObj.lat || null,
      destLongitude: destObj.destLongitude || destObj.lng || null,
      destinationId: destObj.destinationId ?? null,
    },
    ...filtered,
  ].slice(0, 10);

  try {
    localStorage.setItem(key, JSON.stringify(updated));
  } catch (e) {}
  return updated;
};

export const removeRecentDestinationGlobal = (destName, userId) => {
  const key = getRecentDestinationsStorageKey(userId);
  let list = [];
  try {
    const data = localStorage.getItem(key);
    list = data ? JSON.parse(data) : [];
  } catch (e) {
    list = [];
  }

  const updated = list.filter((item) => item.destName !== destName);
  try {
    localStorage.setItem(key, JSON.stringify(updated));
  } catch (e) {}
  return updated;
};

/**
 * 목적지 명칭, 주소 및 좌표 기반 4단계 유연(Fuzzy) 매칭 공통 유틸리티 (실제 DB/저장 목적지 전용)
 */
export const findMatchingDestination = (
  name,
  address,
  recentList = [],
  lat = null,
  lng = null,
) => {
  if (!recentList || recentList.length === 0) return null;

  const isBroadDistrictName = (str) => {
    if (!str) return false;
    const trimmed = str.trim();
    return /^[가-힣]+(구|시|군|도)$/.test(trimmed) || trimmed === '서울특별시';
  };

  /*
  // 0차: 좌표(위경도) 근접 거리 최우선 매칭 (250m 이내에 DB/저장 목적지가 있으면 지오코딩 텍스트 무시하고 최우선 반영 - 단, 광역 구/시 명칭은 제외)
  if (lat != null && lng != null) {
    const inputLat = Number(lat);
    const inputLng = Number(lng);
    if (!isNaN(inputLat) && !isNaN(inputLng)) {
      let minDistance = Infinity;
      let closestItem = null;

      for (const item of recentList) {
        if (isBroadDistrictName(item.destName)) continue;
        const itemLat = Number(item.destLatitude ?? item.lat);
        const itemLng = Number(item.destLongitude ?? item.lng);
        if (!isNaN(itemLat) && !isNaN(itemLng)) {
          const distKm = calculateDistanceKm(
            inputLat,
            inputLng,
            itemLat,
            itemLng,
          );
          if (distKm <= 0.25 && distKm < minDistance) {
            minDistance = distKm;
            closestItem = item;
          }
        }
      }
      if (closestItem) return closestItem;
    }
  }
  */

  const cleanInputName = (name || '').trim().toLowerCase();
  const cleanInputAddress = (address || '').trim().toLowerCase();

  const extractNumbers = (str) =>
    (str.match(/\d+(?:-\d+)?/g) || []).sort().join(',');
  const inputNums = extractNumbers(cleanInputAddress);

  // 1차: 주소 완전 일치
  let matched = recentList.find(
    (item) =>
      item.destAddress &&
      cleanInputAddress &&
      item.destAddress.trim().toLowerCase() === cleanInputAddress,
  );

  // 2차: 목적지 명칭(destName) 부분/포함 일치 (광역 구/시 명칭은 특정 지번/상세주소와 부분일치되지 않도록 제외)
  if (!matched && cleanInputName) {
    matched = recentList.find((item) => {
      if (!item.destName) return false;
      const targetName = item.destName.trim().toLowerCase();
      if (isBroadDistrictName(targetName) && cleanInputName !== targetName) {
        return false;
      }
      return (
        targetName.includes(cleanInputName) ||
        cleanInputName.includes(targetName)
      );
    });
  }

  // 3차: 주소(destAddress) 부분/포함 일치 (단, 번지수 숫자는 완전 일치)
  if (!matched && cleanInputAddress) {
    matched = recentList.find((item) => {
      if (!item.destAddress) return false;
      const targetAddr = item.destAddress.trim().toLowerCase();
      if (isBroadDistrictName(item.destName) && cleanInputAddress !== targetAddr) {
        return false;
      }
      const targetNums = extractNumbers(targetAddr);
      if (inputNums !== targetNums) return false;
      return (
        targetAddr.includes(cleanInputAddress) ||
        cleanInputAddress.includes(targetAddr)
      );
    });
  }

  return matched || null;
};
