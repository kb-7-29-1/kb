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
