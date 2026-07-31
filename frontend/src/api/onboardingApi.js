import axios from 'axios';

const BASE_URL = '/api/destinations';

export default {
  async searchDestinations(keyword) {
    const { data } = await axios.get(`${BASE_URL}/search`, {
      params: { keyword },
    });

    return data;
  },
};
