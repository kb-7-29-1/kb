import { ref } from 'vue';
import commentService from '@/api/commentService.js';

export function usePropertyComments() {
  const comments = ref([]);
  const tags = ref([]);
  const commentCount = ref(0);
  const isLoading = ref(false);
  const loadError = ref('');
  const isSubmitting = ref(false);
  const submitError = ref('');

  const load = async (propertyId) => {
    if (!propertyId) return;

    isLoading.value = true;
    loadError.value = '';
    try {
      const [commentsResult, tagsResult] = await Promise.allSettled([
        commentService.getComments(propertyId),
        commentService.getTags(propertyId),
      ]);

      if (commentsResult.status === 'rejected') {
        throw commentsResult.reason;
      }

      comments.value = Array.isArray(commentsResult.value) ? commentsResult.value : [];
      tags.value = tagsResult.status === 'fulfilled' && Array.isArray(tagsResult.value)
        ? tagsResult.value
        : [];

      if (tagsResult.status === 'rejected') {
        console.error('COMMENT TAG LOAD ERROR:', tagsResult.reason);
      }
      commentCount.value = comments.value.length;
    } catch (error) {
      comments.value = [];
      tags.value = [];
      commentCount.value = 0;
      loadError.value = '댓글을 불러오지 못했습니다.';
      console.error('COMMENT LIST LOAD ERROR:', error);
    } finally {
      isLoading.value = false;
    }
  };

  const create = async (propertyId, content) => {
    isSubmitting.value = true;
    submitError.value = '';
    try {
      await commentService.createComment(propertyId, content);
      await load(propertyId);
      return true;
    } catch (error) {
      submitError.value = '댓글을 등록하지 못했습니다.';
      console.error('COMMENT CREATE ERROR:', error);
      return false;
    } finally {
      isSubmitting.value = false;
    }
  };

  const update = async (propertyId, commentId, content) => {
    isSubmitting.value = true;
    submitError.value = '';
    try {
      await commentService.updateComment(propertyId, commentId, content);
      await load(propertyId);
      return true;
    } catch (error) {
      submitError.value = '댓글을 수정하지 못했습니다.';
      console.error('COMMENT UPDATE ERROR:', error);
      return false;
    } finally {
      isSubmitting.value = false;
    }
  };

  const remove = async (propertyId, commentId) => {
    isSubmitting.value = true;
    submitError.value = '';
    try {
      await commentService.deleteComment(propertyId, commentId);
      await load(propertyId);
      return true;
    } catch (error) {
      submitError.value = '댓글을 삭제하지 못했습니다.';
      console.error('COMMENT DELETE ERROR:', error);
      return false;
    } finally {
      isSubmitting.value = false;
    }
  };

  return {
    comments,
    tags,
    commentCount,
    isLoading,
    loadError,
    isSubmitting,
    submitError,
    load,
    create,
    update,
    remove,
  };
}
