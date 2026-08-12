import { useAuthStore } from '@/stores/useAuthStore.js';

/**
 * URL 주소창 Query 파라미터 <-> 퀵 필터 로컬 캐시 <-> DB 온보딩 간의
 * 3단계 우선순위 계층 구조(URL > 로컬 캐시 > DB 온보딩)를 정밀 전담 관리하는 Composable입니다.
 */
export function useMapUrlSync() {
  const authStore = useAuthStore();

  const getQuickFilterCacheKey = () => {
    const userId = authStore.user?.userId || authStore.user?.id || 'guest';
    return `kb_quick_filter_state_${userId}`;
  };

  const saveQuickFilterToCache = (filters) => {
    if (!filters) return;
    try {
      const key = getQuickFilterCacheKey();
      localStorage.setItem(key, JSON.stringify(filters));
    } catch (e) {
      console.error('Failed to save quick filter cache:', e);
    }
  };

  const loadQuickFilterFromCache = () => {
    try {
      const key = getQuickFilterCacheKey();
      const cached = localStorage.getItem(key);
      if (cached) {
        return JSON.parse(cached);
      }
    } catch (e) {
      console.error('Failed to load quick filter cache:', e);
    }
    return null;
  };

  const syncFiltersToUrlQuery = (filters, route, router, selectedPropertyId = null) => {
    if (!filters || !route || !router) return;
    saveQuickFilterToCache(filters);
    const query = {
      ...route.query,
      dest: filters.destination || filters.destinationName || undefined,
      destName: filters.destination || filters.destinationName || undefined,
      destAddress: filters.destinationAddress || undefined,
      destLat: filters.destinationLat
        ? Number(filters.destinationLat).toFixed(4)
        : undefined,
      destLng: filters.destinationLng
        ? Number(filters.destinationLng).toFixed(4)
        : undefined,
      tradeType: filters.tradeType || undefined,
      minDeposit: filters.minDeposit != null ? filters.minDeposit : undefined,
      maxDeposit: filters.maxDeposit != null ? filters.maxDeposit : undefined,
      minRent: filters.minRent != null ? filters.minRent : undefined,
      maxRent: filters.maxRent != null ? filters.maxRent : undefined,
      mode: filters.transportMode || undefined,
      travelTime: filters.travelTime != null ? filters.travelTime : undefined,
      minTravelTime:
        filters.minTravelTime != null ? filters.minTravelTime : undefined,
      minSafety:
        filters.minSafetyScore != null ? filters.minSafetyScore : undefined,
      propertyId: selectedPropertyId || undefined,
    };
    router.replace({ query }).catch(() => {});
  };

  const parseUrlQueryToFilters = (route, filterStateRef, { includeDestination = true } = {}) => {
    if (!route || !filterStateRef) return false;
    const q = route.query;
    if (
      !q ||
      (!q.destLat && !q.tradeType && !q.maxDeposit && !q.dest && !q.destName)
    )
      return false;

    const databaseDestination = includeDestination
      ? null
      : {
          destination: filterStateRef.value.destination,
          destinationName: filterStateRef.value.destinationName,
          destinationAddress: filterStateRef.value.destinationAddress,
          destinationLat: filterStateRef.value.destinationLat,
          destinationLng: filterStateRef.value.destinationLng,
          destinationId: filterStateRef.value.destinationId,
        };

    const destNameVal = q.destName || q.dest;
    if (destNameVal) {
      filterStateRef.value.destination = String(destNameVal);
      filterStateRef.value.destinationName = String(destNameVal);
    }
    if (q.destAddress) {
      filterStateRef.value.destinationAddress = String(q.destAddress);
    }
    if (q.destLat) filterStateRef.value.destinationLat = Number(q.destLat);
    if (q.destLng) filterStateRef.value.destinationLng = Number(q.destLng);

    if (q.destLat && q.destLng && !destNameVal) {
      filterStateRef.value.destination = '지정한 목적지';
      filterStateRef.value.destinationName = '지정한 목적지';
    }

    if (q.tradeType) filterStateRef.value.tradeType = String(q.tradeType);
    if (q.minDeposit != null) filterStateRef.value.minDeposit = Number(q.minDeposit);
    if (q.maxDeposit != null) filterStateRef.value.maxDeposit = Number(q.maxDeposit);
    if (q.minRent != null) filterStateRef.value.minRent = Number(q.minRent);
    if (q.maxRent != null) filterStateRef.value.maxRent = Number(q.maxRent);
    if (q.mode) filterStateRef.value.transportMode = String(q.mode);
    if (q.travelTime != null) filterStateRef.value.travelTime = Number(q.travelTime);
    if (q.minTravelTime != null)
      filterStateRef.value.minTravelTime = Number(q.minTravelTime);
    if (q.minSafety != null)
      filterStateRef.value.minSafetyScore = Number(q.minSafety);

    if (databaseDestination) {
      Object.assign(filterStateRef.value, databaseDestination);
    }

    return true;
  };

  return {
    saveQuickFilterToCache,
    loadQuickFilterFromCache,
    syncFiltersToUrlQuery,
    parseUrlQueryToFilters,
  };
}
