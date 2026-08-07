import api from '@/api/api.js';

const safetyService = {
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
