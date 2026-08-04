import api from './api.js';

export default {
  async getComments(propertyId) {
    const { data } = await api.get(`/properties/${propertyId}/comments`);
    return data;
  },

  async createComment(propertyId, content) {
    await api.post(`/properties/${propertyId}/comments`, { content });
  },

  async updateComment(propertyId, commentId, content) {
    await api.patch(`/properties/${propertyId}/comments/${commentId}`, { content });
  },

  async deleteComment(propertyId, commentId) {
    await api.delete(`/properties/${propertyId}/comments/${commentId}`);
  },
};
