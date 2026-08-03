import axios from 'axios';

const BASE_URL = '/api/onboarding';

export default {
  async searchDestinations(keyword) {
    const { data } = await axios.get('/api/destinations/search', {
      params: { keyword },
    });

    return data;
  },

  async saveOnboarding(onboarding) {
    const { data } = await axios.post(BASE_URL, onboarding);
    return data;
  },

  async getOnboarding() {
    const { data } = await axios.get(BASE_URL);
    return data;
  },
};
