import axios from 'axios';

const BASE_URL = '/api/amenities';

export default {
  async getCachedAmenities(propertyId) {
    const { data } = await axios.get(`${BASE_URL}/properties/${propertyId}`);
    return data;
  },

  async filterAmenities(propertyId, amenities) {
    const { data } = await axios.post(`${BASE_URL}/filter`, {
      propertyId,
      amenities,
    });

    return data;
  },

  async filterProperties(propertyIds, amenities) {
    const requests = propertyIds.map((propertyId) => ({ propertyId, amenities }));
    const { data } = await axios.post(`${BASE_URL}/filter/properties`, requests);

    return data;
  },
};
