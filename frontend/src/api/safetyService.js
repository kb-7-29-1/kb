import api from '@/api/api.js';

const safetyService = {
  /**
   * 매물 클릭 시 사용하는 경로 조회 API.
   * 백엔드에서 propertyId + destinationId 캐시를 먼저 확인하고,
   * 없을 때만 TMAP을 호출한 뒤 안전점수와 경로를 함께 저장합니다.
   */
  async getSafetyRoute(payload) {
    const { data } = await api.post('/safety/routes/recommend', payload);
    return data;
  },

  async getSafetyDetails(payload) {
    const { data } = await api.post('/safety/score/details', payload);
    return data;
  },

  async getScoresForProperties({
    propertyIds,
    destinationId,
    destinationName,
    destinationAddress,
    destinationLatitude,
    destinationLongitude,
  }) {
    const { data } = await api.post('/safety/scores/batch', {
      propertyIds,
      destinationId: destinationId || null,
      destinationName,
      destinationAddress,
      destinationLatitude,
      destinationLongitude,
    });

    const map = {};
    if (Array.isArray(data?.items)) {
      data.items.forEach((item) => {
        if (item.propertyId != null && item.safetyScore != null) {
          map[item.propertyId] = item.safetyScore;
        }
      });
    }
    return map;
  },

  /**
   * "경로 자세히 보기" 토글 ON 시에만 호출하는 지연 로딩 API.
   * 이미 계산된 경로의 bounding box 안에 있는 CCTV/가로등/파출소 원본 좌표를 반환합니다.
   */
  async getRouteFacilities({ propertyId, destinationId }) {
    const { data } = await api.get('/safety/route/facilities', {
      params: { propertyId, destinationId },
    });
    return data;
  },
};

export default safetyService;
