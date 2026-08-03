import api from '@/api/api';

export default {
  async searchDestinations(keyword) {
    const { data } = await api.get('/destinations/search', {
      params: { keyword },
    });

    return data;
  },

  async saveOnboarding(onboarding) {
    const { data } = await api.post('/onboarding', onboarding);
    return data;
  },

  async getOnboarding() {
    const { data } = await api.get('/onboarding');
    return data;
  },
};
