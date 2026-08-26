import { getHaversineDistance } from '@/utils/geo';

/**
 * 이동 수단(도보/대중교통) 및 시간 조건에 따른 최대 검색 반경(km)을 정밀 계산합니다.
 */
export const getSearchRadiusKm = (filters) => {
  const minutes = Number(filters?.travelTime) || 15;

  if (filters?.transportMode === 'WALK') {
    let speedMetersPerMin = 75;
    if (filters.walkPace === 'SLOW') speedMetersPerMin = 58;
    if (filters.walkPace === 'FAST') speedMetersPerMin = 92;
    const maxMeters = Math.max(200, minutes * speedMetersPerMin);
    return maxMeters / 1000;
  }

  const transitMaxRadius = Math.max(500, minutes * 180);
  return transitMaxRadius / 1000;
};

/**
 * 이동 수단 및 최소 시간 조건에 따른 최소 검색 반경(km)을 정밀 계산합니다.
 * 도보(WALK) 모드는 요구사항에 따라 최소 거리 내접원 제한을 무조건 해제(0m부터 전체 노출)합니다.
 */
export const getMinSearchRadiusKm = (filters) => {
  if (filters?.transportMode === 'WALK') {
    return undefined; // 도보 모드 최소 거리 제한 무조건 해제 (0m부터 전체 노출)
  }

  const minTravelTime =
    Number(filters?.minTravelTime) || (filters?.transportMode === 'TRANSIT' ? 5 : 0);
  if (minTravelTime <= 0) return undefined;
  return (minTravelTime * 180) / 1000;
};

/**
 * 프론트엔드 인메모리 매물이 이소크론 도넛 링 도달 거리 내에 존재하는지 검증합니다.
 */
export const isWithinReachDistance = (property, filters, destLat, destLng) => {
  if (!property || property.latitude == null || property.longitude == null) return false;

  const distKm = getHaversineDistance(destLat, destLng, property.latitude, property.longitude);
  const distMeters = distKm * 1000;

  if (filters?.transportMode === 'WALK') {
    let speedMetersPerMin = 75;
    if (filters.walkPace === 'SLOW') speedMetersPerMin = 58;
    if (filters.walkPace === 'FAST') speedMetersPerMin = 92;
    // 도보 모드는 최소 도달 거리 제한 무조건 0m부터 전체 노출
    const minReachMeters = 0;
    const maxReachMeters = Math.max(200, (filters.travelTime || 15) * speedMetersPerMin);
    if (distMeters < minReachMeters || distMeters > maxReachMeters) return false;
  } else {
    const travelTime = filters?.travelTime || 15;
    let minTime = 5;
    if (filters?.minTravelTime != null && Number(filters.minTravelTime) > 0) {
      minTime = Number(filters.minTravelTime);
    } else if (filters?.flexTime != null && Number(filters.flexTime) > 0) {
      minTime = Number(filters.flexTime);
    }

    const transitMaxRadius = Math.max(500, travelTime * 180);
    const transitBaseRadius = minTime > 0 ? Math.max(200, minTime * 180) : 0;

    if (distMeters < transitBaseRadius || distMeters > transitMaxRadius) return false;
  }

  return true;
};
