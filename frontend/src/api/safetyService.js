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

    return data;
  },
};

export default safetyService;
