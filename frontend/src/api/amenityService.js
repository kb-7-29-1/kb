import axios from 'axios';

const BASE_URL = '/api/amenities';

export default {
  async filterAmenities(propertyId, amenities) {
    const { data } = await axios.post(`${BASE_URL}/filter`, {
      propertyId,
      amenities,
    });

    return data;
  },
};
