import api from './api.js';

export default {
  // 투표 현황 조회
  async getVoteSummary(propertyId, destinationId) {
    const { data } = await api.get(`/route-votes/properties/${propertyId}`, {
      params: { destinationId },
    });
    return data;
  },

  // 투표 저장 및 변경
  async saveVote(propertyId, destinationId, voteType) {
    const { data } = await api.post(`/route-votes/properties/${propertyId}`, {
      destinationId,
      voteType,
    });
    return data;
  },
};
