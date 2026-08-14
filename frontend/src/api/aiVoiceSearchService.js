import api from '@/api/api.js';

export default {
  async searchWithTranscript(
    transcript,
    currentFilters = {},
    currentAmenities = [],
  ) {
    const normalizedTranscript = String(transcript || '').trim();
    if (!normalizedTranscript) {
      throw new Error('인식된 음성이 없습니다.');
    }

    const { data } = await api.post(
      '/ai/voice-search',
      {
        transcript: normalizedTranscript,
        currentFilters: JSON.stringify(currentFilters ?? {}),
        currentAmenities: JSON.stringify(currentAmenities ?? []),
      },
      { timeout: 60000 },
    );

    return data;
  },
};
