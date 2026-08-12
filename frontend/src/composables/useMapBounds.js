import { ref } from 'vue';
import { calculateDistanceKm } from '@/utils/geo.js';

/**
 * 카메라 뷰포트 사각형(Bounds)과 목적지 원형 검색(326개 고정) 간의
 * 공간 쿼리 충돌을 차단하고, 화면 재검색 상태를 모듈화하여 관리합니다.
 */
export function useMapBounds() {
  const isMapMoved = ref(false);
  const pendingBounds = ref(null);
  const lastFetchedCenter = ref({ lat: null, lng: null });

  /**
   * 지도가 이동하거나 줌 변경 시 사각형 뷰포트를 감지합니다.
   * 중심점에서 800m 이상 이동 시에만 재검색 모드로 전환합니다.
   */
  const handleBoundsChange = (bounds) => {
    if (!bounds || bounds.centerLat == null || bounds.centerLng == null) return;

    if (
      lastFetchedCenter.value.lat != null &&
      lastFetchedCenter.value.lng != null
    ) {
      const distKm = calculateDistanceKm(
        lastFetchedCenter.value.lat,
        lastFetchedCenter.value.lng,
        bounds.centerLat,
        bounds.centerLng,
      );
      if (distKm > 0.8) {
        isMapMoved.value = true;
        pendingBounds.value = bounds;
      }
    }
  };

  /**
   * 유저가 '이 위치에서 매물 재검색'을 명시적으로 클릭했을 때만 사각형 뷰포트 조건을 적용합니다.
   */
  const applyPendingBoundsToFilters = (appliedFilterState) => {
    if (!pendingBounds.value) return false;

    appliedFilterState.swLat = pendingBounds.value.swLat;
    appliedFilterState.swLng = pendingBounds.value.swLng;
    appliedFilterState.neLat = pendingBounds.value.neLat;
    appliedFilterState.neLng = pendingBounds.value.neLng;

    isMapMoved.value = false;
    return true;
  };

  /**
   * 목적지 원형 검색 시 사각형 뷰포트 제한을 제거하여 326개 고정 개수를 보장합니다.
   */
  const clearBoundsFromFilters = (appliedFilterState) => {
    delete appliedFilterState.swLat;
    delete appliedFilterState.swLng;
    delete appliedFilterState.neLat;
    delete appliedFilterState.neLng;
  };

  /**
   * 조회가 완료되었을 때 마지막 조회 중심 좌표를 갱신합니다.
   */
  const updateLastFetchedCenter = (lat, lng) => {
    lastFetchedCenter.value = { lat, lng };
  };

  return {
    isMapMoved,
    pendingBounds,
    lastFetchedCenter,
    handleBoundsChange,
    applyPendingBoundsToFilters,
    clearBoundsFromFilters,
    updateLastFetchedCenter,
  };
}
