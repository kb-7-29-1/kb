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
  let loadSequence = 0;

  const load = async (propertyId) => {
    if (!propertyId) return;

    const requestSequence = ++loadSequence;
    isLoading.value = true;
    loadError.value = '';
    comments.value = [];
    tags.value = [];
    commentCount.value = 0;
    try {
      const commentsPromise = commentService.getComments(propertyId);
      const tagsPromise = commentService.getTags(propertyId);

      // 댓글은 태그 응답을 기다리지 않고 먼저 화면에 표시한다.
      commentsPromise.then((result) => {
        if (requestSequence !== loadSequence) return;
        comments.value = Array.isArray(result) ? result : [];
        commentCount.value = comments.value.length;
        isLoading.value = false;
      }).catch(() => {});

      const [commentsResult, tagsResult] = await Promise.allSettled([
        commentsPromise,
        tagsPromise,
      ]);

      if (commentsResult.status === 'rejected') {
        throw commentsResult.reason;
      }

      if (requestSequence !== loadSequence) return;

      tags.value =
        tagsResult.status === 'fulfilled' && Array.isArray(tagsResult.value)
          ? tagsResult.value
          : [];

      if (tagsResult.status === 'rejected') {
        console.error('COMMENT TAG LOAD ERROR:', tagsResult.reason);
      }
      commentCount.value = comments.value.length;
    } catch (error) {
      if (requestSequence !== loadSequence) return;

      comments.value = [];
      tags.value = [];
      commentCount.value = 0;
      loadError.value = '댓글을 불러오지 못했습니다.';
      console.error('COMMENT LIST LOAD ERROR:', error);
    } finally {
      if (requestSequence === loadSequence) {
        isLoading.value = false;
      }
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
      if (error.response?.status === 404) {
        await load(propertyId);
        submitError.value = '이미 삭제된 댓글입니다.';
      } else {
        submitError.value = '댓글을 수정하지 못했습니다.';
      }
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
